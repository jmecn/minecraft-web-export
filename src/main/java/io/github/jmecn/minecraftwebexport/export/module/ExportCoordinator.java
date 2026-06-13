package io.github.jmecn.minecraftwebexport.export.module;

import io.github.jmecn.minecraftwebexport.export.emi.EmiRuntimeExportOrchestrator;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import io.github.jmecn.minecraftwebexport.mod.MinecraftWebExportMod;

public final class ExportCoordinator {

    private final EmiRuntimeExportOrchestrator emiOrchestrator;

    public ExportCoordinator() {
        this(new EmiRuntimeExportOrchestrator());
    }

    ExportCoordinator(EmiRuntimeExportOrchestrator emiOrchestrator) {
        this.emiOrchestrator = Objects.requireNonNull(emiOrchestrator, "emiOrchestrator");
    }

    public ExportResult run(Path outputRoot, Path gameDirectory, Minecraft client) throws IOException {
        Objects.requireNonNull(outputRoot, "outputRoot");
        Objects.requireNonNull(gameDirectory, "gameDirectory");
        Objects.requireNonNull(client, "client");

        ExportMode mode = ExportMode.current();
        ExportScope scope = new ExportScope(outputRoot, gameDirectory, mode);
        List<ExportModule> modules = ExportModuleRegistry.modules();

        for (ExportModule module : modules) {
            module.beforeEmiExport(scope, client);
        }

        ExportPlan plan = ExportPlanner.plan(client, mode, modules, scope);
        ExportSeeds mergedSeeds = plan.mode() == ExportMode.FULL ? ExportSeeds.empty() : plan.sourceSeeds();

        if (plan.mode() == ExportMode.SCOPED) {
            MinecraftWebExportMod.LOGGER.info(
                    "[export] scoped export via modules {} (recipes={})",
                    modules.stream().map(ExportModule::moduleId).toList(),
                    plan.recipeIds().size());
        }

        EmiRuntimeExportOrchestrator.Report emiReport = emiOrchestrator.export(outputRoot, client, plan);
        ExportResult result = ExportResult.from(scope, plan, emiReport, mergedSeeds, modules);

        for (ExportModule module : modules) {
            module.exportExtras(scope, result);
        }
        return result;
    }
}
