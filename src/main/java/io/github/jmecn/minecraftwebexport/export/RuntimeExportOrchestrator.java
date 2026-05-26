package io.github.jmecn.minecraftwebexport.export;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class RuntimeExportOrchestrator {

    private final Clock clock;
    private final ExportBundleWriter bundleWriter;
    private final ExportManifestWriter manifestWriter;

    public RuntimeExportOrchestrator() {
        this(Clock.systemUTC(), new ExportBundleWriter(), new ExportManifestWriter());
    }

    RuntimeExportOrchestrator(
            Clock clock,
            ExportBundleWriter bundleWriter,
            ExportManifestWriter manifestWriter) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.bundleWriter = Objects.requireNonNull(bundleWriter, "bundleWriter");
        this.manifestWriter = Objects.requireNonNull(manifestWriter, "manifestWriter");
    }

    public ExportReport export(ExportRequest request) {
        Instant startedAt = clock.instant();
        ExportOutputPaths paths = ExportOutputPaths.resolve(request.gameDirectory(), request.outputRootOverride());
        List<Path> filesWritten = new ArrayList<>();
        List<ExportIssue> errors = new ArrayList<>();
        boolean bundleWritten = false;

        try {
            paths.prepareDirectories();
        } catch (IOException e) {
            errors.add(new ExportIssue("output-root", describe(e)));
            return buildReport(paths, filesWritten, errors, startedAt, ExportStatus.FAILED);
        }

        try {
            filesWritten.add(bundleWriter.write(paths, request, clock.instant()));
            bundleWritten = true;
        } catch (IOException e) {
            errors.add(new ExportIssue("bundle", describe(e)));
        }

        ExportStatus status = errors.isEmpty()
                ? ExportStatus.SUCCESS
                : bundleWritten ? ExportStatus.PARTIAL : ExportStatus.FAILED;

        long durationMillis = Duration.between(startedAt, clock.instant()).toMillis();
        try {
            List<Path> manifestFiles = new ArrayList<>(filesWritten);
            manifestFiles.add(paths.manifestFile());
            filesWritten.add(manifestWriter.write(
                    paths,
                    request,
                    status,
                    manifestFiles,
                    errors,
                    durationMillis,
                    clock.instant()));
        } catch (IOException e) {
            errors.add(new ExportIssue("manifest", describe(e)));
            status = ExportStatus.FAILED;
        }

        return new ExportReport(
                status,
                paths.rootDir(),
                paths.manifestFile(),
                paths.bundleFile(),
                filesWritten,
                errors,
                Duration.between(startedAt, clock.instant()).toMillis());
    }

    private ExportReport buildReport(
            ExportOutputPaths paths,
            List<Path> filesWritten,
            List<ExportIssue> errors,
            Instant startedAt,
            ExportStatus status) {
        return new ExportReport(
                status,
                paths.rootDir(),
                paths.manifestFile(),
                paths.bundleFile(),
                filesWritten,
                errors,
                Duration.between(startedAt, clock.instant()).toMillis());
    }

    private static String describe(IOException error) {
        return error.getClass().getSimpleName() + ": " + (error.getMessage() == null ? "no message" : error.getMessage());
    }
}
