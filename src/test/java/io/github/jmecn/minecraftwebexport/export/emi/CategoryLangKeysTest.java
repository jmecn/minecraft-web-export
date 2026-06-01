package io.github.jmecn.minecraftwebexport.export.emi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CategoryLangKeysTest {

    @Test
    void emiCategoryLangKeyForVanillaCategory() {
        assertEquals("emi.category.minecraft.crafting", CategoryLangKeys.emiCategoryLangKey("minecraft:crafting"));
    }

    @Test
    void emiCategoryLangKeyForInvalidId() {
        assertEquals("emi.category.gtceu.assembler", CategoryLangKeys.emiCategoryLangKey("gtceu:assembler"));
    }
}
