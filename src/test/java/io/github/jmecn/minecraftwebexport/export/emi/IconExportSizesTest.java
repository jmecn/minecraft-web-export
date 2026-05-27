package io.github.jmecn.minecraftwebexport.export.emi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IconExportSizesTest {

    @Test
    void defaultsToThirtyTwoPixelIcons() {
        System.clearProperty("minecraftWebExport.iconSize");
        System.clearProperty("minecraftWebExport.itemIconSize");
        System.clearProperty("minecraftWebExport.blockItemIconSize");
        System.clearProperty("minecraftWebExport.fluidIconSize");

        assertEquals(32, IconExportSizes.iconCellSize());
    }

    @Test
    void respectsUnifiedOverride() {
        System.setProperty("minecraftWebExport.iconSize", "48");
        try {
            assertEquals(48, IconExportSizes.iconCellSize());
        } finally {
            System.clearProperty("minecraftWebExport.iconSize");
        }
    }
}
