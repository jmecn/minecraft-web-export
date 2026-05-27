package io.github.jmecn.minecraftwebexport.export.emi;

public final class RecipeLayoutPaths {

    public static final int SCHEMA_VERSION = 2;

    public static final String LAYOUTS_DIR = EmiBundlePaths.RECIPES_DIR;
    public static final String LAYOUT_INDEX_FILE = EmiBundlePaths.RECIPE_INDEX_FILE;

    public static final String CHROME_DIR = EmiBundlePaths.CHROME_DIR;

    public static final String TEXTURES_DIR = EmiBundlePaths.TEXTURES_DIR;
    public static final String TEXTURE_MANIFEST_FILE = EmiBundlePaths.TEXTURE_MANIFEST_FILE;

    private RecipeLayoutPaths() {
    }

    public static String safeFileName(String recipeId) {
        if (recipeId == null || recipeId.isEmpty()) {
            return "unknown";
        }
        return recipeId.replace(':', '_').replace('/', '_');
    }

    public static String relativeLayoutJson(String recipeId) {
        return EmiBundlePaths.relativeLayoutJson(recipeId);
    }
}
