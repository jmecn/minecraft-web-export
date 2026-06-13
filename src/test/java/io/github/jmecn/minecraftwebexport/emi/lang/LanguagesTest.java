package io.github.jmecn.minecraftwebexport.emi.lang;

import io.github.jmecn.minecraftwebexport.Constants;
import io.github.jmecn.minecraftwebexport.model.pipeline.Hints;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LanguagesTest {

    @Test
    void returnsEnUsByDefault() {
        System.clearProperty(Constants.PROP_EXPORT_LANGUAGES);

        Set<String> languages = Languages.resolve();

        assertEquals(Set.of("en_us"), languages);
    }

    @Test
    void prefersExportHintsOverSystemProperty() {
        System.setProperty(Constants.PROP_EXPORT_LANGUAGES, "de_de");
        try {
            Hints hints = new Hints(Map.of(), Map.of(), List.of(), false, List.of("zh_cn"));
            assertEquals(Set.of("zh_cn", "en_us"), Languages.resolve(hints));
        } finally {
            System.clearProperty(Constants.PROP_EXPORT_LANGUAGES);
        }
    }

    @Test
    void wildcardTokenIsIgnoredAndStillIncludesEnUs() {
        System.setProperty(Constants.PROP_EXPORT_LANGUAGES, "*");
        try {
            assertEquals(Set.of("en_us"), Languages.resolve());
        } finally {
            System.clearProperty(Constants.PROP_EXPORT_LANGUAGES);
        }
    }

    @Test
    void normalizesConfiguredLanguageList() {
        System.setProperty(Constants.PROP_EXPORT_LANGUAGES, "EN_US, zh_cn ");
        try {
            assertEquals(Set.of("en_us", "zh_cn"), Languages.resolve());
        } finally {
            System.clearProperty(Constants.PROP_EXPORT_LANGUAGES);
        }
    }
}
