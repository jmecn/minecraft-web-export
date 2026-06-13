package io.github.jmecn.minecraftwebexport.model.pipeline;

import io.github.jmecn.minecraftwebexport.config.MweConfig;

public enum Mode {
    FULL,
    SCOPED;

    public static Mode parse(String raw) {
        return switch (MweConfig.parseExportMode(raw)) {
            case "scoped" -> SCOPED;
            default -> FULL;
        };
    }

    public static Mode current() {
        return parse(MweConfig.exportMode());
    }
}
