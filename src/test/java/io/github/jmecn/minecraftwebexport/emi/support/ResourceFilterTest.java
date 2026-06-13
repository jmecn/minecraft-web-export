package io.github.jmecn.minecraftwebexport.emi.support;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceFilterTest {

    @Test
    void includesDefaultExcludedNamespaces() {
        assertTrue(ResourceFilter.excludedNamespaces().contains("additionalplacements"));
    }

    @Test
    void mergesConfiguredNamespaces() {
        Set<String> merged = ResourceFilter.mergeExcludedNamespaces("emi, kubejs");
        assertTrue(merged.contains("additionalplacements"));
        assertTrue(merged.contains("emi"));
        assertTrue(merged.contains("kubejs"));
        assertEquals(3, merged.size());
    }
}
