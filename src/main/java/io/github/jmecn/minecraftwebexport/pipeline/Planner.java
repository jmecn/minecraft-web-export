package io.github.jmecn.minecraftwebexport.pipeline;

import io.github.jmecn.minecraftwebexport.MweMod;
import io.github.jmecn.minecraftwebexport.model.pipeline.ExportContext;
import io.github.jmecn.minecraftwebexport.model.pipeline.Hints;
import io.github.jmecn.minecraftwebexport.model.pipeline.Mode;
import io.github.jmecn.minecraftwebexport.model.pipeline.Plan;
import io.github.jmecn.minecraftwebexport.model.pipeline.Scope;
import io.github.jmecn.minecraftwebexport.model.pipeline.Seeds;
import io.github.jmecn.minecraftwebexport.pipeline.strategy.ExportStrategy;
import io.github.jmecn.minecraftwebexport.pipeline.strategy.FullExportStrategy;
import io.github.jmecn.minecraftwebexport.pipeline.strategy.ScopedExportStrategy;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.minecraft.client.Minecraft;

public final class Planner {

    private Planner() {
    }

    public static Plan plan(Minecraft client, Mode mode, List<Module> modules, Scope scope) {
        Objects.requireNonNull(client, "client");
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(modules, "modules");
        Objects.requireNonNull(scope, "scope");

        Seeds merged = Seeds.empty();
        if (mode == Mode.SCOPED) {
            for (Module module : modules) {
                Seeds seeds = module.collectSeeds(scope);
                if (seeds != null && !seeds.isEmpty()) {
                    merged = merged.merge(seeds);
                }
            }
        }

        Hints hints = mergeModuleHints(modules, scope, merged);
        ExportStrategy strategy = mode == Mode.FULL ? new FullExportStrategy() : new ScopedExportStrategy();
        ExportContext context = strategy.planScope(client, merged);

        if (mode == Mode.SCOPED) {
            Set<String> availableRecipes = collectExportableRecipeIds(client);
            if (merged.recipeIds().size() != context.recipeIds().size()) {
                MweMod.LOGGER.warn(
                        "[export] scoped recipe id count mismatch (unexpected): seeds={}, resolved={}",
                        merged.recipeIds().size(),
                        context.recipeIds().size());
            }
            if (!availableRecipes.containsAll(context.recipeIds())) {
                int hiddenFromEmi = 0;
                for (String id : context.recipeIds()) {
                    if (!availableRecipes.contains(id)) {
                        hiddenFromEmi++;
                    }
                }
                MweMod.LOGGER.info(
                        "[export] scoped recipes include {} EMI-hidden ids (exported anyway)",
                        hiddenFromEmi);
            }
            MweMod.LOGGER.info(
                    "[export] mode=scoped, modules={}, seedRecipes={}, resolvedRecipes={}, items={}, tags={}",
                    modules.size(),
                    merged.recipeIds().size(),
                    context.recipeIds().size(),
                    context.itemIds().size(),
                    context.tagIds().size());
        } else {
            MweMod.LOGGER.info("[export] mode=full, recipes={}", context.recipeIds().size());
        }

        return new Plan(context, hints, mode == Mode.FULL ? Seeds.empty() : merged);
    }

    public static Set<String> filterRecipeIds(Set<String> seedRecipeIds, Set<String> availableRecipeIds) {
        if (seedRecipeIds.isEmpty()) {
            return Set.of();
        }
        Set<String> filtered = new java.util.TreeSet<>();
        for (String id : seedRecipeIds) {
            if (availableRecipeIds.contains(id)) {
                filtered.add(id);
            }
        }
        return Set.copyOf(filtered);
    }

    public static Set<String> resolveRecipeIds(Mode mode, Set<String> seedRecipeIds, Set<String> availableRecipeIds) {
        if (mode == Mode.SCOPED) {
            return seedRecipeIds.isEmpty() ? Set.of() : Set.copyOf(new java.util.TreeSet<>(seedRecipeIds));
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

    public static Set<String> collectExportableRecipeIds(Minecraft client) {
        return io.github.jmecn.minecraftwebexport.emi.pipeline.Visibility.filterExportableRecipeIds(
                client == null ? null : client.getSingleplayerServer(),
                dev.emi.emi.api.EmiApi.getRecipeManager() == null
                        ? List.of()
                        : dev.emi.emi.api.EmiApi.getRecipeManager().getRecipes());
    }
}
