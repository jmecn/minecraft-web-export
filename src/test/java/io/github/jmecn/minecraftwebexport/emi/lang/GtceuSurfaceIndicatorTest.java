package io.github.jmecn.minecraftwebexport.emi.lang;

import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GtceuSurfaceIndicatorTest {

    @Test
    void aluniteIndicatorComposesSurfaceRockLabel() {
        Map<String, String> lang = Map.of(
                "block.surface_rock", "地表岩石（%s）",
                "material.gtceu.alunite", "明矾石");

        RegistryResolver resolver = new RegistryResolver(lang, Map.of());
        String label = resolver.translateRegistry("gtceu:alunite_indicator");

        assertEquals("地表岩石（明矾石）", label);
    }

    @Test
    void surfaceIndicatorLangKeysIncludeMaterialAndTemplate() {
        var keys = new java.util.TreeSet<String>();
        RegistryLangKeys.addForItem(keys, "gtceu:alunite_indicator");

        assertTrue(keys.contains("block.surface_rock"));
        assertTrue(keys.contains("material.gtceu.alunite"));
    }
}
