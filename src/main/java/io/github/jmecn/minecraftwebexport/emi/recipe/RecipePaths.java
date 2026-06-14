package io.github.jmecn.minecraftwebexport.emi.recipe;

import io.github.jmecn.minecraftwebexport.Constants;
import io.github.jmecn.minecraftwebexport.emi.EmiPaths;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

public final class RecipePaths {

    private RecipePaths() {
    }

    public record RecipeIdParts(String namespace, String path) {
    }

    public static RecipeIdParts splitRecipeId(String recipeId) {
        if (recipeId == null || recipeId.isBlank()) {
            return null;
        }
        int sep = recipeId.indexOf(':');
        if (sep <= 0 || sep >= recipeId.length() - 1) {
            return null;
        }
        return new RecipeIdParts(recipeId.substring(0, sep), recipeId.substring(sep + 1));
    }

    public static RecipeIdParts requireParts(String recipeId) {
        RecipeIdParts parts = splitRecipeId(recipeId);
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

    public static void ensureRecipeDirectories(Path exportRoot, Set<String> recipeIds) throws IOException {
        if (recipeIds == null || recipeIds.isEmpty()) {
            return;
        }
        Set<String> namespaces = new java.util.TreeSet<>();
        for (String recipeId : recipeIds) {
            RecipeIdParts parts = splitRecipeId(recipeId);
            if (parts != null) {
                namespaces.add(parts.namespace());
            }
        }
        for (String namespace : namespaces) {
            Files.createDirectories(EmiPaths.resolve(exportRoot, Constants.RECIPES_DIR + "/" + namespace));
        }
    }

    public static Path pngPath(Path exportRoot, String recipeId) {
        RecipeIdParts parts = requireParts(recipeId);
        return EmiPaths.resolve(
                exportRoot,
                Constants.RECIPES_DIR + "/" + parts.namespace() + "/" + pathSafe(parts.path()) + ".png");
    }

    public static Path metaPath(Path exportRoot, String recipeId) {
        RecipeIdParts parts = requireParts(recipeId);
        return EmiPaths.resolve(
                exportRoot,
                Constants.RECIPES_DIR + "/" + parts.namespace() + "/" + pathSafe(parts.path()) + ".json");
    }

    public static int recipeMargin() {
        return Constants.RECIPE_MARGIN;
    }
}
