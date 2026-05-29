package io.github.jmecn.minecraftwebexport.export.emi;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
        Set<String> recipeIds = collectRecipeIds();
        EmiRecipeLayoutExporter.Result layouts = exportLayouts(outputRoot, client, recipeIds);
        client.renderBuffers().bufferSource().endBatch();

        TagMembersIndexExporter.Result tags = new TagMembersIndexExporter.Result(
                0, 0, 0, 0, 0, 0, 0, Set.of(), Set.of(), Set.of());
        MinecraftServer server = client.getSingleplayerServer();
        if (server != null && TagMembersIndexExporter.isEnabled()) {
            tags = TagMembersIndexExporter.export(outputRoot, server, layouts.referencedTags());
        } else if (!layouts.referencedTags().isEmpty()) {
            LOGGER.warn("{} tag-members skipped: no integrated server", ExportLog.EMI);
        }

        LangMergerExporter.Result langs = LangMergerExporter.isEnabled()
                ? LangMergerExporter.exportEmiLang(outputRoot, client, null)
                : emptyLangResult();

        ItemIconRendererExporter.Result icons = ItemIconRendererExporter.isEnabled()
                ? ItemIconRendererExporter.export(
                outputRoot,
                client,
                layouts.referencedItems(),
                layouts.referencedFluids(),
                null,
                layouts.iconVariants())
                : emptyIconResult();

        EmiItemsIndexExporter.Result items = layouts.written() > 0
                ? EmiItemsIndexExporter.export(outputRoot, server)
                : new EmiItemsIndexExporter.Result(0, 0, 0, 0);
        EmiBundleManifestWriter.write(
                outputRoot,
                exportedLanguages(outputRoot),
                EmiRecipeLayoutExporter.layoutScale(),
                layouts.written());

        LOGGER.info(
                "{} export complete: {}/{} layouts, {} indexed items, {} tags, {} lang files, {} icon sprites",
                ExportLog.EMI,
                layouts.written(),
                recipeIds.size(),
                items.itemCount(),
                tags.tagsIndexed(),
                langs.languagesWritten(),
                icons.totalSpritesWritten());

        return new Report(
                outputRoot,
                recipeIds.size(),
                layouts.written(),
                items.itemCount(),
                tags.tagsIndexed(),
                langs.languagesWritten(),
                icons.totalSpritesWritten());
    }

    private static EmiRecipeLayoutExporter.Result exportLayouts(
            Path outputRoot,
            Minecraft client,
            Set<String> recipeIds) throws IOException {
        if (EmiRecipeLayoutExporter.isEnabled()) {
            return EmiRecipeLayoutExporter.export(outputRoot, client, recipeIds);
        }
        LOGGER.info("{} layout export disabled by configuration", ExportLog.EMI);
        return emptyLayoutResult(recipeIds.size());
    }

    private static EmiRecipeLayoutExporter.Result emptyLayoutResult(int totalRecipes) {
        return new EmiRecipeLayoutExporter.Result(
                totalRecipes,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                Set.of(),
                Set.of(),
                Set.of(),
                java.util.Map.of(),
                new RecipeTextureExporter.Result(0, 0, 0, 0));
    }

    private static LangMergerExporter.Result emptyLangResult() {
        return new LangMergerExporter.Result(0, 0, 0, 0, 0, 0);
    }

    private static ItemIconRendererExporter.Result emptyIconResult() {
        return new ItemIconRendererExporter.Result(0, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    private static Set<String> collectRecipeIds() {
        var manager = EmiApi.getRecipeManager();
        if (manager == null) {
            return Set.of();
        }
        Set<String> ids = new TreeSet<>();
        for (EmiRecipe recipe : manager.getRecipes()) {
            if (recipe.getId() != null) {
                ids.add(recipe.getId().toString());
            }
        }
        return ids;
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
