package io.github.jmecn.minecraftwebexport.pipeline;
import io.github.jmecn.minecraftwebexport.emi.pipeline.Orchestrator;
import io.github.jmecn.minecraftwebexport.pipeline.Mode;
import io.github.jmecn.minecraftwebexport.pipeline.Module;
import io.github.jmecn.minecraftwebexport.pipeline.ModuleRegistry;
import io.github.jmecn.minecraftwebexport.pipeline.Plan;
import io.github.jmecn.minecraftwebexport.pipeline.Planner;
import io.github.jmecn.minecraftwebexport.pipeline.Result;
import io.github.jmecn.minecraftwebexport.pipeline.Scope;
import io.github.jmecn.minecraftwebexport.pipeline.Seeds;

import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import io.github.jmecn.minecraftwebexport.mod.MinecraftWebExportMod;

public final class Coordinator {

    private final Orchestrator emiOrchestrator;

    public Coordinator() {
        this(new Orchestrator());
    }

    Coordinator(Orchestrator emiOrchestrator) {
        this.emiOrchestrator = Objects.requireNonNull(emiOrchestrator, "emiOrchestrator");
    }

    public Result run(Path outputRoot, Path gameDirectory, Minecraft client) throws IOException {
        Objects.requireNonNull(outputRoot, "outputRoot");
        Objects.requireNonNull(gameDirectory, "gameDirectory");
        Objects.requireNonNull(client, "client");

        Mode mode = Mode.current();
        Scope scope = new Scope(outputRoot, gameDirectory, mode);
        List<Module> modules = ModuleRegistry.modules();

        for (Module module : modules) {
            module.beforeEmiExport(scope, client);
        }

        Plan plan = Planner.plan(client, mode, modules, scope);
        Seeds mergedSeeds = plan.mode() == Mode.FULL ? Seeds.empty() : plan.sourceSeeds();

        if (plan.mode() == Mode.SCOPED) {
            MinecraftWebExportMod.LOGGER.info(
                    "[export] scoped export via modules {} (recipes={})",
                    modules.stream().map(Module::moduleId).toList(),
                    plan.recipeIds().size());
        }

        Orchestrator.Report emiReport = emiOrchestrator.export(outputRoot, client, plan);
        Result result = Result.from(scope, plan, emiReport, mergedSeeds, modules);

        for (Module module : modules) {
            module.exportExtras(scope, result);
        }
        return result;
    }
}
