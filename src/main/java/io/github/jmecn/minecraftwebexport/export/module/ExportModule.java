package io.github.jmecn.minecraftwebexport.export.module;

import net.minecraft.client.Minecraft;

import java.io.IOException;

/**
 * Upper-layer export extension (e.g. {@code field-guide-export}). Core calls
 * {@link #beforeEmiExport} then {@link #collectSeeds} / {@link #buildHints} before EMI export and
 * {@link #exportExtras} after.
 */
public interface ExportModule {

    String moduleId();

    /**
     * Runs before scoped EMI planning (e.g. write {@code guide-export/} and populate seeds).
     */
    default void beforeEmiExport(ExportScope scope, Minecraft client) throws IOException {
    }

    ExportSeeds collectSeeds(ExportScope scope);

    default ExportHints buildHints(ExportScope scope, ExportSeeds mergedSeeds) {
        return ExportHints.defaults();
    }

    default void exportExtras(ExportScope scope, ExportResult result) throws IOException {
    }
}
