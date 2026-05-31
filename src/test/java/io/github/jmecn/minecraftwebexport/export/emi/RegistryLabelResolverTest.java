package io.github.jmecn.minecraftwebexport.export.emi;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistryLabelResolverTest {

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
}
