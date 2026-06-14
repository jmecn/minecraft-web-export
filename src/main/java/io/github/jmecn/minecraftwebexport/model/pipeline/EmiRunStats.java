package io.github.jmecn.minecraftwebexport.model.pipeline;

public record EmiRunStats(
        int recipesRequested,
        int recipesWritten,
        int itemIndexCount,
        int tagIndexCount,
        int languagesWritten,
        int iconsWritten) {
}
