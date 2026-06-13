package io.github.jmecn.minecraftwebexport.emi.lang;

import io.github.jmecn.minecraftwebexport.config.MweConfig;
import io.github.jmecn.minecraftwebexport.config.MweConfigTestSupport;
import io.github.jmecn.minecraftwebexport.model.pipeline.Hints;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LanguagesTest {

    @AfterEach
    void clearConfig() {
        MweConfig.clearForTests();
    }

    @Test
    void returnsEnUsByDefault() {
        MweConfig.ensureForTests();
        Set<String> languages = Languages.resolve();
        assertEquals(Set.of("en_us"), languages);
    }

    @Test
    void prefersExportHintsOverConfig() {
        MweConfigTestSupport.apply(Map.of("export.languages", "de_de"));
        Hints hints = new Hints(Map.of(), Map.of(), List.of(), false, List.of("zh_cn"));
        assertEquals(Set.of("zh_cn", "en_us"), Languages.resolve(hints));
    }

    @Test
    void wildcardTokenIsIgnoredAndStillIncludesEnUs() {
        assertEquals(Set.of("en_us"), Languages.parseLanguageList("*"));
    }

    @Test
    void normalizesConfiguredLanguageList() {
        assertEquals(Set.of("en_us", "zh_cn"), Languages.parseLanguageList("EN_US, zh_cn "));
    }

    @Test
    void readsLanguagesFromConfig() {
        MweConfigTestSupport.apply(Map.of("export.languages", "de_de,fr_fr"));
        assertEquals(Set.of("de_de", "fr_fr", "en_us"), Languages.resolve());
    }
}
