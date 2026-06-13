package io.github.jmecn.minecraftwebexport.emi;

import io.github.jmecn.minecraftwebexport.Constants;
import java.io.File;
import java.nio.file.Path;

public final class EmiPaths {

    private EmiPaths() {
    }

    public static Path resolve(Path exportRoot, String emiRelative) {
        return exportRoot.resolve(Constants.EMI_ROOT).resolve(emiRelative.replace('/', File.separatorChar));
    }

    public static Path langFile(Path exportRoot, String locale) {
        return resolve(exportRoot, Constants.LANG_DIR + "/" + locale + ".json");
    }
}
