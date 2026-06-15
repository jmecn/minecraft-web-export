package io.github.jmecn.minecraftwebexport.emi.item;

import io.github.jmecn.minecraftwebexport.Constants;
import io.github.jmecn.minecraftwebexport.MweMod;
import io.github.jmecn.minecraftwebexport.config.MweConfig;
import io.github.jmecn.minecraftwebexport.emi.EmiPaths;
import io.github.jmecn.minecraftwebexport.emi.Visibility;
import io.github.jmecn.minecraftwebexport.emi.support.Log;
import io.github.jmecn.minecraftwebexport.emi.support.ProgressLog;
import io.github.jmecn.minecraftwebexport.io.ExportWriteQueue;
import io.github.jmecn.minecraftwebexport.io.JsonIO;
import io.github.jmecn.minecraftwebexport.model.item.ExportedTagCatalog;
import io.github.jmecn.minecraftwebexport.model.pipeline.ItemIndexExportResult;
import io.github.jmecn.minecraftwebexport.model.item.ItemIndexBuild;
import io.github.jmecn.minecraftwebexport.model.item.ItemDetail;
import io.github.jmecn.minecraftwebexport.model.item.ItemIdParts;
import io.github.jmecn.minecraftwebexport.model.item.ItemIndex;
import io.github.jmecn.minecraftwebexport.model.item.RegistryTagSet;
import io.github.jmecn.minecraftwebexport.model.pipeline.ExportContext;
import io.github.jmecn.minecraftwebexport.model.recipe.RecipeMeta;
import io.github.jmecn.minecraftwebexport.model.recipe.RecipeWidget;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.server.MinecraftServer;

public final class ItemIndexExporter {

    private static final int WRITE_THREADS =
            Math.max(2, Math.min(8, Runtime.getRuntime().availableProcessors()));
    private static final int WRITE_PARALLEL_MIN_ITEMS = 512;

    private ItemIndexExporter() {
    }

    public static ItemIndexExportResult export(
            Path outputDir,
            MinecraftServer server,
            ExportContext context,
            ExportedTagCatalog exportedTags,
            ExportWriteQueue writes)
            throws IOException {
        Objects.requireNonNull(writes, "writes");
        if (context.inputs().isEmpty() && context.outputs().isEmpty() && context.itemIds().isEmpty()) {
            MweMod.LOGGER.warn("{} no item refs in export context - skipping items index", Log.EMI_ITEMS);
            return ItemIndexExportResult.empty();
        }
        ExportedTagCatalog tags = exportedTags == null ? ExportedTagCatalog.empty() : exportedTags;
        ItemIndexBuild build = buildFromContext(server, context, tags);
        return writeIndex(outputDir, server, build, writes);
    }

    public static Set<String> collectRegistryTagIds(MinecraftServer server, Set<String> itemIds) {
        return ItemIndexSupport.collectRegistryTagIds(server, itemIds);
    }

    public static void accumulateRecipeRefsFromMeta(
            String recipeId,
            RecipeMeta meta,
            Map<String, Map<String, Set<String>>> inputs,
            Map<String, Map<String, Set<String>>> outputs,
            Set<String> fluidRegistryIds) {
        accumulateRecipeRefsFromMeta(recipeId, meta, inputs, outputs, fluidRegistryIds, null, null);
    }

    public static void accumulateRecipeRefsFromMeta(
            String recipeId,
            RecipeMeta meta,
            Map<String, Map<String, Set<String>>> inputs,
            Map<String, Map<String, Set<String>>> outputs,
            Set<String> fluidRegistryIds,
            MinecraftServer server,
            Map<String, Set<String>> exportedTagMembers) {
        if (ItemIndexSupport.isEmiTagDisplayRecipe(recipeId) || meta == null) {
            return;
        }
        String categoryId = ItemIndexSupport.categoryFromMeta(meta, recipeId);
        for (RecipeWidget widget : meta.widgets()) {
            Map<String, Map<String, Set<String>>> bucket =
                    ItemIndexSupport.bucketForRole(widget.role(), inputs, outputs);
            if (bucket == null) {
                continue;
            }
            Set<String> ids = new TreeSet<>();
            ItemIndexSupport.collectInteractionIds(
                    widget.interaction(), ids, fluidRegistryIds, server, exportedTagMembers);
            ItemIndexSupport.addRecipeRefs(bucket, categoryId, ids, recipeId);
        }
    }

    private static ItemIndexBuild buildFromContext(
            MinecraftServer server, ExportContext context, ExportedTagCatalog exportedTags) {
        Set<String> allItemIds = new TreeSet<>();
        allItemIds.addAll(context.inputs().keySet());
        allItemIds.addAll(context.outputs().keySet());
        ItemIndexSupport.mergeSeedItemIds(allItemIds, context.itemIds());
        allItemIds.removeIf(id -> !ItemIndexSupport.isExportableRegistryId(id));
        MweMod.LOGGER.info("{} resolving registry tags for {} items", Log.EMI_ITEMS, allItemIds.size());
        Map<String, RegistryTagSet> registryTagsByItem =
                ItemIndexSupport.resolveRegistryTags(allItemIds, server);
        return new ItemIndexBuild(
                context.inputs(),
                context.outputs(),
                Set.copyOf(context.fluidRegistryIds()),
                Set.copyOf(allItemIds),
                registryTagsByItem,
                exportedTags);
    }

    private static ItemIndexExportResult writeIndex(
            Path outputDir, MinecraftServer server, ItemIndexBuild build, ExportWriteQueue writes) {
        Objects.requireNonNull(writes, "writes");
        List<String> exportableItemIds = new ArrayList<>();
        int skippedHiddenItems = 0;
        for (String itemId : build.allItemIds()) {
            if (!Visibility.shouldExportRegistryId(server, itemId)) {
                skippedHiddenItems++;
            } else {
                exportableItemIds.add(itemId);
            }
        }
        Set<String> exportableSet = new HashSet<>(exportableItemIds);
        if (exportableItemIds.size() >= WRITE_PARALLEL_MIN_ITEMS) {
            return writeIndexParallel(outputDir, build, exportableItemIds, skippedHiddenItems, writes);
        }
        return writeIndexSequential(outputDir, build, exportableSet, skippedHiddenItems, writes);
    }

    private static ItemIndexExportResult writeIndexSequential(
            Path outputDir,
            ItemIndexBuild build,
            Set<String> exportableItemIds,
            int skippedHiddenItems,
            ExportWriteQueue writes) {
        WriteStats stats = new WriteStats();
        int writeTotal = build.allItemIds().size();
        int writeStride = ProgressLog.stride(writeTotal, MweConfig.itemsIndexWriteLogStride(), 20, 200);
        MweMod.LOGGER.info("{} writing {} item detail files", Log.EMI_ITEMS, writeTotal);
        int writeProgress = 0;
        for (String itemId : build.allItemIds()) {
            writeProgress++;
            if (!exportableItemIds.contains(itemId)) {
                continue;
            }
            writeItemDetail(outputDir, build, itemId, writes, stats);
            if (ProgressLog.shouldLog(writeProgress, writeTotal, writeStride)) {
                int pct = ProgressLog.percent(writeProgress, writeTotal);
                MweMod.LOGGER.info(
                        "{} write {}% {}/{} item files",
                        Log.EMI_ITEMS,
                        pct,
                        writeProgress,
                        writeTotal);
            }
        }
        return finalizeIndex(outputDir, build, exportableItemIds.size(), skippedHiddenItems, stats, writes);
    }

    private static ItemIndexExportResult writeIndexParallel(
            Path outputDir,
            ItemIndexBuild build,
            List<String> exportableItemIds,
            int skippedHiddenItems,
            ExportWriteQueue writes) {
        int writeTotal = build.allItemIds().size();
        int writeStride = ProgressLog.stride(writeTotal, MweConfig.itemsIndexWriteLogStride(), 20, 200);
        MweMod.LOGGER.info(
                "{} writing {} item detail files ({} workers)",
                Log.EMI_ITEMS,
                writeTotal,
                Math.min(exportableItemIds.size(), WRITE_THREADS));
        WriteStats stats = new WriteStats();
        AtomicInteger writeProgress = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(
                Math.min(exportableItemIds.size(), WRITE_THREADS),
                runnable -> {
                    Thread thread = new Thread(runnable, "mwe-item-index");
                    thread.setDaemon(true);
                    return thread;
                });
        try {
            int chunkSize = Math.max(256, (exportableItemIds.size() + WRITE_THREADS - 1) / WRITE_THREADS);
            List<Future<?>> futures = new ArrayList<>();
            for (int start = 0; start < exportableItemIds.size(); start += chunkSize) {
                int from = start;
                int to = Math.min(exportableItemIds.size(), start + chunkSize);
                futures.add(pool.submit(() -> {
                    for (int i = from; i < to; i++) {
                        writeItemDetail(outputDir, build, exportableItemIds.get(i), writes, stats);
                        int n = writeProgress.incrementAndGet();
                        if (n % writeStride == 0) {
                            int pct = ProgressLog.percent(n, writeTotal);
                            MweMod.LOGGER.info(
                                    "{} write {}% {}/{} item files",
                                    Log.EMI_ITEMS,
                                    pct,
                                    n,
                                    writeTotal);
                        }
                    }
                }));
            }
            for (Future<?> future : futures) {
                future.get();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("item index write interrupted", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            throw new RuntimeException("item index write failed: " + cause.getMessage(), cause);
        } finally {
            pool.shutdown();
            try {
                if (!pool.awaitTermination(1, TimeUnit.HOURS)) {
                    pool.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                pool.shutdownNow();
            }
        }
        return finalizeIndex(outputDir, build, exportableItemIds.size(), skippedHiddenItems, stats, writes);
    }

    private static void writeItemDetail(
            Path outputDir,
            ItemIndexBuild build,
            String itemId,
            ExportWriteQueue writes,
            WriteStats stats) {
        ItemIdParts item = ItemIdParts.parse(itemId);
        if (item == null) {
            return;
        }
        stats.indexedItemIds.add(itemId);
        synchronized (stats.indexBuckets) {
            stats.indexBuckets.computeIfAbsent(item.namespace(), ignored -> new TreeSet<>()).add(item.path());
        }
        Map<String, Map<String, Set<String>>> inputs = build.inputs();
        Map<String, Map<String, Set<String>>> outputs = build.outputs();
        if (inputs.containsKey(itemId)) {
            stats.inputRefs.addAndGet(ItemIndexSupport.countRecipeRefs(inputs.get(itemId)));
        }
        if (outputs.containsKey(itemId)) {
            stats.outputRefs.addAndGet(ItemIndexSupport.countRecipeRefs(outputs.get(itemId)));
        }
        RegistryTagSet tagSets = build.registryTagsByItem().get(itemId);
        RegistryTagSet inBundle = tagSets != null ? build.exportedTags().intersect(tagSets) : null;
        ItemDetail detail = ItemDetail.of(
                inputs.containsKey(itemId) ? ItemIndexSupport.categoryBuckets(inputs.get(itemId)) : null,
                outputs.containsKey(itemId) ? ItemIndexSupport.categoryBuckets(outputs.get(itemId)) : null,
                tagSets != null && !tagSets.isEmpty() ? tagSets : null,
                inBundle != null && !inBundle.isEmpty() ? inBundle : null);
        writes.submitBulkJson(item.toItemFile(outputDir), detail);
    }

    private static ItemIndexExportResult finalizeIndex(
            Path outputDir,
            ItemIndexBuild build,
            int indexedItemCount,
            int skippedHiddenItems,
            WriteStats stats,
            ExportWriteQueue writes) {
        Path itemsIndexFile = EmiPaths.resolve(outputDir, Constants.ITEMS_INDEX_FILE);
        Map<String, List<String>> namespacePaths = new LinkedHashMap<>();
        for (Map.Entry<String, Set<String>> entry : stats.indexBuckets.entrySet()) {
            namespacePaths.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        Set<String> indexedItemIds = Set.copyOf(stats.indexedItemIds);
        List<String> indexedFluids = indexedFluidRegistryIds(build.fluidRegistryIds(), indexedItemIds);
        if (indexedFluids.size() < build.fluidRegistryIds().size()) {
            MweMod.LOGGER.info(
                    "{} fluid index: {} indexed, {} omitted (not in item index)",
                    Log.EMI_ITEMS,
                    indexedFluids.size(),
                    build.fluidRegistryIds().size() - indexedFluids.size());
        }
        ItemIndex index = new ItemIndex(namespacePaths, indexedFluids);
        writes.submitJson(itemsIndexFile, index);
        if (skippedHiddenItems > 0) {
            MweMod.LOGGER.info(
                    "{} item visibility: {} indexed, {} skipped (hidden_from_recipe_viewers)",
                    Log.EMI_ITEMS,
                    stats.indexBuckets.values().stream().mapToInt(Set::size).sum(),
                    skippedHiddenItems);
        }
        MweMod.LOGGER.info(
                "{} {} items ({} input refs, {} output refs) -> {}",
                Log.EMI_ITEMS,
                indexedItemCount,
                stats.inputRefs.get(),
                stats.outputRefs.get(),
                itemsIndexFile);
        int indexedCount = stats.indexBuckets.values().stream().mapToInt(Set::size).sum();
        return new ItemIndexExportResult(indexedCount, index, indexedItemIds);
    }

    private static final class WriteStats {
        private final Set<String> indexedItemIds = new TreeSet<>();
        private final Map<String, Set<String>> indexBuckets = new TreeMap<>();
        private final AtomicLong inputRefs = new AtomicLong();
        private final AtomicLong outputRefs = new AtomicLong();
    }

    static List<String> indexedFluidRegistryIds(Set<String> candidateFluids, Set<String> indexedItemIds) {
        if (candidateFluids == null || candidateFluids.isEmpty()) {
            return List.of();
        }
        List<String> indexed = new ArrayList<>();
        for (String fluidId : candidateFluids) {
            if (fluidId != null && indexedItemIds.contains(fluidId)) {
                indexed.add(fluidId);
            }
        }
        return List.copyOf(indexed);
    }
}
