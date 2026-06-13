package io.github.jmecn.minecraftwebexport.model.emi.icon;

public record ItemIconResult(
        int itemsWritten,
        int fluidsWritten,
        int atlasPages,
        int itemFailures,
        int fluidFailures,
        int fluidsSkippedNoStack,
        long atlasPngBytes,
        long indexBytes,
        long cssBytes) {

    public int totalSpritesWritten() {
        return itemsWritten + fluidsWritten;
    }

    public int failures() {
        return itemFailures + fluidFailures;
    }
}
