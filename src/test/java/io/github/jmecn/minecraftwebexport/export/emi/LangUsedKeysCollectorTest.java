package io.github.jmecn.minecraftwebexport.export.emi;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LangUsedKeysCollectorTest {

    @Test
    void collectMetaAddsRegistryAndCategoryKeys() {
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

        LangUsedKeysCollector collector = new LangUsedKeysCollector();
        collector.collectMeta(meta);

        assertTrue(collector.snapshot().contains("emi.category.emi.anvil"));
        assertTrue(collector.snapshot().contains("item.gtceu.aluminium_ingot"));
        assertTrue(collector.snapshot().contains("tagprefix.ingot"));
        assertTrue(collector.snapshot().contains("material.gtceu.aluminium"));
    }
}
