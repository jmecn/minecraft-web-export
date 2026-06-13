package io.github.jmecn.minecraftwebexport.emi.category;

import dev.emi.emi.EmiUtil;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;

public final class LangKeys {

    private LangKeys() {
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
