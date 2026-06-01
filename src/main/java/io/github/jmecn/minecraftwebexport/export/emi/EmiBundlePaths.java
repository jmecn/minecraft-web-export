package io.github.jmecn.minecraftwebexport.export.emi;

import java.nio.file.Path;

/**
 * EMI static bundle layout. Paths in JSON indexes are relative to the bundle root
 * ({@code <exportRoot>/emi/}), which is also {@code emi-recipe-renderer}'s {@code baseUrl}.
 */
public final class EmiBundlePaths {

    /** Directory name under the minecraft-web-export output root. */
    public static final String ROOT = "emi";

    /** Paths relative to the EMI bundle root (renderer {@code baseUrl}). */
    public static final String BUNDLE_FILE = "bundle.json";
    public static final String RECIPES_ROUTES_DIR = "recipes/routes";
    public static final String RECIPES_LAYOUT_PACKS_DIR = "recipes/layout-packs";
    public static final String CHROME_DIR = "chrome";
    public static final String TEXTURES_DIR = "textures";
    public static final String TEXTURE_MANIFEST_FILE = "manifest.json";
    public static final String ICONS_DIR = "icons";
    public static final String ITEMS_INDEX_FILE = "items/index.json";
    public static final String CATEGORIES_INDEX_FILE = "categories/index.json";
    /** Dedicated 16×16 EMI category tab sprites (see {@link EmiCategoryIconsExporter}). */
    public static final String CATEGORY_ICONS_DIR = "categories/icons";
    public static final String TAGS_DIR = "tags";
    public static final String TAGS_INDEX_FILE = "tags/index.json";
    public static final String LANG_DIR = "lang";
    public static final String ITEMS_SEARCH_DIR = "items-search";

    public static final String DEFAULT_LANGUAGE = "en_us";

    private EmiBundlePaths() {
    }

    /** Resolves a bundle-relative path under {@code <exportRoot>/emi/}. */
    public static Path resolve(Path exportRoot, String bundleRelative) {
        return exportRoot.resolve(ROOT).resolve(bundleRelative.replace('/', java.io.File.separatorChar));
    }

    public static Path langFile(Path exportRoot, String locale) {
        return resolve(exportRoot, LANG_DIR + "/" + locale + ".json");
    }
}
