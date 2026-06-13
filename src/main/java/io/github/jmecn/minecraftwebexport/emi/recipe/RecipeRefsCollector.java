package io.github.jmecn.minecraftwebexport.emi.recipe;

import com.google.common.collect.Iterables;
import com.google.gson.JsonElement;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.serializer.EmiIngredientSerializer;
import java.util.Map;
import java.util.Set;

public final class RecipeRefsCollector {

    private RecipeRefsCollector() {
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
            collectFromIngredient(ingredient, itemIds, fluidIds, tagIds, null);
        }
    }

    public static void collectFromIngredient(
            EmiIngredient ingredient,
            Set<String> itemIds,
            Set<String> fluidIds,
            Set<String> tagIds,
            Map<String, net.minecraft.world.item.ItemStack> iconVariants) {
        if (ingredient == null || ingredient.isEmpty()) {
            return;
        }
        JsonElement serialized = EmiIngredientSerializer.getSerialized(ingredient);
        WidgetSerializer.collectSerializedTagRefs(
                WidgetSerializer.normalizeIngredientJson(serialized), tagIds);
        WidgetSerializer.collectReferencedStacks(ingredient, itemIds, fluidIds, iconVariants);
    }
}
