package io.github.jmecn.minecraftwebexport.model.category;

import io.github.jmecn.minecraftwebexport.Constants;
import java.util.List;

public record CategoriesIndex(
        int schema,
        int iconCellSize,
        String iconsDir,
        List<CategoryEntry> categories) {

    public CategoriesIndex {
        categories = List.copyOf(categories == null ? List.of() : categories);
    }

    public static CategoriesIndex of(
            int iconCellSize,
            String iconsDir,
            List<CategoryEntry> categories) {
        return new CategoriesIndex(Constants.CATEGORIES_INDEX_SCHEMA, iconCellSize, iconsDir, categories);
    }
}
