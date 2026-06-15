package io.github.jmecn.minecraftwebexport.io;

import io.github.jmecn.minecraftwebexport.config.MweConfig;
import java.nio.file.Path;

public final class BundleWriteStrategies {

    private BundleWriteStrategies() {
    }

    public static BundleWriteStrategy create(Path exportRoot, boolean compressed) {
        return compressed ? new ZipWriteStrategy() : new FileWriteStrategy();
    }

    public static BundleWriteStrategy fromConfig(Path exportRoot) {
        return create(exportRoot, MweConfig.exportCompressed());
    }
}
