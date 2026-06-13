package io.github.jmecn.minecraftwebexport.model.category;


public record CategoryEntry(
        String id,
        int order,
        Integer priority,
        String nameKey,
        CategoryIconSprite icon) {

    public CategoryEntry withIcon(CategoryIconSprite icon) {
        return new CategoryEntry(id, order, priority, nameKey, icon);
    }
}
