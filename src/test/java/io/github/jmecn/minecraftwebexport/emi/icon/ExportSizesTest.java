package io.github.jmecn.minecraftwebexport.emi.icon;

import io.github.jmecn.minecraftwebexport.config.MweConfig;
import io.github.jmecn.minecraftwebexport.config.MweConfigTestSupport;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExportSizesTest {

    @AfterEach
    void clearConfig() {
        MweConfig.clearForTests();
    }

    @Test
    void defaultsToThirtyTwoPixelIcons() {
        MweConfig.ensureForTests();
        assertEquals(32, AtlasBuilder.iconCellSize());
    }

    @Test
    void respectsUnifiedOverride() {
        MweConfigTestSupport.apply(Map.of("icons.iconSize", 48));
        assertEquals(48, AtlasBuilder.iconCellSize());
    }

    @Test
    void resolveIconCellSizePrefersUnifiedSize() {
        assertEquals(48, AtlasBuilder.resolveIconCellSize(48, 16, 16, 16));
    }
}
