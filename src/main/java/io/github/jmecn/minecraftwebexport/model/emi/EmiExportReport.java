package io.github.jmecn.minecraftwebexport.model.emi;

import java.nio.file.Path;

public record EmiExportReport(
        Path outputRoot,
        int recipesRequested,
        int recipesWritten,
        int itemIndexCount,
        int tagIndexCount,
        int languagesWritten,
        int iconsWritten) {
}
