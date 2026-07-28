package io.github.jmecn.minecraftwebexport.emi.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.jmecn.minecraftwebexport.model.recipe.RecipeWidget;
import io.github.jmecn.minecraftwebexport.model.recipe.WidgetInteraction;
import java.util.ArrayList;
import java.util.List;

final class RecipeInteractionMapper {

    private RecipeInteractionMapper() {
    }

    static RecipeWidget widgetFromSlotJson(JsonObject widget) {
        String type = stringProp(widget, "type");
        if (!"slot".equals(type) && !"tank".equals(type)) {
            return null;
        }
        WidgetInteraction interaction = interactionFromSlotJson(widget);
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

    static WidgetInteraction interactionFromSlotJson(JsonObject widget) {
        if (!widget.has("ingredient")) {
            return null;
        }
        JsonElement ingredient = widget.get("ingredient");
        Integer amount = widget.has("amount") && widget.get("amount").isJsonPrimitive()
                ? widget.get("amount").getAsInt()
                : null;
        if (ingredient.isJsonArray()) {
            return listInteraction(ingredient.getAsJsonArray(), stringProp(widget, "tagDisplayItem"));
        }
        return singleInteraction(ingredient, stringProp(widget, "tagDisplayItem"), amount);
    }

    private static WidgetInteraction listInteraction(JsonArray array, String tagDisplayItem) {
        List<WidgetInteraction> entries = new ArrayList<>();
        for (JsonElement child : array) {
            WidgetInteraction entry = entryInteraction(child);
            if (entry != null) {
                entries.add(entry);
            }
        }
        if (entries.isEmpty()) {
            return null;
        }
        int featured = 0;
        if (tagDisplayItem != null) {
            for (int i = 0; i < entries.size(); i++) {
                if (tagDisplayItem.equals(entries.get(i).id())) {
                    featured = i;
                    break;
                }
            }
        }
        return WidgetInteraction.list(entries, featured);
    }

    private static WidgetInteraction singleInteraction(JsonElement ingredient, String tagDisplayItem, Integer amount) {
        if (ingredient.isJsonPrimitive()) {
            return primitiveInteraction(ingredient.getAsString(), tagDisplayItem, amount);
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
                    tagDisplayItem,
                    amount);
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

    private static WidgetInteraction entryInteraction(JsonElement element) {
        if (element.isJsonPrimitive()) {
            return primitiveInteraction(element.getAsString(), null);
        }
        if (!element.isJsonObject()) {
            return null;
        }
        JsonObject object = element.getAsJsonObject();
        String type = stringProp(object, "type");
        if ("tag".equals(type) && object.has("id")) {
            return WidgetInteraction.tag(
                    object.get("id").getAsString(),
                    tagKindFromRegistry(stringProp(object, "registry")),
                    null);
        }
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

    private static WidgetInteraction primitiveInteraction(String raw, String tagDisplayItem, Integer amount) {
        if (raw.startsWith("#item:")) {
            return WidgetInteraction.tag(raw.substring(6), "item", tagDisplayItem, amount);
        }
        if (raw.startsWith("#block:")) {
            return WidgetInteraction.tag(raw.substring(7), "block", null, amount);
        }
        if (raw.startsWith("#fluid:")) {
            return WidgetInteraction.tag(raw.substring(7), "fluid", null, amount);
        }
        if (raw.startsWith("item:")) {
            return WidgetInteraction.item(raw.substring(5), null, null);
        }
        if (raw.startsWith("fluid:")) {
            return fluidPrimitive(raw.substring(6));
        }
        return WidgetInteraction.item(raw, null, null);
    }

    private static WidgetInteraction fluidPrimitive(String body) {
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
