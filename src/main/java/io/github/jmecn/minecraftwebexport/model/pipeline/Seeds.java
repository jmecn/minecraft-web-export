package io.github.jmecn.minecraftwebexport.model.pipeline;

import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public record Seeds(Set<String> recipeIds, Set<String> itemIds, Set<String> tagIds) {

    public Seeds {
        recipeIds = copy(recipeIds);
        itemIds = copy(itemIds);
        tagIds = copy(tagIds);
    }

    public static Seeds empty() {
        return new Seeds(Set.of(), Set.of(), Set.of());
    }

    public boolean isEmpty() {
        return recipeIds.isEmpty() && itemIds.isEmpty() && tagIds.isEmpty();
    }

    public Seeds merge(Seeds other) {
        Objects.requireNonNull(other, "other");
        return new Seeds(
                union(recipeIds, other.recipeIds),
                union(itemIds, other.itemIds),
                union(tagIds, other.tagIds));
    }

    public static SeedsBuilder builder() {
        return new SeedsBuilder();
    }

    private static Set<String> copy(Set<String> values) {
        return Set.copyOf(values == null ? Set.of() : values);
    }

    private static Set<String> union(Set<String> left, Set<String> right) {
        Set<String> merged = new TreeSet<>(left);
        merged.addAll(right);
        return Set.copyOf(merged);
    }
}
