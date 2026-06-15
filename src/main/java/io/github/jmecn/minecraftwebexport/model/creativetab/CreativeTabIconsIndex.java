package io.github.jmecn.minecraftwebexport.model.creativetab;

import io.github.jmecn.minecraftwebexport.Constants;
import io.github.jmecn.minecraftwebexport.model.icon.AtlasPage;
import io.github.jmecn.minecraftwebexport.model.icon.AtlasSpriteEntry;
import java.util.List;
import java.util.Map;

public record CreativeTabIconsIndex(
        int schema, int cellSize, List<AtlasPage> pages, Map<String, AtlasSpriteEntry> tabs) {

    public CreativeTabIconsIndex {
        pages = List.copyOf(pages == null ? List.of() : pages);
        tabs = Map.copyOf(tabs == null ? Map.of() : tabs);
    }

    public static CreativeTabIconsIndex of(
            int cellSize, List<AtlasPage> pages, Map<String, AtlasSpriteEntry> tabs) {
        return new CreativeTabIconsIndex(Constants.CREATIVE_TAB_ICONS_INDEX_SCHEMA, cellSize, pages, tabs);
    }
}
