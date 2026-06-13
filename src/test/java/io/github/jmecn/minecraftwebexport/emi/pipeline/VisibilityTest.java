package io.github.jmecn.minecraftwebexport.emi.pipeline;
import io.github.jmecn.minecraftwebexport.emi.pipeline.Visibility;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class VisibilityTest {

    @Test
    void hiddenTagIdMatchesEmiConvention() {
        assertEquals("c", Visibility.HIDDEN_FROM_RECIPE_VIEWERS_TAG.getNamespace());
        assertEquals("hidden_from_recipe_viewers", Visibility.HIDDEN_FROM_RECIPE_VIEWERS_TAG.getPath());
        assertFalse(Visibility.isRegistryIdHiddenFromRecipeViewers(null, "minecraft:stick"));
    }
}
