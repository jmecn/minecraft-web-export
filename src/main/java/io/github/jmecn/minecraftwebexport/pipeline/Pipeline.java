package io.github.jmecn.minecraftwebexport.pipeline;

import io.github.jmecn.minecraftwebexport.Constants;
import io.github.jmecn.minecraftwebexport.MweMod;
import io.github.jmecn.minecraftwebexport.emi.EmiPaths;
import io.github.jmecn.minecraftwebexport.emi.icon.CategoryIconWriter;
import io.github.jmecn.minecraftwebexport.emi.icon.ItemIconWriter;
import io.github.jmecn.minecraftwebexport.emi.icon.PlaceholderRenderer;
import io.github.jmecn.minecraftwebexport.emi.item.ItemIndexExporter;
import io.github.jmecn.minecraftwebexport.emi.item.ItemsLangExporter;
import io.github.jmecn.minecraftwebexport.emi.item.NameKeysExporter;
import io.github.jmecn.minecraftwebexport.emi.lang.ClosureKeys;
import io.github.jmecn.minecraftwebexport.emi.lang.Languages;
import io.github.jmecn.minecraftwebexport.emi.lang.Merger;
import io.github.jmecn.minecraftwebexport.emi.lang.UsedKeysCollector;
import io.github.jmecn.minecraftwebexport.emi.recipe.RecipeWriter;
import io.github.jmecn.minecraftwebexport.emi.recipe.TextureWriter;
import io.github.jmecn.minecraftwebexport.emi.support.Log;
import io.github.jmecn.minecraftwebexport.emi.tag.MembersIndexWriter;
import io.github.jmecn.minecraftwebexport.io.JsonIO;
import io.github.jmecn.minecraftwebexport.model.bundle.Bundle;
import io.github.jmecn.minecraftwebexport.model.bundle.ItemsLangRef;
import io.github.jmecn.minecraftwebexport.model.emi.icon.CategoryIconResult;
import io.github.jmecn.minecraftwebexport.model.emi.icon.ItemIconResult;
import io.github.jmecn.minecraftwebexport.model.emi.item.ItemIndexResult;
import io.github.jmecn.minecraftwebexport.model.emi.item.ItemsLangExportResult;
import io.github.jmecn.minecraftwebexport.model.emi.lang.LangMergeResult;
import io.github.jmecn.minecraftwebexport.model.emi.recipe.RecipeWriteResult;
import io.github.jmecn.minecraftwebexport.model.emi.tag.TagMembersResult;
import io.github.jmecn.minecraftwebexport.model.pipeline.ExportResult;
import io.github.jmecn.minecraftwebexport.model.pipeline.Mode;
import io.github.jmecn.minecraftwebexport.model.pipeline.Plan;
import io.github.jmecn.minecraftwebexport.model.pipeline.Scope;
import io.github.jmecn.minecraftwebexport.model.pipeline.Seeds;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;

public final class Pipeline {

    private Pipeline() {
    }

    public static ExportResult run(Path outputRoot, Path gameDirectory, Minecraft client) throws IOException {
        Objects.requireNonNull(outputRoot, "outputRoot");
        Objects.requireNonNull(gameDirectory, "gameDirectory");
        Objects.requireNonNull(client, "client");

        Mode mode = Mode.current();
        Scope scope = new Scope(outputRoot, gameDirectory, mode);
        List<Module> modules = ModuleRegistry.modules();

        for (Module module : modules) {
            module.beforeEmiExport(scope, client);
        }

        Plan plan = Planner.plan(client, mode, modules, scope);
        Seeds mergedSeeds = plan.mode() == Mode.FULL ? Seeds.empty() : plan.sourceSeeds();

        if (plan.mode() == Mode.SCOPED) {
            MweMod.LOGGER.info(
                    "[export] scoped export via modules {} (recipes={})",
                    modules.stream().map(Module::moduleId).toList(),
                    plan.recipeIds().size());
        }

        EmiRunStats stats = exportEmi(outputRoot, client, plan);

        ExportResult result = ExportResult.from(
                scope,
                plan,
                outputRoot,
                stats.recipesRequested(),
                stats.recipesWritten(),
                stats.itemIndexCount(),
                stats.tagIndexCount(),
                stats.languagesWritten(),
                stats.iconsWritten(),
                mergedSeeds,
                modules);

        for (Module module : modules) {
            module.exportExtras(scope, result);
        }
        return result;
    }

    private static EmiRunStats exportEmi(Path outputRoot, Minecraft client, Plan plan) throws IOException {
        Set<String> recipeIds = plan.recipeIds();
        boolean langPrune = plan.mode() == Mode.FULL && Merger.isLangPruneEnabled();
        UsedKeysCollector langCollector = langPrune ? new UsedKeysCollector() : null;

        RecipeWriteResult recipes = exportRecipes(outputRoot, client, recipeIds, langCollector);
        client.renderBuffers().bufferSource().endBatch();

        MinecraftServer server = client.getSingleplayerServer();
        Set<String> tagIds = plan.tagsForExport(recipes.referencedTags());
        if (plan.mode() == Mode.FULL && server != null && recipes.written() > 0) {
            int layoutTagCount = tagIds.size();
            tagIds = ItemIndexExporter.planFullModeTagExport(
                    server,
                    recipes.layoutsByRecipeId(),
                    plan.seedItemsForIndex(),
                    recipes.referencedTags());
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

        CategoryIconResult categories = recipes.written() > 0
                ? CategoryIconWriter.export(outputRoot, client)
                : new CategoryIconResult(0, 0, 0, 0, 0);
        ItemIndexResult items = recipes.written() > 0
                ? ItemIndexExporter.export(
                        outputRoot,
                        server,
                        recipes.layoutsByRecipeId(),
                        plan.seedItemsForIndex())
                : new ItemIndexResult(0, 0, 0, 0);

        if (recipes.written() > 0 && NameKeysExporter.isEnabled()) {
            NameKeysExporter.export(outputRoot, client);
        }

        if (langCollector != null && recipes.written() > 0) {
            langCollector.collectFromCategoriesIndex(outputRoot);
            langCollector.collectFromItemNameKeys(outputRoot);
            langCollector.collectFromItemsIndex(outputRoot);
            langCollector.collectFromTagsIndex(outputRoot);
            MweMod.LOGGER.info("{} lang prune: {} used keys collected", Log.LANG, langCollector.size());
        }

        Set<String> langKeys = resolveLangMergeKeys(plan, langCollector);
        Path emiRoot = EmiPaths.resolve(outputRoot, "");
        Path composeDir = emiRoot.resolve(Constants.COMPOSE_LANG_DIR);
        if (plan.mode() == Mode.SCOPED && items.itemCount() > 0) {
            langKeys = augmentScopedLangKeys(langKeys, plan, emiRoot);
        }
        boolean composeLangForItems = items.itemCount() > 0
                && Merger.isEnabled()
                && (langPrune || plan.mode() == Mode.SCOPED);

        if (composeLangForItems) {
            Merger.exportTo(composeDir, client, null, null, plan.hints());
        }

        LangMergeResult langs = Merger.isEnabled()
                ? Merger.exportEmiLang(
                        outputRoot,
                        client,
                        langPrune ? Merger.filterWebDeployKeys(langKeys) : langKeys,
                        plan.hints())
                : emptyLangResult();

        List<String> languages = List.of();
        ItemsLangExportResult itemsLang = ItemsLangExportResult.EMPTY;
        if (items.itemCount() > 0 && ItemsLangExporter.isEnabled()) {
            languages = exportedLanguages(outputRoot);
            if (languages.isEmpty()) {
                languages = Languages.resolve(plan.hints()).stream().sorted().toList();
            }
            if (!languages.isEmpty()) {
                itemsLang = ItemsLangExporter.export(outputRoot, languages, composeLangForItems);
            }
        }

        if (composeLangForItems && Files.isDirectory(composeDir)) {
            try (Stream<Path> walk = Files.walk(composeDir)) {
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

        Set<String> itemsForIcons = plan.itemsForIcons(recipes.referencedItems());
        Set<String> fluidsForIcons = plan.fluidsForIcons(recipes.referencedFluids());

        ItemIconResult icons = ItemIconWriter.isEnabled()
                ? ItemIconWriter.export(
                        outputRoot,
                        client,
                        itemsForIcons,
                        fluidsForIcons,
                        null,
                        recipes.iconVariants())
                : emptyIconResult();

        List<String> itemsLangLocales = itemsLang.locales();
        ItemsLangRef itemsLangRef = itemsLangLocales.isEmpty()
                ? null
                : new ItemsLangRef(Constants.ITEMS_LANG_DIR, List.copyOf(itemsLangLocales));
        Bundle bundle = Bundle.of(
                recipes.imageScale(),
                recipes.written(),
                languages,
                PlaceholderRenderer.REGISTRY_ID,
                itemsLangRef);
        JsonIO.write(EmiPaths.resolve(outputRoot, Constants.BUNDLE_FILE), bundle);

        TextureWriter.export(outputRoot, client, Set.of());

        if (plan.mode() == Mode.SCOPED) {
            MweMod.LOGGER.info(
                    "{} scoped export complete: {}/{} layouts, {} categories, {} indexed items, {} tags, {} lang files, {} icon sprites",
                    Log.EMI,
                    recipes.written(),
                    recipeIds.size(),
                    categories.categoryCount(),
                    items.itemCount(),
                    tags.tagsIndexed(),
                    langs.languagesWritten(),
                    icons.totalSpritesWritten());
        } else {
            MweMod.LOGGER.info(
                    "{} export complete: {}/{} recipes, {} categories, {} indexed items, {} tags, {} lang files, {} icon sprites",
                    Log.EMI,
                    recipes.written(),
                    recipeIds.size(),
                    categories.categoryCount(),
                    items.itemCount(),
                    tags.tagsIndexed(),
                    langs.languagesWritten(),
                    icons.totalSpritesWritten());
        }

        return new EmiRunStats(
                recipeIds.size(),
                recipes.written(),
                items.itemCount(),
                tags.tagsIndexed(),
                langs.languagesWritten(),
                icons.totalSpritesWritten());
    }

    private record EmiRunStats(
            int recipesRequested,
            int recipesWritten,
            int itemIndexCount,
            int tagIndexCount,
            int languagesWritten,
            int iconsWritten) {
    }

    private static Set<String> augmentScopedLangKeys(Set<String> langKeys, Plan plan, Path emiRoot) {
        Set<String> seed = langKeys == null ? Set.of() : langKeys;
        try {
            Set<String> merged = ClosureKeys.mergeClosureLangKeys(
                    seed,
                    ItemsLangExporter.readIndexedItemIds(emiRoot),
                    ItemsLangExporter.readFluidRegistryIds(emiRoot));
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

    private static RecipeWriteResult exportRecipes(
            Path outputRoot,
            Minecraft client,
            Set<String> recipeIds,
            UsedKeysCollector langCollector) throws IOException {
        if (RecipeWriter.isEnabled()) {
            return RecipeWriter.export(outputRoot, client, recipeIds, langCollector);
        }
        MweMod.LOGGER.info("{} recipe export disabled by configuration", Log.EMI);
        return new RecipeWriteResult(
                recipeIds.size(),
                0,
                0,
                0,
                0,
                0,
                RecipeWriter.imageScale(),
                Set.of(),
                Set.of(),
                Set.of(),
                Map.of(),
                Map.of());
    }

    private static LangMergeResult emptyLangResult() {
        return new LangMergeResult(0, 0, 0, 0, 0, 0);
    }

    private static ItemIconResult emptyIconResult() {
        return new ItemIconResult(0, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    private static List<String> exportedLanguages(Path outputRoot) throws IOException {
        Path langDir = EmiPaths.resolve(outputRoot, Constants.LANG_DIR);
        if (!Files.isDirectory(langDir)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(langDir)) {
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
