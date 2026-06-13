package io.github.jmecn.minecraftwebexport.pipeline;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.EmiRecipeManager;
import io.github.jmecn.minecraftwebexport.emi.category.LangKeys;
import io.github.jmecn.minecraftwebexport.emi.lang.ClosureKeys;
import io.github.jmecn.minecraftwebexport.model.pipeline.ExportContext;
import java.util.Set;
import java.util.TreeSet;

public final class LangPlanner {

    private LangPlanner() {
    }

    public static Set<String> deriveLangKeys(ExportContext context) {
        Set<String> keys = new TreeSet<>(context.seedLangKeys());
        if (!context.recipeLangKeys().isEmpty()) {
            keys.addAll(context.recipeLangKeys());
        }
        keys = new TreeSet<>(ClosureKeys.mergeClosureLangKeys(keys, context.itemIds(), context.fluidIds()));
        keys = new TreeSet<>(ClosureKeys.mergeTagLangKeys(keys, context.tagIds()));
        keys.addAll(categoryLangKeys(context.categoryIds()));
        return Set.copyOf(keys);
    }

    private static Set<String> categoryLangKeys(Set<String> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return Set.of();
        }
        EmiRecipeManager manager = EmiApi.getRecipeManager();
        if (manager == null) {
            Set<String> keys = new TreeSet<>();
            for (String categoryId : categoryIds) {
                keys.add(LangKeys.emiCategoryLangKey(categoryId));
            }
            return Set.copyOf(keys);
        }
        Set<String> keys = new TreeSet<>();
        for (EmiRecipeCategory category : manager.getCategories()) {
            if (category == null || category.getId() == null) {
                continue;
            }
            String categoryId = category.getId().toString();
            if (categoryIds.contains(categoryId)) {
                keys.add(LangKeys.resolveNameKey(category));
            }
        }
        for (String categoryId : categoryIds) {
            keys.add(LangKeys.emiCategoryLangKey(categoryId));
        }
        return Set.copyOf(keys);
    }
}
