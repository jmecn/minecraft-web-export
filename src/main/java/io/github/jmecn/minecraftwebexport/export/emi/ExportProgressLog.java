package io.github.jmecn.minecraftwebexport.export.emi;

final class ExportProgressLog {

    private static final int TARGET_LINES = 30;

    private ExportProgressLog() {}

    static int stride(int total, String propertyName, int smallBatch, int mediumBatch) {
        String prop = System.getProperty(propertyName, "").trim();
        if (!prop.isEmpty()) {
            return Math.max(1, Integer.parseInt(prop));
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
        return Math.max(2_000, (total + TARGET_LINES - 1) / TARGET_LINES);
    }

    static boolean shouldLog(int progress, int total, int stride) {
        return progress == total || progress % stride == 0;
    }

    static int percent(int progress, int total) {
        return total > 0 ? (progress * 100 / total) : 100;
    }
}
