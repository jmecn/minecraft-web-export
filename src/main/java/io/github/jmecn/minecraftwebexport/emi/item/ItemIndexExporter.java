package io.github.jmecn.minecraftwebexport.emi.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.jmecn.minecraftwebexport.Constants;
import io.github.jmecn.minecraftwebexport.MweMod;
import io.github.jmecn.minecraftwebexport.config.MweConfig;
import io.github.jmecn.minecraftwebexport.emi.EmiPaths;
import io.github.jmecn.minecraftwebexport.emi.pipeline.Visibility;
import io.github.jmecn.minecraftwebexport.emi.support.Log;
import io.github.jmecn.minecraftwebexport.emi.support.ProgressLog;
import io.github.jmecn.minecraftwebexport.io.ExportWriteQueue;
import io.github.jmecn.minecraftwebexport.io.JsonIO;
import io.github.jmecn.minecraftwebexport.model.emi.item.ExportedTagCatalog;
import io.github.jmecn.minecraftwebexport.model.emi.item.ItemIndexBuild;
import io.github.jmecn.minecraftwebexport.model.emi.item.ItemIndexResult;
import io.github.jmecn.minecraftwebexport.model.item.ItemDetail;
import io.github.jmecn.minecraftwebexport.model.item.ItemIndex;
import io.github.jmecn.minecraftwebexport.model.item.RegistryTagSet;
import io.github.jmecn.minecraftwebexport.model.pipeline.ExportContext;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import net.minecraft.server.MinecraftServer;

public final class ItemIndexExporter {

    private ItemIndexExporter() {
    }

    public static ItemIndexResult export(
            Path outputDir, MinecraftServer server, ExportContext context, ExportWriteQueue writes)
            throws IOException {
        Objects.requireNonNull(writes, "writes");
        if (context.inputs().isEmpty() && context.outputs().isEmpty() && context.itemIds().isEmpty()) {
            MweMod.LOGGER.warn("{} no item refs in export context - skipping items index", Log.EMI_ITEMS);
            return new ItemIndexResult(0, 0, 0, 0);
        }
        ItemIndexBuild build = buildFromContext(outputDir, server, context);
        return writeIndex(outputDir, server, build, writes);
    }

    public static Set<String> collectRegistryTagIds(MinecraftServer server, Set<String> itemIds) {
        return ItemIndexSupport.collectRegistryTagIds(server, itemIds);
    }

    public static void accumulateRecipeRefsFromLayout(
            String recipeId,
            JsonObject layout,
            Map<String, Map<String, Set<String>>> inputs,
            Map<String, Map<String, Set<String>>> outputs,
            Set<String> fluidRegistryIds) {
        if (ItemIndexSupport.isEmiTagDisplayRecipe(recipeId)) {
            return;
        }
        String categoryId = ItemIndexSupport.readCategoryId(layout, recipeId);
        JsonArray widgets = ItemIndexSupport.readWidgets(layout);
        for (JsonElement widgetElement : widgets) {
            if (!widgetElement.isJsonObject()) {
                continue;
            }
            JsonObject widget = widgetElement.getAsJsonObject();
            Map<String, Map<String, Set<String>>> bucket =
                    ItemIndexSupport.bucketForRole(widget, inputs, outputs);
            if (bucket == null) {
                continue;
            }
            Set<String> ids = new TreeSet<>();
            if (widget.has("tagDisplayItem") && widget.get("tagDisplayItem").isJsonPrimitive()) {
                ItemIndexSupport.addCanonicalId(widget.get("tagDisplayItem").getAsString(), ids);
            }
            if (widget.has("ingredient")) {
                ItemIndexSupport.collectIngredientIds(
                        widget.get("ingredient"), ids, new HashMap<>(), fluidRegistryIds, null);
            }
            ItemIndexSupport.addRecipeRefs(bucket, categoryId, ids, recipeId);
        }
    }

    private static ItemIndexBuild buildFromContext(
            Path outputDir, MinecraftServer server, ExportContext context) throws IOException {
        ExportedTagCatalog exportedTags = ItemIndexSupport.loadExportedTagCatalog(outputDir);
        Set<String> allItemIds = new TreeSet<>();
        allItemIds.addAll(context.inputs().keySet());
        allItemIds.addAll(context.outputs().keySet());
        ItemIndexSupport.mergeSeedItemIds(allItemIds, context.itemIds());
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

    private static ItemIndexResult writeIndex(
            Path outputDir, MinecraftServer server, ItemIndexBuild build, ExportWriteQueue writes) {
        Objects.requireNonNull(writes, "writes");
        Path itemsIndexFile = EmiPaths.resolve(outputDir, Constants.ITEMS_INDEX_FILE);
        Map<String, Map<String, Set<String>>> inputs = build.inputs();
        Map<String, Map<String, Set<String>>> outputs = build.outputs();
        Set<String> allItemIds = build.allItemIds();

        int inputRefs = 0;
        int outputRefs = 0;
        Map<String, Set<String>> indexBuckets = new TreeMap<>();
        int writeTotal = allItemIds.size();
        int writeStride = ProgressLog.stride(writeTotal, MweConfig.itemsIndexWriteLogStride(), 20, 200);
        int writeProgress = 0;
        int skippedHiddenItems = 0;
        MweMod.LOGGER.info("{} writing {} item detail files", Log.EMI_ITEMS, writeTotal);

        for (String itemId : allItemIds) {
            writeProgress++;
            if (!Visibility.shouldExportRegistryId(server, itemId)) {
                skippedHiddenItems++;
                continue;
            }
            ItemIndexSupport.IdParts item = ItemIndexSupport.IdParts.parse(itemId);
            if (item == null) {
                continue;
            }
            indexBuckets.computeIfAbsent(item.namespace(), ignored -> new TreeSet<>()).add(item.path());
            Path itemFile = item.toItemFile(outputDir);

            if (inputs.containsKey(itemId)) {
                inputRefs += ItemIndexSupport.countRecipeRefs(inputs.get(itemId));
            }
            if (outputs.containsKey(itemId)) {
                outputRefs += ItemIndexSupport.countRecipeRefs(outputs.get(itemId));
            }
            RegistryTagSet tagSets = build.registryTagsByItem().get(itemId);
            RegistryTagSet inBundle = tagSets != null ? build.exportedTags().intersect(tagSets) : null;
            ItemDetail detail = ItemDetail.of(
                    inputs.containsKey(itemId)
                            ? ItemIndexSupport.categoryBuckets(inputs.get(itemId))
                            : null,
                    outputs.containsKey(itemId)
                            ? ItemIndexSupport.categoryBuckets(outputs.get(itemId))
                            : null,
                    tagSets != null && !tagSets.isEmpty() ? tagSets : null,
                    inBundle != null && !inBundle.isEmpty() ? inBundle : null);
            writes.submitJson(itemFile, detail);
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

        Map<String, List<String>> namespacePaths = new LinkedHashMap<>();
        for (Map.Entry<String, Set<String>> entry : indexBuckets.entrySet()) {
            namespacePaths.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        ItemIndex index = new ItemIndex(namespacePaths, List.copyOf(build.fluidRegistryIds()));
        writes.submitJson(itemsIndexFile, index);
        if (skippedHiddenItems > 0) {
            MweMod.LOGGER.info(
                    "{} item visibility: {} indexed, {} skipped (hidden_from_recipe_viewers)",
                    Log.EMI_ITEMS,
                    indexBuckets.values().stream().mapToInt(Set::size).sum(),
                    skippedHiddenItems);
        }
        MweMod.LOGGER.info(
                "{} {} items ({} input refs, {} output refs) -> {}",
                Log.EMI_ITEMS,
                allItemIds.size() - skippedHiddenItems,
                inputRefs,
                outputRefs,
                itemsIndexFile);
        int indexedCount = indexBuckets.values().stream().mapToInt(Set::size).sum();
        long indexBytes = JsonIO.toUtf8Bytes(index).length;
        return new ItemIndexResult(indexedCount, inputRefs, outputRefs, indexBytes);
    }
}
