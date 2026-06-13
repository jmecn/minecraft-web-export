package io.github.jmecn.minecraftwebexport.emi.support;

import io.github.jmecn.minecraftwebexport.mod.MinecraftWebExportMod;

public final class Log {

    public static final String EMI = "[emi]";
    public static final String EMI_LAYOUT = "[emi-layout]";
    public static final String EMI_ITEMS = "[emi-items]";
    public static final String ICONS = "[icons]";
    public static final String LANG = "[lang]";
    public static final String TAGS = "[tags]";
    public static final String INDEX_TAGS = "[index]";
    public static final String RECIPE_TEXTURES = "[recipe-textures]";
    public static final String ITEMS_LANG = "[items-lang]";
    public static final String ITEM_NAME_KEYS = "[item-name-keys]";

    public static final int DETAIL_FAILURE_LIMIT = 20;

    private static final boolean DETAIL_FAILURES_ENABLED =
            Boolean.getBoolean("minecraftWebExport.export.logDetailFailures");

    private Log() {}

    public static void detailFailure(int failureCount, String message, Object... args) {
        if (failureCount > DETAIL_FAILURE_LIMIT) {
            return;
        }
        if (DETAIL_FAILURES_ENABLED) {
            MinecraftWebExportMod.LOGGER.warn(message, args);
        } else if (MinecraftWebExportMod.LOGGER.isDebugEnabled()) {
            MinecraftWebExportMod.LOGGER.debug(message, args);
        }
    }
}
