package io.github.jmecn.minecraftwebexport.export.module;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExportSeedsTest {

    @AfterEach
    void cleanup() {
        ExportModuleRegistry.clearForTests();
    }

    @Test
    void emptySeedsAreEmpty() {
        assertTrue(ExportSeeds.empty().isEmpty());
    }

    @Test
    void mergeCombinesAllBuckets() {
        ExportSeeds left = ExportSeeds.builder()
                .recipeId("minecraft:stick")
                .itemId("minecraft:oak_log")
                .tagId("minecraft:logs")
                .build();
        ExportSeeds right = ExportSeeds.builder()
                .recipeId("minecraft:crafting_table")
                .langKey("item.minecraft.stick")
                .build();

        ExportSeeds merged = left.merge(right);

        assertEquals(Set.of("minecraft:stick", "minecraft:crafting_table"), merged.recipeIds());
        assertEquals(Set.of("minecraft:oak_log"), merged.itemIds());
        assertEquals(Set.of("minecraft:logs"), merged.tagIds());
        assertEquals(Set.of("item.minecraft.stick"), merged.langKeys());
    }

    @Test
    void builderIgnoresBlankValues() {
        ExportSeeds seeds = ExportSeeds.builder()
                .recipeId("  ")
                .itemId("minecraft:stick")
                .build();
        assertEquals(Set.of("minecraft:stick"), seeds.itemIds());
        assertTrue(seeds.recipeIds().isEmpty());
    }
}
