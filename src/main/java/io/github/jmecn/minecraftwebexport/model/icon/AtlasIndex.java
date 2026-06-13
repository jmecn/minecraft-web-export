package io.github.jmecn.minecraftwebexport.model.icon;

import java.util.List;
import java.util.Map;

public record AtlasIndex(
        int schema,
        int cellSize,
        String sort,
        List<AtlasPage> pages,
        Map<String, AtlasSpriteEntry> items) {

    public AtlasIndex {
        pages = List.copyOf(pages == null ? List.of() : pages);
        items = Map.copyOf(items == null ? Map.of() : items);
    }

    public static AtlasIndex of(int cellSize, String sort, List<AtlasPage> pages, Map<String, AtlasSpriteEntry> items) {
        return new AtlasIndex(1, cellSize, sort, pages, items);
    }
}
