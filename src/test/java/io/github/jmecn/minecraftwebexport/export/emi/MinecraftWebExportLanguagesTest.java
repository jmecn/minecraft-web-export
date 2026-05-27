package io.github.jmecn.minecraftwebexport.export.emi;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinecraftWebExportLanguagesTest {

    @Test
    void returnsSupportedSetByDefault() {
        System.clearProperty("minecraftWebExport.exportLanguages");

        Set<String> languages = MinecraftWebExportLanguages.resolve();

        assertTrue(languages.contains("en_us"));
        assertTrue(languages.contains("zh_cn"));
    }

    @Test
    void returnsNullForWildcard() {
        System.setProperty("minecraftWebExport.exportLanguages", "*");
        try {
            assertNull(MinecraftWebExportLanguages.resolve());
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
