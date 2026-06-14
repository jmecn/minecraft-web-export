package io.github.jmecn.minecraftwebexport.pipeline;

import io.github.jmecn.minecraftwebexport.model.recipe.RecipeMeta;
import io.github.jmecn.minecraftwebexport.model.recipe.RecipeWidget;
import io.github.jmecn.minecraftwebexport.model.recipe.WidgetInteraction;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RelationPlannerLangKeysTest {

    @Test
    void collectMetaAddsRegistryKeysNotCategoryFromRecipeMeta() {
        RecipeMeta meta = RecipeMeta.of(
                "emi:anvil",
                1,
                1,
                null,
                "emi:anvil",
                List.of(RecipeWidget.of(0, 0, 1, 1, null, WidgetInteraction.item("gtceu:aluminium_ingot", null, null))));

        RelationPlanner.LangKeysCollector collector = new RelationPlanner.LangKeysCollector();
        collector.collectMeta(meta);

        assertFalse(collector.snapshot().contains("emi.category.emi.anvil"));
        assertTrue(collector.snapshot().contains("item.gtceu.aluminium_ingot"));
        assertTrue(collector.snapshot().contains("tagprefix.ingot"));
        assertTrue(collector.snapshot().contains("material.gtceu.aluminium"));
    }
}
