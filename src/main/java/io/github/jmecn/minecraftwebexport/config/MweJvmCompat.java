package io.github.jmecn.minecraftwebexport.config;

/**
 * Legacy {@code -DminecraftWebExport.*} JVM flags used by existing CI workflows.
 */
final class MweJvmCompat {

    static final String EXPORT_ENABLED = "minecraftWebExport.export.enabled";
    static final String EXPORT_OUTPUT_DIR = "minecraftWebExport.export.outputDir";
    static final String EXPORT_MODE = "minecraftWebExport.exportMode";
    static final String EXPORT_LANGUAGES = "minecraftWebExport.exportLanguages";
    static final String EXPORT_WORLD_NAME = "minecraftWebExport.exportWorldName";
    static final String EXPORT_EXCLUDED_NAMESPACES = "minecraftWebExport.exportExcludedNamespaces";
    static final String EXPORT_WORLD_DELAY_TICKS = "minecraftWebExport.exportWorldDelayTicks";
    static final String EXPORT_TIMEOUT_SECONDS = "minecraftWebExport.exportTimeoutSeconds";

    /** Legacy; ignored since export now starts as soon as EMI is ready. */
    static final String EXPORT_WARMUP_TICKS = "minecraftWebExport.exportWarmupTicks";

    private MweJvmCompat() {}

    static boolean has(String key) {
        return System.getProperty(key) != null;
    }

    static boolean getBoolean(String key) {
        return Boolean.parseBoolean(System.getProperty(key));
    }

    static String getString(String key) {
        String raw = System.getProperty(key);
        return raw == null ? "" : raw.trim();
    }

    static int getInt(String key, int fallback) {
        String raw = System.getProperty(key);
        if (raw == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
