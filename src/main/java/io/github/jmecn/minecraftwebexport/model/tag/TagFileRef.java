package io.github.jmecn.minecraftwebexport.model.tag;

import io.github.jmecn.minecraftwebexport.enums.tag.TagKind;
import java.nio.file.Path;

public record TagFileRef(TagKind kind, String namespace, String path) {

    public static TagFileRef fromPath(Path tagsRoot, Path file) {
        Path relative = tagsRoot.relativize(file);
        if (relative.getNameCount() < 3) {
            return null;
        }
        String namespace = relative.getName(0).toString();
        String kindName = relative.getName(1).toString();
        TagKind kind = switch (kindName) {
            case "items" -> TagKind.ITEMS;
            case "blocks" -> TagKind.BLOCKS;
            case "fluids" -> TagKind.FLUIDS;
            default -> null;
        };
        if (kind == null) {
            return null;
        }
        Path pathPart = relative.subpath(2, relative.getNameCount());
        String path = pathPart.toString().replace('\\', '/');
        if (!path.endsWith(".json")) {
            return null;
        }
        path = path.substring(0, path.length() - ".json".length());
        return new TagFileRef(kind, namespace, path);
    }

    public String tagId() {
        return namespace + ":" + path;
    }
}
