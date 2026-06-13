package io.github.jmecn.minecraftwebexport.emi.item;
import io.github.jmecn.minecraftwebexport.emi.item.SearchIndexWriter;
import io.github.jmecn.minecraftwebexport.emi.lang.RegistryResolver;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ItemsSearchRegistryKindTest {

    @Test
    void resolveRegistryKindUsesFluidListWhenPresent() {
        var resolver = new RegistryResolver(Map.of(), Map.of());
        assertEquals(
                "fluid",
                SearchIndexWriter.resolveRegistryKind(
                        "gtceu:acetic_acid",
                        Set.of("gtceu:acetic_acid"),
                        resolver));
        assertEquals(
                "item",
                SearchIndexWriter.resolveRegistryKind(
                        "gtceu:acetic_acid_bucket",
                        Set.of("gtceu:acetic_acid"),
                        resolver));
    }

    @Test
    void resolveRegistryKindInfersFluidWhenIndexMissing() {
        var resolver = new RegistryResolver(
                Map.of(
                        "material.gtceu.acetic_acid", "Acetic Acid",
                        "gtceu.fluid.generic", "%s",
                        "item.gtceu.bucket", "%s Bucket"),
                Map.of());
        assertEquals(
                "fluid",
                SearchIndexWriter.resolveRegistryKind(
                        "gtceu:acetic_acid",
                        Set.of(),
                        resolver));
        assertEquals(
                "Acetic Acid",
                resolver.translateRegistry(
                        "gtceu:acetic_acid",
                        SearchIndexWriter.resolveRegistryKind(
                                "gtceu:acetic_acid",
                                Set.of(),
                                resolver)));
        assertEquals(
                "item",
                SearchIndexWriter.resolveRegistryKind(
                        "gtceu:acetic_acid_bucket",
                        Set.of(),
                        resolver));
    }
}
