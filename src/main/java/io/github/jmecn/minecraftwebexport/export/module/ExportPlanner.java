package io.github.jmecn.minecraftwebexport.export.module;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipe;
import io.github.jmecn.minecraftwebexport.export.emi.EmiExportVisibility;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import io.github.jmecn.minecraftwebexport.mod.MinecraftWebExportMod;

public final class ExportPlanner {

    private ExportPlanner() {
    }

    public static ExportPlan plan(Minecraft client, ExportMode mode, List<ExportModule> modules, ExportScope scope) {
        Objects.requireNonNull(client, "client");
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(modules, "modules");
        Objects.requireNonNull(scope, "scope");

        if (mode == ExportMode.FULL) {
            Set<String> allRecipes = collectExportableRecipeIds(client);
            ExportHints hints = mergeModuleHints(modules, scope, ExportSeeds.empty());
            MinecraftWebExportMod.LOGGER.info("[export] mode=full, recipes={}", allRecipes.size());
            return new ExportPlan(
                    ExportMode.FULL,
                    allRecipes,
                    Set.of(),
                    Set.of(),
                    Set.of(),
                    Set.of(),
                    hints,
                    ExportSeeds.empty());
        }

        ExportSeeds merged = ExportSeeds.empty();
        for (ExportModule module : modules) {
            ExportSeeds seeds = module.collectSeeds(scope);
            if (seeds != null && !seeds.isEmpty()) {
                merged = merged.merge(seeds);
            }
        }

        ExportHints hints = mergeModuleHints(modules, scope, merged);

        Set<String> availableRecipes = collectExportableRecipeIds(client);
        Set<String> recipeIds = resolveRecipeIds(mode, merged.recipeIds(), availableRecipes);

        MinecraftServer server = client.getSingleplayerServer();
        ExportClosureExpander.ClosureResult closure = ExportClosureExpander.expand(server, merged);

        if (mode == ExportMode.SCOPED && merged.recipeIds().size() != recipeIds.size()) {
            MinecraftWebExportMod.LOGGER.warn("[export] scoped recipe id count mismatch (unexpected): seeds={}, resolved={}",
                    merged.recipeIds().size(), recipeIds.size());
        }
        if (mode == ExportMode.SCOPED && !availableRecipes.containsAll(recipeIds)) {
            int hiddenFromEmi = 0;
            for (String id : recipeIds) {
                if (!availableRecipes.contains(id)) {
                    hiddenFromEmi++;
                }
            }
            MinecraftWebExportMod.LOGGER.info(
                    "[export] scoped handbook recipes include {} EMI-hidden ids (exported anyway)",
                    hiddenFromEmi);
        }

        MinecraftWebExportMod.LOGGER.info(
                "[export] mode=scoped, modules={}, seedRecipes={}, resolvedRecipes={}, seedTags={}, closureItems={}, closureTags={}",
                modules.size(),
                merged.recipeIds().size(),
                recipeIds.size(),
                merged.tagIds().size(),
                closure.itemIds().size(),
                closure.tagIds().size());

        if (server == null && !merged.tagIds().isEmpty()) {
            MinecraftWebExportMod.LOGGER.warn("[export] tag closure skipped: no integrated server (seedTags={})", merged.tagIds().size());
        }

        return new ExportPlan(
                ExportMode.SCOPED,
                recipeIds,
                closure.itemIds(),
                closure.fluidIds(),
                closure.tagIds(),
                closure.langKeys(),
                hints,
                merged);
    }

    static Set<String> filterRecipeIds(Set<String> seedRecipeIds, Set<String> availableRecipeIds) {
        if (seedRecipeIds.isEmpty()) {
            return Set.of();
        }
        Set<String> filtered = new TreeSet<>();
        for (String id : seedRecipeIds) {
            if (availableRecipeIds.contains(id)) {
                filtered.add(id);
            }
        }
        return Set.copyOf(filtered);
    }

    static Set<String> resolveRecipeIds(ExportMode mode, Set<String> seedRecipeIds, Set<String> availableRecipeIds) {
        if (mode == ExportMode.SCOPED) {
            return seedRecipeIds.isEmpty() ? Set.of() : Set.copyOf(new TreeSet<>(seedRecipeIds));
        }
        return filterRecipeIds(seedRecipeIds, availableRecipeIds);
    }

    private static ExportHints mergeModuleHints(List<ExportModule> modules, ExportScope scope, ExportSeeds seeds) {
        ExportHints hints = ExportHints.defaults();
        for (ExportModule module : modules) {
            hints = hints.merge(module.buildHints(scope, seeds));
        }
        return hints;
    }

    @Deprecated
    public static Set<String> collectAllRecipeIds() {
        var manager = EmiApi.getRecipeManager();
        if (manager == null) {
            return Set.of();
        }
        Set<String> ids = new TreeSet<>();
        for (EmiRecipe recipe : manager.getRecipes()) {
            if (recipe.getId() != null) {
                ids.add(recipe.getId().toString());
            }
        }
        return Set.copyOf(ids);
    }

    public static Set<String> collectExportableRecipeIds(Minecraft client) {
        var manager = EmiApi.getRecipeManager();
        if (manager == null) {
            return Set.of();
        }
        MinecraftServer server = client == null ? null : client.getSingleplayerServer();
        return EmiExportVisibility.filterExportableRecipeIds(server, manager.getRecipes());
    }
}
