package io.github.jmecn.minecraftwebexport.model.pipeline;

import io.github.jmecn.minecraftwebexport.model.item.ExportedTagCatalog;

public record TagExportResult(int tagRefCount, ExportedTagCatalog catalog) {

    public static TagExportResult empty() {
        return new TagExportResult(0, ExportedTagCatalog.empty());
    }
}
