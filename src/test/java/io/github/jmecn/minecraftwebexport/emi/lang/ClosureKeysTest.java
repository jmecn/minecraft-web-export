package io.github.jmecn.minecraftwebexport.emi.lang;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ClosureKeysTest {

    @Test
    void addForItemIncludesFlatAndGtceuComposedKeys() {
        Set<String> keys = new TreeSet<>();
        ClosureKeys.addForItem(keys, "gtceu:aluminium_ingot");
        assertTrue(keys.contains("item.gtceu.aluminium_ingot"));
        assertTrue(keys.contains("tagprefix.ingot"));
        assertTrue(keys.contains("material.gtceu.aluminium"));
    }

    @Test
    void addForItemIncludesWireGtSingleComposedKeys() {
        Set<String> keys = new TreeSet<>();
        ClosureKeys.addForItem(keys, "gtceu:copper_single_wire");
        assertTrue(keys.contains("tagprefix.wire_gt_single"));
        assertTrue(keys.contains("material.gtceu.copper"));
    }

    @Test
    void addForItemIncludesTfgBucketGtceuTemplateKeys() {
        Set<String> keys = new TreeSet<>();
        ClosureKeys.addForItem(keys, "tfg:acetylene_bucket");
        assertTrue(keys.contains("item.tfg.bucket"));
        assertTrue(keys.contains("item.gtceu.bucket"));
        assertTrue(keys.contains("material.tfg.acetylene"));
    }

    @Test
    void addForItemIncludesTfgComposedKeys() {
        Set<String> keys = new TreeSet<>();
        ClosureKeys.addForItem(keys, "tfg:latex_ingot");
        assertTrue(keys.contains("item.tfg.latex_ingot"));
        assertTrue(keys.contains("tagprefix.ingot"));
        assertTrue(keys.contains("material.tfg.latex"));
    }

    @Test
    void addForItemIncludesFluidPipeComposedKeys() {
        Set<String> keys = new TreeSet<>();
        ClosureKeys.addForItem(keys, "gtceu:aluminium_huge_fluid_pipe");
        assertTrue(keys.contains("tagprefix.pipe_huge_fluid"));
        assertTrue(keys.contains("material.gtceu.aluminium"));
    }

    @Test
    void addForItemIncludesGtToolComposedKeys() {
        Set<String> keys = new TreeSet<>();
        ClosureKeys.addForItem(keys, "gtceu:bismuth_bronze_axe");
        assertTrue(keys.contains("item.gtceu.tool.axe"));
        assertTrue(keys.contains("material.gtceu.bismuth_bronze"));
    }

    @Test
    void addForItemIncludesTfgToolComposedKeys() {
        Set<String> keys = new TreeSet<>();
        ClosureKeys.addForItem(keys, "tfg:arsenic_bronze_pickaxe");
        assertTrue(keys.contains("item.gtceu.tool.pickaxe"));
        assertTrue(keys.contains("material.tfg.arsenic_bronze"));
    }

    @Test
    void addForItemIncludesBudIndicatorKeys() {
        Set<String> keys = new TreeSet<>();
        ClosureKeys.addForItem(keys, "gtceu:amethyst_bud_indicator");
        assertTrue(keys.contains("block.bud_indicator"));
        assertTrue(keys.contains("material.gtceu.amethyst"));
    }

    @Test
    void addForItemIncludesAe2LookupKeys() {
        Set<String> keys = new TreeSet<>();
        ClosureKeys.addForItem(keys, "ae2:calculation_processor");
        assertTrue(keys.contains("item.ae2.calculation_processor"));
    }

    @Test
    void mergeClosureLangKeysUnionsSeedAndRegistryKeys() {
        Set<String> merged = ClosureKeys.mergeClosureLangKeys(
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
