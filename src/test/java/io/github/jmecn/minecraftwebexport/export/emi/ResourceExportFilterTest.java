package io.github.jmecn.minecraftwebexport.export.emi;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceExportFilterTest {

    @Test
    void excludesDefaultNamespaces() {
        System.clearProperty("minecraftWebExport.exportExcludedNamespaces");

        assertTrue(ResourceExportFilter.isExcluded(ResourceLocation.parse("additionalplacements:test")));
        assertFalse(ResourceExportFilter.isExcluded(ResourceLocation.parse("minecraft:test")));
    }

    @Test
    void mergesConfiguredNamespaces() {
        System.setProperty("minecraftWebExport.exportExcludedNamespaces", "emi, kubejs");
        try {
            assertTrue(ResourceExportFilter.isExcluded(ResourceLocation.parse("emi:test")));
            assertTrue(ResourceExportFilter.isExcluded(ResourceLocation.parse("kubejs:test")));
        } finally {
            System.clearProperty("minecraftWebExport.exportExcludedNamespaces");
        }
    }
}
