package io.github.jmecn.minecraftwebexport.model.recipe;

import io.github.jmecn.minecraftwebexport.Constants;

import java.util.List;

public record RecipeMeta(
        int schema,
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
            int width,
            int height,
            Integer margin,
            String category,
            List<RecipeWidget> widgets) {
        return new RecipeMeta(Constants.RECIPE_META_SCHEMA, width, height, margin, category, widgets);
    }

    public RecipeMeta withCategory(String category) {
        return new RecipeMeta(schema, width, height, margin, category, widgets);
    }
}
