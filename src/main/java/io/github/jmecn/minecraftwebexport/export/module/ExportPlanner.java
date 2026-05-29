package io.github.jmecn.minecraftwebexport.export.module;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public final class ExportPlanner {

    private static final Logger LOGGER = LogManager.getLogger(ExportPlanner.class);

    private ExportPlanner() {
    }

    public static ExportPlan plan(Minecraft client, ExportMode mode, List<ExportModule> modules, ExportScope scope) {
        Objects.requireNonNull(client, "client");
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(modules, "modules");
        Objects.requireNonNull(scope, "scope");

        if (mode == ExportMode.FULL) {
            Set<String> allRecipes = collectAllRecipeIds();
            LOGGER.info("[export] mode=full, recipes={}", allRecipes.size());
            return ExportPlan.full(allRecipes);
        }

        ExportSeeds merged = ExportSeeds.empty();
        for (ExportModule module : modules) {
            ExportSeeds seeds = module.collectSeeds(scope);
            if (seeds != null && !seeds.isEmpty()) {
                merged = merged.merge(seeds);
            }
        }

        ExportHints hints = ExportHints.defaults();
        for (ExportModule module : modules) {
            hints = hints.merge(module.buildHints(scope, merged));
        }

        Set<String> availableRecipes = collectAllRecipeIds();
        Set<String> recipeIds = filterRecipeIds(merged.recipeIds(), availableRecipes);

        MinecraftServer server = client.getSingleplayerServer();
        ExportClosureExpander.ClosureResult closure = ExportClosureExpander.expand(server, merged);

        LOGGER.info(
                "[export] mode=scoped, modules={}, seedRecipes={}, resolvedRecipes={}, seedTags={}, closureItems={}, closureTags={}",
                modules.size(),
                merged.recipeIds().size(),
                recipeIds.size(),
                merged.tagIds().size(),
                closure.itemIds().size(),
                closure.tagIds().size());

        if (server == null && !merged.tagIds().isEmpty()) {
            LOGGER.warn("[export] tag closure skipped: no integrated server (seedTags={})", merged.tagIds().size());
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
}
