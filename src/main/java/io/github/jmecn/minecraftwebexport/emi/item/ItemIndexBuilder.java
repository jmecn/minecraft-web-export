package io.github.jmecn.minecraftwebexport.emi.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.jmecn.minecraftwebexport.Constants;
import io.github.jmecn.minecraftwebexport.MweMod;
import io.github.jmecn.minecraftwebexport.emi.support.Log;
import io.github.jmecn.minecraftwebexport.emi.support.ProgressLog;
import io.github.jmecn.minecraftwebexport.model.emi.item.ExportedTagCatalog;
import io.github.jmecn.minecraftwebexport.model.emi.item.ItemIndexBuild;
import io.github.jmecn.minecraftwebexport.model.item.RegistryTagSet;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import net.minecraft.server.MinecraftServer;

final class ItemIndexBuilder {

    @FunctionalInterface
    interface LayoutLoader {
        JsonObject load(String recipeId);
    }

    private ItemIndexBuilder() {
    }

    static ItemIndexBuild build(
            Path outputDir,
            MinecraftServer server,
            List<String> recipeIds,
            LayoutLoader layoutLookup,
            Set<String> seedItemIds) throws IOException {
        Map<String, Set<String>> tagItems = ItemIndexSupport.loadTagItems(outputDir);
        ExportedTagCatalog exportedTags = ItemIndexSupport.loadExportedTagCatalog(outputDir);

        Map<String, Map<String, Set<String>>> inputs = new TreeMap<>();
        Map<String, Map<String, Set<String>>> outputs = new TreeMap<>();
        Set<String> fluidRegistryIds = new TreeSet<>();
        int scanTotal = recipeIds.size();
        int scanStride = ProgressLog.stride(scanTotal, Constants.PROP_ITEMS_INDEX_SCAN_LOG_STRIDE, 20, 200);
        int scanProgress = 0;
        MweMod.LOGGER.info("{} scanning {} recipes for item refs", Log.EMI_ITEMS, scanTotal);

        for (String recipeId : recipeIds) {
            scanProgress++;
            if (ItemIndexSupport.isEmiTagDisplayRecipe(recipeId)) {
                logScanProgress(scanProgress, scanTotal, scanStride);
                continue;
            }
            JsonObject layout = layoutLookup.load(recipeId);
            if (layout == null) {
                logScanProgress(scanProgress, scanTotal, scanStride);
                continue;
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
                            widget.get("ingredient"), ids, tagItems, fluidRegistryIds, null);
                }
                ItemIndexSupport.addRecipeRefs(bucket, categoryId, ids, recipeId);
            }
            logScanProgress(scanProgress, scanTotal, scanStride);
        }

        Set<String> allItemIds = new TreeSet<>();
        allItemIds.addAll(inputs.keySet());
        allItemIds.addAll(outputs.keySet());
        ItemIndexSupport.mergeSeedItemIds(allItemIds, seedItemIds);
        MweMod.LOGGER.info("{} resolving registry tags for {} items", Log.EMI_ITEMS, allItemIds.size());
        Map<String, RegistryTagSet> registryTagsByItem =
                ItemIndexSupport.resolveRegistryTags(allItemIds, server);

        return new ItemIndexBuild(
                inputs,
                outputs,
                fluidRegistryIds,
                allItemIds,
                registryTagsByItem,
                exportedTags);
    }

    private static void logScanProgress(int progress, int total, int stride) {
        if (ProgressLog.shouldLog(progress, total, stride)) {
            int pct = ProgressLog.percent(progress, total);
            MweMod.LOGGER.info(
                    "{} scan {}% {}/{} recipes",
                    Log.EMI_ITEMS,
                    pct,
                    progress,
                    total);
        }
    }
}
