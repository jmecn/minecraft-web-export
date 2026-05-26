package io.github.jmecn.minecraftwebexport.export;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeExportOrchestratorTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-05-26T09:41:00Z"),
            ZoneOffset.UTC);

    @TempDir
    Path tempDir;

    @Test
    void writesManifestAndBundleForMinimalExport() throws IOException {
        RuntimeExportOrchestrator orchestrator = new RuntimeExportOrchestrator(
                FIXED_CLOCK,
                new ExportBundleWriter(),
                new ExportManifestWriter());

        ExportRequest request = new ExportRequest(
                "minecraft_web_export",
                "1.20.1",
                "phase1/v1",
                "mod_init",
                tempDir,
                null);

        ExportReport report = orchestrator.export(request);

        assertEquals(ExportStatus.SUCCESS, report.status());
        assertTrue(Files.exists(report.bundleFile()));
        assertTrue(Files.exists(report.manifestFile()));
        assertTrue(report.errors().isEmpty());

        String bundle = Files.readString(report.bundleFile());
        assertTrue(bundle.contains("\"kind\": \"minecraft-web-export.phase1.bundle\""));
        assertTrue(bundle.contains("\"generatedAt\": \"2026-05-26T09:41:00Z\""));

        String manifest = Files.readString(report.manifestFile());
        assertTrue(manifest.contains("\"status\": \"success\""));
        assertTrue(manifest.contains("\"bundlePath\": \"bundle.json\""));
        assertTrue(manifest.contains("\"schemaVersion\": \"phase1/v1\""));
        assertTrue(manifest.contains("\"filesWritten\": ["));
        assertTrue(manifest.contains("\"bundle.json\""));
        assertTrue(manifest.contains("\"manifest.json\""));
        assertTrue(manifest.contains("\"errors\": []"));
    }

    @Test
    void writesFailureManifestWhenBundleWriteFails() throws IOException {
        RuntimeExportOrchestrator orchestrator = new RuntimeExportOrchestrator(
                FIXED_CLOCK,
                new ExportBundleWriter() {
                    @Override
                    public Path write(ExportOutputPaths paths, ExportRequest request, Instant generatedAt) throws IOException {
                        throw new IOException("simulated bundle failure");
                    }
                },
                new ExportManifestWriter());

        ExportRequest request = new ExportRequest(
                "minecraft_web_export",
                "1.20.1",
                "phase1/v1",
                "mod_init",
                tempDir,
                null);

        ExportReport report = orchestrator.export(request);

        assertEquals(ExportStatus.FAILED, report.status());
        assertTrue(Files.exists(report.manifestFile()));
        assertTrue(report.bundleFile().endsWith("bundle.json"));
        assertEquals(1, report.errors().size());

        String manifest = Files.readString(report.manifestFile());
        assertTrue(manifest.contains("\"status\": \"failed\""));
        assertTrue(manifest.contains("\"stage\": \"bundle\""));
        assertTrue(manifest.contains("simulated bundle failure"));
    }
}
