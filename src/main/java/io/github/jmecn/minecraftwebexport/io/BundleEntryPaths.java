package io.github.jmecn.minecraftwebexport.io;

import io.github.jmecn.minecraftwebexport.Constants;
import java.nio.file.Path;

public final class BundleEntryPaths {

    private BundleEntryPaths() {
    }

    public static Path bundleRoot(Path exportRoot) {
        return exportRoot.resolve(Constants.EMI_ROOT);
    }

    public static Path zipFile(Path exportRoot) {
        return exportRoot.resolve(Constants.EMI_ZIP_FILE);
    }

    public static String entryName(Path bundleRoot, Path bundleAbsolutePath) {
        Path normalized = bundleAbsolutePath.normalize();
        Path relative = bundleRoot.normalize().relativize(normalized);
        String name = relative.toString().replace('\\', '/');
        if (name.isEmpty() || name.startsWith("..") || name.contains("/../")) {
            throw new IllegalArgumentException("entry outside bundle root: " + bundleAbsolutePath);
        }
        return name;
    }
}
