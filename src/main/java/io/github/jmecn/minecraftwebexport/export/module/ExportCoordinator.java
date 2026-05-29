package io.github.jmecn.minecraftwebexport.export.module;

import io.github.jmecn.minecraftwebexport.export.emi.EmiRuntimeExportOrchestrator;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Phase 3 entry: plan export scope, run EMI pipeline, invoke module extras.
 */
public final class ExportCoordinator {

    private static final Logger LOGGER = LogManager.getLogger(ExportCoordinator.class);

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

        ExportPlan plan = ExportPlanner.plan(client, mode, modules, scope);
        ExportSeeds mergedSeeds = plan.mode() == ExportMode.FULL ? ExportSeeds.empty() : plan.sourceSeeds();

        if (plan.mode() == ExportMode.SCOPED) {
            LOGGER.info(
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
