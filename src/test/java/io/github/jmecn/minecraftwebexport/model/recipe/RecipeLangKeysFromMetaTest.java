package io.github.jmecn.minecraftwebexport.model.recipe;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipeLangKeysFromMetaTest {

    @Test
    void collectMetaAddsRegistryKeysNotCategoryFromRecipeMeta() {
        RecipeMeta meta = RecipeMeta.of(
                "emi:anvil",
                1,
                1,
                null,
                "emi:anvil",
                List.of(RecipeWidget.of(0, 0, 1, 1, null, WidgetInteraction.item("gtceu:aluminium_ingot", null, null))));

        RecipeLangKeysCollector collector = new RecipeLangKeysCollector();
        collector.collectMeta(meta);

        assertFalse(collector.snapshot().contains("emi.category.emi.anvil"));
        assertTrue(collector.snapshot().contains("item.gtceu.aluminium_ingot"));
        assertTrue(collector.snapshot().contains("tagprefix.ingot"));
        assertTrue(collector.snapshot().contains("material.gtceu.aluminium"));
    }
}
