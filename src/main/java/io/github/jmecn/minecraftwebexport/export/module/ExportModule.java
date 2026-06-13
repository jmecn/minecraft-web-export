package io.github.jmecn.minecraftwebexport.export.module;

import net.minecraft.client.Minecraft;

import java.io.IOException;

public interface ExportModule {

    String moduleId();

    default void beforeEmiExport(ExportScope scope, Minecraft client) throws IOException {
    }

    ExportSeeds collectSeeds(ExportScope scope);

    default ExportHints buildHints(ExportScope scope, ExportSeeds mergedSeeds) {
        return ExportHints.defaults();
    }

    default void exportExtras(ExportScope scope, ExportResult result) throws IOException {
    }
}
