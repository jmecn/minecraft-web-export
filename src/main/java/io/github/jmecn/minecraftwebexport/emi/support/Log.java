package io.github.jmecn.minecraftwebexport.emi.support;

import io.github.jmecn.minecraftwebexport.Constants;
import io.github.jmecn.minecraftwebexport.config.MweConfig;
import io.github.jmecn.minecraftwebexport.MweMod;

public final class Log {

    public static final String EMI = Constants.LOG_TAG_EMI;
    public static final String EMI_LAYOUT = Constants.LOG_TAG_EMI_LAYOUT;
    public static final String EMI_ITEMS = Constants.LOG_TAG_EMI_ITEMS;
    public static final String ICONS = Constants.LOG_TAG_ICONS;
    public static final String LANG = Constants.LOG_TAG_LANG;
    public static final String TAGS = Constants.LOG_TAG_TAGS;
    public static final String INDEX_TAGS = Constants.LOG_TAG_INDEX;
    public static final String RECIPE_TEXTURES = Constants.LOG_TAG_RECIPE_TEXTURES;
    public static final String ITEMS_LANG = Constants.LOG_TAG_ITEMS_LANG;
    public static final String ITEM_NAME_KEYS = Constants.LOG_TAG_ITEM_NAME_KEYS;

    public static final int DETAIL_FAILURE_LIMIT = Constants.DETAIL_FAILURE_LIMIT;

    private Log() {}

    public static void detailFailure(int failureCount, String message, Object... args) {
        if (failureCount > DETAIL_FAILURE_LIMIT) {
            return;
        }
        if (MweConfig.logDetailFailures()) {
            MweMod.LOGGER.warn(message, args);
        } else if (MweMod.LOGGER.isDebugEnabled()) {
            MweMod.LOGGER.debug(message, args);
        }
    }
}
