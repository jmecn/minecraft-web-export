package io.github.jmecn.minecraftwebexport.runtime;

import io.github.jmecn.minecraftwebexport.config.MweConfig;
import io.github.jmecn.minecraftwebexport.config.MweConfigTestSupport;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MweConfigCiTest {

    @AfterEach
    void clearState() {
        System.clearProperty("minecraftWebExport.export.enabled");
        System.clearProperty("minecraftWebExport.exportWorldName");
        System.clearProperty("minecraftWebExport.exportWorldDelayTicks");
        System.clearProperty("minecraftWebExport.exportTimeoutSeconds");
        MweConfig.clearForTests();
    }

    @Test
    void defaultsToCommandOnlyMode() {
        MweConfig.ensureForTests();
        assertFalse(MweConfig.exportEnabled());
        assertEquals(600, MweConfig.worldDelayTicks());
        assertEquals(3600, MweConfig.timeoutSeconds());
        assertEquals("emi-export", MweConfig.exportWorldName());
    }

    @Test
    void exportEnabledTrueArmsCiMode() {
        MweConfig.ensureForTests();
        System.setProperty("minecraftWebExport.export.enabled", "true");
        System.setProperty("minecraftWebExport.exportWorldName", "custom-save");
        System.setProperty("minecraftWebExport.exportWorldDelayTicks", "30");
        System.setProperty("minecraftWebExport.exportTimeoutSeconds", "5400");

        assertTrue(MweConfig.exportEnabled());
        assertEquals("custom-save", MweConfig.exportWorldName());
        assertEquals(30, MweConfig.worldDelayTicks());
        assertEquals(5400, MweConfig.timeoutSeconds());
    }

    @Test
    void readsConfigOverridesWhenJvmUnset() {
        MweConfigTestSupport.apply(Map.of(
                "ci.worldDelayTicks", 120,
                "ci.timeoutSeconds", 1800));

        assertEquals(120, MweConfig.worldDelayTicks());
        assertEquals(1800, MweConfig.timeoutSeconds());
    }

    @Test
    void timedOutUsesConfiguredTimeout() {
        MweConfigTestSupport.apply(Map.of("ci.timeoutSeconds", 1));
        long start = System.nanoTime() - 2_000_000_000L;
        assertTrue(MweConfig.timedOut(start));
    }
}
