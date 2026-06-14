package io.github.jmecn.minecraftwebexport.model.pipeline;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public final class SeedsBuilder {

    private final Set<String> recipeIds = new TreeSet<>();
    private final Set<String> itemIds = new TreeSet<>();
    private final Set<String> tagIds = new LinkedHashSet<>();

    public SeedsBuilder recipeId(String id) {
        add(recipeIds, id);
        return this;
    }

    public SeedsBuilder itemId(String id) {
        add(itemIds, id);
        return this;
    }

    public SeedsBuilder tagId(String id) {
        add(tagIds, id);
        return this;
    }

    public SeedsBuilder addAll(Seeds seeds) {
        Objects.requireNonNull(seeds, "seeds");
        recipeIds.addAll(seeds.recipeIds());
        itemIds.addAll(seeds.itemIds());
        tagIds.addAll(seeds.tagIds());
        return this;
    }

    public Seeds build() {
        return new Seeds(recipeIds, itemIds, tagIds);
    }

    private static void add(Set<String> target, String value) {
        if (value != null && !value.isBlank()) {
            target.add(value.trim());
        }
    }
}
