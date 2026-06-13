package io.github.jmecn.minecraftwebexport.emi.recipe;

public final class IndexIds {

    private IndexIds() {
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
}
