package io.github.jmecn.minecraftwebexport.model.tag;

import io.github.jmecn.minecraftwebexport.Constants;
import io.github.jmecn.minecraftwebexport.enums.tag.TagKind;
import io.github.jmecn.minecraftwebexport.emi.EmiPaths;
import java.nio.file.Path;

public record ParsedTagId(String namespace, String path) {

    public static ParsedTagId parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String[] parts = raw.trim().split(":", 2);
        if (parts.length != 2) {
            return null;
        }
        if (parts[0].isBlank() || parts[1].isBlank()) {
            return null;
        }
        return new ParsedTagId(parts[0], parts[1]);
    }

    public String tagId() {
        return namespace + ":" + path;
    }

    public Path toTagFilePath(Path outputDir, TagKind kind) {
        return EmiPaths.resolve(
                outputDir,
                Constants.TAGS_DIR + "/" + namespace + "/" + kind.dirName() + "/" + path + ".json");
    }
}
