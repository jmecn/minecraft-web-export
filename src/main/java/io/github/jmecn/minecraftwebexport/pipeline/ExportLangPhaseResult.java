package io.github.jmecn.minecraftwebexport.pipeline;

import io.github.jmecn.minecraftwebexport.model.pipeline.LangExportResult;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public record ExportLangPhaseResult(
        Map<String, String> nameKeys,
        LangExportResult langExport,
        CompletableFuture<List<String>> itemsLangFuture) {

    public static ExportLangPhaseResult empty() {
        return new ExportLangPhaseResult(Map.of(), LangExportResult.empty(), CompletableFuture.completedFuture(List.of()));
    }
}
