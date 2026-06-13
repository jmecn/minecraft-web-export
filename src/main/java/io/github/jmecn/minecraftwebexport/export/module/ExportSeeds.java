package io.github.jmecn.minecraftwebexport.export.module;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public record ExportSeeds(
        Set<String> recipeIds,
        Set<String> itemIds,
        Set<String> blockIds,
        Set<String> fluidIds,
        Set<String> tagIds,
        Set<String> entityIds,
        Set<String> langKeys,
        Set<String> textureIds,
        Set<String> resourcePaths) {

    public ExportSeeds {
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

    public static ExportSeeds empty() {
        return new ExportSeeds(
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

    public ExportSeeds merge(ExportSeeds other) {
        Objects.requireNonNull(other, "other");
        return new ExportSeeds(
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

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final Set<String> recipeIds = new TreeSet<>();
        private final Set<String> itemIds = new TreeSet<>();
        private final Set<String> blockIds = new TreeSet<>();
        private final Set<String> fluidIds = new TreeSet<>();
        private final Set<String> tagIds = new LinkedHashSet<>();
        private final Set<String> entityIds = new TreeSet<>();
        private final Set<String> langKeys = new TreeSet<>();
        private final Set<String> textureIds = new TreeSet<>();
        private final Set<String> resourcePaths = new TreeSet<>();

        public Builder recipeId(String id) {
            add(recipeIds, id);
            return this;
        }

        public Builder itemId(String id) {
            add(itemIds, id);
            return this;
        }

        public Builder blockId(String id) {
            add(blockIds, id);
            return this;
        }

        public Builder fluidId(String id) {
            add(fluidIds, id);
            return this;
        }

        public Builder tagId(String id) {
            add(tagIds, id);
            return this;
        }

        public Builder entityId(String id) {
            add(entityIds, id);
            return this;
        }

        public Builder langKey(String key) {
            add(langKeys, key);
            return this;
        }

        public Builder textureId(String id) {
            add(textureIds, id);
            return this;
        }

        public Builder resourcePath(String path) {
            add(resourcePaths, path);
            return this;
        }

        public Builder addAll(ExportSeeds seeds) {
            Objects.requireNonNull(seeds, "seeds");
            recipeIds.addAll(seeds.recipeIds);
            itemIds.addAll(seeds.itemIds);
            blockIds.addAll(seeds.blockIds);
            fluidIds.addAll(seeds.fluidIds);
            tagIds.addAll(seeds.tagIds);
            entityIds.addAll(seeds.entityIds);
            langKeys.addAll(seeds.langKeys);
            textureIds.addAll(seeds.textureIds);
            resourcePaths.addAll(seeds.resourcePaths);
            return this;
        }

        public ExportSeeds build() {
            return new ExportSeeds(
                    recipeIds,
                    itemIds,
                    blockIds,
                    fluidIds,
                    tagIds,
                    entityIds,
                    langKeys,
                    textureIds,
                    resourcePaths);
        }

        private static void add(Set<String> target, String value) {
            if (value != null && !value.isBlank()) {
                target.add(value.trim());
            }
        }
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
