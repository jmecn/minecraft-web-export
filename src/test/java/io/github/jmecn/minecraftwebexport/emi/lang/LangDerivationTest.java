package io.github.jmecn.minecraftwebexport.emi.lang;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LangDerivationTest {

    @Test
    void emiCategoryLangKeyForVanillaCategory() {
        assertEquals("emi.category.minecraft.crafting", LangDerivation.emiCategoryLangKey("minecraft:crafting"));
    }

    @Test
    void emiCategoryLangKeyForInvalidId() {
        assertEquals("emi.category.gtceu.assembler", LangDerivation.emiCategoryLangKey("gtceu:assembler"));
    }
}
