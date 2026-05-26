package io.github.jmecn.minecraftwebexport.export;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

public record ExportOutputPaths(Path rootDir, Path manifestFile, Path bundleFile) {

    private static final String EXPORT_DIR = "export";
    private static final String EXPORT_NAME = "minecraft-web-export";
    private static final String MANIFEST_FILE = "manifest.json";
    private static final String BUNDLE_FILE = "bundle.json";

    public ExportOutputPaths {
        rootDir = Objects.requireNonNull(rootDir, "rootDir").toAbsolutePath().normalize();
        manifestFile = Objects.requireNonNull(manifestFile, "manifestFile").toAbsolutePath().normalize();
        bundleFile = Objects.requireNonNull(bundleFile, "bundleFile").toAbsolutePath().normalize();
    }

    public static ExportOutputPaths resolve(Path gameDirectory, String explicitOutputRoot) {
        Path root = isBlank(explicitOutputRoot)
                ? gameDirectory.resolve(EXPORT_DIR).resolve(EXPORT_NAME)
                : Path.of(explicitOutputRoot);

        root = root.toAbsolutePath().normalize();
        return new ExportOutputPaths(
                root,
                root.resolve(MANIFEST_FILE),
                root.resolve(BUNDLE_FILE));
    }

    public void prepareDirectories() throws IOException {
        java.nio.file.Files.createDirectories(rootDir);
    }

    public String relativeBundlePath() {
        return rootDir.relativize(bundleFile).toString().replace('\\', '/');
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
