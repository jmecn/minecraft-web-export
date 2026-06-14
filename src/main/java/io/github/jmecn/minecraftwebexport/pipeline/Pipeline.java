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
import io.github.jmecn.minecraftwebexport.emi.lang.Languages;
import io.github.jmecn.minecraftwebexport.emi.lang.Merger;
import io.github.jmecn.minecraftwebexport.emi.recipe.RecipePaths;
import io.github.jmecn.minecraftwebexport.emi.recipe.RecipeWriter;
import io.github.jmecn.minecraftwebexport.emi.recipe.TextureWriter;
import io.github.jmecn.minecraftwebexport.emi.support.Log;
import io.github.jmecn.minecraftwebexport.emi.tag.MembersIndexWriter;
import io.github.jmecn.minecraftwebexport.io.ExportWriteQueue;
import io.github.jmecn.minecraftwebexport.model.bundle.Bundle;
import io.github.jmecn.minecraftwebexport.model.bundle.ItemsLangRef;
import io.github.jmecn.minecraftwebexport.model.emi.icon.CategoryIconResult;
import io.github.jmecn.minecraftwebexport.model.emi.icon.ItemIconResult;
import io.github.jmecn.minecraftwebexport.model.emi.item.ItemIndexResult;
import io.github.jmecn.minecraftwebexport.model.emi.item.ItemsLangExportResult;
import io.github.jmecn.minecraftwebexport.model.emi.lang.LangMergeResult;
import io.github.jmecn.minecraftwebexport.model.emi.recipe.RecipeWriteResult;
import io.github.jmecn.minecraftwebexport.model.emi.tag.TagMembersResult;
import io.github.jmecn.minecraftwebexport.model.pipeline.ExportContext;
import io.github.jmecn.minecraftwebexport.model.pipeline.ExportResult;
import io.github.jmecn.minecraftwebexport.model.pipeline.Mode;
import io.github.jmecn.minecraftwebexport.model.pipeline.Plan;
import io.github.jmecn.minecraftwebexport.model.pipeline.Scope;
import io.github.jmecn.minecraftwebexport.model.pipeline.Seeds;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
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
        ExportContext context = plan.context();
        Seeds mergedSeeds = plan.mode() == Mode.FULL ? Seeds.empty() : plan.sourceSeeds();

        if (plan.mode() == Mode.SCOPED) {
            MweMod.LOGGER.info(
                    "[export] scoped export via modules {} (recipes={})",
                    modules.stream().map(Module::moduleId).toList(),
                    context.recipeIds().size());
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
        ExportContext context = plan.context();
        Set<String> recipeIds = context.recipeIds();

        try (ExportWriteQueue writes = new ExportWriteQueue()) {
            RelationPlanner.buildRelations(client, context);
            RecipePaths.ensureRecipeDirectories(outputRoot, recipeIds);

            RecipeWriteResult recipes = RecipeWriter.export(outputRoot, client, context, writes);
            client.renderBuffers().bufferSource().endBatch();

            MinecraftServer server = client.getSingleplayerServer();

            TagMembersResult tags = new TagMembersResult(
                    0, 0, 0, 0, 0, 0, 0, Set.of(), Set.of(), Set.of());
            if (server != null && MembersIndexWriter.isEnabled() && !context.tagIds().isEmpty()) {
                tags = MembersIndexWriter.export(outputRoot, server, context.tagIds(), writes);
            } else if (!context.tagIds().isEmpty() && server == null) {
                MweMod.LOGGER.warn("{} tag-members skipped: no integrated server", Log.EMI);
            }

            CategoryIconResult categories = recipes.written() > 0
                    ? CategoryIconWriter.export(outputRoot, client, context.categoryIds(), writes)
                    : new CategoryIconResult(0, 0, 0, 0, 0);

            ItemIndexResult items = recipes.written() > 0
                    ? ItemIndexExporter.export(outputRoot, server, context, writes)
                    : new ItemIndexResult(0, 0, 0, 0);
            writes.awaitIdle("after items index");

            if (recipes.written() > 0 && NameKeysExporter.isEnabled()) {
                NameKeysExporter.export(outputRoot, client, writes);
            }
            writes.awaitIdle("after name keys");

            Set<String> langKeys = LangPlanner.deriveLangKeys(context);
            LangMergeResult langs = Merger.isEnabled()
                    ? Merger.exportEmiLang(outputRoot, client, langKeys, plan.hints(), writes)
                    : emptyLangResult();
            writes.awaitIdle("after emi lang");

            List<String> languages = List.of();
            ItemsLangExportResult itemsLang = ItemsLangExportResult.EMPTY;
            if (items.itemCount() > 0 && ItemsLangExporter.isEnabled()) {
                languages = exportedLanguages(outputRoot);
                if (languages.isEmpty()) {
                    languages = Languages.resolve(plan.hints()).stream().sorted().toList();
                }
                if (!languages.isEmpty()) {
                    itemsLang = ItemsLangExporter.export(
                            outputRoot,
                            languages,
                            indexedItemIds(context),
                            context.fluidRegistryIds(),
                            writes);
                }
            }
            writes.awaitIdle("after items lang");

            if (languages.isEmpty()) {
                languages = exportedLanguages(outputRoot);
            }

            ItemIconResult icons = ItemIconWriter.isEnabled()
                    ? ItemIconWriter.export(
                            outputRoot,
                            client,
                            context.itemIds(),
                            context.fluidIds(),
                            null,
                            context.iconVariants(),
                            writes)
                    : emptyIconResult();

            ItemsLangRef itemsLangRef = itemsLang.locales().isEmpty()
                    ? null
                    : new ItemsLangRef(Constants.ITEMS_LANG_DIR, List.copyOf(itemsLang.locales()));
            Bundle bundle = Bundle.of(
                    recipes.imageScale(),
                    recipes.written(),
                    languages,
                    PlaceholderRenderer.REGISTRY_ID,
                    itemsLangRef);
            writes.submitJson(EmiPaths.resolve(outputRoot, Constants.BUNDLE_FILE), bundle);

            TextureWriter.export(outputRoot, client, Set.of(), writes);
            writes.awaitIdle("final");

            logCompletion(plan.mode(), recipes, recipeIds.size(), categories, items, tags, langs, icons);

            return new EmiRunStats(
                    recipeIds.size(),
                    recipes.written(),
                    items.itemCount(),
                    tags.tagsIndexed(),
                    langs.languagesWritten(),
                    icons.totalSpritesWritten());
        }
    }

    private static Set<String> indexedItemIds(ExportContext context) {
        Set<String> ids = new LinkedHashSet<>(context.itemIds());
        ids.addAll(context.inputs().keySet());
        ids.addAll(context.outputs().keySet());
        return Set.copyOf(ids);
    }

    private static void logCompletion(
            Mode mode,
            RecipeWriteResult recipes,
            int recipesRequested,
            CategoryIconResult categories,
            ItemIndexResult items,
            TagMembersResult tags,
            LangMergeResult langs,
            ItemIconResult icons) {
        if (mode == Mode.SCOPED) {
            MweMod.LOGGER.info(
                    "{} scoped export complete: {}/{} layouts, {} categories, {} indexed items, {} tags, {} lang files, {} icon sprites",
                    Log.EMI,
                    recipes.written(),
                    recipesRequested,
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
                    recipesRequested,
                    categories.categoryCount(),
                    items.itemCount(),
                    tags.tagsIndexed(),
                    langs.languagesWritten(),
                    icons.totalSpritesWritten());
        }
    }

    private record EmiRunStats(
            int recipesRequested,
            int recipesWritten,
            int itemIndexCount,
            int tagIndexCount,
            int languagesWritten,
            int iconsWritten) {
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
