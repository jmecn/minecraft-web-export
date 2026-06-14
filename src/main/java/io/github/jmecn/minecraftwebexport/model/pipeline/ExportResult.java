package io.github.jmecn.minecraftwebexport.model.pipeline;

import java.nio.file.Path;
import java.util.Objects;

public record ExportResult(
        Path outputRoot,
        int recipesRequested,
        int recipesWritten,
        int itemIndexCount,
        int tagIndexCount,
        int languagesWritten,
        int iconsWritten) {

    public ExportResult {
        outputRoot = Objects.requireNonNull(outputRoot, "outputRoot").toAbsolutePath().normalize();
    }

    public static ExportResult from(
            Path outputRoot,
            int recipesRequested,
            int recipesWritten,
            int itemIndexCount,
            int tagIndexCount,
            int languagesWritten,
            int iconsWritten) {
        return new ExportResult(
                outputRoot,
                recipesRequested,
                recipesWritten,
                itemIndexCount,
                tagIndexCount,
                languagesWritten,
                iconsWritten);
    }
}
