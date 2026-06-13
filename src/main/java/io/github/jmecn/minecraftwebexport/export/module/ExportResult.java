package io.github.jmecn.minecraftwebexport.export.module;

import io.github.jmecn.minecraftwebexport.export.emi.EmiRuntimeExportOrchestrator;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public record ExportResult(
        ExportScope scope,
        ExportPlan plan,
        Path outputRoot,
        int recipesRequested,
        int recipesWritten,
        int itemIndexCount,
        int tagIndexCount,
        int languagesWritten,
        int iconsWritten,
        ExportSeeds mergedSeeds,
        List<String> moduleIds) {

    public ExportResult {
        scope = Objects.requireNonNull(scope, "scope");
        plan = Objects.requireNonNull(plan, "plan");
        outputRoot = Objects.requireNonNull(outputRoot, "outputRoot").toAbsolutePath().normalize();
        mergedSeeds = Objects.requireNonNull(mergedSeeds, "mergedSeeds");
        moduleIds = List.copyOf(moduleIds == null ? List.of() : moduleIds);
    }

    public static ExportResult from(
            ExportScope scope,
            ExportPlan plan,
            EmiRuntimeExportOrchestrator.Report emiReport,
            ExportSeeds mergedSeeds,
            List<ExportModule> modules) {
        Objects.requireNonNull(emiReport, "emiReport");
        List<String> ids = modules.stream().map(ExportModule::moduleId).toList();
        return new ExportResult(
                scope,
                plan,
                emiReport.outputRoot(),
                emiReport.recipesRequested(),
                emiReport.recipesWritten(),
                emiReport.itemIndexCount(),
                emiReport.tagIndexCount(),
                emiReport.languagesWritten(),
                emiReport.iconsWritten(),
                mergedSeeds,
                ids);
    }
}
