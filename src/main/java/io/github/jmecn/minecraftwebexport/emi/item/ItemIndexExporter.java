package io.github.jmecn.minecraftwebexport.emi.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.jmecn.minecraftwebexport.MweMod;
import io.github.jmecn.minecraftwebexport.emi.support.Log;
import io.github.jmecn.minecraftwebexport.model.emi.item.ItemIndexBuild;
import io.github.jmecn.minecraftwebexport.model.emi.item.ItemIndexResult;
import io.github.jmecn.minecraftwebexport.model.pipeline.ExportContext;
import io.github.jmecn.minecraftwebexport.io.ExportWriteQueue;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
        ItemIndexBuild build = ItemIndexBuilder.buildFromContext(outputDir, server, context);
        return ItemIndexPersistence.write(outputDir, server, build, writes);
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
}
