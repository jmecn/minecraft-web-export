package io.github.jmecn.minecraftwebexport.emi.lang;
import io.github.jmecn.minecraftwebexport.emi.lang.Merger;
import io.github.jmecn.minecraftwebexport.emi.lang.VanillaSupplement;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MergerTest {

    @Test
    void isMinecraftRegistryLangKeyMatchesRegistryPrefixes() {
        assertTrue(VanillaSupplement.isMinecraftRegistryLangKey("item.minecraft.stick"));
        assertTrue(VanillaSupplement.isMinecraftRegistryLangKey("block.minecraft.dirt"));
        assertFalse(VanillaSupplement.isMinecraftRegistryLangKey("tag.item.minecraft.dirt"));
    }

    @Test
    void shouldMergeLangKeyAllowsTfgMaterialWhenFullMerge() {
        assertTrue(Merger.shouldMergeLangKey("material.tfg.latex", null));
        assertTrue(Merger.shouldMergeLangKey("tagprefix.ingot", null));
    }

    @Test
    void filterWebDeployKeysOmitsRegistryComposeTables() {
        Set<String> closure = Set.of(
                "item.tfg.latex_ingot",
                "material.tfg.latex",
                "tagprefix.ingot",
                "tag.item.c.latex",
                "gtceu.assembler");
        Set<String> web = Merger.filterWebDeployKeys(closure);
        assertFalse(web.contains("material.tfg.latex"));
        assertFalse(web.contains("tagprefix.ingot"));
        assertFalse(web.contains("item.tfg.latex_ingot"));
        assertTrue(web.contains("tag.item.c.latex"));
        assertTrue(web.contains("gtceu.assembler"));
    }

    @Test
    void matchesCanonicalLangPaths() {
        assertTrue(Merger.matchesLangPath(ResourceLocation.parse("minecraft:lang/en_us.json"), "en_us.json"));
        assertTrue(Merger.matchesLangPath(ResourceLocation.parse("tfc:assets/lang/en_us.json"), "en_us.json"));
        assertTrue(Merger.matchesLangPath(ResourceLocation.parse("minecraft:en_us.json"), "en_us.json"));
        assertFalse(Merger.matchesLangPath(ResourceLocation.parse("minecraft:lang/zh_cn.json"), "en_us.json"));
    }
}
