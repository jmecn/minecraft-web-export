package io.github.jmecn.minecraftwebexport.model.pipeline;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public record Seeds(
        Set<String> recipeIds,
        Set<String> itemIds,
        Set<String> blockIds,
        Set<String> fluidIds,
        Set<String> tagIds,
        Set<String> entityIds,
        Set<String> langKeys,
        Set<String> textureIds,
        Set<String> resourcePaths) {

    public Seeds {
        recipeIds = copy(recipeIds);
        itemIds = copy(itemIds);
        blockIds = copy(blockIds);
        fluidIds = copy(fluidIds);
        tagIds = copy(tagIds);
        entityIds = copy(entityIds);
        langKeys = copy(langKeys);
        textureIds = copy(textureIds);
        resourcePaths = copy(resourcePaths);
    }

    public static Seeds empty() {
        return new Seeds(
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of());
    }

    public boolean isEmpty() {
        return recipeIds.isEmpty()
                && itemIds.isEmpty()
                && blockIds.isEmpty()
                && fluidIds.isEmpty()
                && tagIds.isEmpty()
                && entityIds.isEmpty()
                && langKeys.isEmpty()
                && textureIds.isEmpty()
                && resourcePaths.isEmpty();
    }

    public Seeds merge(Seeds other) {
        Objects.requireNonNull(other, "other");
        return new Seeds(
                union(recipeIds, other.recipeIds),
                union(itemIds, other.itemIds),
                union(blockIds, other.blockIds),
                union(fluidIds, other.fluidIds),
                union(tagIds, other.tagIds),
                union(entityIds, other.entityIds),
                union(langKeys, other.langKeys),
                union(textureIds, other.textureIds),
                union(resourcePaths, other.resourcePaths));
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
