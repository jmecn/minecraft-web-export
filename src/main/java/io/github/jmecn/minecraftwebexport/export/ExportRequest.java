package io.github.jmecn.minecraftwebexport.export;

import java.nio.file.Path;
import java.util.Objects;

public record ExportRequest(
        String modId,
        String minecraftVersion,
        String schemaVersion,
        String trigger,
        Path gameDirectory,
        String outputRootOverride) {

    public ExportRequest {
        modId = Objects.requireNonNull(modId, "modId");
        minecraftVersion = Objects.requireNonNull(minecraftVersion, "minecraftVersion");
        schemaVersion = Objects.requireNonNull(schemaVersion, "schemaVersion");
        trigger = Objects.requireNonNull(trigger, "trigger");
        gameDirectory = Objects.requireNonNull(gameDirectory, "gameDirectory").toAbsolutePath().normalize();
    }
}
