package io.github.jmecn.minecraftwebexport.pipeline;

import io.github.jmecn.minecraftwebexport.MweMod;
import io.github.jmecn.minecraftwebexport.emi.item.ItemIndexExporter;
import io.github.jmecn.minecraftwebexport.emi.support.Log;
import io.github.jmecn.minecraftwebexport.emi.tag.TagIndexWriter;
import io.github.jmecn.minecraftwebexport.io.ExportWriteQueue;
import io.github.jmecn.minecraftwebexport.model.pipeline.ExportContext;
import io.github.jmecn.minecraftwebexport.model.pipeline.ItemIndexExportResult;
import io.github.jmecn.minecraftwebexport.model.pipeline.TagExportResult;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import net.minecraft.server.MinecraftServer;

public final class ExportPrep {

    private ExportPrep() {
    }

    public static CompletableFuture<ExportPrepResult> runServerPhaseAsync(
            Path outputRoot,
            MinecraftServer server,
            ExportContext context,
            ExportWriteQueue writes) {
        if (server == null || !context.hasRecipes()) {
            return CompletableFuture.completedFuture(ExportPrepResult.empty());
        }
        return CompletableFuture.supplyAsync(() -> {
            long startedAt = System.currentTimeMillis();
            MweMod.LOGGER.info("{} starting server prep (tags + items index) while recipes export", Log.EMI);
            try {
                ExportPrepResult result = ServerTasks.call(server, () -> runServerPhase(outputRoot, server, context, writes));
                MweMod.LOGGER.info(
                        "{} server prep done ({} ms)",
                        Log.EMI,
                        System.currentTimeMillis() - startedAt);
                return result;
            } catch (CompletionException e) {
                throw e;
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }

    private static ExportPrepResult runServerPhase(
            Path outputRoot,
            MinecraftServer server,
            ExportContext context,
            ExportWriteQueue writes) {
        try {
            TagExportResult tagExport = TagExportResult.empty();
            if (!context.tagIds().isEmpty()) {
                tagExport = TagIndexWriter.export(outputRoot, server, context.tagIds(), writes);
            }
            ItemIndexExportResult itemExport = ItemIndexExporter.export(
                    outputRoot, server, context, tagExport.catalog(), writes);
            return new ExportPrepResult(tagExport, itemExport);
        } catch (IOException e) {
            throw new CompletionException(e);
        }
    }
}
