package io.github.jmecn.minecraftwebexport.export.emi;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LangClosureKeysTest {

    @Test
    void addForItemIncludesFlatAndGtceuComposedKeys() {
        Set<String> keys = new TreeSet<>();
        LangClosureKeys.addForItem(keys, "gtceu:aluminium_ingot");
        assertTrue(keys.contains("item.gtceu.aluminium_ingot"));
        assertTrue(keys.contains("tagprefix.ingot"));
        assertTrue(keys.contains("material.gtceu.aluminium"));
    }

    @Test
    void addForItemIncludesAe2LookupKeys() {
        Set<String> keys = new TreeSet<>();
        LangClosureKeys.addForItem(keys, "ae2:calculation_processor");
        assertTrue(keys.contains("item.ae2.calculation_processor"));
    }

    @Test
    void mergeClosureLangKeysUnionsSeedAndRegistryKeys() {
        Set<String> merged = LangClosureKeys.mergeClosureLangKeys(
                Set.of("gui.custom.title"),
                Set.of("ae2:calculation_processor"),
                Set.of("gtceu:liquid_air"));
        assertTrue(merged.contains("gui.custom.title"));
        assertTrue(merged.contains("item.ae2.calculation_processor"));
        assertTrue(merged.contains("fluid.gtceu.liquid_air"));
        assertTrue(merged.contains("material.gtceu.air"));
        assertTrue(merged.contains("gtceu.fluid.liquid_generic"));
    }
}
