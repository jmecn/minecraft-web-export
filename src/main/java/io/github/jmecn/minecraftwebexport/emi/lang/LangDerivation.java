package io.github.jmecn.minecraftwebexport.emi.lang;

import dev.emi.emi.EmiUtil;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.EmiRecipeManager;
import io.github.jmecn.minecraftwebexport.model.pipeline.ExportContext;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;

public final class LangDerivation {

    private LangDerivation() {
    }

    public static Set<String> derive(ExportContext context) {
        Set<String> keys = new TreeSet<>();
        if (!context.recipeLangKeys().isEmpty()) {
            keys.addAll(context.recipeLangKeys());
        }
        keys.addAll(RegistryLangKeys.langKeysForItemFluids(context.itemIds(), context.fluidIds()));
        keys.addAll(RegistryLangKeys.langKeysForTags(context.tagIds()));
        keys.addAll(categoryLangKeys(context.categoryIds()));
        return Set.copyOf(keys);
    }

    public static String emiCategoryLangKey(String categoryId) {
        if (categoryId == null || categoryId.isEmpty()) {
            return "emi.category.unknown";
        }
        ResourceLocation id = ResourceLocation.tryParse(categoryId);
        if (id == null) {
            return "emi.category." + categoryId.replace(':', '.').replace('/', '.');
        }
        return EmiUtil.translateId("emi.category.", id);
    }

    public static String resolveNameKey(EmiRecipeCategory category) {
        if (category == null) {
            return "emi.category.unknown";
        }
        String fromComponent = translationKeyFromComponent(category.getName());
        if (fromComponent != null) {
            return fromComponent;
        }
        return emiCategoryLangKey(category.getId().toString());
    }

    private static Set<String> categoryLangKeys(Set<String> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return Set.of();
        }
        EmiRecipeManager manager = EmiApi.getRecipeManager();
        if (manager == null) {
            Set<String> keys = new TreeSet<>();
            for (String categoryId : categoryIds) {
                keys.add(emiCategoryLangKey(categoryId));
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
                keys.add(resolveNameKey(category));
            }
        }
        for (String categoryId : categoryIds) {
            keys.add(emiCategoryLangKey(categoryId));
        }
        return Set.copyOf(keys);
    }

    private static String translationKeyFromComponent(Component component) {
        if (component == null) {
            return null;
        }
        if (component.getContents() instanceof TranslatableContents translatable) {
            String key = translatable.getKey();
            if (!key.isBlank()) {
                return key;
            }
        }
        return null;
    }
}
