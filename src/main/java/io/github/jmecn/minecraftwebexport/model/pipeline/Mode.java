package io.github.jmecn.minecraftwebexport.model.pipeline;

import io.github.jmecn.minecraftwebexport.Constants;
import java.util.Locale;

public enum Mode {
    FULL,
    SCOPED;

    public static Mode current() {
        String prop = System.getProperty(Constants.PROP_EXPORT_MODE, "full")
                .trim()
                .toLowerCase(Locale.ROOT);
        return switch (prop) {
            case "scoped", "closure" -> SCOPED;
            default -> FULL;
        };
    }
}
