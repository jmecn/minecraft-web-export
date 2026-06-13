package io.github.jmecn.minecraftwebexport.emi.recipe;

import com.google.gson.JsonParser;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.widget.ButtonWidget;
import dev.emi.emi.api.widget.GeneratedSlotWidget;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.Widget;
import dev.emi.emi.api.widget.WidgetHolder;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WidgetSerializerTest {

    @Test
    void collectsStructuredTagRefsFromSerializedIngredient() {
        var tags = new TreeSet<String>();
        var ingredient = JsonParser.parseString("""
                {
                  "entries": [
                    { "tag": "forge:ingots/iron" },
                    { "entries": [ { "tag": "minecraft:planks" } ] }
                  ]
                }
                """);

        WidgetSerializer.collectSerializedTagRefs(ingredient, tags);

        assertEquals(Set.of("forge:ingots/iron", "minecraft:planks"), tags);
    }

    @Test
    void treatsRecipeContextGeneratedSlotAsOutput() throws Exception {
        EmiRecipe recipe = recipe();
        SlotWidget slot = new GeneratedSlotWidget(r -> null, 1, 0, 0)
                .recipeContext(recipe);

        assertEquals("output", inferSlotRole(slot));
    }

    @Test
    void doesNotSkipGenericEmiButtons() throws Exception {
        Widget widget = new ButtonWidget(
                0,
                0,
                18,
                18,
                0,
                0,
                () -> true,
                (mouseX, mouseY, button) -> {
                });

        assertFalse(shouldSkipWidget(widget));
    }

    @Test
    void skipsRecipeButtonsByConcreteType() throws Exception {
        Widget widget = new dev.emi.emi.widget.RecipeButtonWidget(0, 0, 0, 0, recipe());

        assertTrue(shouldSkipWidget(widget));
    }

    private static EmiRecipe recipe() {
        return new EmiRecipe() {
            @Override
            public dev.emi.emi.api.recipe.EmiRecipeCategory getCategory() {
                return null;
            }

            @Override
            public net.minecraft.resources.ResourceLocation getId() {
                return null;
            }

            @Override
            public List<dev.emi.emi.api.stack.EmiIngredient> getInputs() {
                return List.of();
            }

            @Override
            public List<dev.emi.emi.api.stack.EmiStack> getOutputs() {
                return List.of();
            }

            @Override
            public int getDisplayWidth() {
                return 0;
            }

            @Override
            public int getDisplayHeight() {
                return 0;
            }

            @Override
            public void addWidgets(WidgetHolder widgets) {
            }
        };
    }

    private static String inferSlotRole(SlotWidget slot) throws Exception {
        Method method = WidgetSerializer.class.getDeclaredMethod("inferSlotRole", SlotWidget.class);
        method.setAccessible(true);
        return (String) method.invoke(null, slot);
    }

    private static boolean shouldSkipWidget(Widget widget) throws Exception {
        Method method = WidgetSerializer.class.getDeclaredMethod("shouldSkipWidget", Widget.class);
        method.setAccessible(true);
        return (boolean) method.invoke(null, widget);
    }
}
