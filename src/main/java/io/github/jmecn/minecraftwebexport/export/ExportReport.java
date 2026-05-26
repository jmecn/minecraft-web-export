package io.github.jmecn.minecraftwebexport.export;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public record ExportReport(
        ExportStatus status,
        Path outputRoot,
        Path manifestFile,
        Path bundleFile,
        List<Path> filesWritten,
        List<ExportIssue> errors,
        long durationMillis) {

    public ExportReport {
        status = Objects.requireNonNull(status, "status");
        outputRoot = Objects.requireNonNull(outputRoot, "outputRoot").toAbsolutePath().normalize();
        manifestFile = Objects.requireNonNull(manifestFile, "manifestFile").toAbsolutePath().normalize();
        bundleFile = Objects.requireNonNull(bundleFile, "bundleFile").toAbsolutePath().normalize();
        filesWritten = List.copyOf(filesWritten);
        errors = List.copyOf(errors);
    }

    public boolean isSuccessLike() {
        return status == ExportStatus.SUCCESS || status == ExportStatus.PARTIAL;
    }
}
