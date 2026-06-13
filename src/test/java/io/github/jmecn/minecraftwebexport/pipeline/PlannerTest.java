package io.github.jmecn.minecraftwebexport.pipeline;

import io.github.jmecn.minecraftwebexport.model.pipeline.Mode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlannerTest {

    @AfterEach
    void cleanup() {
        ModuleRegistry.clearForTests();
    }

    @Test
    void filterRecipeIdsKeepsAvailableOnly() {
        Set<String> filtered = Planner.filterRecipeIds(
                Set.of("minecraft:stick", "missing:recipe"),
                Set.of("minecraft:stick", "minecraft:crafting_table"));
        assertEquals(Set.of("minecraft:stick"), filtered);
    }

    @Test
    void scopedModeExportsAllSeedRecipesRegardlessOfEmiVisibility() {
        Set<String> seeds = Set.of("tfc:crafting/wattle", "minecraft:stick", "missing:recipe");
        Set<String> emiVisible = Set.of("minecraft:stick");
        Set<String> resolved = Planner.resolveRecipeIds(Mode.SCOPED, seeds, emiVisible);
        assertEquals(seeds, resolved);
    }

    @Test
    void fullModeStillFiltersToEmiVisibleRecipes() {
        Set<String> seeds = Set.of("tfc:crafting/wattle", "minecraft:stick");
        Set<String> emiVisible = Set.of("minecraft:stick");
        Set<String> resolved = Planner.resolveRecipeIds(Mode.FULL, seeds, emiVisible);
        assertEquals(Set.of("minecraft:stick"), resolved);
    }

    @Test
    void filterRecipeIdsReturnsEmptyWhenSeedsEmpty() {
        assertTrue(Planner.filterRecipeIds(Set.of(), Set.of("minecraft:stick")).isEmpty());
    }

    @Test
    void exportModeParsesAliases() {
        assertEquals(Mode.SCOPED, Mode.parse("scoped"));
        assertEquals(Mode.SCOPED, Mode.parse("closure"));
        assertEquals(Mode.FULL, Mode.parse("full"));
        assertEquals(Mode.FULL, Mode.parse("unknown"));
    }
}
