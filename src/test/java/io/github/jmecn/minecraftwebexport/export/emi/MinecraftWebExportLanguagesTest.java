package io.github.jmecn.minecraftwebexport.export.emi;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MinecraftWebExportLanguagesTest {

    @Test
    void returnsEnUsByDefault() {
        System.clearProperty("minecraftWebExport.exportLanguages");

        Set<String> languages = MinecraftWebExportLanguages.resolve();

        assertEquals(Set.of("en_us"), languages);
    }

    @Test
    void wildcardTokenIsIgnoredAndStillIncludesEnUs() {
        System.setProperty("minecraftWebExport.exportLanguages", "*");
        try {
            assertEquals(Set.of("en_us"), MinecraftWebExportLanguages.resolve());
        } finally {
            System.clearProperty("minecraftWebExport.exportLanguages");
        }
    }

    @Test
    void normalizesConfiguredLanguageList() {
        System.setProperty("minecraftWebExport.exportLanguages", "EN_US, zh_cn ");
        try {
            assertEquals(Set.of("en_us", "zh_cn"), MinecraftWebExportLanguages.resolve());
        } finally {
            System.clearProperty("minecraftWebExport.exportLanguages");
        }
    }
}
