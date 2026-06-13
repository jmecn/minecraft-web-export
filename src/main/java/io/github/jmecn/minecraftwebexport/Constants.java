package io.github.jmecn.minecraftwebexport;

import java.util.Set;
import java.util.regex.Pattern;

public final class Constants {

    public static final String MOD_ID = "minecraft_web_export";

    public static final int BUNDLE_SCHEMA = 2;
    public static final int RECIPE_META_SCHEMA = 1;
    public static final int ITEM_INDEX_SCHEMA = 1;
    public static final int ITEM_DETAIL_SCHEMA = 2;
    public static final int ITEMS_LANG_SCHEMA = 2;
    public static final int CATEGORIES_INDEX_SCHEMA = 2;
    public static final int TAGS_CATALOG_SCHEMA = 1;
    public static final int LAYOUT_PACK_SCHEMA = 2;

    public static final String FLUID_REGISTRY_IDS_KEY = "fluidRegistryIds";

    public static final String EMI_ROOT = "emi";
    public static final String BUNDLE_FILE = "bundle.json";
    public static final String RECIPES_DIR = "recipes";
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
    public static final String ITEMS_LANG_DIR = "items-lang";

    public static final String EXPORT_DIR = "export";
    public static final String EXPORT_NAME = "minecraft-web-export";

    public static final String DEFAULT_LANGUAGE = "en_us";
    public static final String MISSING_ICON_REGISTRY_ID = "minecraft_web_export:missing_icon";

    public static final int RECIPE_MARGIN = 8;
    public static final int PANEL_MARGIN = 4;
    public static final int DEFAULT_RECIPE_LAYOUT_SCALE = 2;

    public static final int DEFAULT_ICON_SIZE = 32;
    public static final int DEFAULT_CATEGORY_ICON_SIZE = 32;
    public static final int DEFAULT_ATLAS_MAX_SIZE = 2048;
    public static final int ICON_HASH_LEN = 16;
    public static final int FLUID_GUI_SIZE = 16;
    public static final int ICON_FLUSH_RENDER_EVERY = 256;

    public static final int PLACEHOLDER_MAGENTA = 0xFFFF00FF;
    public static final int PLACEHOLDER_BLACK = 0xFF000000;

    public static final int PROGRESS_LOG_TARGET_LINES = 30;
    public static final int SEARCH_INDEX_PROGRESS_EVERY = 5000;
    public static final int DETAIL_FAILURE_LIMIT = 20;

    public static final int HEARTBEAT_TICKS = 200;
    public static final int CI_DEFAULT_EXPORT_WORLD_DELAY_TICKS = 600;
    public static final int CI_DEFAULT_EXPORT_TIMEOUT_SECONDS = 3600;
    public static final String CI_DEFAULT_EXPORT_WORLD_NAME = "emi-export";

    public static final String COMMAND_LOG_PREFIX = "[minecraft-web-export] ";

    public static final String LOG_TAG_EMI = "[emi]";
    public static final String LOG_TAG_EMI_LAYOUT = "[emi-layout]";
    public static final String LOG_TAG_EMI_ITEMS = "[emi-items]";
    public static final String LOG_TAG_ICONS = "[icons]";
    public static final String LOG_TAG_LANG = "[lang]";
    public static final String LOG_TAG_TAGS = "[tags]";
    public static final String LOG_TAG_INDEX = "[index]";
    public static final String LOG_TAG_RECIPE_TEXTURES = "[recipe-textures]";
    public static final String LOG_TAG_ITEMS_LANG = "[items-lang]";
    public static final String LOG_TAG_ITEM_NAME_KEYS = "[item-name-keys]";

    public static final Set<String> DEFAULT_EXCLUDED_NAMESPACES = Set.of("additionalplacements");

    public static final Pattern FLUID_NBT_NAME_PATTERN = Pattern.compile("FluidName:\"([^\"]+)\"");

    private Constants() {}
}
