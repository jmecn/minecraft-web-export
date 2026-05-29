package io.github.jmecn.minecraftwebexport.export.module;

import java.util.Locale;

/**
 * {@link #FULL} exports all runtime EMI recipes (Phase 2 default).
 * {@link #SCOPED} merges {@link ExportModule} seeds, expands tag closure, and exports the subset.
 */
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
