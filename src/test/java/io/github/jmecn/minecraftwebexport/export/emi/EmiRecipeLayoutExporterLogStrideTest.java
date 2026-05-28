package io.github.jmecn.minecraftwebexport.export.emi;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmiRecipeLayoutExporterLogStrideTest {

    @AfterEach
    void clearProperty() {
        System.clearProperty("minecraftWebExport.layoutLogStride");
    }

    @Test
    void largeExportTargetsAboutThirtyLines() {
        int total = 135_317;
        int stride = EmiRecipeLayoutExporter.progressLogStride(total);
        assertTrue(stride >= 2_000);
        int lines = total / stride + 1;
        assertTrue(lines <= 35, "expected ~30 progress logs, got stride=" + stride + " lines=" + lines);
    }

    @Test
    void propertyOverride() {
        System.setProperty("minecraftWebExport.layoutLogStride", "10000");
        assertEquals(10_000, EmiRecipeLayoutExporter.progressLogStride(135_317));
    }
}
