package io.github.jmecn.minecraftwebexport.pipeline;
import io.github.jmecn.minecraftwebexport.pipeline.Mode;
import io.github.jmecn.minecraftwebexport.pipeline.ModuleRegistry;
import io.github.jmecn.minecraftwebexport.pipeline.Planner;

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
    void exportModeReadsSystemProperty() {
        String previous = System.getProperty("minecraftWebExport.exportMode");
        try {
            System.setProperty("minecraftWebExport.exportMode", "scoped");
            assertEquals(Mode.SCOPED, Mode.current());
            System.setProperty("minecraftWebExport.exportMode", "closure");
            assertEquals(Mode.SCOPED, Mode.current());
            System.setProperty("minecraftWebExport.exportMode", "full");
            assertEquals(Mode.FULL, Mode.current());
        } finally {
            if (previous == null) {
                System.clearProperty("minecraftWebExport.exportMode");
            } else {
                System.setProperty("minecraftWebExport.exportMode", previous);
            }
        }
    }
}
