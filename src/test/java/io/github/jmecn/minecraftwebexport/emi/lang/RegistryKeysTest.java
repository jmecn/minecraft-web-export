package io.github.jmecn.minecraftwebexport.emi.lang;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistryKeysTest {

    @Test
    void itemKeyStripsNbtAndItemPrefix() {
        assertEquals("item.ae2.calculation_processor", RegistryKeys.itemKey("ae2:calculation_processor"));
        assertEquals("item.gtceu.liquid_air_bucket", RegistryKeys.itemKey("item:gtceu:liquid_air_bucket"));
        assertEquals("item.gtceu.ev_distillery", RegistryKeys.itemKey("gtceu:ev_distillery{Configuration:1}"));
    }

    @Test
    void namespaceStripsNbt() {
        assertEquals("gtceu", RegistryKeys.namespace("gtceu:ev_distillery{Configuration:1}"));
    }

    @Test
    void itemLookupKeysPreferItemBlockFluid() {
        var keys = RegistryKeys.itemLookupKeys("ae2:calculation_processor");
        assertEquals(3, keys.size());
        assertTrue(keys.get(0).startsWith("item."));
    }
}
