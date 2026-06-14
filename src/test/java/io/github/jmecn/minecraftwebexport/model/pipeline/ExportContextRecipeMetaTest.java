package io.github.jmecn.minecraftwebexport.model.pipeline;

import io.github.jmecn.minecraftwebexport.enums.pipeline.Mode;
import io.github.jmecn.minecraftwebexport.model.recipe.RecipeMeta;
import io.github.jmecn.minecraftwebexport.model.recipe.RecipeWidget;
import io.github.jmecn.minecraftwebexport.model.recipe.WidgetInteraction;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class ExportContextRecipeMetaTest {

    @Test
    void storesAndReturnsRecipeMeta() {
        ExportContext context = ExportContext.builder(Mode.FULL).build();
        RecipeMeta meta = RecipeMeta.of(
                "minecraft:stick",
                72,
                72,
                4,
                "minecraft:crafting",
                List.of(RecipeWidget.of(
                        0, 0, 18, 18, "output", WidgetInteraction.item("minecraft:stick", null, null))));

        context.putRecipeMeta("minecraft:stick", meta);

        assertSame(meta, context.recipeMeta("minecraft:stick"));
        assertNull(context.recipeMeta("missing"));
    }

    @Test
    void overwritesRecipeMetaForSameId() {
        ExportContext context = ExportContext.builder(Mode.FULL).build();
        RecipeMeta first = RecipeMeta.of("minecraft:stick", 1, 1, 4, "a", List.of());
        RecipeMeta second = RecipeMeta.of("minecraft:stick", 2, 2, 4, "b", List.of());

        context.putRecipeMeta("minecraft:stick", first);
        context.putRecipeMeta("minecraft:stick", second);

        assertEquals(second, context.recipeMeta("minecraft:stick"));
    }
}
