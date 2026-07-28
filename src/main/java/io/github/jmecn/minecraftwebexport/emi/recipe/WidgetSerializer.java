package io.github.jmecn.minecraftwebexport.emi.recipe;

import com.google.common.collect.Iterables;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.serializer.EmiIngredientSerializer;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.TankWidget;
import dev.emi.emi.api.widget.Widget;
import dev.emi.emi.widget.RecipeButtonWidget;
import io.github.jmecn.minecraftwebexport.emi.icon.StackKey;
import io.github.jmecn.minecraftwebexport.emi.support.Log;
import io.github.jmecn.minecraftwebexport.model.recipe.RecipeWidget;
import io.github.jmecn.minecraftwebexport.model.recipe.WidgetInteraction;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

public final class WidgetSerializer {

    private WidgetSerializer() {
    }

    public static void collectFromRecipe(
            EmiRecipe recipe,
            Set<String> itemIds,
            Set<String> fluidIds,
            Set<String> tagIds,
            Set<String> categoryIds) {
        if (recipe == null) {
            return;
        }
        if (recipe.getCategory() != null && recipe.getCategory().getId() != null) {
            categoryIds.add(recipe.getCategory().getId().toString());
        }
        for (EmiIngredient ingredient : Iterables.concat(
                recipe.getInputs(), recipe.getOutputs(), recipe.getCatalysts())) {
            collectFromIngredient(ingredient, itemIds, fluidIds, tagIds);
        }
    }

    static void collectFromIngredient(
            EmiIngredient ingredient,
            Set<String> itemIds,
            Set<String> fluidIds,
            Set<String> tagIds) {
        if (ingredient == null || ingredient.isEmpty()) {
            return;
        }
        JsonElement serialized = EmiIngredientSerializer.getSerialized(ingredient);
        collectSerializedTagRefs(normalizeIngredientJson(serialized), tagIds);
        collectReferencedStacks(ingredient, itemIds, fluidIds, null);
    }

    static List<RecipeWidget> compileRecipeWidgets(
            EmiRecipe recipe,
            List<Widget> widgets,
            Map<String, ItemStack> iconVariants) {
        List<RecipeWidget> compiled = new ArrayList<>();
        for (Widget widget : widgets) {
            if (shouldSkipWidget(widget)) {
                continue;
            }
            RecipeWidget recipeWidget = compileWidget(widget, iconVariants);
            if (recipeWidget != null) {
                compiled.add(recipeWidget);
            }
        }
        return compiled;
    }

    private static RecipeWidget compileWidget(
            Widget widget,
            Map<String, ItemStack> iconVariants) {
        try {
            if (widget instanceof TankWidget tank) {
                return compileSlot(tank, iconVariants);
            }
            if (widget instanceof SlotWidget slot) {
                return compileSlot(slot, iconVariants);
            }
            return null;
        } catch (Exception e) {
            Log.detailFailure(1,
                    "{} widget {} failed: {}",
                    Log.EMI_LAYOUT,
                    widget.getClass().getName(),
                    e);
            throw new RuntimeException("widget export failed: " + widget.getClass().getName(), e);
        }
    }

    private static RecipeWidget compileSlot(SlotWidget slot, Map<String, ItemStack> iconVariants) {
        EmiIngredient stack = slot.getStack();
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        JsonElement rawIngredient = EmiIngredientSerializer.getSerialized(stack);
        Integer amount = extractTagAmount(rawIngredient);
        JsonElement ingredient = normalizeIngredientJson(rawIngredient);
        collectIconVariants(stack, iconVariants);
        String tagDisplayItem = tagDisplayItemFor(stack);
        WidgetInteraction interaction = RecipeInteractionMapper.interactionFromSlotJson(
                slotIngredientJson(ingredient, tagDisplayItem, amount));
        if (interaction == null) {
            return null;
        }
        Bounds bounds = slot.getBounds();
        return RecipeWidget.of(
                bounds.x(),
                bounds.y(),
                bounds.width(),
                bounds.height(),
                inferSlotRole(slot),
                interaction);
    }

    private static Integer extractTagAmount(JsonElement ingredient) {
        if (ingredient == null || !ingredient.isJsonObject()) {
            return null;
        }
        JsonObject object = ingredient.getAsJsonObject();
        if ("tag".equals(stringProp(object, "type"))
                && object.has("amount") && object.get("amount").isJsonPrimitive()) {
            return object.get("amount").getAsInt();
        }
        return null;
    }

    private static JsonObject slotIngredientJson(JsonElement ingredient, String tagDisplayItem, Integer amount) {
        JsonObject object = new JsonObject();
        object.add("ingredient", ingredient);
        if (tagDisplayItem != null) {
            object.addProperty("tagDisplayItem", tagDisplayItem);
        }
        if (amount != null) {
            object.addProperty("amount", amount);
        }
        return object;
    }

    private static String tagDisplayItemFor(EmiIngredient ingredient) {
        for (EmiStack stack : ingredient.getEmiStacks()) {
            String key = StackKey.forEmiStack(stack);
            if (key != null) {
                return key;
            }
        }
        return null;
    }

    private static void collectIconVariants(EmiIngredient ingredient, Map<String, ItemStack> iconVariants) {
        if (iconVariants == null) {
            return;
        }
        for (EmiStack emiStack : ingredient.getEmiStacks()) {
            String iconKey = StackKey.forEmiStack(emiStack);
            if (!StackKey.isVariantKey(iconKey)) {
                continue;
            }
            ItemStack stack = StackKey.toItemStack(emiStack);
            if (!stack.isEmpty()) {
                iconVariants.put(iconKey, stack);
            }
        }
    }

    static void collectReferencedStacks(
            EmiIngredient ingredient,
            Set<String> items,
            Set<String> fluids,
            Map<String, ItemStack> iconVariants) {
        if (ingredient == null || ingredient.isEmpty()) {
            return;
        }
        for (EmiStack emiStack : ingredient.getEmiStacks()) {
            ResourceLocation id = emiStack.getId();
            if (id == null) {
                continue;
            }
            if (emiStack.getKey() instanceof Fluid) {
                fluids.add(id.toString());
            } else {
                String itemId = StackKey.itemIdForEmiStack(emiStack);
                if (itemId != null && !itemId.isBlank()) {
                    items.add(itemId);
                } else {
                    items.add(id.toString());
                }
                if (iconVariants != null) {
                    String iconKey = StackKey.forEmiStack(emiStack);
                    if (StackKey.isVariantKey(iconKey)) {
                        ItemStack stack = StackKey.toItemStack(emiStack);
                        if (!stack.isEmpty()) {
                            iconVariants.put(iconKey, stack);
                        }
                    }
                }
            }
        }
    }

    static JsonElement normalizeIngredientJson(JsonElement ingredient) {
        if (ingredient == null || !ingredient.isJsonObject()) {
            return ingredient;
        }
        JsonObject object = ingredient.getAsJsonObject();
        if (!object.has("type") || !object.get("type").isJsonPrimitive()) {
            return ingredient;
        }
        if (!object.has("id") || !object.get("id").isJsonPrimitive()) {
            return ingredient;
        }
        if (!"tag".equals(object.get("type").getAsString())) {
            return ingredient;
        }
        String id = object.get("id").getAsString();
        String registry = object.has("registry") && object.get("registry").isJsonPrimitive()
                ? object.get("registry").getAsString()
                : "minecraft:item";
        if (registry.contains("fluid")) {
            return new JsonPrimitive("#fluid:" + id);
        }
        if (registry.contains("block")) {
            return new JsonPrimitive("#block:" + id);
        }
        return new JsonPrimitive("#item:" + id);
    }

    static void collectSerializedTagRefs(JsonElement ingredient, Set<String> tags) {
        if (ingredient == null || ingredient.isJsonNull()) {
            return;
        }
        if (ingredient.isJsonPrimitive()) {
            String raw = ingredient.getAsString();
            if (raw.startsWith("#item:")) {
                tags.add(raw.substring(6));
            } else if (raw.startsWith("#block:")) {
                tags.add(raw.substring(7));
            } else if (raw.startsWith("#fluid:")) {
                tags.add(raw.substring(7));
            }
            return;
        }
        if (ingredient.isJsonArray()) {
            for (JsonElement child : ingredient.getAsJsonArray()) {
                collectSerializedTagRefs(child, tags);
            }
            return;
        }
        if (!ingredient.isJsonObject()) {
            return;
        }

        JsonObject object = ingredient.getAsJsonObject();
        if (object.has("type")
                && object.get("type").isJsonPrimitive()
                && "tag".equals(object.get("type").getAsString())
                && object.has("id")
                && object.get("id").isJsonPrimitive()) {
            tags.add(object.get("id").getAsString());
        }
        if (object.has("tag") && object.get("tag").isJsonPrimitive()) {
            tags.add(object.get("tag").getAsString());
        }
        if (object.has("entries") && object.get("entries").isJsonArray()) {
            for (JsonElement child : object.getAsJsonArray("entries")) {
                collectSerializedTagRefs(child, tags);
            }
        }
    }

    static String inferSlotRole(SlotWidget slot) {
        if (readBooleanField(slot, "catalyst")) {
            return "catalyst";
        }
        if (slot.getRecipe() != null) {
            return "output";
        }
        return "input";
    }

    private static boolean shouldSkipWidget(Widget widget) {
        return widget instanceof RecipeButtonWidget;
    }

    private static boolean readBooleanField(Object target, String name) {
        Boolean value = readField(target, name, Boolean.class);
        return value != null && value;
    }

    @SuppressWarnings("unchecked")
    private static <T> T readField(Object target, String name, Class<T> type) {
        Class<?> current = target.getClass();
        while (current != null) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                Object value = field.get(target);
                if (value == null) {
                    return null;
                }
                return type.cast(value);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }
}
