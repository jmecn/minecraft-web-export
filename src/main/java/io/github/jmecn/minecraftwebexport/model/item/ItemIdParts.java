package io.github.jmecn.minecraftwebexport.model.item;

import io.github.jmecn.minecraftwebexport.emi.EmiPaths;
import java.nio.file.Path;

public record ItemIdParts(String namespace, String path) {

    public static ItemIdParts parse(String fullId) {
        if (fullId == null || fullId.isBlank()) {
            return null;
        }
        int sep = fullId.indexOf(':');
        if (sep <= 0 || sep >= fullId.length() - 1) {
            return null;
        }
        return new ItemIdParts(fullId.substring(0, sep), fullId.substring(sep + 1));
    }

    public Path toItemFile(Path outputDir) {
        return EmiPaths.resolve(outputDir, "items/" + namespace + "/" + path + ".json");
    }
}
