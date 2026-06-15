package io.github.jmecn.minecraftwebexport.pipeline;

import io.github.jmecn.minecraftwebexport.Constants;
import io.github.jmecn.minecraftwebexport.MweMod;
import io.github.jmecn.minecraftwebexport.emi.EmiPaths;
import io.github.jmecn.minecraftwebexport.emi.icon.CategoryIconWriter;
import io.github.jmecn.minecraftwebexport.emi.icon.ItemIconWriter;
import io.github.jmecn.minecraftwebexport.emi.item.ItemsLangExporter;
import io.github.jmecn.minecraftwebexport.emi.item.NameKeysExporter;
import io.github.jmecn.minecraftwebexport.emi.lang.LangDerivation;
import io.github.jmecn.minecraftwebexport.emi.lang.Languages;
import io.github.jmecn.minecraftwebexport.emi.lang.Merger;
import io.github.jmecn.minecraftwebexport.emi.recipe.RecipePaths;
import io.github.jmecn.minecraftwebexport.emi.recipe.RecipeWriter;
import io.github.jmecn.minecraftwebexport.emi.recipe.TextureWriter;
import io.github.jmecn.minecraftwebexport.emi.support.Log;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
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

            MinecraftServer server = client.getSingleplayerServer();
            boolean hasRecipeExport = context.hasRecipes();

            CompletableFuture<ExportPrepResult> prepFuture = ExportPrep.runServerPhaseAsync(
                    outputRoot, server, context, writes);

            int recipesWritten = hasRecipeExport
                    ? RecipeWriter.export(outputRoot, client, context, writes)
                    : 0;
            client.renderBuffers().bufferSource().endBatch();

            ExportPrepResult prep = awaitPrep(prepFuture);
            writes.awaitIdle("after server prep");

            if (hasRecipeExport && server == null && !context.tagIds().isEmpty()) {
                MweMod.LOGGER.warn("{} tag-members skipped: no integrated server", Log.EMI);
            }

            TagExportResult tagExport = prep.tagExport();
            ItemIndexExportResult itemExport = prep.itemExport();
            int tagsIndexed = tagExport.tagRefCount();
            int itemCount = itemExport.indexedCount();

            Map<String, String> nameKeys = Map.of();
            if (hasRecipeExport) {
                nameKeys = NameKeysExporter.export(outputRoot, client, itemExport.index(), writes);
            }
            writes.awaitIdle("after name keys");

            LangExportResult langExport = LangExportResult.empty();
            if (hasRecipeExport) {
                Set<String> langKeys = LangDerivation.withNameKeys(
                        LangDerivation.derive(context),
                        nameKeys);
                langExport = Merger.exportEmiLang(outputRoot, client, langKeys, plan.hints(), writes);
            }
            writes.awaitIdle("after emi lang");

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

            List<String> itemsLangLocales = awaitItemsLang(itemsLangFuture);
            writes.awaitIdle("after items lang");

            if (languages.isEmpty()) {
                languages = Languages.resolve(plan.hints()).stream().sorted().toList();
            }

            int categoryCount = recipesWritten > 0
                    ? CategoryIconWriter.export(outputRoot, client, context.categoryIds(), writes)
                    : 0;

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

    private static ExportPrepResult awaitPrep(CompletableFuture<ExportPrepResult> future) throws IOException {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("export prep interrupted", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            if (cause instanceof IOException io) {
                throw io;
            }
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new IOException("export prep failed: " + cause.getMessage(), cause);
        }
    }

    private static List<String> awaitItemsLang(CompletableFuture<List<String>> future) throws IOException {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("items-lang export interrupted", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            if (cause instanceof IOException io) {
                throw io;
            }
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new IOException("items-lang export failed: " + cause.getMessage(), cause);
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
