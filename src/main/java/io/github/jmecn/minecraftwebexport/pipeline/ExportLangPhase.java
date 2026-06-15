package io.github.jmecn.minecraftwebexport.pipeline;

import io.github.jmecn.minecraftwebexport.MweMod;
import io.github.jmecn.minecraftwebexport.emi.item.ItemsLangExporter;
import io.github.jmecn.minecraftwebexport.emi.item.NameKeysExporter;
import io.github.jmecn.minecraftwebexport.emi.lang.LangDerivation;
import io.github.jmecn.minecraftwebexport.emi.lang.Merger;
import io.github.jmecn.minecraftwebexport.emi.support.Log;
import io.github.jmecn.minecraftwebexport.io.ExportWriteQueue;
import io.github.jmecn.minecraftwebexport.model.pipeline.ExportContext;
import io.github.jmecn.minecraftwebexport.model.pipeline.ItemIndexExportResult;
import io.github.jmecn.minecraftwebexport.model.pipeline.LangExportResult;
import io.github.jmecn.minecraftwebexport.model.pipeline.Plan;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public final class ExportLangPhase {

    private ExportLangPhase() {
    }

    public static CompletableFuture<ExportLangPhaseResult> runAsyncAfterPrep(
            Path outputRoot,
            net.minecraft.client.Minecraft client,
            ExportContext context,
            Plan plan,
            ExportPrepResult prep,
            ExportWriteQueue writes,
            boolean hasRecipeExport) {
        if (!hasRecipeExport) {
            return CompletableFuture.completedFuture(ExportLangPhaseResult.empty());
        }
        return CompletableFuture.supplyAsync(() -> {
            long startedAt = System.currentTimeMillis();
            MweMod.LOGGER.info("{} starting lang merge + items-lang while recipes export", Log.EMI);
            try {
                ExportLangPhaseResult result = runBlocking(outputRoot, client, context, plan, prep, writes);
                MweMod.LOGGER.info(
                        "{} lang phase bootstrap done ({} ms), items-lang in flight",
                        Log.EMI,
                        System.currentTimeMillis() - startedAt);
                return result;
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        });
    }

    private static ExportLangPhaseResult runBlocking(
            Path outputRoot,
            net.minecraft.client.Minecraft client,
            ExportContext context,
            Plan plan,
            ExportPrepResult prep,
            ExportWriteQueue writes) throws IOException {
        ItemIndexExportResult itemExport = prep.itemExport();
        Map<String, String> nameKeys = NameKeysExporter.export(outputRoot, client, itemExport.index(), writes);

        Set<String> langKeys = LangDerivation.withNameKeys(LangDerivation.derive(context), nameKeys);
        LangExportResult langExport =
                Merger.exportEmiLang(outputRoot, client, langKeys, plan.hints(), writes);

        int itemCount = itemExport.indexedCount();
        List<String> languages = langExport.locales();
        CompletableFuture<List<String>> itemsLangFuture = CompletableFuture.completedFuture(List.of());
        if (itemCount > 0 && !languages.isEmpty()) {
            MweMod.LOGGER.info(
                    "{} starting async build for {} locales",
                    Log.ITEMS_LANG,
                    languages.size());
            itemsLangFuture = ItemsLangExporter.exportAsync(
                    outputRoot,
                    languages,
                    List.copyOf(itemExport.indexedItemIds()),
                    Set.copyOf(context.fluidRegistryIds()),
                    langExport.tablesByLocale(),
                    Map.copyOf(nameKeys),
                    writes);
        }

        return new ExportLangPhaseResult(Map.copyOf(nameKeys), langExport, itemsLangFuture);
    }
}
