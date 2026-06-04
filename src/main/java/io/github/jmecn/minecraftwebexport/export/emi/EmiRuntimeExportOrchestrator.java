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
        return export(outputRoot, client, ExportPlan.full(ExportPlanner.collectExportableRecipeIds(client)));
    }

    public Report export(Path outputRoot, Minecraft client, ExportPlan plan) throws IOException {
        Set<String> recipeIds = plan.recipeIds();
        boolean langPrune = plan.mode() == ExportMode.FULL && LangMergerExporter.isLangPruneEnabled();
        LangUsedKeysCollector langCollector = langPrune ? new LangUsedKeysCollector() : null;

        EmiRecipeCardExporter.Result cards = exportRecipeCards(outputRoot, client, recipeIds, langCollector);
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

        EmiRecipeCategoriesExporter.Result categories = cards.written() > 0
                ? EmiRecipeCategoriesExporter.export(outputRoot, client)
                : new EmiRecipeCategoriesExporter.Result(0, 0);
        EmiItemsIndexExporter.Result items = cards.written() > 0
                ? EmiItemsIndexExporter.export(outputRoot, server, cards.layoutsByRecipeId())
                : new EmiItemsIndexExporter.Result(0, 0, 0, 0);

        if (cards.written() > 0 && ItemNameKeysExporter.isEnabled()) {
            ItemNameKeysExporter.export(outputRoot, client);
        }

        if (langCollector != null && cards.written() > 0) {
            langCollector.collectFromCategoriesIndex(outputRoot);
            langCollector.collectFromItemNameKeys(outputRoot);
            langCollector.collectFromItemsIndex(outputRoot);
            langCollector.collectFromTagsIndex(outputRoot);
            LOGGER.info("{} lang prune: {} used keys collected", ExportLog.LANG, langCollector.size());
        }

        Set<String> langKeys = resolveLangMergeKeys(plan, langCollector);
        Path emiRoot = EmiBundlePaths.resolve(outputRoot, "");
        Path composeDir = emiRoot.resolve(EmiBundlePaths.COMPOSE_LANG_DIR);
        if (plan.mode() == ExportMode.SCOPED && items.itemCount() > 0) {
            langKeys = augmentScopedLangKeys(langKeys, plan, emiRoot);
        }
        boolean langPruneForWeb = langPrune && langCollector != null;
        boolean composeLangForItems = items.itemCount() > 0
                && LangMergerExporter.isEnabled()
                && (langPruneForWeb || plan.mode() == ExportMode.SCOPED);

        // items-lang needs full mod lang (compose-lang). SCOPED deploy keeps closure-filtered emi/lang/.
        if (composeLangForItems) {
            LangMergerExporter.exportTo(composeDir, client, null, null, plan.hints());
        }

        LangMergerExporter.Result langs = LangMergerExporter.isEnabled()
                ? LangMergerExporter.exportEmiLang(
                        outputRoot,
                        client,
                        langPruneForWeb ? LangMergerExporter.filterWebDeployKeys(langKeys) : langKeys,
                        plan.hints())
                : emptyLangResult();

        List<String> languages = List.of();
        ItemsSearchIndexExporter.Result itemsLang = ItemsSearchIndexExporter.Result.EMPTY;
        if (items.itemCount() > 0 && ItemsSearchIndexExporter.isEnabled()) {
            languages = exportedLanguages(outputRoot);
            if (languages.isEmpty()) {
                languages = MinecraftWebExportLanguages.resolve(plan.hints()).stream().sorted().toList();
            }
            if (!languages.isEmpty()) {
                itemsLang = ItemsSearchIndexExporter.export(outputRoot, languages, composeLangForItems);
            }
        }

        if (composeLangForItems && Files.isDirectory(composeDir)) {
            try (var walk = Files.walk(composeDir)) {
                walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException e) {
                        LOGGER.warn("{} failed to delete {}: {}", ExportLog.LANG, path, e.toString());
                    }
                });
            }
        }

        if (languages.isEmpty()) {
            languages = exportedLanguages(outputRoot);
        }

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

        EmiBundleManifestWriter.write(
                outputRoot,
                languages,
                cards.imageScale(),
                cards.written(),
                itemsLang.locales());

        // Tag/list popovers in emi-recipe-renderer need EMI GUI nine-patch + widget sprites.
        RecipeTextureExporter.export(outputRoot, client, java.util.Set.of());

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

    private static Set<String> augmentScopedLangKeys(Set<String> langKeys, ExportPlan plan, Path emiRoot) {
        Set<String> seed = langKeys == null ? Set.of() : langKeys;
        try {
            Set<String> merged = LangClosureKeys.mergeClosureLangKeys(
                    seed,
                    ItemsSearchIndexExporter.readIndexedItemIds(emiRoot),
                    ItemsSearchIndexExporter.readFluidRegistryIds(emiRoot));
            merged = LangClosureKeys.mergeTagLangKeys(merged, plan.closureTagIds());
            return merged.isEmpty() ? null : merged;
        } catch (IOException e) {
            LOGGER.warn("{} scoped lang keys: failed to read items index: {}", ExportLog.LANG, e.toString());
            Set<String> merged = LangClosureKeys.mergeTagLangKeys(seed, plan.closureTagIds());
            return merged.isEmpty() ? langKeys : merged;
        }
    }

    private static Set<String> resolveLangMergeKeys(ExportPlan plan, LangUsedKeysCollector langCollector) {
        if (plan.mode() == ExportMode.SCOPED) {
            return plan.langKeysForExport();
        }
        if (langCollector != null && langCollector.size() > 0) {
            return langCollector.snapshot();
        }
        return null;
    }

    private static EmiRecipeCardExporter.Result exportRecipeCards(
            Path outputRoot,
            Minecraft client,
            Set<String> recipeIds,
            LangUsedKeysCollector langCollector) throws IOException {
        if (EmiRecipeCardExporter.isEnabled()) {
            return EmiRecipeCardExporter.export(outputRoot, client, recipeIds, langCollector);
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
