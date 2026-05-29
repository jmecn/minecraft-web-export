package io.github.jmecn.minecraftwebexport.export.module;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ExportPlanTest {

    @Test
    void fullModeUsesLayoutTagsOnly() {
        ExportPlan plan = ExportPlan.full(Set.of("minecraft:stick"));
        assertEquals(Set.of("tag:a"), plan.tagsForExport(Set.of("tag:a")));
        assertEquals(Set.of("item:a"), plan.itemsForIcons(Set.of("item:a")));
        assertNull(plan.langKeysForExport());
    }

    @Test
    void scopedModeUnionsClosureSets() {
        ExportPlan plan = new ExportPlan(
                ExportMode.SCOPED,
                Set.of("minecraft:stick"),
                Set.of("minecraft:oak_log"),
                Set.of("minecraft:water"),
                Set.of("minecraft:logs"),
                Set.of("item.minecraft.stick"),
                ExportHints.defaults(),
                ExportSeeds.empty());

        assertEquals(
                Set.of("layout:tag", "minecraft:logs"),
                plan.tagsForExport(Set.of("layout:tag")));
        assertEquals(
                Set.of("layout:item", "minecraft:oak_log"),
                plan.itemsForIcons(Set.of("layout:item")));
        assertEquals(
                Set.of("layout:fluid", "minecraft:water"),
                plan.fluidsForIcons(Set.of("layout:fluid")));
        assertEquals(Set.of("item.minecraft.stick"), plan.langKeysForExport());
    }
}
