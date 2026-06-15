package io.github.jmecn.minecraftwebexport.pipeline;

import io.github.jmecn.minecraftwebexport.Constants;
import io.github.jmecn.minecraftwebexport.MweMod;
import io.github.jmecn.minecraftwebexport.emi.EmiPaths;
import io.github.jmecn.minecraftwebexport.emi.icon.CategoryIconWriter;
import io.github.jmecn.minecraftwebexport.emi.icon.ItemIconWriter;
import io.github.jmecn.minecraftwebexport.emi.item.ItemIndexExporter;
import io.github.jmecn.minecraftwebexport.emi.item.ItemsLangExporter;
import io.github.jmecn.minecraftwebexport.emi.item.NameKeysExporter;
import io.github.jmecn.minecraftwebexport.emi.lang.LangDerivation;
import io.github.jmecn.minecraftwebexport.emi.lang.Languages;
import io.github.jmecn.minecraftwebexport.emi.lang.Merger;
import io.github.jmecn.minecraftwebexport.emi.recipe.RecipePaths;
import io.github.jmecn.minecraftwebexport.emi.recipe.RecipeWriter;
import io.github.jmecn.minecraftwebexport.emi.recipe.TextureWriter;
import io.github.jmecn.minecraftwebexport.emi.support.Log;
import io.github.jmecn.minecraftwebexport.emi.tag.TagIndexWriter;
import io.github.jmecn.minecraftwebexport.io.ExportWriteQueue;
import io.github.jmecn.minecraftwebexport.model.bundle.Bundle;
import io.github.jmecn.minecraftwebexport.model.bundle.ItemsLangRef;
import io.github.jmecn.minecraftwebexport.model.pipeline.ExportContext;
import io.github.jmecn.minecraftwebexport.model.pipeline.ExportResult;
import io.github.jmecn.minecraftwebexport.model.pipeline.ItemIndexExportResult;
import io.github.jmecn.minecraftwebexport.model.pipeline.LangExportResult;
import io.github.jmecn.minecraftwebexport.model.pipeline.TagExportResult;
import io.github.jmecn.minecraftwebexport.enums.pipeline.Mode;
import io.github.jmecn.minecraftwebexport.model.pipeline.Plan;
import io.github.jmecn.minecraftwebexport.model.pipeline.Scope;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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

        Plan plan = ExportScopeBuilder.plan(client, mode, modules, scope);

        if (plan.mode() == Mode.SCOPED) {
            MweMod.LOGGER.info(
                    "[export] scoped export via modules {} (recipes={})",
                    modules.stream().map(Module::moduleId).toList(),
                    plan.context().recipeIds().size());
        }

        ExportResult result = exportEmi(outputRoot, client, plan);

        for (Module module : modules) {
            module.exportExtras(scope);
        }
        return result;
    }

    private static ExportResult exportEmi(Path outputRoot, Minecraft client, Plan plan) throws IOException {
        ExportContext context = plan.context();
        Set<String> recipeIds = context.recipeIds();

        try (ExportWriteQueue writes = ExportWriteQueue.create(outputRoot)) {
            RecipeGraphBuilder.buildGraph(client, context);
            if (writes.usesFileTree()) {
                RecipePaths.ensureRecipeDirectories(outputRoot, recipeIds);
            }

            int recipesWritten = RecipeWriter.export(outputRoot, client, context, writes);
            client.renderBuffers().bufferSource().endBatch();

            MinecraftServer server = client.getSingleplayerServer();

            int tagsIndexed = 0;
            TagExportResult tagExport = TagExportResult.empty();
            if (server != null && !context.tagIds().isEmpty()) {
                tagExport = TagIndexWriter.export(outputRoot, server, context.tagIds(), writes);
                tagsIndexed = tagExport.tagRefCount();
                writes.awaitIdle("after tags index");
            } else if (!context.tagIds().isEmpty()) {
                MweMod.LOGGER.warn("{} tag-members skipped: no integrated server", Log.EMI);
            }

            int categoryCount = recipesWritten > 0
                    ? CategoryIconWriter.export(outputRoot, client, context.categoryIds(), writes)
                    : 0;

            ItemIndexExportResult itemExport = recipesWritten > 0
                    ? ItemIndexExporter.export(outputRoot, server, context, tagExport.catalog(), writes)
                    : ItemIndexExportResult.empty();
            int itemCount = itemExport.indexedCount();
            writes.awaitIdle("after items index");

            Map<String, String> nameKeys = Map.of();
            if (recipesWritten > 0) {
                nameKeys = NameKeysExporter.export(outputRoot, client, itemExport.index(), writes);
            }
            writes.awaitIdle("after name keys");

            Set<String> langKeys = LangDerivation.withNameKeys(
                    LangDerivation.derive(context),
                    nameKeys);
            LangExportResult langExport =
                    Merger.exportEmiLang(outputRoot, client, langKeys, plan.hints(), writes);
            writes.awaitIdle("after emi lang");

            List<String> languages = langExport.locales();
            List<String> itemsLangLocales = List.of();
            if (itemCount > 0 && !languages.isEmpty()) {
                itemsLangLocales = ItemsLangExporter.export(
                        outputRoot,
                        languages,
                        itemExport.indexedItemIds(),
                        context.fluidRegistryIds(),
                        langExport.tablesByLocale(),
                        nameKeys,
                        writes);
            }
            writes.awaitIdle("after items lang");

            if (languages.isEmpty()) {
                languages = Languages.resolve(plan.hints()).stream().sorted().toList();
            }

            int languagesWritten = langExport.languagesWritten();

            int iconSprites = ItemIconWriter.export(
                    outputRoot,
                    client,
                    context.itemIds(),
                    context.fluidIds(),
                    null,
                    context.iconVariants(),
                    writes);

            ItemsLangRef itemsLangRef = itemsLangLocales.isEmpty()
                    ? null
                    : new ItemsLangRef(Constants.ITEMS_LANG_DIR, List.copyOf(itemsLangLocales));
            Bundle bundle = Bundle.of(
                    RecipeWriter.imageScale(),
                    recipesWritten,
                    languages,
                    Constants.MISSING_ICON_REGISTRY_ID,
                    itemsLangRef);
            writes.submitJson(EmiPaths.resolve(outputRoot, Constants.BUNDLE_FILE), bundle);

            TextureWriter.export(outputRoot, client, writes);
            writes.awaitIdle("final");

            logCompletion(
                    plan.mode(),
                    recipesWritten,
                    recipeIds.size(),
                    categoryCount,
                    itemCount,
                    tagsIndexed,
                    languagesWritten,
                    iconSprites);

            return ExportResult.from(
                    outputRoot,
                    recipeIds.size(),
                    recipesWritten,
                    itemCount,
                    tagsIndexed,
                    languagesWritten,
                    iconSprites);
        }
    }

    private static void logCompletion(
            Mode mode,
            int recipesWritten,
            int recipesRequested,
            int categoryCount,
            int itemCount,
            int tagsIndexed,
            int languagesWritten,
            int iconSprites) {
        if (mode == Mode.SCOPED) {
            MweMod.LOGGER.info(
                    "{} scoped export complete: {}/{} recipes, {} categories, {} indexed items, {} tags, {} lang files, {} icon sprites",
                    Log.EMI,
                    recipesWritten,
                    recipesRequested,
                    categoryCount,
                    itemCount,
                    tagsIndexed,
                    languagesWritten,
                    iconSprites);
        } else {
            MweMod.LOGGER.info(
                    "{} export complete: {}/{} recipes, {} categories, {} indexed items, {} tags, {} lang files, {} icon sprites",
                    Log.EMI,
                    recipesWritten,
                    recipesRequested,
                    categoryCount,
                    itemCount,
                    tagsIndexed,
                    languagesWritten,
                    iconSprites);
        }
    }
}
