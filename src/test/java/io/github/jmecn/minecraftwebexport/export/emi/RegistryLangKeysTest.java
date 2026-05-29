package io.github.jmecn.minecraftwebexport.export.emi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistryLangKeysTest {

    @Test
    void itemKeyStripsNbtAndItemPrefix() {
        assertEquals("item.ae2.calculation_processor", RegistryLangKeys.itemKey("ae2:calculation_processor"));
        assertEquals("item.gtceu.liquid_air_bucket", RegistryLangKeys.itemKey("item:gtceu:liquid_air_bucket"));
        assertEquals("item.gtceu.ev_distillery", RegistryLangKeys.itemKey("gtceu:ev_distillery{Configuration:1}"));
    }

    @Test
    void itemLookupKeysPreferItemBlockFluid() {
        var keys = RegistryLangKeys.itemLookupKeys("ae2:calculation_processor");
        assertEquals(3, keys.size());
        assertTrue(keys.get(0).startsWith("item."));
    }
}
