package io.github.jmecn.minecraftwebexport.model.icon;

public record AtlasBuildResult(
        int itemsPlaced,
        int pageCount,
        long indexBytes,
        long pngBytes,
        long cssBytes) {
}
