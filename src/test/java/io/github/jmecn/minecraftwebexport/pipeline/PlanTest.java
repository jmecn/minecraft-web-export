package io.github.jmecn.minecraftwebexport.pipeline;
import io.github.jmecn.minecraftwebexport.model.pipeline.ExportContext;
import io.github.jmecn.minecraftwebexport.model.pipeline.Hints;
import io.github.jmecn.minecraftwebexport.model.pipeline.Mode;
import io.github.jmecn.minecraftwebexport.model.pipeline.Plan;
import io.github.jmecn.minecraftwebexport.model.pipeline.Seeds;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlanTest {

    @Test
    void fullModeUsesLayoutTagsOnly() {
        Plan plan = Plan.full(Set.of("minecraft:stick"));
        assertEquals(Set.of("tag:a"), plan.tagsForExport(Set.of("tag:a")));
        assertEquals(Set.of("item:a"), plan.itemsForIcons(Set.of("item:a")));
        assertNull(plan.langKeysForExport());
    }

    @Test
    void scopedModeUnionsClosureSets() {
        Plan plan = new Plan(
                ExportContext.builder(Mode.SCOPED)
                        .recipeIds(Set.of("minecraft:stick"))
                        .itemIds(Set.of("minecraft:oak_log"))
                        .fluidIds(Set.of("minecraft:water"))
                        .tagIds(Set.of("minecraft:logs"))
                        .seedLangKeys(Set.of("item.minecraft.stick"))
                        .build(),
                Hints.defaults(),
                Seeds.empty());

        assertEquals(
                Set.of("layout:tag", "minecraft:logs"),
                plan.tagsForExport(Set.of("layout:tag")));
        assertEquals(
                Set.of("layout:item", "minecraft:oak_log"),
                plan.itemsForIcons(Set.of("layout:item")));
        assertEquals(Set.of("minecraft:oak_log"), plan.seedItemsForIndex());
        assertEquals(
                Set.of("layout:fluid", "minecraft:water"),
                plan.fluidsForIcons(Set.of("layout:fluid")));
        Set<String> langKeys = plan.langKeysForExport();
        assertTrue(langKeys.contains("item.minecraft.stick"));
        assertTrue(langKeys.contains("item.minecraft.oak_log"));
        assertTrue(langKeys.contains("fluid.minecraft.water"));
    }
}
