package io.github.jmecn.minecraftwebexport.export.emi;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistryLabelResolverTest {

    @Test
    void translateRegistryPrefersExportedNameKeyForDefaultNamespace() {
        var resolver = new RegistryLabelResolver(
                Map.of("block.afc.wood.planks.hanging_sign.copper.baobab", "Copper Baobab Hanging Sign"),
                Map.of(),
                Map.of("afc:wood/hanging_sign/copper/baobab", "block.afc.wood.planks.hanging_sign.copper.baobab"));
        assertEquals(
                "Copper Baobab Hanging Sign",
                resolver.translateRegistry("afc:wood/hanging_sign/copper/baobab"));
    }

    @Test
    void translateRegistryGtceuComposedBeforeExportedNameKey() {
        var resolver = new RegistryLabelResolver(
                Map.of(
                        "tagprefix.ingot", "%s Ingot",
                        "material.gtceu.aluminium", "Aluminium",
                        "item.gtceu.aluminium_ingot", "Flat Ingot"),
                Map.of(),
                Map.of("gtceu:aluminium_ingot", "item.gtceu.aluminium_ingot"));
        assertEquals("Aluminium Ingot", resolver.translateRegistry("gtceu:aluminium_ingot"));
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

    @Test
    void translateRegistryResolvesEvElectricToolsAndRepairKit() {
        var resolver = new RegistryLabelResolver(
                Map.of(
                        "item.gtceu.tool.ev_buzzsaw", "%s Buzzsaw (EV)",
                        "item.gtceu.tool.ev_wrench", "%s Wrench (EV)",
                        "item.gtceu.tool.mv_wrench", "%s Wrench (MV)",
                        "tagprefix.repair_kit", "%s Repair Kit",
                        "material.gtceu.tungsten_carbide", "Tungsten Carbide",
                        "material.gtceu.vanadium_steel", "Vanadium Steel"),
                Map.of());
        assertEquals(
                "Tungsten Carbide Buzzsaw (EV)",
                resolver.translateRegistry("gtceu:ev_tungsten_carbide_buzzsaw"));
        assertEquals(
                "Tungsten Carbide Wrench (EV)",
                resolver.translateRegistry("gtceu:ev_tungsten_carbide_wrench"));
        assertEquals(
                "Vanadium Steel Wrench (MV)",
                resolver.translateRegistry("gtceu:mv_vanadium_steel_wrench"));
        assertEquals(
                "Tungsten Carbide Repair Kit",
                resolver.translateRegistry("gtceu:repair_kit_tungsten_carbide"));
    }

    @Test
    void translateRegistryResolvesTfgFluidsWithoutGasPrefix() {
        var resolver = new RegistryLabelResolver(
                Map.of(
                        "material.tfg.latex", "Latex",
                        "material.tfg.vulcanized_latex", "Vulcanized Latex",
                        "material.gtceu.polytetrafluoroethylene", "PTFE",
                        "gtceu.fluid.generic", "%s",
                        "gtceu.fluid.gas_generic", "Gaseous %s",
                        "item.gtceu.bucket", "%s Bucket"),
                Map.of());
        assertEquals("Latex", resolver.translateRegistry("tfg:latex", "fluid"));
        assertEquals("Vulcanized Latex", resolver.translateRegistry("tfg:vulcanized_latex", "fluid"));
        assertEquals("Latex Bucket", resolver.translateRegistry("tfg:latex_bucket"));
        assertEquals("PTFE", resolver.translateRegistry("gtceu:polytetrafluoroethylene", "fluid"));
    }
}
