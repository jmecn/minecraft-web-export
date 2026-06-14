package io.github.jmecn.minecraftwebexport.pipeline;

import io.github.jmecn.minecraftwebexport.model.pipeline.Hints;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HintsTest {

    @Test
    void mergeLanguagesPreservesFirstModuleOrder() {
        Hints first = new Hints(List.of("zh_cn", "en_us"));
        Hints second = new Hints(List.of("de_de", "zh_cn"));
        assertEquals(List.of("zh_cn", "en_us", "de_de"), first.merge(second).exportLanguages());
    }

    @Test
    void mergeLanguagesReturnsOtherWhenFirstEmpty() {
        Hints first = new Hints(List.of());
        Hints second = new Hints(List.of("zh_cn"));
        assertEquals(List.of("zh_cn"), first.merge(second).exportLanguages());
    }
}
