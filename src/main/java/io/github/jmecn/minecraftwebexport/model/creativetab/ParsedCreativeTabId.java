package io.github.jmecn.minecraftwebexport.model.creativetab;

import io.github.jmecn.minecraftwebexport.Constants;
import io.github.jmecn.minecraftwebexport.emi.EmiPaths;
import java.nio.file.Path;

public record ParsedCreativeTabId(String namespace, String path) {

    public static ParsedCreativeTabId parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String[] parts = raw.trim().split(":", 2);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            return null;
        }
        return new ParsedCreativeTabId(parts[0], parts[1]);
    }

    public String tabId() {
        return namespace + ":" + path;
    }

    public Path toMemberFilePath(Path outputDir) {
        return EmiPaths.resolve(
                outputDir, Constants.CREATIVE_TABS_DIR + "/" + namespace + "/" + path + ".json");
    }
}
