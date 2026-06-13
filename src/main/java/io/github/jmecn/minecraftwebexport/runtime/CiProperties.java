package io.github.jmecn.minecraftwebexport.runtime;

import io.github.jmecn.minecraftwebexport.Constants;

public final class CiProperties {

    private CiProperties() {}

    public static boolean runExportAndExit() {
        return Boolean.getBoolean(Constants.PROP_RUN_EXPORT_AND_EXIT);
    }

    public static int exportWarmupTicks() {
        return Math.max(0, Integer.getInteger(Constants.PROP_EXPORT_WARMUP_TICKS, Constants.CI_DEFAULT_EXPORT_WARMUP_TICKS));
    }

    public static int exportWorldDelayTicks() {
        return Math.max(0, Integer.getInteger(Constants.PROP_EXPORT_WORLD_DELAY_TICKS, Constants.CI_DEFAULT_EXPORT_WORLD_DELAY_TICKS));
    }

    public static int exportTimeoutSeconds() {
        return Integer.getInteger(Constants.PROP_EXPORT_TIMEOUT_SECONDS, Constants.CI_DEFAULT_EXPORT_TIMEOUT_SECONDS);
    }

    public static String exportWorldName() {
        String raw = System.getProperty(Constants.PROP_EXPORT_WORLD_NAME, Constants.CI_DEFAULT_EXPORT_WORLD_NAME).trim();
        return raw.isEmpty() ? Constants.CI_DEFAULT_EXPORT_WORLD_NAME : raw;
    }

    public static boolean timedOut(long startNanos) {
        int sec = exportTimeoutSeconds();
        if (sec <= 0) {
            return false;
        }
        return (System.nanoTime() - startNanos) >= sec * 1_000_000_000L;
    }
}
