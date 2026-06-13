package io.github.jmecn.minecraftwebexport.emi.recipe;

import io.github.jmecn.minecraftwebexport.config.MweConfig;
import io.github.jmecn.minecraftwebexport.config.MweConfigTestSupport;
import io.github.jmecn.minecraftwebexport.emi.support.ProgressLog;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LayoutBuilderLogStrideTest {

    @AfterEach
    void clearConfig() {
        MweConfig.clearForTests();
    }

    @Test
    void largeExportTargetsAboutThirtyLines() {
        MweConfig.ensureForTests();
        int total = 135_317;
        int stride = LayoutBuilder.progressLogStride(total);
        assertTrue(stride >= 2_000);
        int lines = total / stride + 1;
        assertTrue(lines <= 35, "expected ~30 progress logs, got stride=" + stride + " lines=" + lines);
    }

    @Test
    void configOverride() {
        MweConfigTestSupport.apply(Map.of("logging.layoutLogStride", 10_000));
        assertEquals(10_000, LayoutBuilder.progressLogStride(135_317));
    }

    @Test
    void progressLogUsesConfiguredStride() {
        assertEquals(10_000, ProgressLog.stride(135_317, 10_000, 20, 200));
    }
}
