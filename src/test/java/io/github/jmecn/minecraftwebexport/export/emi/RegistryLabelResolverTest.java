package io.github.jmecn.minecraftwebexport.export.emi;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistryLabelResolverTest {

    @Test
    void translateRegistryPrefersExportedNameKey() {
        var resolver = new RegistryLabelResolver(
                Map.of("block.afc.wood.planks.hanging_sign.copper.baobab", "Copper Baobab Hanging Sign"),
                Map.of(),
                Map.of("afc:wood/hanging_sign/copper/baobab", "block.afc.wood.planks.hanging_sign.copper.baobab"));
        assertEquals(
                "Copper Baobab Hanging Sign",
                resolver.translateRegistry("afc:wood/hanging_sign/copper/baobab"));
    }

    @Test
    void translateRegistryResolvesFlatItemKey() {
        var resolver = new RegistryLabelResolver(
                Map.of("item.test.smoke", "Smoke Item"),
                Map.of());
        assertEquals("Smoke Item", resolver.translateRegistry("test:smoke"));
    }

    @Test
    void translateRegistryResolvesGtceuIngot() {
        var resolver = new RegistryLabelResolver(
                Map.of(
                        "tagprefix.ingot", "%s Ingot",
                        "material.gtceu.aluminium", "Aluminium"),
                Map.of());
        assertEquals("Aluminium Ingot", resolver.translateRegistry("gtceu:aluminium_ingot"));
    }

    @Test
    void translateRegistryResolvesGtceuSingleWire() {
        var resolver = new RegistryLabelResolver(
                Map.of(
                        "tagprefix.wire_gt_single", "1x%s导线",
                        "material.gtceu.copper", "铜"),
                Map.of());
        assertEquals("1x铜导线", resolver.translateRegistry("gtceu:copper_single_wire"));
    }

    @Test
    void translateRegistryResolvesTfgBucketViaGtceuTemplate() {
        var resolver = new RegistryLabelResolver(
                Map.of(
                        "item.gtceu.bucket", "%s Bucket",
                        "material.tfg.aniline", "Aniline",
                        "gtceu.fluid.generic", "%s"),
                Map.of());
        assertEquals("Aniline Bucket", resolver.translateRegistry("tfg:aniline_bucket"));
    }

    @Test
    void translateRegistryResolvesTfgIngot() {
        var resolver = new RegistryLabelResolver(
                Map.of(
                        "tagprefix.ingot", "%s Ingot",
                        "material.tfg.latex", "Latex"),
                Map.of());
        assertEquals("Latex Ingot", resolver.translateRegistry("tfg:latex_ingot"));
    }
}
