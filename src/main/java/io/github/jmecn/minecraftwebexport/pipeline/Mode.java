package io.github.jmecn.minecraftwebexport.pipeline;

import java.util.Locale;

public enum Mode {
    FULL,
    SCOPED;

    public static Mode current() {
        String prop = System.getProperty("minecraftWebExport.exportMode", "full")
                .trim()
                .toLowerCase(Locale.ROOT);
        return switch (prop) {
            case "scoped", "closure" -> SCOPED;
            default -> FULL;
        };
    }
}
