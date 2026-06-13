package io.github.jmecn.minecraftwebexport.emi.support;

import io.github.jmecn.minecraftwebexport.Constants;

public final class ProgressLog {

    private ProgressLog() {}

    public static int stride(int total, int configuredStride, int smallBatch, int mediumBatch) {
        if (configuredStride > 0) {
            return configuredStride;
        }
        if (total <= 0) {
            return 1;
        }
        if (total <= 200) {
            return smallBatch;
        }
        if (total <= 2_000) {
            return mediumBatch;
        }
        return Math.max(2_000, (total + Constants.PROGRESS_LOG_TARGET_LINES - 1) / Constants.PROGRESS_LOG_TARGET_LINES);
    }

    public static boolean shouldLog(int progress, int total, int stride) {
        return progress == total || progress % stride == 0;
    }

    public static int percent(int progress, int total) {
        return total > 0 ? (progress * 100 / total) : 100;
    }
}
