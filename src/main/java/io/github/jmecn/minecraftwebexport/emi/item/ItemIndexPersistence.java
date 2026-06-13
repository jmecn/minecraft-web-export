package io.github.jmecn.minecraftwebexport.emi.item;

import io.github.jmecn.minecraftwebexport.Constants;
import io.github.jmecn.minecraftwebexport.config.MweConfig;
import io.github.jmecn.minecraftwebexport.MweMod;
import io.github.jmecn.minecraftwebexport.emi.EmiPaths;
import io.github.jmecn.minecraftwebexport.emi.pipeline.Visibility;
import io.github.jmecn.minecraftwebexport.emi.support.Log;
import io.github.jmecn.minecraftwebexport.emi.support.ProgressLog;
import io.github.jmecn.minecraftwebexport.io.ExportWriteQueue;
import io.github.jmecn.minecraftwebexport.io.JsonIO;
import io.github.jmecn.minecraftwebexport.model.emi.item.ItemIndexBuild;
import io.github.jmecn.minecraftwebexport.model.emi.item.ItemIndexResult;
import io.github.jmecn.minecraftwebexport.model.item.ItemDetail;
import io.github.jmecn.minecraftwebexport.model.item.ItemIndex;
import io.github.jmecn.minecraftwebexport.model.item.RegistryTagSet;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import net.minecraft.server.MinecraftServer;

final class ItemIndexPersistence {

    private ItemIndexPersistence() {
    }

    static ItemIndexResult write(
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
