package io.github.jmecn.minecraftwebexport.model.emi.lang;


public record LangMergeResult(
        int languagesWritten,
        long totalBytes,
        int duplicateKeyWarnings,
        int closureKeysRequested,
        int keysSkipped,
        int keysPerLanguage) {
}
