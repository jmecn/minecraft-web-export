package io.github.jmecn.minecraftwebexport.export.emi;

import java.nio.file.Path;

public final class EmiBundlePaths {

    public static final String ROOT = "emi";
    public static final String BUNDLE_FILE = ROOT + "/bundle.json";

    public static final String RECIPES_DIR = ROOT + "/recipes/layouts";
    public static final String RECIPE_INDEX_FILE = ROOT + "/recipes/index.json";

    public static final String CHROME_DIR = ROOT + "/chrome";
    public static final String TEXTURES_DIR = ROOT + "/textures";
    public static final String TEXTURE_MANIFEST_FILE = "manifest.json";

    public static final String ICONS_DIR = ROOT + "/icons";
    public static final String ITEMS_INDEX_FILE = ROOT + "/items/index.json";
    public static final String TAG_MEMBERS_FILE = ROOT + "/tags/members.json";
    public static final String LANG_DIR = ROOT + "/lang";

    public static final String DEFAULT_LANGUAGE = "en_us";

    private EmiBundlePaths() {
    }

    public static Path resolve(Path exportRoot, String relative) {
        return exportRoot.resolve(relative.replace('/', java.io.File.separatorChar));
    }

    public static Path langFile(Path exportRoot, String locale) {
        return resolve(exportRoot, LANG_DIR + "/" + locale + ".json");
    }

    public static String relativeLayoutJson(String recipeId) {
        return RecipeLayoutPaths.safeFileName(recipeId) + ".json";
    }
}
