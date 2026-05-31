package io.github.jmecn.minecraftwebexport.export.emi;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * Bakes v2 recipe meta JSON from an in-memory layout (export intermediate).
 */
final class RecipeMetaBaker {

    private RecipeMetaBaker() {
    }

    static JsonObject bake(JsonObject layout) {
        JsonObject meta = new JsonObject();
        meta.addProperty("schema", RecipeCardPaths.META_SCHEMA);

        JsonObject panel = layout.getAsJsonObject("panel");
        int width = panel != null && panel.has("width") ? panel.get("width").getAsInt() : 1;
        int height = panel != null && panel.has("height") ? panel.get("height").getAsInt() : 1;
        int margin = panel != null && panel.has("margin") ? panel.get("margin").getAsInt()
                : EmiRecipeLayoutExporter.panelMargin();
        meta.addProperty("width", width);
        meta.addProperty("height", height);
        meta.addProperty("margin", margin);

        if (layout.has("id") && layout.get("id").isJsonPrimitive()) {
            meta.addProperty("id", layout.get("id").getAsString());
        }
        if (layout.has("category") && layout.get("category").isJsonPrimitive()) {
            meta.addProperty("category", layout.get("category").getAsString());
        }

        JsonArray widgets = new JsonArray();
        JsonElement layoutWidgets = layout.get("widgets");
        if (layoutWidgets != null && layoutWidgets.isJsonArray()) {
            for (JsonElement element : layoutWidgets.getAsJsonArray()) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject baked = bakeWidget(element.getAsJsonObject());
                if (baked != null) {
                    widgets.add(baked);
                }
            }
        }
        meta.add("widgets", widgets);
        return meta;
    }

    private static JsonObject bakeWidget(JsonObject widget) {
        String type = stringProp(widget, "type");
        if (type == null) {
            return null;
        }
        if (!"slot".equals(type) && !"tank".equals(type)) {
            return null;
        }
        JsonObject interaction = bakeInteraction(widget);
        if (interaction == null) {
            return null;
        }

        JsonObject out = new JsonObject();
        copyBounds(widget, out);
        if (widget.has("role") && widget.get("role").isJsonPrimitive()) {
            out.addProperty("role", widget.get("role").getAsString());
        }
        out.add("interaction", interaction);
        return out;
    }

    private static void copyBounds(JsonObject from, JsonObject to) {
        for (String key : new String[] { "x", "y", "w", "h" }) {
            if (from.has(key)) {
                to.add(key, from.get(key));
            }
        }
    }

    private static JsonObject bakeInteraction(JsonObject widget) {
        if (!widget.has("ingredient")) {
            return null;
        }
        JsonElement ingredient = widget.get("ingredient");
        if (ingredient.isJsonArray()) {
            return bakeListInteraction(ingredient.getAsJsonArray(), widget);
        }
        return bakeSingleInteraction(ingredient, widget);
    }

    private static JsonObject bakeListInteraction(JsonArray array, JsonObject widget) {
        JsonArray entries = new JsonArray();
        for (JsonElement child : array) {
            JsonObject entry = bakeEntry(child);
            if (entry != null) {
                entries.add(entry);
            }
        }
        if (entries.isEmpty()) {
            return null;
        }
        JsonObject interaction = new JsonObject();
        interaction.addProperty("kind", "list");
        interaction.add("entries", entries);
        int featured = 0;
        if (widget.has("tagDisplayItem") && widget.get("tagDisplayItem").isJsonPrimitive()) {
            String display = widget.get("tagDisplayItem").getAsString();
            for (int i = 0; i < entries.size(); i++) {
                JsonObject entry = entries.get(i).getAsJsonObject();
                if (entry.has("id") && display.equals(entry.get("id").getAsString())) {
                    featured = i;
                    break;
                }
            }
        }
        interaction.addProperty("featuredIndex", featured);
        return interaction;
    }

    private static JsonObject bakeSingleInteraction(JsonElement ingredient, JsonObject widget) {
        if (ingredient.isJsonPrimitive()) {
            String raw = ingredient.getAsString();
            if (raw.startsWith("#item:")) {
                JsonObject tag = new JsonObject();
                tag.addProperty("kind", "tag");
                tag.addProperty("tag", raw.substring(6));
                tag.addProperty("tagKind", "item");
                if (widget.has("tagDisplayItem") && widget.get("tagDisplayItem").isJsonPrimitive()) {
                    tag.addProperty("displayId", widget.get("tagDisplayItem").getAsString());
                }
                return tag;
            }
            if (raw.startsWith("#block:")) {
                JsonObject tag = new JsonObject();
                tag.addProperty("kind", "tag");
                tag.addProperty("tag", raw.substring(7));
                tag.addProperty("tagKind", "block");
                return tag;
            }
            if (raw.startsWith("#fluid:")) {
                JsonObject tag = new JsonObject();
                tag.addProperty("kind", "tag");
                tag.addProperty("tag", raw.substring(7));
                tag.addProperty("tagKind", "fluid");
                return tag;
            }
            if (raw.startsWith("item:")) {
                JsonObject item = new JsonObject();
                item.addProperty("kind", "item");
                item.addProperty("id", raw.substring(5));
                return item;
            }
            if (raw.startsWith("fluid:")) {
                return bakeFluidPrimitive(raw.substring(6));
            }
            JsonObject item = new JsonObject();
            item.addProperty("kind", "item");
            item.addProperty("id", raw);
            return item;
        }
        if (!ingredient.isJsonObject()) {
            return null;
        }
        JsonObject object = ingredient.getAsJsonObject();
        String type = stringProp(object, "type");
        if ("tag".equals(type) && object.has("id")) {
            JsonObject tag = new JsonObject();
            tag.addProperty("kind", "tag");
            tag.addProperty("tag", object.get("id").getAsString());
            String registry = stringProp(object, "registry");
            tag.addProperty("tagKind", tagKindFromRegistry(registry));
            if (widget.has("tagDisplayItem") && widget.get("tagDisplayItem").isJsonPrimitive()) {
                tag.addProperty("displayId", widget.get("tagDisplayItem").getAsString());
            }
            return tag;
        }
        if ("fluid".equals(type)) {
            JsonObject fluid = new JsonObject();
            fluid.addProperty("kind", "fluid");
            if (object.has("id")) {
                fluid.addProperty("id", object.get("id").getAsString());
            }
            if (object.has("amount")) {
                fluid.addProperty("amountMb", object.get("amount").getAsLong());
            }
            return fluid;
        }
        if ("item".equals(type) || object.has("id")) {
            JsonObject item = new JsonObject();
            item.addProperty("kind", "item");
            if (object.has("id")) {
                item.addProperty("id", object.get("id").getAsString());
            }
            if (object.has("amount") && object.get("amount").isJsonPrimitive()) {
                item.addProperty("amount", object.get("amount").getAsInt());
            }
            if (object.has("nbt") && object.get("nbt").isJsonObject()) {
                item.add("nbt", object.get("nbt"));
            }
            return item;
        }
        return null;
    }

    private static JsonObject bakeEntry(JsonElement element) {
        if (element.isJsonPrimitive()) {
            String raw = element.getAsString();
            if (raw.startsWith("item:")) {
                JsonObject item = new JsonObject();
                item.addProperty("kind", "item");
                item.addProperty("id", raw.substring(5));
                return item;
            }
            if (raw.startsWith("fluid:")) {
                return bakeFluidPrimitive(raw.substring(6));
            }
            JsonObject item = new JsonObject();
            item.addProperty("kind", "item");
            item.addProperty("id", raw);
            return item;
        }
        if (!element.isJsonObject()) {
            return null;
        }
        JsonObject object = element.getAsJsonObject();
        String type = stringProp(object, "type");
        if ("fluid".equals(type)) {
            JsonObject fluid = new JsonObject();
            fluid.addProperty("kind", "fluid");
            if (object.has("id")) {
                fluid.addProperty("id", object.get("id").getAsString());
            }
            if (object.has("amount")) {
                fluid.addProperty("amountMb", object.get("amount").getAsLong());
            }
            return fluid;
        }
        JsonObject item = new JsonObject();
        item.addProperty("kind", "item");
        if (object.has("id")) {
            item.addProperty("id", object.get("id").getAsString());
        }
        if (object.has("amount") && object.get("amount").isJsonPrimitive()) {
            item.addProperty("amount", object.get("amount").getAsInt());
        }
        return item;
    }

    private static JsonObject bakeFluidPrimitive(String body) {
        int colon = body.indexOf(':');
        if (colon <= 0) {
            return null;
        }
        JsonObject fluid = new JsonObject();
        fluid.addProperty("kind", "fluid");
        fluid.addProperty("id", body);
        return fluid;
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
}
