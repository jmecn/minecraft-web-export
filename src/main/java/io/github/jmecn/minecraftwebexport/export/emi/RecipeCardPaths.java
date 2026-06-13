package io.github.jmecn.minecraftwebexport.export.emi;

import java.nio.file.Path;

public final class RecipeCardPaths {

    public static final int META_SCHEMA = 1;
    public static final int BUNDLE_SCHEMA = 2;
    public static final String RECIPES_DIR = "recipes";
    private static final int EMI_RECIPE_MARGIN = 8;

    private RecipeCardPaths() {
    }

    public static RecipeIndexIds.RecipeIdParts requireParts(String recipeId) {
        RecipeIndexIds.RecipeIdParts parts = RecipeIndexIds.splitRecipeId(recipeId);
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
        RecipeIndexIds.RecipeIdParts parts = requireParts(recipeId);
        return EmiBundlePaths.resolve(
                exportRoot,
                RECIPES_DIR + "/" + parts.namespace() + "/" + pathSafe(parts.path()) + ".png");
    }

    public static Path metaPath(Path exportRoot, String recipeId) {
        RecipeIndexIds.RecipeIdParts parts = requireParts(recipeId);
        return EmiBundlePaths.resolve(
                exportRoot,
                RECIPES_DIR + "/" + parts.namespace() + "/" + pathSafe(parts.path()) + ".json");
    }

    public static int recipeMargin() {
        return EMI_RECIPE_MARGIN;
    }
}
