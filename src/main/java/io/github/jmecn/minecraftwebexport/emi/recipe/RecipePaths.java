package io.github.jmecn.minecraftwebexport.emi.recipe;

import io.github.jmecn.minecraftwebexport.Constants;
import io.github.jmecn.minecraftwebexport.emi.EmiPaths;
import java.nio.file.Path;

public final class RecipePaths {

    private RecipePaths() {
    }

    public static IndexIds.RecipeIdParts requireParts(String recipeId) {
        IndexIds.RecipeIdParts parts = IndexIds.splitRecipeId(recipeId);
        if (parts == null) {
            throw new IllegalArgumentException("invalid recipe id: " + recipeId);
        }
        return parts;
    }

    public static String pathSafe(String path) {
        if (path == null || path.isEmpty()) {
            return "unknown";
        }
        return path.replace('/', '_');
    }

    public static String fileStem(String recipeId) {
        return pathSafe(requireParts(recipeId).path());
    }

    public static Path pngPath(Path exportRoot, String recipeId) {
        IndexIds.RecipeIdParts parts = requireParts(recipeId);
        return EmiPaths.resolve(
                exportRoot,
                Constants.RECIPES_DIR + "/" + parts.namespace() + "/" + pathSafe(parts.path()) + ".png");
    }

    public static Path metaPath(Path exportRoot, String recipeId) {
        IndexIds.RecipeIdParts parts = requireParts(recipeId);
        return EmiPaths.resolve(
                exportRoot,
                Constants.RECIPES_DIR + "/" + parts.namespace() + "/" + pathSafe(parts.path()) + ".json");
    }

    public static int recipeMargin() {
        return Constants.RECIPE_MARGIN;
    }
}
