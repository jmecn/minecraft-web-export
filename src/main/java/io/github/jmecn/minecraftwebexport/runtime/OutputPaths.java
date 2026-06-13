package io.github.jmecn.minecraftwebexport.runtime;
import io.github.jmecn.minecraftwebexport.Constants;
import io.github.jmecn.minecraftwebexport.pipeline.ModuleRegistry;

import java.nio.file.Path;
import java.util.Objects;

public record OutputPaths(Path rootDir) {

    public OutputPaths {
        rootDir = Objects.requireNonNull(rootDir, "rootDir").toAbsolutePath().normalize();
    }

    public static OutputPaths resolve(Path gameDirectory, String explicitOutputRoot) {
        Path root = isBlank(explicitOutputRoot)
                ? gameDirectory.resolve(Constants.EXPORT_DIR).resolve(Constants.EXPORT_NAME)
                : Path.of(explicitOutputRoot);
        return pathsForRoot(root);
    }

    public static OutputPaths resolveForRun(Path gameDirectory, String explicitOutputRoot) {
        if (!isBlank(explicitOutputRoot)) {
            return resolve(gameDirectory, explicitOutputRoot);
        }
        if (!ModuleRegistry.modules().isEmpty()) {
            return pathsForRoot(gameDirectory.resolve(Constants.EXPORT_DIR));
        }
        return resolve(gameDirectory, null);
    }

    private static OutputPaths pathsForRoot(Path root) {
        return new OutputPaths(root.toAbsolutePath().normalize());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
