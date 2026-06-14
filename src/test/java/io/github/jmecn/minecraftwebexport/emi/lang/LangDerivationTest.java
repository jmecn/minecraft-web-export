package io.github.jmecn.minecraftwebexport.emi.lang;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LangDerivationTest {

    @Test
    void withNameKeysMergesResolvedDescriptionKeys() {
        Set<String> merged = LangDerivation.withNameKeys(
                Set.of("tag.item.minecraft.candles"),
                Map.of("gtceu:wrought_iron_sword_head", "tagprefix.sword_head"));

        assertTrue(merged.contains("tag.item.minecraft.candles"));
        assertTrue(merged.contains("tagprefix.sword_head"));
    }
}
