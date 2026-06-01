package io.github.jmecn.minecraftwebexport.export.emi;

import org.apache.logging.log4j.Logger;

/**
 * Shared export log stage prefixes and bulk-export detail logging policy.
 *
 * <p>Per-entity failures during large exports are logged at {@code DEBUG} for the first
 * {@link #DETAIL_FAILURE_LIMIT} occurrences; summaries stay at {@code INFO}/{@code WARN}.
 * Enable with {@code -DminecraftWebExport.export.logDetailFailures=true} or logger DEBUG on
 * {@code io.github.jmecn.minecraftwebexport.export.emi}.</p>
 */
final class ExportLog {

    static final String EMI = "[emi]";
    static final String EMI_LAYOUT = "[emi-layout]";
    static final String EMI_ITEMS = "[emi-items]";
    static final String ICONS = "[icons]";
    static final String LANG = "[lang]";
    static final String TAGS = "[tags]";
    static final String INDEX_TAGS = "[index]";
    static final String RECIPE_TEXTURES = "[recipe-textures]";
    static final String ITEMS_SEARCH = "[items-search]";
    static final String ITEM_NAME_KEYS = "[item-name-keys]";

    /** Per-item failure lines at DEBUG beyond this count (summary lines still log at INFO/WARN). */
    static final int DETAIL_FAILURE_LIMIT = 20;

    private static final boolean DETAIL_FAILURES_ENABLED =
            Boolean.getBoolean("minecraftWebExport.export.logDetailFailures");

    private ExportLog() {}

    /**
     * Logs a per-entity export failure when {@link #DETAIL_FAILURES_ENABLED} is set (WARN, up to
     * {@link #DETAIL_FAILURE_LIMIT}) or when the logger has DEBUG enabled.
     */
    static void detailFailure(Logger logger, int failureCount, String message, Object... args) {
        if (failureCount > DETAIL_FAILURE_LIMIT) {
            return;
        }
        if (DETAIL_FAILURES_ENABLED) {
            logger.warn(message, args);
        } else if (logger.isDebugEnabled()) {
            logger.debug(message, args);
        }
    }
}
