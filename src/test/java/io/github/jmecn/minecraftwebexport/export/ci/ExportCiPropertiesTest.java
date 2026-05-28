package io.github.jmecn.minecraftwebexport.export.ci;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExportCiPropertiesTest {

    @AfterEach
    void clearProperties() {
        System.clearProperty(ExportCiProperties.RUN_EXPORT_AND_EXIT_PROPERTY);
        System.clearProperty(ExportCiProperties.EXPORT_WARMUP_TICKS_PROPERTY);
        System.clearProperty(ExportCiProperties.EXPORT_WORLD_DELAY_TICKS_PROPERTY);
        System.clearProperty(ExportCiProperties.EXPORT_TIMEOUT_SECONDS_PROPERTY);
        System.clearProperty(ExportCiProperties.EXPORT_WORLD_NAME_PROPERTY);
    }

    @Test
    void defaultsWhenUnset() {
        assertFalse(ExportCiProperties.runExportAndExit());
        assertEquals(40, ExportCiProperties.exportWarmupTicks());
        assertEquals(600, ExportCiProperties.exportWorldDelayTicks());
        assertEquals(7200, ExportCiProperties.exportTimeoutSeconds());
        assertEquals("emi-export", ExportCiProperties.exportWorldName());
    }

    @Test
    void readsOverrides() {
        System.setProperty(ExportCiProperties.RUN_EXPORT_AND_EXIT_PROPERTY, "true");
        System.setProperty(ExportCiProperties.EXPORT_WARMUP_TICKS_PROPERTY, "120");
        System.setProperty(ExportCiProperties.EXPORT_WORLD_DELAY_TICKS_PROPERTY, "30");
        System.setProperty(ExportCiProperties.EXPORT_TIMEOUT_SECONDS_PROPERTY, "90");
        System.setProperty(ExportCiProperties.EXPORT_WORLD_NAME_PROPERTY, "custom-save");

        assertTrue(ExportCiProperties.runExportAndExit());
        assertEquals(120, ExportCiProperties.exportWarmupTicks());
        assertEquals(30, ExportCiProperties.exportWorldDelayTicks());
        assertEquals(90, ExportCiProperties.exportTimeoutSeconds());
        assertEquals("custom-save", ExportCiProperties.exportWorldName());
    }

    @Test
    void timedOutRespectsDisabledTimeout() {
        long start = System.nanoTime();
        assertFalse(ExportCiProperties.timedOut(start));
    }

    @Test
    void timedOutWhenElapsed() {
        System.setProperty(ExportCiProperties.EXPORT_TIMEOUT_SECONDS_PROPERTY, "0");
        long start = System.nanoTime() - 2_000_000_000L;
        assertFalse(ExportCiProperties.timedOut(start));

        System.setProperty(ExportCiProperties.EXPORT_TIMEOUT_SECONDS_PROPERTY, "1");
        assertTrue(ExportCiProperties.timedOut(start));
    }
}
