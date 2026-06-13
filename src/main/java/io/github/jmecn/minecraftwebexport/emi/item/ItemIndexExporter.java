package io.github.jmecn.minecraftwebexport.emi.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.jmecn.minecraftwebexport.MweMod;
import io.github.jmecn.minecraftwebexport.emi.recipe.BundleMods;
import io.github.jmecn.minecraftwebexport.emi.recipe.IndexIds;
import io.github.jmecn.minecraftwebexport.emi.support.Log;
import io.github.jmecn.minecraftwebexport.model.emi.item.ItemIndexBuild;
import io.github.jmecn.minecraftwebexport.model.emi.item.ItemIndexResult;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.server.MinecraftServer;

public final class ItemIndexExporter {

    private ItemIndexExporter() {
    }

    public static ItemIndexResult export(Path outputDir) throws IOException {
        return export(outputDir, null, BundleMods.empty());
    }

    public static ItemIndexResult export(Path outputDir, MinecraftServer server) throws IOException {
        return export(outputDir, server, BundleMods.empty());
    }

    public static ItemIndexResult export(Path outputDir, MinecraftServer server, BundleMods mods) throws IOException {
        if (mods == null || mods.isEmpty()) {
            MweMod.LOGGER.warn("{} no recipe mods in bundle - skipping items index", Log.EMI_ITEMS);
            return new ItemIndexResult(0, 0, 0, 0);
        }
        List<String> recipeIds = IndexIds.allRecipeIds(outputDir, mods);
        return exportWithLayouts(outputDir, server, recipeIds, recipeId -> {
            try {
                return IndexIds.loadLayout(outputDir, recipeId, mods);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, Set.of());
    }

    public static ItemIndexResult export(
            Path outputDir,
            MinecraftServer server,
            Map<String, JsonObject> layoutsByRecipeId) throws IOException {
        return export(outputDir, server, layoutsByRecipeId, Set.of());
    }

    public static Set<String> collectReferencedItemIds(
            MinecraftServer server,
            Map<String, JsonObject> layoutsByRecipeId,
            Set<String> seedItemIds,
            Set<String> seedTagIds) {
        Set<String> itemIds = new TreeSet<>();
        ItemIndexSupport.mergeSeedItemIds(itemIds, seedItemIds);
        if (server != null && seedTagIds != null && !seedTagIds.isEmpty()) {
            itemIds.addAll(io.github.jmecn.minecraftwebexport.emi.tag.ClosureExpander
                    .expand(server, seedTagIds).items());
        }
        if (layoutsByRecipeId == null || layoutsByRecipeId.isEmpty()) {
            return Set.copyOf(itemIds);
        }

        List<String> recipeIds = new ArrayList<>(layoutsByRecipeId.keySet());
        Collections.sort(recipeIds);
        Set<String> fluidRegistryIds = new TreeSet<>();

        for (String recipeId : recipeIds) {
            if (ItemIndexSupport.isEmiTagDisplayRecipe(recipeId)) {
                continue;
            }
            JsonObject layout = layoutsByRecipeId.get(recipeId);
            if (layout == null) {
                continue;
            }
            JsonArray widgets = ItemIndexSupport.readWidgets(layout);
            for (JsonElement widgetElement : widgets) {
                if (!widgetElement.isJsonObject()) {
                    continue;
                }
                JsonObject widget = widgetElement.getAsJsonObject();
                Set<String> ids = new TreeSet<>();
                if (widget.has("tagDisplayItem") && widget.get("tagDisplayItem").isJsonPrimitive()) {
                    ItemIndexSupport.addCanonicalId(widget.get("tagDisplayItem").getAsString(), ids);
                }
                if (widget.has("ingredient")) {
                    ItemIndexSupport.collectIngredientIds(
                            widget.get("ingredient"), ids, Map.of(), fluidRegistryIds, server);
                }
                itemIds.addAll(ids);
            }
        }
        return Set.copyOf(itemIds);
    }

    public static Set<String> collectRegistryTagIds(MinecraftServer server, Set<String> itemIds) {
        return ItemIndexSupport.collectRegistryTagIds(server, itemIds);
    }

    public static Set<String> planFullModeTagExport(
            MinecraftServer server,
            Map<String, JsonObject> layoutsByRecipeId,
            Set<String> seedItemIds,
            Set<String> layoutReferencedTags) {
        if (server == null) {
            return layoutReferencedTags == null ? Set.of() : Set.copyOf(layoutReferencedTags);
        }
        Set<String> items = new TreeSet<>(collectReferencedItemIds(
                server, layoutsByRecipeId, seedItemIds, layoutReferencedTags));
        Set<String> tags = new TreeSet<>(layoutReferencedTags == null ? Set.of() : layoutReferencedTags);

        while (true) {
            Set<String> nextTags = new TreeSet<>(tags);
            nextTags.addAll(collectRegistryTagIds(server, items));

            Set<String> nextItems = new TreeSet<>(items);
            if (!nextTags.isEmpty()) {
                nextItems.addAll(io.github.jmecn.minecraftwebexport.emi.tag.ClosureExpander
                        .expand(server, nextTags).items());
            }

            if (nextTags.equals(tags) && nextItems.equals(items)) {
                return Set.copyOf(nextTags);
            }
            tags = nextTags;
            items = nextItems;
        }
    }

    public static ItemIndexResult export(
            Path outputDir,
            MinecraftServer server,
            Map<String, JsonObject> layoutsByRecipeId,
            Set<String> seedItemIds) throws IOException {
        if (layoutsByRecipeId == null || layoutsByRecipeId.isEmpty()) {
            MweMod.LOGGER.warn("{} no in-memory layouts - skipping items index", Log.EMI_ITEMS);
            return new ItemIndexResult(0, 0, 0, 0);
        }
        List<String> recipeIds = new ArrayList<>(layoutsByRecipeId.keySet());
        Collections.sort(recipeIds);
        return exportWithLayouts(outputDir, server, recipeIds, layoutsByRecipeId::get, seedItemIds);
    }

    private static ItemIndexResult exportWithLayouts(
            Path outputDir,
            MinecraftServer server,
            List<String> recipeIds,
            ItemIndexBuilder.LayoutLoader layoutLookup,
            Set<String> seedItemIds) throws IOException {
        ItemIndexBuild build = ItemIndexBuilder.build(outputDir, server, recipeIds, layoutLookup, seedItemIds);
        return ItemIndexPersistence.write(outputDir, server, build);
    }
}
