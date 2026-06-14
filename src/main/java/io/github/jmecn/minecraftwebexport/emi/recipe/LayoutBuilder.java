package io.github.jmecn.minecraftwebexport.emi.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.jmecn.minecraftwebexport.model.recipe.RecipeMeta;
import io.github.jmecn.minecraftwebexport.model.recipe.RecipeWidget;
import io.github.jmecn.minecraftwebexport.model.recipe.WidgetInteraction;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.widget.Widget;
import dev.emi.emi.api.widget.WidgetHolder;
import io.github.jmecn.minecraftwebexport.Constants;
import io.github.jmecn.minecraftwebexport.config.MweConfig;
import io.github.jmecn.minecraftwebexport.emi.support.ProgressLog;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class LayoutBuilder {

    private LayoutBuilder() {
    }

    static int panelMargin() {
        return Constants.PANEL_MARGIN;
    }

    static int progressLogStride(int total) {
        return ProgressLog.stride(total, MweConfig.layoutLogStride(), 20, 200);
    }


    public static boolean isEnabled() {
        if (MweConfig.skipEmiLayoutExport()) {
            return false;
        }
        return MweConfig.exportEmiLayout();
    }

    public static int layoutScale() {
        return MweConfig.recipeLayoutScale();
    }

    public static JsonObject buildLayoutInMemory(
            Minecraft client,
            EmiRecipe recipe,
            String recipeId,
            Set<String> textureIds,
            Set<String> referencedItems,
            Set<String> referencedFluids,
            Set<String> referencedTags,
            Map<String, ItemStack> iconVariants) {
        int[] chromeWritten = {0};
        int[] chromeDedupedCount = {0};
        return buildLayout(
                client,
                recipe,
                recipeId,
                textureIds,
                referencedItems,
                referencedFluids,
                referencedTags,
                iconVariants,
                null,
                new HashMap<>(),
                chromeWritten,
                chromeDedupedCount);
    }

    private static JsonObject buildLayout(
            Minecraft client,
            EmiRecipe recipe,
            String recipeId,
            Set<String> textureIds,
            Set<String> referencedItems,
            Set<String> referencedFluids,
            Set<String> referencedTags,
            Map<String, ItemStack> iconVariants,
            Path chromeRoot,
            Map<String, String> chromeHashToRelative,
            int[] chromeWritten,
            int[] chromeDedupedCount) {
        int displayWidth = Math.max(1, recipe.getDisplayWidth());
        int displayHeight = Math.max(1, recipe.getDisplayHeight());

        List<Widget> widgets = new ArrayList<>();
        WidgetHolder holder = new WidgetHolder() {
            @Override
            public int getWidth() {
                return displayWidth;
            }

            @Override
            public int getHeight() {
                return displayHeight;
            }

            @Override
            public <T extends Widget> T add(T widget) {
                widgets.add(widget);
                return widget;
            }
        };
        recipe.addWidgets(holder);

        JsonArray widgetArray = new JsonArray();
        WidgetSerializer.Context context = new WidgetSerializer.Context(
                client,
                chromeRoot,
                chromeHashToRelative,
                chromeWritten,
                chromeDedupedCount,
                referencedItems,
                referencedFluids,
                referencedTags,
                iconVariants);
        WidgetSerializer.serializeWidgets(recipe, widgets, textureIds, context, widgetArray::add);

        JsonObject panel = new JsonObject();
        panel.addProperty("width", displayWidth);
        panel.addProperty("height", displayHeight);
        panel.addProperty("margin", Constants.PANEL_MARGIN);
        panel.addProperty("frameWidth", displayWidth + Constants.PANEL_MARGIN * 2);
        panel.addProperty("frameHeight", displayHeight + Constants.PANEL_MARGIN * 2);

        JsonObject root = new JsonObject();
        root.addProperty("schema", Constants.LAYOUT_PACK_SCHEMA);
        root.addProperty("id", recipeId);
        root.addProperty("scale", layoutScale());
        root.add("panel", panel);

        ResourceLocation emiId = recipe.getId();
        if (emiId != null) {
            root.addProperty("emiId", emiId.toString());
        }
        if (recipe.getCategory() != null && recipe.getCategory().getId() != null) {
            root.addProperty("category", recipe.getCategory().getId().toString());
        }

        root.add("widgets", widgetArray);
        return root;
    }

    public static RecipeMeta bake(JsonObject layout) {
        JsonObject panel = layout.getAsJsonObject("panel");
        int width = panel != null && panel.has("width") ? panel.get("width").getAsInt() : 1;
        int height = panel != null && panel.has("height") ? panel.get("height").getAsInt() : 1;
        int margin = panel != null && panel.has("margin")
                ? panel.get("margin").getAsInt()
                : panelMargin();
        String id = stringProp(layout, "id");
        String category = stringProp(layout, "category");

        List<RecipeWidget> widgets = new ArrayList<>();
        JsonElement layoutWidgets = layout.get("widgets");
        if (layoutWidgets != null && layoutWidgets.isJsonArray()) {
            for (JsonElement element : layoutWidgets.getAsJsonArray()) {
                if (!element.isJsonObject()) {
                    continue;
                }
                RecipeWidget baked = bakeWidget(element.getAsJsonObject());
                if (baked != null) {
                    widgets.add(baked);
                }
            }
        }
        return RecipeMeta.of(id, width, height, margin, category, widgets);
    }

    private static RecipeWidget bakeWidget(JsonObject widget) {
        String type = stringProp(widget, "type");
        if ((!"slot".equals(type) && !"tank".equals(type))) {
            return null;
        }
        WidgetInteraction interaction = bakeInteraction(widget);
        if (interaction == null) {
            return null;
        }
        return RecipeWidget.of(
                intProp(widget, "x"),
                intProp(widget, "y"),
                intProp(widget, "w"),
                intProp(widget, "h"),
                stringProp(widget, "role"),
                interaction);
    }

    private static WidgetInteraction bakeInteraction(JsonObject widget) {
        if (!widget.has("ingredient")) {
            return null;
        }
        JsonElement ingredient = widget.get("ingredient");
        if (ingredient.isJsonArray()) {
            return bakeListInteraction(ingredient.getAsJsonArray(), widget);
        }
        return bakeSingleInteraction(ingredient, widget);
    }

    private static WidgetInteraction bakeListInteraction(JsonArray array, JsonObject widget) {
        List<WidgetInteraction> entries = new ArrayList<>();
        for (JsonElement child : array) {
            WidgetInteraction entry = bakeEntry(child);
            if (entry != null) {
                entries.add(entry);
            }
        }
        if (entries.isEmpty()) {
            return null;
        }
        int featured = 0;
        if (widget.has("tagDisplayItem") && widget.get("tagDisplayItem").isJsonPrimitive()) {
            String display = widget.get("tagDisplayItem").getAsString();
            for (int i = 0; i < entries.size(); i++) {
                WidgetInteraction entry = entries.get(i);
                if (display.equals(entry.id())) {
                    featured = i;
                    break;
                }
            }
        }
        return WidgetInteraction.list(entries, featured);
    }

    private static WidgetInteraction bakeSingleInteraction(JsonElement ingredient, JsonObject widget) {
        if (ingredient.isJsonPrimitive()) {
            String raw = ingredient.getAsString();
            if (raw.startsWith("#item:")) {
                return WidgetInteraction.tag(
                        raw.substring(6),
                        "item",
                        stringProp(widget, "tagDisplayItem"));
            }
            if (raw.startsWith("#block:")) {
                return WidgetInteraction.tag(raw.substring(7), "block", null);
            }
            if (raw.startsWith("#fluid:")) {
                return WidgetInteraction.tag(raw.substring(7), "fluid", null);
            }
            if (raw.startsWith("item:")) {
                return WidgetInteraction.item(raw.substring(5), null, null);
            }
            if (raw.startsWith("fluid:")) {
                return bakeFluidPrimitive(raw.substring(6));
            }
            return WidgetInteraction.item(raw, null, null);
        }
        if (!ingredient.isJsonObject()) {
            return null;
        }
        JsonObject object = ingredient.getAsJsonObject();
        String type = stringProp(object, "type");
        if ("tag".equals(type) && object.has("id")) {
            return WidgetInteraction.tag(
                    object.get("id").getAsString(),
                    tagKindFromRegistry(stringProp(object, "registry")),
                    stringProp(widget, "tagDisplayItem"));
        }
        if ("fluid".equals(type)) {
            return WidgetInteraction.fluid(
                    stringProp(object, "id"),
                    object.has("amount") ? object.get("amount").getAsLong() : null);
        }
        if ("item".equals(type) || object.has("id")) {
            return WidgetInteraction.item(
                    stringProp(object, "id"),
                    object.has("amount") && object.get("amount").isJsonPrimitive()
                            ? object.get("amount").getAsInt()
                            : null,
                    object.has("nbt") && object.get("nbt").isJsonObject()
                            ? object.getAsJsonObject("nbt")
                            : null);
        }
        return null;
    }

    private static WidgetInteraction bakeEntry(JsonElement element) {
        if (element.isJsonPrimitive()) {
            String raw = element.getAsString();
            if (raw.startsWith("item:")) {
                return WidgetInteraction.item(raw.substring(5), null, null);
            }
            if (raw.startsWith("fluid:")) {
                return bakeFluidPrimitive(raw.substring(6));
            }
            return WidgetInteraction.item(raw, null, null);
        }
        if (!element.isJsonObject()) {
            return null;
        }
        JsonObject object = element.getAsJsonObject();
        String type = stringProp(object, "type");
        if ("fluid".equals(type)) {
            return WidgetInteraction.fluid(
                    stringProp(object, "id"),
                    object.has("amount") ? object.get("amount").getAsLong() : null);
        }
        return WidgetInteraction.item(
                stringProp(object, "id"),
                object.has("amount") && object.get("amount").isJsonPrimitive()
                        ? object.get("amount").getAsInt()
                        : null,
                null);
    }

    private static WidgetInteraction bakeFluidPrimitive(String body) {
        int colon = body.indexOf(':');
        if (colon <= 0) {
            return null;
        }
        return WidgetInteraction.fluid(body, null);
    }

    private static String tagKindFromRegistry(String registry) {
        if (registry == null) {
            return "item";
        }
        if (registry.contains("fluid")) {
            return "fluid";
        }
        if (registry.contains("block")) {
            return "block";
        }
        return "item";
    }

    private static String stringProp(JsonObject object, String key) {
        if (object.has(key) && object.get(key).isJsonPrimitive()) {
            return object.get(key).getAsString();
        }
        return null;
    }

    private static int intProp(JsonObject object, String key) {
        if (object.has(key) && object.get(key).isJsonPrimitive()) {
            return object.get(key).getAsInt();
        }
        return 0;
    }
}
