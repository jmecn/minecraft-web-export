package io.github.jmecn.minecraftwebexport.export.emi;

import io.github.jmecn.minecraftwebexport.export.module.ExportMode;
import io.github.jmecn.minecraftwebexport.export.module.ExportPlan;
import io.github.jmecn.minecraftwebexport.export.module.ExportPlanner;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public final class EmiRuntimeExportOrchestrator {

    private static final Logger LOGGER = LogManager.getLogger(EmiRuntimeExportOrchestrator.class);

    public record Report(
            Path outputRoot,
            int recipesRequested,
            int recipesWritten,
            int itemIndexCount,
            int tagIndexCount,
            int languagesWritten,
            int iconsWritten) {
    }

    public Report export(Path outputRoot, Minecraft client) throws IOException {
        return export(outputRoot, client, ExportPlan.full(ExportPlanner.collectAllRecipeIds()));
    }

    public Report export(Path outputRoot, Minecraft client, ExportPlan plan) throws IOException {
        Set<String> recipeIds = plan.recipeIds();
        EmiRecipeCardExporter.Result cards = exportRecipeCards(outputRoot, client, recipeIds);
        client.renderBuffers().bufferSource().endBatch();

        Set<String> tagIds = plan.tagsForExport(cards.referencedTags());

        TagMembersIndexExporter.Result tags = new TagMembersIndexExporter.Result(
                0, 0, 0, 0, 0, 0, 0, Set.of(), Set.of(), Set.of());
        MinecraftServer server = client.getSingleplayerServer();
        if (server != null && TagMembersIndexExporter.isEnabled() && !tagIds.isEmpty()) {
            tags = TagMembersIndexExporter.export(outputRoot, server, tagIds);
        } else if (!tagIds.isEmpty() && server == null) {
            LOGGER.warn("{} tag-members skipped: no integrated server", ExportLog.EMI);
        }

        Set<String> langKeys = plan.langKeysForExport();
        LangMergerExporter.Result langs = LangMergerExporter.isEnabled()
                ? LangMergerExporter.exportEmiLang(outputRoot, client, langKeys)
                : emptyLangResult();

        Set<String> itemsForIcons = plan.itemsForIcons(cards.referencedItems());
        Set<String> fluidsForIcons = plan.fluidsForIcons(cards.referencedFluids());

        ItemIconRendererExporter.Result icons = ItemIconRendererExporter.isEnabled()
                ? ItemIconRendererExporter.export(
                outputRoot,
                client,
                itemsForIcons,
                fluidsForIcons,
                null,
                cards.iconVariants())
                : emptyIconResult();

        EmiRecipeCategoriesExporter.Result categories = cards.written() > 0
                ? EmiRecipeCategoriesExporter.export(outputRoot)
                : new EmiRecipeCategoriesExporter.Result(0, 0);
        EmiItemsIndexExporter.Result items = cards.written() > 0
                ? EmiItemsIndexExporter.export(outputRoot, server, cards.layoutsByRecipeId())
                : new EmiItemsIndexExporter.Result(0, 0, 0, 0);
        EmiBundleManifestWriter.write(
                outputRoot,
                exportedLanguages(outputRoot),
                cards.imageScale(),
                cards.written());

        if (plan.mode() == ExportMode.SCOPED) {
            LOGGER.info(
                    "{} scoped export complete: {}/{} layouts, {} categories, {} indexed items, {} tags, {} lang files, {} icon sprites",
                    ExportLog.EMI,
                    cards.written(),
                    recipeIds.size(),
                    categories.categoryCount(),
                    items.itemCount(),
                    tags.tagsIndexed(),
                    langs.languagesWritten(),
                    icons.totalSpritesWritten());
        } else {
            LOGGER.info(
                    "{} export complete: {}/{} recipe cards, {} categories, {} indexed items, {} tags, {} lang files, {} icon sprites",
                    ExportLog.EMI,
                    cards.written(),
                    recipeIds.size(),
                    categories.categoryCount(),
                    items.itemCount(),
                    tags.tagsIndexed(),
                    langs.languagesWritten(),
                    icons.totalSpritesWritten());
        }

        return new Report(
                outputRoot,
                recipeIds.size(),
                cards.written(),
                items.itemCount(),
                tags.tagsIndexed(),
                langs.languagesWritten(),
                icons.totalSpritesWritten());
    }

    private static EmiRecipeCardExporter.Result exportRecipeCards(
            Path outputRoot,
            Minecraft client,
            Set<String> recipeIds) throws IOException {
        if (EmiRecipeCardExporter.isEnabled()) {
            return EmiRecipeCardExporter.export(outputRoot, client, recipeIds);
        }
        LOGGER.info("{} recipe card export disabled by configuration", ExportLog.EMI);
        return new EmiRecipeCardExporter.Result(
                recipeIds.size(),
                0,
                0,
                0,
                0,
                0,
                EmiRecipeCardExporter.imageScale(),
                Set.of(),
                Set.of(),
                Set.of(),
                java.util.Map.of(),
                java.util.Map.of());
    }

    private static LangMergerExporter.Result emptyLangResult() {
        return new LangMergerExporter.Result(0, 0, 0, 0, 0, 0);
    }

    private static ItemIconRendererExporter.Result emptyIconResult() {
        return new ItemIconRendererExporter.Result(0, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    private static List<String> exportedLanguages(Path outputRoot) throws IOException {
        Path langDir = EmiBundlePaths.resolve(outputRoot, EmiBundlePaths.LANG_DIR);
        if (!Files.isDirectory(langDir)) {
            return List.of();
        }
        try (var stream = Files.list(langDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".json"))
                    .map(name -> name.substring(0, name.length() - 5))
                    .sorted(Comparator.naturalOrder())
                    .toList();
        }
    }
}
