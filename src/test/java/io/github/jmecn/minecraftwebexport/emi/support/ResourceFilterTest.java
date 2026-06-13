package io.github.jmecn.minecraftwebexport.emi.support;
import io.github.jmecn.minecraftwebexport.emi.support.ResourceFilter;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceFilterTest {

    @Test
    void excludesDefaultNamespaces() {
        System.clearProperty("minecraftWebExport.exportExcludedNamespaces");

        assertTrue(ResourceFilter.isExcluded(ResourceLocation.parse("additionalplacements:test")));
        assertFalse(ResourceFilter.isExcluded(ResourceLocation.parse("minecraft:test")));
    }

    @Test
    void mergesConfiguredNamespaces() {
        System.setProperty("minecraftWebExport.exportExcludedNamespaces", "emi, kubejs");
        try {
            assertTrue(ResourceFilter.isExcluded(ResourceLocation.parse("emi:test")));
            assertTrue(ResourceFilter.isExcluded(ResourceLocation.parse("kubejs:test")));
        } finally {
            System.clearProperty("minecraftWebExport.exportExcludedNamespaces");
        }
    }
}
