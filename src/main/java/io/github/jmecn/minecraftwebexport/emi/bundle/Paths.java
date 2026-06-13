package io.github.jmecn.minecraftwebexport.emi.bundle;

import io.github.jmecn.minecraftwebexport.Constants;

import java.nio.file.Path;

public final class Paths {

    private Paths() {
    }

    public static Path resolve(Path exportRoot, String bundleRelative) {
        return exportRoot.resolve(Constants.EMI_ROOT).resolve(bundleRelative.replace('/', java.io.File.separatorChar));
    }

    public static Path langFile(Path exportRoot, String locale) {
        return resolve(exportRoot, Constants.LANG_DIR + "/" + locale + ".json");
    }
}
