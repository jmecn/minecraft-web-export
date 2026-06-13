package io.github.jmecn.minecraftwebexport.emi.icon;
import io.github.jmecn.minecraftwebexport.emi.icon.ExportSizes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExportSizesTest {

    @Test
    void defaultsToThirtyTwoPixelIcons() {
        System.clearProperty("minecraftWebExport.iconSize");
        System.clearProperty("minecraftWebExport.itemIconSize");
        System.clearProperty("minecraftWebExport.blockItemIconSize");
        System.clearProperty("minecraftWebExport.fluidIconSize");

        assertEquals(32, ExportSizes.iconCellSize());
    }

    @Test
    void respectsUnifiedOverride() {
        System.setProperty("minecraftWebExport.iconSize", "48");
        try {
            assertEquals(48, ExportSizes.iconCellSize());
        } finally {
            System.clearProperty("minecraftWebExport.iconSize");
        }
    }
}
