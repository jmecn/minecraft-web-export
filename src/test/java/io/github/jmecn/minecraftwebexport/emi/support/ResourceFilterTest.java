package io.github.jmecn.minecraftwebexport.emi.lang;

import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceFilterTest {

    @Test
    void defaultExcludedNamespacesIncludeAdditionalPlacements() {
        assertTrue(Merger.excludedNamespaces().contains("additionalplacements"));
    }

    @Test
    void mergeExcludedNamespacesAddsConfiguredValues() {
        Set<String> merged = Merger.mergeExcludedNamespaces("emi, kubejs");
        assertTrue(merged.contains("emi"));
        assertTrue(merged.contains("kubejs"));
        assertTrue(merged.contains("additionalplacements"));
    }
}
