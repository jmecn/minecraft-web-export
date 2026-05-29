package io.github.jmecn.minecraftwebexport.export.module;

import java.io.IOException;

/**
 * Upper-layer export extension (e.g. {@code field-guide-export}). Core calls
 * {@link #collectSeeds} / {@link #buildHints} before EMI export and {@link #exportExtras} after.
 */
public interface ExportModule {

    String moduleId();

    ExportSeeds collectSeeds(ExportScope scope);

    default ExportHints buildHints(ExportScope scope, ExportSeeds mergedSeeds) {
        return ExportHints.defaults();
    }

    default void exportExtras(ExportScope scope, ExportResult result) throws IOException {
    }
}
