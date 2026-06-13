package io.github.jmecn.minecraftwebexport.emi.recipe;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeManager;
import net.minecraft.resources.ResourceLocation;

public final class Resolver {

    private Resolver() {
    }

    public static boolean isEmiAvailable() {
        try {
            Class.forName("EmiApi");
            return EmiApi.getRecipeManager() != null;
        } catch (Throwable t) {
            return false;
        }
    }

    public static EmiRecipe resolve(String recipeId) {
        EmiRecipeManager manager = EmiApi.getRecipeManager();
        if (manager == null) {
            return null;
        }
        ResourceLocation direct = ResourceLocation.parse(recipeId);
        EmiRecipe found = manager.getRecipe(direct);
        if (found != null) {
            return found;
        }
        ResourceLocation tmrv = ResourceLocation.fromNamespaceAndPath(
                "toomanyrecipeviewers", "/" + recipeId.replace(':', '/'));
        found = manager.getRecipe(tmrv);
        if (found != null) {
            return found;
        }
        for (EmiRecipe recipe : manager.getRecipes()) {
            ResourceLocation id = recipe.getId();
            if (id == null) {
                continue;
            }
            if (recipeId.equals(id.toString())) {
                return recipe;
            }
            if ("toomanyrecipeviewers".equals(id.getNamespace())
                    && recipeId.equals(id.getPath().substring(1).replace('/', ':'))) {
                return recipe;
            }
        }
        return null;
    }
}
