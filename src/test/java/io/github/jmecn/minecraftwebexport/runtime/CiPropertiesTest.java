package io.github.jmecn.minecraftwebexport.runtime;

import io.github.jmecn.minecraftwebexport.Constants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CiPropertiesTest {

    @AfterEach
    void clearProperties() {
        System.clearProperty(Constants.PROP_RUN_EXPORT_AND_EXIT);
        System.clearProperty(Constants.PROP_EXPORT_WARMUP_TICKS);
        System.clearProperty(Constants.PROP_EXPORT_WORLD_DELAY_TICKS);
        System.clearProperty(Constants.PROP_EXPORT_TIMEOUT_SECONDS);
        System.clearProperty(Constants.PROP_EXPORT_WORLD_NAME);
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
        System.setProperty(Constants.PROP_RUN_EXPORT_AND_EXIT, "true");
        System.setProperty(Constants.PROP_EXPORT_WARMUP_TICKS, "120");
        System.setProperty(Constants.PROP_EXPORT_WORLD_DELAY_TICKS, "30");
        System.setProperty(Constants.PROP_EXPORT_TIMEOUT_SECONDS, "90");
        System.setProperty(Constants.PROP_EXPORT_WORLD_NAME, "custom-save");

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
        System.setProperty(Constants.PROP_EXPORT_TIMEOUT_SECONDS, "0");
        long start = System.nanoTime() - 2_000_000_000L;
        assertFalse(CiProperties.timedOut(start));

        System.setProperty(Constants.PROP_EXPORT_TIMEOUT_SECONDS, "1");
        assertTrue(CiProperties.timedOut(start));
    }
}
