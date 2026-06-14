package io.github.jmecn.minecraftwebexport.model.recipe;

public record RecipeIdParts(String namespace, String path) {

    public static RecipeIdParts parse(String recipeId) {
        if (recipeId == null || recipeId.isBlank()) {
            return null;
        }
        int sep = recipeId.indexOf(':');
        if (sep <= 0 || sep >= recipeId.length() - 1) {
            return null;
        }
        return new RecipeIdParts(recipeId.substring(0, sep), recipeId.substring(sep + 1));
    }

    public static RecipeIdParts require(String recipeId) {
        RecipeIdParts parts = parse(recipeId);
        if (parts == null) {
            throw new IllegalArgumentException("invalid recipe id: " + recipeId);
        }
        return parts;
    }

    public String pathSafe() {
        if (path == null || path.isEmpty()) {
            return "unknown";
        }
        return path.replace('/', '_');
    }
}
