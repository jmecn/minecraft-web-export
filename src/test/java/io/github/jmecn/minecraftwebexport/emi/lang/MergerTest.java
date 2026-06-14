package io.github.jmecn.minecraftwebexport.emi.lang;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MergerTest {

    @Test
    void isMinecraftRegistryLangKeyMatchesRegistryPrefixes() {
        assertTrue(Merger.isMinecraftRegistryLangKey("item.minecraft.stick"));
        assertTrue(Merger.isMinecraftRegistryLangKey("block.minecraft.dirt"));
        assertFalse(Merger.isMinecraftRegistryLangKey("tag.item.minecraft.dirt"));
    }

    @Test
    void shouldMergeLangKeyAllowsTfgMaterialWhenFullMerge() {
        assertTrue(Merger.shouldMergeLangKey("material.tfg.latex", null));
        assertTrue(Merger.shouldMergeLangKey("tagprefix.ingot", null));
    }

    @Test
    void shouldMergeLangKeyAllowsClosureMaterialKeys() {
        Set<String> closure = Set.of("material.tfg.latex", "tagprefix.ingot", "tag.item.c.latex");
        assertTrue(Merger.shouldMergeLangKey("material.tfg.latex", closure));
        assertTrue(Merger.shouldMergeLangKey("tagprefix.ingot", closure));
        assertTrue(Merger.shouldMergeLangKey("tag.item.c.latex", closure));
        assertFalse(Merger.shouldMergeLangKey("item.tfg.unused_ingot", closure));
    }

    @Test
    void matchesCanonicalLangPaths() {
        assertTrue(Merger.matchesLangPath(ResourceLocation.parse("minecraft:lang/en_us.json"), "en_us.json"));
        assertTrue(Merger.matchesLangPath(ResourceLocation.parse("tfc:assets/lang/en_us.json"), "en_us.json"));
        assertTrue(Merger.matchesLangPath(ResourceLocation.parse("minecraft:en_us.json"), "en_us.json"));
        assertFalse(Merger.matchesLangPath(ResourceLocation.parse("minecraft:lang/zh_cn.json"), "en_us.json"));
    }
}
