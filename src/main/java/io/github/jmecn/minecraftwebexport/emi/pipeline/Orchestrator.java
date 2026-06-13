package io.github.jmecn.minecraftwebexport.emi.pipeline;
import io.github.jmecn.minecraftwebexport.Constants;
import io.github.jmecn.minecraftwebexport.emi.bundle.ManifestWriter;
import io.github.jmecn.minecraftwebexport.emi.bundle.Paths;
import io.github.jmecn.minecraftwebexport.emi.icon.ItemIconWriter;
import io.github.jmecn.minecraftwebexport.emi.item.IndexWriter;
import io.github.jmecn.minecraftwebexport.emi.item.NameKeysWriter;
import io.github.jmecn.minecraftwebexport.emi.item.SearchIndexWriter;
import io.github.jmecn.minecraftwebexport.emi.lang.ClosureKeys;
import io.github.jmecn.minecraftwebexport.emi.lang.Languages;
import io.github.jmecn.minecraftwebexport.emi.lang.Merger;
import io.github.jmecn.minecraftwebexport.emi.lang.UsedKeysCollector;
import io.github.jmecn.minecraftwebexport.emi.recipe.CardWriter;
import io.github.jmecn.minecraftwebexport.emi.recipe.TextureWriter;
import io.github.jmecn.minecraftwebexport.emi.support.Log;
import io.github.jmecn.minecraftwebexport.emi.tag.MembersIndexWriter;
import io.github.jmecn.minecraftwebexport.model.emi.EmiExportReport;
import io.github.jmecn.minecraftwebexport.model.emi.category.CategoryIndexResult;
import io.github.jmecn.minecraftwebexport.model.emi.icon.ItemIconResult;
import io.github.jmecn.minecraftwebexport.model.emi.item.ItemIndexResult;
import io.github.jmecn.minecraftwebexport.model.emi.item.SearchIndexResult;
import io.github.jmecn.minecraftwebexport.model.emi.lang.LangMergeResult;
import io.github.jmecn.minecraftwebexport.model.emi.recipe.CardWriteResult;
import io.github.jmecn.minecraftwebexport.model.emi.tag.TagMembersResult;
import io.github.jmecn.minecraftwebexport.model.pipeline.Mode;
import io.github.jmecn.minecraftwebexport.model.pipeline.Plan;
import io.github.jmecn.minecraftwebexport.MweMod;
import io.github.jmecn.minecraftwebexport.pipeline.Planner;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public final class Orchestrator {


    public EmiExportReport export(Path outputRoot, Minecraft client) throws IOException {
        return export(outputRoot, client, Plan.full(Planner.collectExportableRecipeIds(client)));
    }

    public EmiExportReport export(Path outputRoot, Minecraft client, Plan plan) throws IOException {
        Set<String> recipeIds = plan.recipeIds();
        boolean langPrune = plan.mode() == Mode.FULL && Merger.isLangPruneEnabled();
        UsedKeysCollector langCollector = langPrune ? new UsedKeysCollector() : null;

        CardWriteResult cards = exportRecipeCards(outputRoot, client, recipeIds, langCollector);
        client.renderBuffers().bufferSource().endBatch();

        MinecraftServer server = client.getSingleplayerServer();
        Set<String> tagIds = plan.tagsForExport(cards.referencedTags());
        if (plan.mode() == Mode.FULL && server != null && cards.written() > 0) {
            int layoutTagCount = tagIds.size();
            tagIds = IndexWriter.planFullModeTagExport(
                    server,
                    cards.layoutsByRecipeId(),
                    plan.seedItemsForIndex(),
                    cards.referencedTags());
            MweMod.LOGGER.info(
                    "{} full tag export: {} -> {} tags",
                    Log.TAGS,
                    layoutTagCount,
                    tagIds.size());
        }

        TagMembersResult tags = new TagMembersResult(
                0, 0, 0, 0, 0, 0, 0, Set.of(), Set.of(), Set.of());
        if (server != null && MembersIndexWriter.isEnabled() && !tagIds.isEmpty()) {
            tags = MembersIndexWriter.export(outputRoot, server, tagIds);
        } else if (!tagIds.isEmpty() && server == null) {
            MweMod.LOGGER.warn("{} tag-members skipped: no integrated server", Log.EMI);
        }

        CategoryIndexResult categories = cards.written() > 0
                ? io.github.jmecn.minecraftwebexport.emi.category.IndexWriter.export(outputRoot, client)
                : new CategoryIndexResult(0, 0);
        ItemIndexResult items = cards.written() > 0
                ? IndexWriter.export(
                        outputRoot,
                        server,
                        cards.layoutsByRecipeId(),
                        plan.seedItemsForIndex())
                : new ItemIndexResult(0, 0, 0, 0);

        if (cards.written() > 0 && NameKeysWriter.isEnabled()) {
            NameKeysWriter.export(outputRoot, client);
        }

        if (langCollector != null && cards.written() > 0) {
            langCollector.collectFromCategoriesIndex(outputRoot);
            langCollector.collectFromItemNameKeys(outputRoot);
            langCollector.collectFromItemsIndex(outputRoot);
            langCollector.collectFromTagsIndex(outputRoot);
            MweMod.LOGGER.info("{} lang prune: {} used keys collected", Log.LANG, langCollector.size());
        }

        Set<String> langKeys = resolveLangMergeKeys(plan, langCollector);
        Path emiRoot = Paths.resolve(outputRoot, "");
        Path composeDir = emiRoot.resolve(Constants.COMPOSE_LANG_DIR);
        if (plan.mode() == Mode.SCOPED && items.itemCount() > 0) {
            langKeys = augmentScopedLangKeys(langKeys, plan, emiRoot);
        }
        boolean langPruneForWeb = langPrune && langCollector != null;
        boolean composeLangForItems = items.itemCount() > 0
                && Merger.isEnabled()
                && (langPruneForWeb || plan.mode() == Mode.SCOPED);

        if (composeLangForItems) {
            Merger.exportTo(composeDir, client, null, null, plan.hints());
        }

        LangMergeResult langs = Merger.isEnabled()
                ? Merger.exportEmiLang(
                        outputRoot,
                        client,
                        langPruneForWeb ? Merger.filterWebDeployKeys(langKeys) : langKeys,
                        plan.hints())
                : emptyLangResult();

        List<String> languages = List.of();
        SearchIndexResult itemsLang = SearchIndexResult.EMPTY;
        if (items.itemCount() > 0 && SearchIndexWriter.isEnabled()) {
            languages = exportedLanguages(outputRoot);
            if (languages.isEmpty()) {
                languages = Languages.resolve(plan.hints()).stream().sorted().toList();
            }
            if (!languages.isEmpty()) {
                itemsLang = SearchIndexWriter.export(outputRoot, languages, composeLangForItems);
            }
        }

        if (composeLangForItems && Files.isDirectory(composeDir)) {
            try (var walk = Files.walk(composeDir)) {
                walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException e) {
                        MweMod.LOGGER.warn("{} failed to delete {}: {}", Log.LANG, path, e.toString());
                    }
                });
            }
        }

        if (languages.isEmpty()) {
            languages = exportedLanguages(outputRoot);
        }

        Set<String> itemsForIcons = plan.itemsForIcons(cards.referencedItems());
        Set<String> fluidsForIcons = plan.fluidsForIcons(cards.referencedFluids());

        ItemIconResult icons = ItemIconWriter.isEnabled()
                ? ItemIconWriter.export(
                outputRoot,
                client,
                itemsForIcons,
                fluidsForIcons,
                null,
                cards.iconVariants())
                : emptyIconResult();

        ManifestWriter.write(
                outputRoot,
                languages,
                cards.imageScale(),
                cards.written(),
                itemsLang.locales());

        TextureWriter.export(outputRoot, client, java.util.Set.of());

        if (plan.mode() == Mode.SCOPED) {
            MweMod.LOGGER.info(
                    "{} scoped export complete: {}/{} layouts, {} categories, {} indexed items, {} tags, {} lang files, {} icon sprites",
                    Log.EMI,
                    cards.written(),
                    recipeIds.size(),
                    categories.categoryCount(),
                    items.itemCount(),
                    tags.tagsIndexed(),
                    langs.languagesWritten(),
                    icons.totalSpritesWritten());
        } else {
            MweMod.LOGGER.info(
                    "{} export complete: {}/{} recipe cards, {} categories, {} indexed items, {} tags, {} lang files, {} icon sprites",
                    Log.EMI,
                    cards.written(),
                    recipeIds.size(),
                    categories.categoryCount(),
                    items.itemCount(),
                    tags.tagsIndexed(),
                    langs.languagesWritten(),
                    icons.totalSpritesWritten());
        }

        return new EmiExportReport(
                outputRoot,
                recipeIds.size(),
                cards.written(),
                items.itemCount(),
                tags.tagsIndexed(),
                langs.languagesWritten(),
                icons.totalSpritesWritten());
    }

    private static Set<String> augmentScopedLangKeys(Set<String> langKeys, Plan plan, Path emiRoot) {
        Set<String> seed = langKeys == null ? Set.of() : langKeys;
        try {
            Set<String> merged = ClosureKeys.mergeClosureLangKeys(
                    seed,
                    SearchIndexWriter.readIndexedItemIds(emiRoot),
                    SearchIndexWriter.readFluidRegistryIds(emiRoot));
            merged = ClosureKeys.mergeTagLangKeys(merged, plan.closureTagIds());
            return merged.isEmpty() ? null : merged;
        } catch (IOException e) {
            MweMod.LOGGER.warn("{} scoped lang keys: failed to read items index: {}", Log.LANG, e.toString());
            Set<String> merged = ClosureKeys.mergeTagLangKeys(seed, plan.closureTagIds());
            return merged.isEmpty() ? langKeys : merged;
        }
    }

    private static Set<String> resolveLangMergeKeys(Plan plan, UsedKeysCollector langCollector) {
        if (plan.mode() == Mode.SCOPED) {
            return plan.langKeysForExport();
        }
        if (langCollector != null && langCollector.size() > 0) {
            return langCollector.snapshot();
        }
        return null;
    }

    private static CardWriteResult exportRecipeCards(
            Path outputRoot,
            Minecraft client,
            Set<String> recipeIds,
            UsedKeysCollector langCollector) throws IOException {
        if (CardWriter.isEnabled()) {
            return CardWriter.export(outputRoot, client, recipeIds, langCollector);
        }
        MweMod.LOGGER.info("{} recipe card export disabled by configuration", Log.EMI);
        return new CardWriteResult(
                recipeIds.size(),
                0,
                0,
                0,
                0,
                0,
                CardWriter.imageScale(),
                Set.of(),
                Set.of(),
                Set.of(),
                java.util.Map.of(),
                java.util.Map.of());
    }

    private static LangMergeResult emptyLangResult() {
        return new LangMergeResult(0, 0, 0, 0, 0, 0);
    }

    private static ItemIconResult emptyIconResult() {
        return new ItemIconResult(0, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    private static List<String> exportedLanguages(Path outputRoot) throws IOException {
        Path langDir = Paths.resolve(outputRoot, Constants.LANG_DIR);
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
