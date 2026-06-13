package io.github.jmecn.minecraftwebexport.model.category;

public record CategoryEntry(
        String id,
        int order,
        Integer priority,
        String nameKey,
        CategoryIconSprite icon) {
}
