package io.github.jmecn.minecraftwebexport.runtime;

import io.github.jmecn.minecraftwebexport.config.MweConfig;

public final class CiProperties {

    private CiProperties() {}

    public static boolean exportEnabled() {
        return MweConfig.exportEnabled();
    }

    public static int exportWorldDelayTicks() {
        return MweConfig.worldDelayTicks();
    }

    public static int exportTimeoutSeconds() {
        return MweConfig.timeoutSeconds();
    }

    public static String exportWorldName() {
        return MweConfig.exportWorldName();
    }

    public static boolean timedOut(long startNanos) {
        return MweConfig.timedOut(startNanos);
    }
}
