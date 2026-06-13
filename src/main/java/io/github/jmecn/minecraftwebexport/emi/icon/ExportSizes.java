package io.github.jmecn.minecraftwebexport.emi.icon;

import io.github.jmecn.minecraftwebexport.Constants;
import io.github.jmecn.minecraftwebexport.config.MweConfig;

public final class ExportSizes {

    private ExportSizes() {}

    public static int iconCellSize() {
        return resolveIconCellSize(
                MweConfig.iconSize(),
                MweConfig.itemIconSize(),
                MweConfig.blockItemIconSize(),
                MweConfig.fluidIconSize());
    }

    static int resolveIconCellSize(int unified, int item, int block, int fluid) {
        if (unified > 0) {
            return boundedSize(unified, "icons.iconSize");
        }
        if (item > 0) {
            return boundedSize(item, "icons.itemIconSize");
        }
        if (block > 0) {
            return boundedSize(block, "icons.blockItemIconSize");
        }
        if (fluid > 0) {
            return boundedSize(fluid, "icons.fluidIconSize");
        }
        return Constants.DEFAULT_ICON_SIZE;
    }

    public static int categoryIconCellSize() {
        return boundedSize(MweConfig.categoryIconSize(), "icons.categoryIconSize");
    }

    public static int atlasMaxSize() {
        int size = MweConfig.itemIconAtlasMaxSize();
        if (size < 256 || size > 8192) {
            throw new IllegalArgumentException("icons.itemIconAtlasMaxSize must be 256..8192, got " + size);
        }
        return size;
    }

    private static int boundedSize(int size, String property) {
        if (size < 8 || size > 256) {
            throw new IllegalArgumentException(property + " must be 8..256, got " + size);
        }
        return size;
    }
}
