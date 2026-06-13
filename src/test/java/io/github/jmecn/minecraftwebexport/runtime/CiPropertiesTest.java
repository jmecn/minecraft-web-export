package io.github.jmecn.minecraftwebexport.runtime;

import io.github.jmecn.minecraftwebexport.config.MweConfig;
import io.github.jmecn.minecraftwebexport.config.MweConfigTestSupport;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CiPropertiesTest {

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
        assertFalse(CiProperties.exportEnabled());
        assertEquals(600, CiProperties.exportWorldDelayTicks());
        assertEquals(3600, CiProperties.exportTimeoutSeconds());
        assertEquals("emi-export", CiProperties.exportWorldName());
    }

    @Test
    void exportEnabledTrueArmsCiMode() {
        MweConfig.ensureForTests();
        System.setProperty("minecraftWebExport.export.enabled", "true");
        System.setProperty("minecraftWebExport.exportWorldName", "custom-save");
        System.setProperty("minecraftWebExport.exportWorldDelayTicks", "30");
        System.setProperty("minecraftWebExport.exportTimeoutSeconds", "5400");

        assertTrue(CiProperties.exportEnabled());
        assertEquals("custom-save", CiProperties.exportWorldName());
        assertEquals(30, CiProperties.exportWorldDelayTicks());
        assertEquals(5400, CiProperties.exportTimeoutSeconds());
    }

    @Test
    void readsConfigOverridesWhenJvmUnset() {
        MweConfigTestSupport.apply(Map.of(
                "ci.worldDelayTicks", 120,
                "ci.timeoutSeconds", 1800));

        assertEquals(120, CiProperties.exportWorldDelayTicks());
        assertEquals(1800, CiProperties.exportTimeoutSeconds());
    }

    @Test
    void timedOutUsesConfiguredTimeout() {
        MweConfigTestSupport.apply(Map.of("ci.timeoutSeconds", 1));
        long start = System.nanoTime() - 2_000_000_000L;
        assertTrue(CiProperties.timedOut(start));
    }
}
