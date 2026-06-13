package io.github.jmecn.minecraftwebexport.pipeline;
import io.github.jmecn.minecraftwebexport.emi.pipeline.Orchestrator;
import io.github.jmecn.minecraftwebexport.model.emi.EmiExportReport;
import io.github.jmecn.minecraftwebexport.model.pipeline.ExportResult;
import io.github.jmecn.minecraftwebexport.model.pipeline.Mode;
import io.github.jmecn.minecraftwebexport.model.pipeline.Plan;
import io.github.jmecn.minecraftwebexport.model.pipeline.Scope;
import io.github.jmecn.minecraftwebexport.model.pipeline.Seeds;

import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import io.github.jmecn.minecraftwebexport.MweMod;

public final class Coordinator {

    private final Orchestrator emiOrchestrator;

    public Coordinator() {
        this(new Orchestrator());
    }

    Coordinator(Orchestrator emiOrchestrator) {
        this.emiOrchestrator = Objects.requireNonNull(emiOrchestrator, "emiOrchestrator");
    }

    public ExportResult run(Path outputRoot, Path gameDirectory, Minecraft client) throws IOException {
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
            MweMod.LOGGER.info(
                    "[export] scoped export via modules {} (recipes={})",
                    modules.stream().map(Module::moduleId).toList(),
                    plan.recipeIds().size());
        }

        EmiExportReport emiReport = emiOrchestrator.export(outputRoot, client, plan);
        ExportResult result = ExportResult.from(scope, plan, emiReport, mergedSeeds, modules);

        for (Module module : modules) {
            module.exportExtras(scope, result);
        }
        return result;
    }
}
