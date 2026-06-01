package io.github.jmecn.minecraftwebexport.export.emi;

public final class IconExportSizes {

    private static final int DEFAULT_ICON = 32;
    private static final int DEFAULT_CATEGORY_ICON = 32;
    private static final int DEFAULT_ATLAS_MAX = 2048;

    private IconExportSizes() {
    }

    public static int iconCellSize() {
        Integer unified = Integer.getInteger("minecraftWebExport.iconSize");
        if (unified != null) {
            return boundedSize(unified, "minecraftWebExport.iconSize");
        }
        if (System.getProperty("minecraftWebExport.itemIconSize") != null) {
            return boundedSize(Integer.getInteger("minecraftWebExport.itemIconSize"), "minecraftWebExport.itemIconSize");
        }
        if (System.getProperty("minecraftWebExport.blockItemIconSize") != null) {
            return boundedSize(Integer.getInteger("minecraftWebExport.blockItemIconSize"), "minecraftWebExport.blockItemIconSize");
        }
        if (System.getProperty("minecraftWebExport.fluidIconSize") != null) {
            return boundedSize(Integer.getInteger("minecraftWebExport.fluidIconSize"), "minecraftWebExport.fluidIconSize");
        }
        return DEFAULT_ICON;
    }

    /** EMI recipe category tab icons (default export size 32×32 for web readability). */
    public static int categoryIconCellSize() {
        int size = Integer.getInteger("minecraftWebExport.categoryIconSize", DEFAULT_CATEGORY_ICON);
        return boundedSize(size, "minecraftWebExport.categoryIconSize");
    }

    public static int atlasMaxSize() {
        int size = Integer.getInteger("minecraftWebExport.itemIconAtlasMaxSize", DEFAULT_ATLAS_MAX);
        if (size < 256 || size > 8192) {
            throw new IllegalArgumentException("minecraftWebExport.itemIconAtlasMaxSize must be 256..8192, got " + size);
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
