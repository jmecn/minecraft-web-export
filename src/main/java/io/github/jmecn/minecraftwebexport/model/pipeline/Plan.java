package io.github.jmecn.minecraftwebexport.model.pipeline;

import io.github.jmecn.minecraftwebexport.emi.lang.ClosureKeys;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public record Plan(ExportContext context, Hints hints, Seeds sourceSeeds) {

    public Plan {
        context = Objects.requireNonNull(context, "context");
        hints = Objects.requireNonNull(hints, "hints");
        sourceSeeds = Objects.requireNonNull(sourceSeeds, "sourceSeeds");
    }

    public Mode mode() {
        return context.mode();
    }

    public Set<String> recipeIds() {
        return context.recipeIds();
    }

    public Set<String> closureItemIds() {
        return context.itemIds();
    }

    public Set<String> closureFluidIds() {
        return context.fluidIds();
    }

    public Set<String> closureTagIds() {
        return context.tagIds();
    }

    public Set<String> closureLangKeys() {
        return context.seedLangKeys();
    }

    public static Plan full(Set<String> allRecipeIds) {
        return new Plan(
                ExportContext.builder(Mode.FULL).recipeIds(allRecipeIds).build(),
                Hints.defaults(),
                Seeds.empty());
    }

    public Set<String> itemsForIcons(Set<String> layoutReferencedItems) {
        return union(layoutReferencedItems, context.itemIds());
    }

    public Set<String> seedItemsForIndex() {
        return context.itemIds();
    }

    public Set<String> fluidsForIcons(Set<String> layoutReferencedFluids) {
        return union(layoutReferencedFluids, context.fluidIds());
    }

    public Set<String> tagsForExport(Set<String> layoutReferencedTags) {
        if (mode() == Mode.FULL) {
            return copy(layoutReferencedTags);
        }
        return union(layoutReferencedTags, context.tagIds());
    }

    public Set<String> langKeysForExport() {
        if (mode() == Mode.FULL) {
            return null;
        }
        Set<String> merged = ClosureKeys.mergeClosureLangKeys(
                context.seedLangKeys(), context.itemIds(), context.fluidIds());
        merged = ClosureKeys.mergeTagLangKeys(merged, context.tagIds());
        return merged.isEmpty() ? null : merged;
    }

    private static Set<String> copy(Set<String> values) {
        return Set.copyOf(values == null ? Set.of() : values);
    }

    private static Set<String> union(Set<String> left, Set<String> right) {
        if (right.isEmpty()) {
            return copy(left);
        }
        Set<String> merged = new TreeSet<>(left == null ? Set.of() : left);
        merged.addAll(right);
        return Set.copyOf(merged);
    }
}
