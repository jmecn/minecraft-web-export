package io.github.jmecn.minecraftwebexport.export.ci;

/**
 * System properties for headless CI export ({@code -DminecraftWebExport.*}).
 */
public final class ExportCiProperties {

    public static final String RUN_EXPORT_AND_EXIT_PROPERTY = "minecraftWebExport.runExportAndExit";
    public static final String EXPORT_TIMEOUT_SECONDS_PROPERTY = "minecraftWebExport.exportTimeoutSeconds";
    public static final String EXPORT_WORLD_NAME_PROPERTY = "minecraftWebExport.exportWorldName";
    public static final String EXPORT_WORLD_DELAY_TICKS_PROPERTY = "minecraftWebExport.exportWorldDelayTicks";
    public static final String EXPORT_WARMUP_TICKS_PROPERTY = "minecraftWebExport.exportWarmupTicks";

    /** Extra ticks after {@link dev.emi.emi.runtime.EmiReloadManager#isLoaded()} before export. */
    private static final int DEFAULT_EXPORT_WARMUP_TICKS = 40;
    private static final int DEFAULT_EXPORT_WORLD_DELAY_TICKS = 600;
    private static final int DEFAULT_EXPORT_TIMEOUT_SECONDS = 7200;
    private static final String DEFAULT_EXPORT_WORLD_NAME = "emi-export";

    private ExportCiProperties() {}

    public static boolean runExportAndExit() {
        return Boolean.getBoolean(RUN_EXPORT_AND_EXIT_PROPERTY);
    }

    public static int exportWarmupTicks() {
        return Math.max(0, Integer.getInteger(EXPORT_WARMUP_TICKS_PROPERTY, DEFAULT_EXPORT_WARMUP_TICKS));
    }

    public static int exportWorldDelayTicks() {
        return Math.max(0, Integer.getInteger(EXPORT_WORLD_DELAY_TICKS_PROPERTY, DEFAULT_EXPORT_WORLD_DELAY_TICKS));
    }

    public static int exportTimeoutSeconds() {
        return Integer.getInteger(EXPORT_TIMEOUT_SECONDS_PROPERTY, DEFAULT_EXPORT_TIMEOUT_SECONDS);
    }

    public static String exportWorldName() {
        String raw = System.getProperty(EXPORT_WORLD_NAME_PROPERTY, DEFAULT_EXPORT_WORLD_NAME).trim();
        return raw.isEmpty() ? DEFAULT_EXPORT_WORLD_NAME : raw;
    }

    public static boolean timedOut(long startNanos) {
        int sec = exportTimeoutSeconds();
        if (sec <= 0) {
            return false;
        }
        return (System.nanoTime() - startNanos) >= sec * 1_000_000_000L;
    }
}
