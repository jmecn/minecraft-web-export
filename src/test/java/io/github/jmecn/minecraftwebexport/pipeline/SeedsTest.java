package io.github.jmecn.minecraftwebexport.pipeline;

import io.github.jmecn.minecraftwebexport.model.pipeline.Seeds;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeedsTest {

    @AfterEach
    void cleanup() {
        ModuleRegistry.clearForTests();
    }

    @Test
    void emptySeedsAreEmpty() {
        assertTrue(Seeds.empty().isEmpty());
    }

    @Test
    void mergeCombinesAllBuckets() {
        Seeds left = Seeds.builder()
                .recipeId("minecraft:stick")
                .itemId("minecraft:oak_log")
                .tagId("minecraft:logs")
                .build();
        Seeds right = Seeds.builder()
                .recipeId("minecraft:crafting_table")
                .langKey("item.minecraft.stick")
                .build();

        Seeds merged = left.merge(right);

        assertEquals(Set.of("minecraft:stick", "minecraft:crafting_table"), merged.recipeIds());
        assertEquals(Set.of("minecraft:oak_log"), merged.itemIds());
        assertEquals(Set.of("minecraft:logs"), merged.tagIds());
        assertEquals(Set.of("item.minecraft.stick"), merged.langKeys());
    }

    @Test
    void builderIgnoresBlankValues() {
        Seeds seeds = Seeds.builder()
                .recipeId("  ")
                .itemId("minecraft:stick")
                .build();
        assertEquals(Set.of("minecraft:stick"), seeds.itemIds());
        assertTrue(seeds.recipeIds().isEmpty());
    }
}
