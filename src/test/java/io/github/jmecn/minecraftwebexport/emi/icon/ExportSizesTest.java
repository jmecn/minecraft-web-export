package io.github.jmecn.minecraftwebexport.emi.icon;

import io.github.jmecn.minecraftwebexport.Constants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExportSizesTest {

    @Test
    void defaultsToThirtyTwoPixelIcons() {
        System.clearProperty(Constants.PROP_ICON_SIZE);
        System.clearProperty(Constants.PROP_ITEM_ICON_SIZE);
        System.clearProperty(Constants.PROP_BLOCK_ITEM_ICON_SIZE);
        System.clearProperty(Constants.PROP_FLUID_ICON_SIZE);

        assertEquals(32, ExportSizes.iconCellSize());
    }

    @Test
    void respectsUnifiedOverride() {
        System.setProperty(Constants.PROP_ICON_SIZE, "48");
        try {
            assertEquals(48, ExportSizes.iconCellSize());
        } finally {
            System.clearProperty(Constants.PROP_ICON_SIZE);
        }
    }
}
