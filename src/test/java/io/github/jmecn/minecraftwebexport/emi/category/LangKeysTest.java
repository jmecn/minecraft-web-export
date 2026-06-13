package io.github.jmecn.minecraftwebexport.emi.category;
import io.github.jmecn.minecraftwebexport.emi.category.LangKeys;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LangKeysTest {

    @Test
    void emiCategoryLangKeyForVanillaCategory() {
        assertEquals("emi.category.minecraft.crafting", LangKeys.emiCategoryLangKey("minecraft:crafting"));
    }

    @Test
    void emiCategoryLangKeyForInvalidId() {
        assertEquals("emi.category.gtceu.assembler", LangKeys.emiCategoryLangKey("gtceu:assembler"));
    }
}
