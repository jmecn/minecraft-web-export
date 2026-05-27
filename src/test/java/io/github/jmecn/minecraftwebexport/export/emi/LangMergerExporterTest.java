package io.github.jmecn.minecraftwebexport.export.emi;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LangMergerExporterTest {

    @Test
    void matchesCanonicalLangPaths() {
        assertTrue(LangMergerExporter.matchesLangPath(ResourceLocation.parse("minecraft:lang/en_us.json"), "en_us.json"));
        assertTrue(LangMergerExporter.matchesLangPath(ResourceLocation.parse("tfc:assets/lang/en_us.json"), "en_us.json"));
        assertTrue(LangMergerExporter.matchesLangPath(ResourceLocation.parse("minecraft:en_us.json"), "en_us.json"));
        assertFalse(LangMergerExporter.matchesLangPath(ResourceLocation.parse("minecraft:lang/zh_cn.json"), "en_us.json"));
    }
}
