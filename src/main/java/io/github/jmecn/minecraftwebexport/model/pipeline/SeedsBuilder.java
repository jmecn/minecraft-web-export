package io.github.jmecn.minecraftwebexport.model.pipeline;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public final class SeedsBuilder {

    private final Set<String> recipeIds = new TreeSet<>();
    private final Set<String> itemIds = new TreeSet<>();
    private final Set<String> blockIds = new TreeSet<>();
    private final Set<String> fluidIds = new TreeSet<>();
    private final Set<String> tagIds = new LinkedHashSet<>();
    private final Set<String> entityIds = new TreeSet<>();
    private final Set<String> langKeys = new TreeSet<>();
    private final Set<String> textureIds = new TreeSet<>();
    private final Set<String> resourcePaths = new TreeSet<>();

    public SeedsBuilder recipeId(String id) {
        add(recipeIds, id);
        return this;
    }

    public SeedsBuilder itemId(String id) {
        add(itemIds, id);
        return this;
    }

    public SeedsBuilder blockId(String id) {
        add(blockIds, id);
        return this;
    }

    public SeedsBuilder fluidId(String id) {
        add(fluidIds, id);
        return this;
    }

    public SeedsBuilder tagId(String id) {
        add(tagIds, id);
        return this;
    }

    public SeedsBuilder entityId(String id) {
        add(entityIds, id);
        return this;
    }

    public SeedsBuilder langKey(String key) {
        add(langKeys, key);
        return this;
    }

    public SeedsBuilder textureId(String id) {
        add(textureIds, id);
        return this;
    }

    public SeedsBuilder resourcePath(String path) {
        add(resourcePaths, path);
        return this;
    }

    public SeedsBuilder addAll(Seeds seeds) {
        Objects.requireNonNull(seeds, "seeds");
        recipeIds.addAll(seeds.recipeIds());
        itemIds.addAll(seeds.itemIds());
        blockIds.addAll(seeds.blockIds());
        fluidIds.addAll(seeds.fluidIds());
        tagIds.addAll(seeds.tagIds());
        entityIds.addAll(seeds.entityIds());
        langKeys.addAll(seeds.langKeys());
        textureIds.addAll(seeds.textureIds());
        resourcePaths.addAll(seeds.resourcePaths());
        return this;
    }

    public Seeds build() {
        return new Seeds(
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
