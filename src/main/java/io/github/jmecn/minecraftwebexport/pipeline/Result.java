package io.github.jmecn.minecraftwebexport.pipeline;
import io.github.jmecn.minecraftwebexport.emi.pipeline.Orchestrator;
import io.github.jmecn.minecraftwebexport.pipeline.Module;
import io.github.jmecn.minecraftwebexport.pipeline.Plan;
import io.github.jmecn.minecraftwebexport.pipeline.Scope;
import io.github.jmecn.minecraftwebexport.pipeline.Seeds;


import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public record Result(
        Scope scope,
        Plan plan,
        Path outputRoot,
        int recipesRequested,
        int recipesWritten,
        int itemIndexCount,
        int tagIndexCount,
        int languagesWritten,
        int iconsWritten,
        Seeds mergedSeeds,
        List<String> moduleIds) {

    public Result {
        scope = Objects.requireNonNull(scope, "scope");
        plan = Objects.requireNonNull(plan, "plan");
        outputRoot = Objects.requireNonNull(outputRoot, "outputRoot").toAbsolutePath().normalize();
        mergedSeeds = Objects.requireNonNull(mergedSeeds, "mergedSeeds");
        moduleIds = List.copyOf(moduleIds == null ? List.of() : moduleIds);
    }

    public static Result from(
            Scope scope,
            Plan plan,
            Orchestrator.Report emiReport,
            Seeds mergedSeeds,
            List<Module> modules) {
        Objects.requireNonNull(emiReport, "emiReport");
        List<String> ids = modules.stream().map(Module::moduleId).toList();
        return new Result(
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
