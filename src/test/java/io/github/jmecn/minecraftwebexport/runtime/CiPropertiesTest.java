package io.github.jmecn.minecraftwebexport.runtime;
import io.github.jmecn.minecraftwebexport.runtime.CiProperties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CiPropertiesTest {

    @AfterEach
    void clearProperties() {
        System.clearProperty(CiProperties.RUN_EXPORT_AND_EXIT_PROPERTY);
        System.clearProperty(CiProperties.EXPORT_WARMUP_TICKS_PROPERTY);
        System.clearProperty(CiProperties.EXPORT_WORLD_DELAY_TICKS_PROPERTY);
        System.clearProperty(CiProperties.EXPORT_TIMEOUT_SECONDS_PROPERTY);
        System.clearProperty(CiProperties.EXPORT_WORLD_NAME_PROPERTY);
    }

    @Test
    void defaultsWhenUnset() {
        assertFalse(CiProperties.runExportAndExit());
        assertEquals(40, CiProperties.exportWarmupTicks());
        assertEquals(600, CiProperties.exportWorldDelayTicks());
        assertEquals(7200, CiProperties.exportTimeoutSeconds());
        assertEquals("emi-export", CiProperties.exportWorldName());
    }

    @Test
    void readsOverrides() {
        System.setProperty(CiProperties.RUN_EXPORT_AND_EXIT_PROPERTY, "true");
        System.setProperty(CiProperties.EXPORT_WARMUP_TICKS_PROPERTY, "120");
        System.setProperty(CiProperties.EXPORT_WORLD_DELAY_TICKS_PROPERTY, "30");
        System.setProperty(CiProperties.EXPORT_TIMEOUT_SECONDS_PROPERTY, "90");
        System.setProperty(CiProperties.EXPORT_WORLD_NAME_PROPERTY, "custom-save");

        assertTrue(CiProperties.runExportAndExit());
        assertEquals(120, CiProperties.exportWarmupTicks());
        assertEquals(30, CiProperties.exportWorldDelayTicks());
        assertEquals(90, CiProperties.exportTimeoutSeconds());
        assertEquals("custom-save", CiProperties.exportWorldName());
    }

    @Test
    void timedOutRespectsDisabledTimeout() {
        long start = System.nanoTime();
        assertFalse(CiProperties.timedOut(start));
    }

    @Test
    void timedOutWhenElapsed() {
        System.setProperty(CiProperties.EXPORT_TIMEOUT_SECONDS_PROPERTY, "0");
        long start = System.nanoTime() - 2_000_000_000L;
        assertFalse(CiProperties.timedOut(start));

        System.setProperty(CiProperties.EXPORT_TIMEOUT_SECONDS_PROPERTY, "1");
        assertTrue(CiProperties.timedOut(start));
    }
}
