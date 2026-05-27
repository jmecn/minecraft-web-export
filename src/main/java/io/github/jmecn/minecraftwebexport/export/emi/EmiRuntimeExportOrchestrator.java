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
import java.util.logging.Logger;

public final class EmiRuntimeExportOrchestrator {

    private static final Logger LOGGER = Logger.getLogger(EmiRuntimeExportOrchestrator.class.getName());

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
        EmiRecipeLayoutExporter.Result layouts;
        if (EmiRecipeLayoutExporter.isEnabled()) {
            layouts = EmiRecipeLayoutExporter.export(outputRoot, client, recipeIds);
        } else {
            LOGGER.info("[emi] layout export disabled by configuration");
            layouts = new EmiRecipeLayoutExporter.Result(
                    recipeIds.size(),
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

        TagMembersIndexExporter.Result tags = new TagMembersIndexExporter.Result(0, 0, 0, 0, 0, 0);
        MinecraftServer server = client.getSingleplayerServer();
        if (server != null && TagMembersIndexExporter.isEnabled()) {
            tags = TagMembersIndexExporter.export(outputRoot, server, layouts.referencedTags());
        } else if (!layouts.referencedTags().isEmpty()) {
            LOGGER.warning("[emi] tag-members skipped because no integrated server is available");
        }

        LangMergerExporter.Result langs = LangMergerExporter.isEnabled()
                ? LangMergerExporter.exportEmiLang(outputRoot, client, null)
                : new LangMergerExporter.Result(0, 0, 0, 0, 0, 0);

        ItemIconRendererExporter.Result icons = ItemIconRendererExporter.isEnabled()
                ? ItemIconRendererExporter.export(
                outputRoot,
                client,
                layouts.referencedItems(),
                layouts.referencedFluids(),
                null,
                layouts.iconVariants())
                : new ItemIconRendererExporter.Result(0, 0, 0, 0, 0, 0, 0, 0, 0);

        EmiItemsIndexExporter.Result items = layouts.written() > 0
                ? EmiItemsIndexExporter.export(outputRoot)
                : new EmiItemsIndexExporter.Result(0, 0, 0, 0);
        EmiBundleManifestWriter.write(
                outputRoot,
                exportedLanguages(outputRoot),
                EmiRecipeLayoutExporter.layoutScale(),
                layouts.written());

        LOGGER.info("[emi] export complete: " + layouts.written() + "/" + recipeIds.size() + " layouts, "
                + items.itemCount() + " indexed items, " + tags.tagsIndexed() + " tags, "
                + langs.languagesWritten() + " lang files, " + icons.totalSpritesWritten() + " icon sprites");

        return new Report(
                outputRoot,
                recipeIds.size(),
                layouts.written(),
                items.itemCount(),
                tags.tagsIndexed(),
                langs.languagesWritten(),
                icons.totalSpritesWritten());
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
