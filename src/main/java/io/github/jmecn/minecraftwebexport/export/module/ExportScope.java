package io.github.jmecn.minecraftwebexport.export.module;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Context passed to {@link ExportModule} hooks during an export run.
 */
public record ExportScope(
        Path outputRoot,
        Path gameDirectory,
        ExportMode mode) {

    public ExportScope {
        outputRoot = Objects.requireNonNull(outputRoot, "outputRoot").toAbsolutePath().normalize();
        gameDirectory = Objects.requireNonNull(gameDirectory, "gameDirectory").toAbsolutePath().normalize();
        mode = Objects.requireNonNull(mode, "mode");
    }
}
