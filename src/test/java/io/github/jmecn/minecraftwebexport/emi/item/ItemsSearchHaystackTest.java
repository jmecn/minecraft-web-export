package io.github.jmecn.minecraftwebexport.emi.item;

import io.github.jmecn.minecraftwebexport.emi.lang.RegistryResolver;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemsSearchHaystackTest {

    @Test
    void zhHaystackIncludesEnglishAndPinyin() {
        var zh = new RegistryResolver(
                Map.of("item.test.iron_ingot", "铁锭"),
                Map.of());
        var en = new RegistryResolver(
                Map.of("item.test.iron_ingot", "Iron Ingot"),
                Map.of());
        String haystack = SearchIndexWriter.buildHaystack(
                "test:iron_ingot",
                "item",
                "zh_cn",
                zh,
                en);
        assertTrue(haystack.contains("test:iron_ingot"));
        assertTrue(haystack.contains("铁锭"));
        assertTrue(haystack.contains("iron ingot"));
        assertTrue(haystack.contains("tie ding") || haystack.contains("tieding"), haystack);
    }

    @Test
    void enHaystackOmitsPinyin() {
        var en = new RegistryResolver(
                Map.of("item.test.iron_ingot", "Iron Ingot"),
                Map.of());
        String haystack = SearchIndexWriter.buildHaystack(
                "test:iron_ingot",
                "item",
                "en_us",
                en,
                null);
        assertTrue(haystack.contains("iron ingot"));
        assertEquals(-1, haystack.indexOf("tie"));
    }
}
