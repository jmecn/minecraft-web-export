package io.github.jmecn.minecraftwebexport.pipeline;

import io.github.jmecn.minecraftwebexport.enums.pipeline.Mode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExportScopeBuilderTest {

    @AfterEach
    void cleanup() {
        ModuleRegistry.clearForTests();
    }

    @Test
    void filterRecipeIdsKeepsAvailableOnly() {
        Set<String> filtered = ExportScopeBuilder.filterRecipeIds(
                Set.of("minecraft:stick", "missing:recipe"),
                Set.of("minecraft:stick", "minecraft:crafting_table"));
        assertEquals(Set.of("minecraft:stick"), filtered);
    }

    @Test
    void scopedModeExportsAllSeedRecipesRegardlessOfEmiVisibility() {
        Set<String> seeds = Set.of("tfc:crafting/wattle", "minecraft:stick", "missing:recipe");
        Set<String> emiVisible = Set.of("minecraft:stick");
        Set<String> resolved = ExportScopeBuilder.resolveRecipeIds(Mode.SCOPED, seeds, emiVisible);
        assertEquals(seeds, resolved);
    }

    @Test
    void fullModeStillFiltersToEmiVisibleRecipes() {
        Set<String> seeds = Set.of("tfc:crafting/wattle", "minecraft:stick");
        Set<String> emiVisible = Set.of("minecraft:stick");
        Set<String> resolved = ExportScopeBuilder.resolveRecipeIds(Mode.FULL, seeds, emiVisible);
        assertEquals(Set.of("minecraft:stick"), resolved);
    }

    @Test
    void filterRecipeIdsReturnsEmptyWhenSeedsEmpty() {
        assertTrue(ExportScopeBuilder.filterRecipeIds(Set.of(), Set.of("minecraft:stick")).isEmpty());
    }

    @Test
    void exportModeParsesAliases() {
        assertEquals(Mode.SCOPED, Mode.parse("scoped"));
        assertEquals(Mode.FULL, Mode.parse("full"));
        assertEquals(Mode.FULL, Mode.parse("unknown"));
    }
}
