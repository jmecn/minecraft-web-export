package io.github.jmecn.minecraftwebexport.model.pipeline;
import io.github.jmecn.minecraftwebexport.emi.lang.ClosureKeys;


import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public record Plan(
        Mode mode,
        Set<String> recipeIds,
        Set<String> closureItemIds,
        Set<String> closureFluidIds,
        Set<String> closureTagIds,
        Set<String> closureLangKeys,
        Hints hints,
        Seeds sourceSeeds) {

    public Plan {
        mode = Objects.requireNonNull(mode, "mode");
        recipeIds = copy(recipeIds);
        closureItemIds = copy(closureItemIds);
        closureFluidIds = copy(closureFluidIds);
        closureTagIds = copy(closureTagIds);
        closureLangKeys = copy(closureLangKeys);
        hints = Objects.requireNonNull(hints, "hints");
        sourceSeeds = Objects.requireNonNull(sourceSeeds, "sourceSeeds");
    }

    public static Plan full(Set<String> allRecipeIds) {
        return new Plan(
                Mode.FULL,
                allRecipeIds,
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                Hints.defaults(),
                Seeds.empty());
    }

    public Set<String> itemsForIcons(Set<String> layoutReferencedItems) {
        return union(layoutReferencedItems, closureItemIds);
    }

    public Set<String> seedItemsForIndex() {
        return closureItemIds;
    }

    public Set<String> fluidsForIcons(Set<String> layoutReferencedFluids) {
        return union(layoutReferencedFluids, closureFluidIds);
    }

    public Set<String> tagsForExport(Set<String> layoutReferencedTags) {
        if (mode == Mode.FULL) {
            return copy(layoutReferencedTags);
        }
        return union(layoutReferencedTags, closureTagIds);
    }

    public Set<String> langKeysForExport() {
        if (mode == Mode.FULL) {
            return null;
        }
        Set<String> merged = ClosureKeys.mergeClosureLangKeys(closureLangKeys, closureItemIds, closureFluidIds);
        merged = ClosureKeys.mergeTagLangKeys(merged, closureTagIds);
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
