package io.github.jmecn.minecraftwebexport.model.pipeline;

import java.nio.file.Path;
import java.util.Objects;

public record Scope(
        Path outputRoot,
        Path gameDirectory,
        Mode mode) {

    public Scope {
        outputRoot = Objects.requireNonNull(outputRoot, "outputRoot").toAbsolutePath().normalize();
        gameDirectory = Objects.requireNonNull(gameDirectory, "gameDirectory").toAbsolutePath().normalize();
        mode = Objects.requireNonNull(mode, "mode");
    }
}
