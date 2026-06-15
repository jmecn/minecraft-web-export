package io.github.jmecn.minecraftwebexport.pipeline;

import io.github.jmecn.minecraftwebexport.model.pipeline.CreativeTabExportResult;
import io.github.jmecn.minecraftwebexport.model.pipeline.ItemIndexExportResult;
import io.github.jmecn.minecraftwebexport.model.pipeline.TagExportResult;

public record ExportPrepResult(
        TagExportResult tagExport,
        ItemIndexExportResult itemExport,
        CreativeTabExportResult creativeTabExport) {

    public static ExportPrepResult empty() {
        return new ExportPrepResult(
                TagExportResult.empty(), ItemIndexExportResult.empty(), CreativeTabExportResult.empty());
    }
}
