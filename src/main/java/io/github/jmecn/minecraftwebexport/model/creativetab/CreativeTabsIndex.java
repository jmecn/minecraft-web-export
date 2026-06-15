package io.github.jmecn.minecraftwebexport.model.creativetab;

import io.github.jmecn.minecraftwebexport.Constants;
import java.util.List;

public record CreativeTabsIndex(int schema, int iconCellSize, String iconsDir, List<CreativeTabEntry> tabs) {

    public CreativeTabsIndex {
        tabs = List.copyOf(tabs == null ? List.of() : tabs);
    }

    public static CreativeTabsIndex of(int iconCellSize, String iconsDir, List<CreativeTabEntry> tabs) {
        return new CreativeTabsIndex(Constants.CREATIVE_TABS_INDEX_SCHEMA, iconCellSize, iconsDir, tabs);
    }
}
