package io.github.jmecn.minecraftwebexport.emi.bundle;

import java.nio.file.Path;

public final class Paths {

    public static final String ROOT = "emi";

    public static final String BUNDLE_FILE = "bundle.json";
    public static final String RECIPES_ROUTES_DIR = "recipes/routes";
    public static final String RECIPES_LAYOUT_PACKS_DIR = "recipes/layout-packs";
    public static final String CHROME_DIR = "chrome";
    public static final String TEXTURES_DIR = "textures";
    public static final String TEXTURE_MANIFEST_FILE = "manifest.json";
    public static final String ICONS_DIR = "icons";
    public static final String ITEMS_INDEX_FILE = "items/index.json";

    public static final String ITEM_NAME_KEYS_FILE = "items/name-keys.json";
    public static final String CATEGORIES_INDEX_FILE = "categories/index.json";

    public static final String CATEGORY_ICONS_DIR = "categories/icons";
    public static final String TAGS_DIR = "tags";
    public static final String TAGS_INDEX_FILE = "tags/index.json";
    public static final String LANG_DIR = "lang";

    public static final String COMPOSE_LANG_DIR = ".compose-lang";
    public static final String ITEMS_LANG_DIR = "items-lang";

    public static final String DEFAULT_LANGUAGE = "en_us";

    private Paths() {
    }

    public static Path resolve(Path exportRoot, String bundleRelative) {
        return exportRoot.resolve(ROOT).resolve(bundleRelative.replace('/', java.io.File.separatorChar));
    }

    public static Path langFile(Path exportRoot, String locale) {
        return resolve(exportRoot, LANG_DIR + "/" + locale + ".json");
    }
}
