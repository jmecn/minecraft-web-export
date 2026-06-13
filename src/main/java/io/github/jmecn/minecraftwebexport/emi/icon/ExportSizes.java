package io.github.jmecn.minecraftwebexport.emi.icon;

import io.github.jmecn.minecraftwebexport.Constants;

public final class ExportSizes {

    private ExportSizes() {}

    public static int iconCellSize() {
        Integer unified = Integer.getInteger(Constants.PROP_ICON_SIZE);
        if (unified != null) {
            return boundedSize(unified, Constants.PROP_ICON_SIZE);
        }
        if (System.getProperty(Constants.PROP_ITEM_ICON_SIZE) != null) {
            return boundedSize(Integer.getInteger(Constants.PROP_ITEM_ICON_SIZE), Constants.PROP_ITEM_ICON_SIZE);
        }
        if (System.getProperty(Constants.PROP_BLOCK_ITEM_ICON_SIZE) != null) {
            return boundedSize(Integer.getInteger(Constants.PROP_BLOCK_ITEM_ICON_SIZE), Constants.PROP_BLOCK_ITEM_ICON_SIZE);
        }
        if (System.getProperty(Constants.PROP_FLUID_ICON_SIZE) != null) {
            return boundedSize(Integer.getInteger(Constants.PROP_FLUID_ICON_SIZE), Constants.PROP_FLUID_ICON_SIZE);
        }
        return Constants.DEFAULT_ICON_SIZE;
    }

    public static int categoryIconCellSize() {
        int size = Integer.getInteger(Constants.PROP_CATEGORY_ICON_SIZE, Constants.DEFAULT_CATEGORY_ICON_SIZE);
        return boundedSize(size, Constants.PROP_CATEGORY_ICON_SIZE);
    }

    public static int atlasMaxSize() {
        int size = Integer.getInteger(Constants.PROP_ITEM_ICON_ATLAS_MAX_SIZE, Constants.DEFAULT_ATLAS_MAX_SIZE);
        if (size < 256 || size > 8192) {
            throw new IllegalArgumentException(Constants.PROP_ITEM_ICON_ATLAS_MAX_SIZE + " must be 256..8192, got " + size);
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
