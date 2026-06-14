package io.github.jmecn.minecraftwebexport.emi.recipe;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.widget.Widget;
import dev.emi.emi.api.widget.WidgetHolder;
import io.github.jmecn.minecraftwebexport.model.recipe.RecipeMeta;
import io.github.jmecn.minecraftwebexport.model.recipe.RecipeWidget;
import io.github.jmecn.minecraftwebexport.Constants;
import io.github.jmecn.minecraftwebexport.config.MweConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.ItemStack;

public final class LayoutBuilder {

    private LayoutBuilder() {
    }

    static int panelMargin() {
        return Constants.PANEL_MARGIN;
    }

    public static int layoutScale() {
        return MweConfig.recipeLayoutScale();
    }

    public static RecipeMeta compileMeta(
            EmiRecipe recipe,
            String recipeId,
            Map<String, ItemStack> iconVariants) {
        int displayWidth = Math.max(1, recipe.getDisplayWidth());
        int displayHeight = Math.max(1, recipe.getDisplayHeight());

        List<Widget> widgets = new ArrayList<>();
        WidgetHolder holder = new WidgetHolder() {
            @Override
            public int getWidth() {
                return displayWidth;
            }

            @Override
            public int getHeight() {
                return displayHeight;
            }

            @Override
            public <T extends Widget> T add(T widget) {
                widgets.add(widget);
                return widget;
            }
        };
        recipe.addWidgets(holder);

        List<RecipeWidget> recipeWidgets = WidgetSerializer.compileRecipeWidgets(recipe, widgets, iconVariants);
        String category = null;
        if (recipe.getCategory() != null && recipe.getCategory().getId() != null) {
            category = recipe.getCategory().getId().toString();
        }
        return RecipeMeta.of(recipeId, displayWidth, displayHeight, panelMargin(), category, recipeWidgets);
    }
}
