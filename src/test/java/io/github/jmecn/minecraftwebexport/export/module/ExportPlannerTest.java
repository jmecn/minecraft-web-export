package io.github.jmecn.minecraftwebexport.export.module;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExportPlannerTest {

    @AfterEach
    void cleanup() {
        ExportModuleRegistry.clearForTests();
    }

    @Test
    void filterRecipeIdsKeepsAvailableOnly() {
        Set<String> filtered = ExportPlanner.filterRecipeIds(
                Set.of("minecraft:stick", "missing:recipe"),
                Set.of("minecraft:stick", "minecraft:crafting_table"));
        assertEquals(Set.of("minecraft:stick"), filtered);
    }

    @Test
    void scopedModeExportsAllSeedRecipesRegardlessOfEmiVisibility() {
        Set<String> seeds = Set.of("tfc:crafting/wattle", "minecraft:stick", "missing:recipe");
        Set<String> emiVisible = Set.of("minecraft:stick");
        Set<String> resolved = ExportPlanner.resolveRecipeIds(ExportMode.SCOPED, seeds, emiVisible);
        assertEquals(seeds, resolved);
    }

    @Test
    void fullModeStillFiltersToEmiVisibleRecipes() {
        Set<String> seeds = Set.of("tfc:crafting/wattle", "minecraft:stick");
        Set<String> emiVisible = Set.of("minecraft:stick");
        Set<String> resolved = ExportPlanner.resolveRecipeIds(ExportMode.FULL, seeds, emiVisible);
        assertEquals(Set.of("minecraft:stick"), resolved);
    }

    @Test
    void filterRecipeIdsReturnsEmptyWhenSeedsEmpty() {
        assertTrue(ExportPlanner.filterRecipeIds(Set.of(), Set.of("minecraft:stick")).isEmpty());
    }

    @Test
    void exportModeReadsSystemProperty() {
        String previous = System.getProperty("minecraftWebExport.exportMode");
        try {
            System.setProperty("minecraftWebExport.exportMode", "scoped");
            assertEquals(ExportMode.SCOPED, ExportMode.current());
            System.setProperty("minecraftWebExport.exportMode", "closure");
            assertEquals(ExportMode.SCOPED, ExportMode.current());
            System.setProperty("minecraftWebExport.exportMode", "full");
            assertEquals(ExportMode.FULL, ExportMode.current());
        } finally {
            if (previous == null) {
                System.clearProperty("minecraftWebExport.exportMode");
            } else {
                System.setProperty("minecraftWebExport.exportMode", previous);
            }
        }
    }
}
