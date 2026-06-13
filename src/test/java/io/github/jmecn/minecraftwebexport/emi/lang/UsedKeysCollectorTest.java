package io.github.jmecn.minecraftwebexport.emi.lang;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UsedKeysCollectorTest {

    @TempDir
    Path outputDir;

    @Test
    void collectMetaAddsRegistryKeysNotCategoryFromRecipeMeta() {
        JsonObject meta = JsonParser.parseString("""
                {
                  "category": "emi:anvil",
                  "widgets": [
                    {
                      "interaction": {
                        "kind": "item",
                        "id": "gtceu:aluminium_ingot"
                      }
                    }
                  ]
                }
                """).getAsJsonObject();

        UsedKeysCollector collector = new UsedKeysCollector();
        collector.collectMeta(meta);

        assertFalse(collector.snapshot().contains("emi.category.emi.anvil"));
        assertTrue(collector.snapshot().contains("item.gtceu.aluminium_ingot"));
        assertTrue(collector.snapshot().contains("tagprefix.ingot"));
        assertTrue(collector.snapshot().contains("material.gtceu.aluminium"));
    }

    @Test
    void collectFromCategoriesIndexUsesExportedNameKey() throws Exception {
        Path categoriesDir = outputDir.resolve("emi").resolve("categories");
        Files.createDirectories(categoriesDir);
        Files.writeString(categoriesDir.resolve("index.json"), """
                {
                  "schema": 2,
                  "categories": [
                    { "id": "gtceu:assembler", "order": 0, "nameKey": "gtceu.assembler" }
                  ]
                }
                """);

        UsedKeysCollector collector = new UsedKeysCollector();
        collector.collectFromCategoriesIndex(outputDir);

        assertTrue(collector.snapshot().contains("gtceu.assembler"));
        assertFalse(collector.snapshot().contains("emi.category.gtceu.assembler"));
    }
}
