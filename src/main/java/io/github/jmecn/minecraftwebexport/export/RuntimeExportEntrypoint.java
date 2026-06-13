package io.github.jmecn.minecraftwebexport.export;

import io.github.jmecn.minecraftwebexport.mod.MinecraftWebExportMod;

import java.nio.file.Path;
import java.util.Objects;

public final class RuntimeExportEntrypoint {

    public static final String ENABLE_PROPERTY = "minecraftWebExport.export.enabled";
    public static final String OUTPUT_ROOT_PROPERTY = "minecraftWebExport.export.outputDir";
    public static final String SCHEMA_VERSION = "phase1/v1";
    public static final String TRIGGER = "mod_init";

    private final RuntimeExportOrchestrator orchestrator;

    public RuntimeExportEntrypoint() {
        this(new RuntimeExportOrchestrator());
    }

    RuntimeExportEntrypoint(RuntimeExportOrchestrator orchestrator) {
        this.orchestrator = Objects.requireNonNull(orchestrator, "orchestrator");
    }

    public void runIfEnabled(String modId, String minecraftVersion, Path gameDirectory) {
        if (!Boolean.getBoolean(ENABLE_PROPERTY)) {
            MinecraftWebExportMod.LOGGER.info(
                    "[export] skipped; set -D{}=true to enable minimal export",
                    ENABLE_PROPERTY);
            return;
        }

        ExportRequest request = new ExportRequest(
                modId,
                minecraftVersion,
                SCHEMA_VERSION,
                TRIGGER,
                gameDirectory,
                System.getProperty(OUTPUT_ROOT_PROPERTY));

        ExportReport report = orchestrator.export(request);
        if (report.isSuccessLike()) {
            MinecraftWebExportMod.LOGGER.info(
                    "[export] wrote {} (status={}, files={}, took={}ms)",
                    report.manifestFile().toAbsolutePath(),
                    report.status().wireValue(),
                    report.filesWritten().size(),
                    report.durationMillis());
        } else {
            MinecraftWebExportMod.LOGGER.error(
                    "[export] failed (status={}, errors={}, output={})",
                    report.status().wireValue(),
                    report.errors().size(),
                    report.outputRoot().toAbsolutePath());
        }

        for (ExportIssue issue : report.errors()) {
            MinecraftWebExportMod.LOGGER.error("[export:{}] {}", issue.stage(), issue.message());
        }
    }
}
