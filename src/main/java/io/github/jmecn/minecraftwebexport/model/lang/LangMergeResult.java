package io.github.jmecn.minecraftwebexport.model.lang;


public record LangMergeResult(
        int languagesWritten,
        long totalBytes,
        int duplicateKeyWarnings,
        int filteredKeysRequested,
        int keysSkipped,
        int keysPerLanguage) {
}
