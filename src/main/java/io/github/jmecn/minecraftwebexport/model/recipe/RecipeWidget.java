package io.github.jmecn.minecraftwebexport.model.recipe;

import java.util.Objects;

public record RecipeWidget(
        int x,
        int y,
        int w,
        int h,
        String role,
        WidgetInteraction interaction,
        WidgetTooltip tooltip) {

    public RecipeWidget {
        interaction = Objects.requireNonNull(interaction, "interaction");
    }
}
