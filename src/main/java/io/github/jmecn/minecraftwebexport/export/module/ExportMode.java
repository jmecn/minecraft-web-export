package io.github.jmecn.minecraftwebexport.export.module;

import java.util.Locale;

public enum ExportMode {
    FULL,
    SCOPED;

    public static ExportMode current() {
        String prop = System.getProperty("minecraftWebExport.exportMode", "full")
                .trim()
                .toLowerCase(Locale.ROOT);
        return switch (prop) {
            case "scoped", "closure" -> SCOPED;
            default -> FULL;
        };
    }
}
