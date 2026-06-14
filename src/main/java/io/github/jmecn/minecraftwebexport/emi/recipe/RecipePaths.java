package io.github.jmecn.minecraftwebexport.emi.recipe;

import io.github.jmecn.minecraftwebexport.Constants;
import io.github.jmecn.minecraftwebexport.emi.EmiPaths;
import io.github.jmecn.minecraftwebexport.model.recipe.RecipeIdParts;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

public final class RecipePaths {

    private RecipePaths() {
    }

    public static void ensureRecipeDirectories(Path exportRoot, Set<String> recipeIds) throws IOException {
        if (recipeIds == null || recipeIds.isEmpty()) {
            return;
        }
        Set<String> namespaces = new java.util.TreeSet<>();
        for (String recipeId : recipeIds) {
            RecipeIdParts parts = RecipeIdParts.parse(recipeId);
            if (parts != null) {
                namespaces.add(parts.namespace());
            }
        }
        for (String namespace : namespaces) {
            Files.createDirectories(EmiPaths.resolve(exportRoot, Constants.RECIPES_DIR + "/" + namespace));
        }
    }

    public static Path pngPath(Path exportRoot, String recipeId) {
        RecipeIdParts parts = RecipeIdParts.require(recipeId);
        return EmiPaths.resolve(
                exportRoot,
                Constants.RECIPES_DIR + "/" + parts.namespace() + "/" + parts.pathSafe() + ".png");
    }

    public static Path metaPath(Path exportRoot, String recipeId) {
        RecipeIdParts parts = RecipeIdParts.require(recipeId);
        return EmiPaths.resolve(
                exportRoot,
                Constants.RECIPES_DIR + "/" + parts.namespace() + "/" + parts.pathSafe() + ".json");
    }

    public static int recipeMargin() {
        return Constants.RECIPE_MARGIN;
    }
}
