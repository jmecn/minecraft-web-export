package io.github.jmecn.minecraftwebexport.export.emi;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.widget.Widget;
import dev.emi.emi.api.widget.WidgetHolder;
import io.github.jmecn.minecraftwebexport.export.ExportGson;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

public final class EmiRecipeLayoutExporter {

    private static final Logger LOGGER = LogManager.getLogger(EmiRecipeLayoutExporter.class);
    private static final Gson GSON = ExportGson.GSON;

    private static final int PANEL_MARGIN = 4;
    private static final String LAYOUT_LOG_STRIDE_PROPERTY = "minecraftWebExport.layoutLogStride";

    private EmiRecipeLayoutExporter() {
    }

    /** Per-side inset around EMI recipe display (PNG + meta frame = width + 2×margin). */
    static int panelMargin() {
        return PANEL_MARGIN;
    }

    static int progressLogStride(int total) {
        return ExportProgressLog.stride(total, LAYOUT_LOG_STRIDE_PROPERTY, 20, 200);
    }

    public record Result(
            int requested,
            int written,
            int missing,
            int failures,
            int chromeLayers,
            int chromeDeduped,
            int uniqueChromeFiles,
            long jsonBytes,
            long chromeBytes,
            Set<String> referencedItems,
            Set<String> referencedFluids,
            Set<String> referencedTags,
            Map<String, ItemStack> iconVariants,
            RecipeTextureExporter.Result textures,
            RecipeBundleMods mods) {
    }

    public static boolean isEnabled() {
        if (Boolean.getBoolean("minecraftWebExport.skipEmiLayoutExport")) {
            return false;
        }
        String prop = System.getProperty("minecraftWebExport.exportEmiLayout");
        if (prop != null) {
            return !"false".equalsIgnoreCase(prop);
        }
        return true;
    }

    public static int layoutScale() {
        return Math.max(1, Integer.getInteger("minecraftWebExport.recipeLayoutScale", 2));
    }

    public static Result export(Path outputDir, Minecraft client, Set<String> recipeIds) throws IOException {
        EmiRecipeCardExporter.Result cards = EmiRecipeCardExporter.export(outputDir, client, recipeIds);
        return new Result(
                cards.requested(),
                cards.written(),
                cards.missing(),
                cards.failures(),
                0,
                0,
                0,
                cards.metaBytes(),
                0,
                cards.referencedItems(),
                cards.referencedFluids(),
                cards.referencedTags(),
                cards.iconVariants(),
                new RecipeTextureExporter.Result(0, 0, 0, 0),
                RecipeBundleMods.empty());
    }

    /** In-memory layout for meta baking (no chrome / route packs). */
    static JsonObject buildLayoutInMemory(
            Minecraft client,
            EmiRecipe recipe,
            String recipeId,
            Set<String> textureIds,
            Set<String> referencedItems,
            Set<String> referencedFluids,
            Set<String> referencedTags,
            Map<String, ItemStack> iconVariants) {
        int[] chromeWritten = {0};
        int[] chromeDedupedCount = {0};
        return buildLayout(
                client,
                recipe,
                recipeId,
                textureIds,
                referencedItems,
                referencedFluids,
                referencedTags,
                iconVariants,
                null,
                java.util.Map.of(),
                chromeWritten,
                chromeDedupedCount);
    }

    private static JsonObject buildLayout(
            Minecraft client,
            EmiRecipe recipe,
            String recipeId,
            Set<String> textureIds,
            Set<String> referencedItems,
            Set<String> referencedFluids,
            Set<String> referencedTags,
            Map<String, ItemStack> iconVariants,
            Path chromeRoot,
            Map<String, String> chromeHashToRelative,
            int[] chromeWritten,
            int[] chromeDedupedCount) {
        int displayWidth = Math.max(1, recipe.getDisplayWidth());
        int displayHeight = Math.max(1, recipe.getDisplayHeight());

        List<Widget> widgets = new ArrayList<>();
        WidgetHolder holder = new WidgetHolder() {
            @Override
            public int getWidth() {
                return displayWidth;
            }

            @Override
            public int getHeight() {
                return displayHeight;
            }

            @Override
            public <T extends Widget> T add(T widget) {
                widgets.add(widget);
                return widget;
            }
        };
        recipe.addWidgets(holder);

        JsonArray widgetArray = new JsonArray();
        EmiWidgetSerializer.Context context = new EmiWidgetSerializer.Context(
                client,
                chromeRoot,
                chromeHashToRelative,
                chromeWritten,
                chromeDedupedCount,
                referencedItems,
                referencedFluids,
                referencedTags,
                iconVariants);
        EmiWidgetSerializer.serializeWidgets(recipe, widgets, textureIds, context, widgetArray::add);

        JsonObject panel = new JsonObject();
        panel.addProperty("width", displayWidth);
        panel.addProperty("height", displayHeight);
        panel.addProperty("margin", PANEL_MARGIN);
        panel.addProperty("frameWidth", displayWidth + PANEL_MARGIN * 2);
        panel.addProperty("frameHeight", displayHeight + PANEL_MARGIN * 2);

        JsonObject root = new JsonObject();
        root.addProperty("schema", RecipeLayoutPaths.SCHEMA_VERSION);
        root.addProperty("id", recipeId);
        root.addProperty("scale", layoutScale());
        root.add("panel", panel);

        ResourceLocation emiId = recipe.getId();
        if (emiId != null) {
            root.addProperty("emiId", emiId.toString());
        }
        if (recipe.getCategory() != null && recipe.getCategory().getId() != null) {
            root.addProperty("category", recipe.getCategory().getId().toString());
        }

        root.add("widgets", widgetArray);
        return root;
    }

    private static long dirSize(Path root) {
        if (!Files.isDirectory(root)) {
            return 0;
        }
        try (var walk = Files.walk(root)) {
            return walk.filter(Files::isRegularFile).mapToLong(path -> {
                try {
                    return Files.size(path);
                } catch (IOException e) {
                    return 0;
                }
            }).sum();
        } catch (IOException e) {
            return 0;
        }
    }
}
