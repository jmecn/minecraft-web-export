package io.github.jmecn.minecraftwebexport.export.module;

import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

/**
 * Resolved export plan consumed by {@link io.github.jmecn.minecraftwebexport.export.emi.EmiRuntimeExportOrchestrator}.
 */
public record ExportPlan(
        ExportMode mode,
        Set<String> recipeIds,
        Set<String> closureItemIds,
        Set<String> closureFluidIds,
        Set<String> closureTagIds,
        Set<String> closureLangKeys,
        ExportHints hints,
        ExportSeeds sourceSeeds) {

    public ExportPlan {
        mode = Objects.requireNonNull(mode, "mode");
        recipeIds = copy(recipeIds);
        closureItemIds = copy(closureItemIds);
        closureFluidIds = copy(closureFluidIds);
        closureTagIds = copy(closureTagIds);
        closureLangKeys = copy(closureLangKeys);
        hints = Objects.requireNonNull(hints, "hints");
        sourceSeeds = Objects.requireNonNull(sourceSeeds, "sourceSeeds");
    }

    public static ExportPlan full(Set<String> allRecipeIds) {
        return new ExportPlan(
                ExportMode.FULL,
                allRecipeIds,
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                ExportHints.defaults(),
                ExportSeeds.empty());
    }

    public Set<String> itemsForIcons(Set<String> layoutReferencedItems) {
        return union(layoutReferencedItems, closureItemIds);
    }

    public Set<String> fluidsForIcons(Set<String> layoutReferencedFluids) {
        return union(layoutReferencedFluids, closureFluidIds);
    }

    public Set<String> tagsForExport(Set<String> layoutReferencedTags) {
        if (mode == ExportMode.FULL) {
            return copy(layoutReferencedTags);
        }
        return union(layoutReferencedTags, closureTagIds);
    }

    public Set<String> langKeysForExport() {
        if (mode == ExportMode.FULL || closureLangKeys.isEmpty()) {
            return null;
        }
        return closureLangKeys;
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
