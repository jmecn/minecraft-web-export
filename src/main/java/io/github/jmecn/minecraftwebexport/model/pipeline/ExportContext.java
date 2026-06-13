package io.github.jmecn.minecraftwebexport.model.pipeline;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import net.minecraft.world.item.ItemStack;

/**
 * Mutable export state carried through Scope → Relation → Materialize → Lang.
 * Scope fields are filled first and treated as frozen before relation scanning;
 * relation fields are populated in the relation phase on the same instance.
 */
public final class ExportContext {

    private final Mode mode;

    private final Set<String> recipeIds = new TreeSet<>();
    private final Set<String> categoryIds = new TreeSet<>();
    private final Set<String> itemIds = new TreeSet<>();
    private final Set<String> fluidIds = new TreeSet<>();
    private final Set<String> blockIds = new TreeSet<>();
    private final Set<String> tagIds = new TreeSet<>();
    private final Set<String> seedLangKeys = new TreeSet<>();

    private final Map<String, Map<String, Set<String>>> inputs = new TreeMap<>();
    private final Map<String, Map<String, Set<String>>> outputs = new TreeMap<>();
    private final Set<String> fluidRegistryIds = new TreeSet<>();
    private final Set<String> referencedItems = new TreeSet<>();
    private final Set<String> referencedFluids = new TreeSet<>();
    private final Set<String> referencedTags = new TreeSet<>();
    private final Set<String> recipeLangKeys = new TreeSet<>();
    private final Map<String, ItemStack> iconVariants = new LinkedHashMap<>();

    public ExportContext(Mode mode) {
        this.mode = Objects.requireNonNull(mode, "mode");
    }

    public static Builder builder(Mode mode) {
        return new Builder(mode);
    }

    public Mode mode() {
        return mode;
    }

    public boolean hasRecipes() {
        return !recipeIds.isEmpty();
    }

    public Set<String> recipeIds() {
        return Collections.unmodifiableSet(recipeIds);
    }

    public Set<String> categoryIds() {
        return Collections.unmodifiableSet(categoryIds);
    }

    public Set<String> itemIds() {
        return Collections.unmodifiableSet(itemIds);
    }

    public Set<String> fluidIds() {
        return Collections.unmodifiableSet(fluidIds);
    }

    public Set<String> blockIds() {
        return Collections.unmodifiableSet(blockIds);
    }

    public Set<String> tagIds() {
        return Collections.unmodifiableSet(tagIds);
    }

    public Set<String> seedLangKeys() {
        return Collections.unmodifiableSet(seedLangKeys);
    }

    public Map<String, Map<String, Set<String>>> inputs() {
        return inputs;
    }

    public Map<String, Map<String, Set<String>>> outputs() {
        return outputs;
    }

    public Set<String> fluidRegistryIds() {
        return fluidRegistryIds;
    }

    public Set<String> referencedItems() {
        return referencedItems;
    }

    public Set<String> referencedFluids() {
        return referencedFluids;
    }

    public Set<String> referencedTags() {
        return referencedTags;
    }

    public Set<String> recipeLangKeys() {
        return recipeLangKeys;
    }

    public Map<String, ItemStack> iconVariants() {
        return iconVariants;
    }

    public static final class Builder {
        private final ExportContext context;

        private Builder(Mode mode) {
            this.context = new ExportContext(mode);
        }

        public Builder recipeIds(Set<String> values) {
            if (values != null) {
                context.recipeIds.addAll(values);
            }
            return this;
        }

        public Builder addRecipeId(String id) {
            if (id != null && !id.isBlank()) {
                context.recipeIds.add(id);
            }
            return this;
        }

        public Builder categoryIds(Set<String> values) {
            if (values != null) {
                context.categoryIds.addAll(values);
            }
            return this;
        }

        public Builder addCategoryId(String id) {
            if (id != null && !id.isBlank()) {
                context.categoryIds.add(id);
            }
            return this;
        }

        public Builder itemIds(Set<String> values) {
            if (values != null) {
                context.itemIds.addAll(values);
            }
            return this;
        }

        public Builder addItemId(String id) {
            if (id != null && !id.isBlank()) {
                context.itemIds.add(id);
            }
            return this;
        }

        public Builder fluidIds(Set<String> values) {
            if (values != null) {
                context.fluidIds.addAll(values);
            }
            return this;
        }

        public Builder addFluidId(String id) {
            if (id != null && !id.isBlank()) {
                context.fluidIds.add(id);
            }
            return this;
        }

        public Builder blockIds(Set<String> values) {
            if (values != null) {
                context.blockIds.addAll(values);
            }
            return this;
        }

        public Builder tagIds(Set<String> values) {
            if (values != null) {
                context.tagIds.addAll(values);
            }
            return this;
        }

        public Builder addTagId(String id) {
            if (id != null && !id.isBlank()) {
                context.tagIds.add(id);
            }
            return this;
        }

        public Builder seedLangKeys(Set<String> values) {
            if (values != null) {
                context.seedLangKeys.addAll(values);
            }
            return this;
        }

        public ExportContext build() {
            return context;
        }
    }
}
