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
    public static final int CONTAINER_SCHEMA = 1;

    public static final String FLUID_REGISTRY_IDS_KEY = "fluidRegistryIds";

    public static final String EMI_ROOT = "emi";
    public static final String BUNDLE_FILE = "bundle.json";
    public static final String RECIPES_DIR = "recipes";
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

    public static final String EXPORT_DIR = "export";
    public static final String EXPORT_NAME = "minecraft-web-export";

    public static final String DEFAULT_LANGUAGE = "en_us";
    public static final String MISSING_ICON_REGISTRY_ID = "minecraft_web_export:missing_icon";

    public static final int RECIPE_MARGIN = 8;
    public static final int PANEL_MARGIN = 4;
    public static final int DEFAULT_RECIPE_LAYOUT_SCALE = 2;
    public static final int DEFAULT_PACK_MAX_BYTES = 262144;

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
    public static final int CLIENT_DEFAULT_WARMUP_TICKS = 100;
    public static final int CI_DEFAULT_EXPORT_WARMUP_TICKS = 40;
    public static final int CI_DEFAULT_EXPORT_WORLD_DELAY_TICKS = 600;
    public static final int CI_DEFAULT_EXPORT_TIMEOUT_SECONDS = 7200;
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

    public static final String PROP_EXPORT_ENABLED = "minecraftWebExport.export.enabled";
    public static final String PROP_EXPORT_OUTPUT_DIR = "minecraftWebExport.export.outputDir";
    public static final String PROP_RUN_EXPORT_AND_EXIT = "minecraftWebExport.runExportAndExit";
    public static final String PROP_EXPORT_TIMEOUT_SECONDS = "minecraftWebExport.exportTimeoutSeconds";
    public static final String PROP_EXPORT_WORLD_NAME = "minecraftWebExport.exportWorldName";
    public static final String PROP_EXPORT_WORLD_DELAY_TICKS = "minecraftWebExport.exportWorldDelayTicks";
    public static final String PROP_EXPORT_WARMUP_TICKS = "minecraftWebExport.exportWarmupTicks";
    public static final String PROP_EXPORT_MODE = "minecraftWebExport.exportMode";
    public static final String PROP_EXPORT_LANGUAGES = "minecraftWebExport.exportLanguages";
    public static final String PROP_EXPORT_EXCLUDED_NAMESPACES = "minecraftWebExport.exportExcludedNamespaces";
    public static final String PROP_LOG_DETAIL_FAILURES = "minecraftWebExport.export.logDetailFailures";

    public static final String PROP_ICON_LOG_STRIDE = "minecraftWebExport.iconLogStride";
    public static final String PROP_ITEMS_INDEX_SCAN_LOG_STRIDE = "minecraftWebExport.itemsIndexScanLogStride";
    public static final String PROP_ITEMS_INDEX_WRITE_LOG_STRIDE = "minecraftWebExport.itemsIndexWriteLogStride";
    public static final String PROP_RECIPE_CARD_LOG_STRIDE = "minecraftWebExport.recipeCardLogStride";
    public static final String PROP_LAYOUT_LOG_STRIDE = "minecraftWebExport.layoutLogStride";

    public static final String PROP_SKIP_ITEM_ICON_EXPORT = "minecraftWebExport.skipItemIconExport";
    public static final String PROP_SKIP_FLUID_ICON_EXPORT = "minecraftWebExport.skipFluidIconExport";
    public static final String PROP_SKIP_LANG_EXPORT = "minecraftWebExport.skipLangExport";
    public static final String PROP_SKIP_LANG_PRUNE_EXPORT = "minecraftWebExport.skipLangPruneExport";
    public static final String PROP_SKIP_CATEGORY_ICON_EXPORT = "minecraftWebExport.skipCategoryIconExport";
    public static final String PROP_SKIP_EMI_VISIBILITY_FILTER = "minecraftWebExport.skipEmiVisibilityFilter";
    public static final String PROP_SKIP_ITEM_NAME_KEYS_EXPORT = "minecraftWebExport.skipItemNameKeysExport";
    public static final String PROP_SKIP_TAG_MEMBERS_INDEX_EXPORT = "minecraftWebExport.skipTagMembersIndexExport";
    public static final String PROP_SKIP_ITEMS_SEARCH_EXPORT = "minecraftWebExport.skipItemsSearchExport";
    public static final String PROP_SKIP_EMI_LAYOUT_EXPORT = "minecraftWebExport.skipEmiLayoutExport";

    public static final String PROP_EXPORT_EMI_LAYOUT = "minecraftWebExport.exportEmiLayout";
    public static final String PROP_RECIPE_LAYOUT_SCALE = "minecraftWebExport.recipeLayoutScale";
    public static final String PROP_PACK_MAX_BYTES = "minecraftWebExport.packMaxBytes";

    public static final String PROP_ICON_SIZE = "minecraftWebExport.iconSize";
    public static final String PROP_ITEM_ICON_SIZE = "minecraftWebExport.itemIconSize";
    public static final String PROP_BLOCK_ITEM_ICON_SIZE = "minecraftWebExport.blockItemIconSize";
    public static final String PROP_FLUID_ICON_SIZE = "minecraftWebExport.fluidIconSize";
    public static final String PROP_CATEGORY_ICON_SIZE = "minecraftWebExport.categoryIconSize";
    public static final String PROP_ITEM_ICON_ATLAS_MAX_SIZE = "minecraftWebExport.itemIconAtlasMaxSize";

    private Constants() {}
}
