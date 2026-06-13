package io.github.jmecn.minecraftwebexport.model.recipe;

import io.github.jmecn.minecraftwebexport.Constants;
import java.util.List;

public record RecipeMeta(
        int schema,
        String id,
        int width,
        int height,
        Integer margin,
        String category,
        List<RecipeWidget> widgets) {

    public RecipeMeta {
        schema = schema;
        widgets = List.copyOf(widgets == null ? List.of() : widgets);
    }

    public static RecipeMeta of(
            String id,
            int width,
            int height,
            Integer margin,
            String category,
            List<RecipeWidget> widgets) {
        return new RecipeMeta(Constants.RECIPE_META_SCHEMA, id, width, height, margin, category, widgets);
    }
}
