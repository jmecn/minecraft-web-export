package io.github.jmecn.minecraftwebexport.emi.support;
import io.github.jmecn.minecraftwebexport.Constants;
import io.github.jmecn.minecraftwebexport.emi.support.ResourceFilter;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceFilterTest {

    @Test
    void excludesDefaultNamespaces() {
        System.clearProperty(Constants.PROP_EXPORT_EXCLUDED_NAMESPACES);

        assertTrue(ResourceFilter.isExcluded(ResourceLocation.parse("additionalplacements:test")));
        assertFalse(ResourceFilter.isExcluded(ResourceLocation.parse("minecraft:test")));
    }

    @Test
    void mergesConfiguredNamespaces() {
        System.setProperty(Constants.PROP_EXPORT_EXCLUDED_NAMESPACES, "emi, kubejs");
        try {
            assertTrue(ResourceFilter.isExcluded(ResourceLocation.parse("emi:test")));
            assertTrue(ResourceFilter.isExcluded(ResourceLocation.parse("kubejs:test")));
        } finally {
            System.clearProperty(Constants.PROP_EXPORT_EXCLUDED_NAMESPACES);
        }
    }
}
