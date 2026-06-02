package io.github.jmecn.minecraftwebexport.export.emi;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ItemsSearchRegistryKindTest {

    @Test
    void resolveRegistryKindUsesFluidListWhenPresent() {
        var resolver = new RegistryLabelResolver(Map.of(), Map.of());
        assertEquals(
                "fluid",
                ItemsSearchIndexExporter.resolveRegistryKind(
                        "gtceu:acetic_acid",
                        Set.of("gtceu:acetic_acid"),
                        resolver));
        assertEquals(
                "item",
                ItemsSearchIndexExporter.resolveRegistryKind(
                        "gtceu:acetic_acid_bucket",
                        Set.of("gtceu:acetic_acid"),
                        resolver));
    }

    @Test
    void resolveRegistryKindInfersFluidWhenIndexMissing() {
        var resolver = new RegistryLabelResolver(
                Map.of(
                        "material.gtceu.acetic_acid", "Acetic Acid",
                        "gtceu.fluid.generic", "%s",
                        "item.gtceu.bucket", "%s Bucket"),
                Map.of());
        assertEquals(
                "fluid",
                ItemsSearchIndexExporter.resolveRegistryKind(
                        "gtceu:acetic_acid",
                        Set.of(),
                        resolver));
        assertEquals(
                "Acetic Acid",
                resolver.translateRegistry(
                        "gtceu:acetic_acid",
                        ItemsSearchIndexExporter.resolveRegistryKind(
                                "gtceu:acetic_acid",
                                Set.of(),
                                resolver)));
        assertEquals(
                "item",
                ItemsSearchIndexExporter.resolveRegistryKind(
                        "gtceu:acetic_acid_bucket",
                        Set.of(),
                        resolver));
    }
}
