package io.github.jmecn.minecraftwebexport.export.emi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class EmiExportVisibilityTest {

    @Test
    void hiddenTagIdMatchesEmiConvention() {
        assertEquals("c", EmiExportVisibility.HIDDEN_FROM_RECIPE_VIEWERS_TAG.getNamespace());
        assertEquals("hidden_from_recipe_viewers", EmiExportVisibility.HIDDEN_FROM_RECIPE_VIEWERS_TAG.getPath());
        assertFalse(EmiExportVisibility.isRegistryIdHiddenFromRecipeViewers(null, "minecraft:stick"));
    }
}
