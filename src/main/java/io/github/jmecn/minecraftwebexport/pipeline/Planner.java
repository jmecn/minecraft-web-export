package io.github.jmecn.minecraftwebexport.pipeline;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipe;
import io.github.jmecn.minecraftwebexport.MweMod;
import io.github.jmecn.minecraftwebexport.emi.pipeline.Visibility;
import io.github.jmecn.minecraftwebexport.model.pipeline.ClosureResult;
import io.github.jmecn.minecraftwebexport.model.pipeline.Hints;
import io.github.jmecn.minecraftwebexport.model.pipeline.Mode;
import io.github.jmecn.minecraftwebexport.model.pipeline.Plan;
import io.github.jmecn.minecraftwebexport.model.pipeline.Scope;
import io.github.jmecn.minecraftwebexport.model.pipeline.Seeds;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public final class Planner {

    private Planner() {
    }

    public static Plan plan(Minecraft client, Mode mode, List<Module> modules, Scope scope) {
        Objects.requireNonNull(client, "client");
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(modules, "modules");
        Objects.requireNonNull(scope, "scope");

        if (mode == Mode.FULL) {
            Set<String> allRecipes = collectExportableRecipeIds(client);
            Hints hints = mergeModuleHints(modules, scope, Seeds.empty());
            MweMod.LOGGER.info("[export] mode=full, recipes={}", allRecipes.size());
            return new Plan(
                    Mode.FULL,
                    allRecipes,
                    Set.of(),
                    Set.of(),
                    Set.of(),
                    Set.of(),
                    hints,
                    Seeds.empty());
        }

        Seeds merged = Seeds.empty();
        for (Module module : modules) {
            Seeds seeds = module.collectSeeds(scope);
            if (seeds != null && !seeds.isEmpty()) {
                merged = merged.merge(seeds);
            }
        }

        Hints hints = mergeModuleHints(modules, scope, merged);

        Set<String> availableRecipes = collectExportableRecipeIds(client);
        Set<String> recipeIds = resolveRecipeIds(mode, merged.recipeIds(), availableRecipes);

        MinecraftServer server = client.getSingleplayerServer();
        ClosureResult closure = ClosureExpander.expand(server, merged);

        if (mode == Mode.SCOPED && merged.recipeIds().size() != recipeIds.size()) {
            MweMod.LOGGER.warn("[export] scoped recipe id count mismatch (unexpected): seeds={}, resolved={}",
                    merged.recipeIds().size(), recipeIds.size());
        }
        if (mode == Mode.SCOPED && !availableRecipes.containsAll(recipeIds)) {
            int hiddenFromEmi = 0;
            for (String id : recipeIds) {
                if (!availableRecipes.contains(id)) {
                    hiddenFromEmi++;
                }
            }
            MweMod.LOGGER.info(
                    "[export] scoped handbook recipes include {} EMI-hidden ids (exported anyway)",
                    hiddenFromEmi);
        }

        MweMod.LOGGER.info(
                "[export] mode=scoped, modules={}, seedRecipes={}, resolvedRecipes={}, seedTags={}, closureItems={}, closureTags={}",
                modules.size(),
                merged.recipeIds().size(),
                recipeIds.size(),
                merged.tagIds().size(),
                closure.itemIds().size(),
                closure.tagIds().size());

        if (server == null && !merged.tagIds().isEmpty()) {
            MweMod.LOGGER.warn("[export] tag closure skipped: no integrated server (seedTags={})", merged.tagIds().size());
        }

        return new Plan(
                Mode.SCOPED,
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

    static Set<String> resolveRecipeIds(Mode mode, Set<String> seedRecipeIds, Set<String> availableRecipeIds) {
        if (mode == Mode.SCOPED) {
            return seedRecipeIds.isEmpty() ? Set.of() : Set.copyOf(new TreeSet<>(seedRecipeIds));
        }
        return filterRecipeIds(seedRecipeIds, availableRecipeIds);
    }

    private static Hints mergeModuleHints(List<Module> modules, Scope scope, Seeds seeds) {
        Hints hints = Hints.defaults();
        for (Module module : modules) {
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
        return Visibility.filterExportableRecipeIds(server, manager.getRecipes());
    }
}
