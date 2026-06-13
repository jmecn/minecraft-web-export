package io.github.jmecn.minecraftwebexport.export.emi;

import io.github.jmecn.minecraftwebexport.mod.MinecraftWebExportMod;

final class ExportLog {

    static final String EMI = "[emi]";
    static final String EMI_LAYOUT = "[emi-layout]";
    static final String EMI_ITEMS = "[emi-items]";
    static final String ICONS = "[icons]";
    static final String LANG = "[lang]";
    static final String TAGS = "[tags]";
    static final String INDEX_TAGS = "[index]";
    static final String RECIPE_TEXTURES = "[recipe-textures]";
    static final String ITEMS_LANG = "[items-lang]";
    static final String ITEM_NAME_KEYS = "[item-name-keys]";

    static final int DETAIL_FAILURE_LIMIT = 20;

    private static final boolean DETAIL_FAILURES_ENABLED =
            Boolean.getBoolean("minecraftWebExport.export.logDetailFailures");

    private ExportLog() {}

    static void detailFailure(int failureCount, String message, Object... args) {
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
