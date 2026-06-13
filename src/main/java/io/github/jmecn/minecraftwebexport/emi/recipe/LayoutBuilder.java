package io.github.jmecn.minecraftwebexport.emi.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.widget.Widget;
import dev.emi.emi.api.widget.WidgetHolder;
import io.github.jmecn.minecraftwebexport.Constants;
import io.github.jmecn.minecraftwebexport.emi.support.ProgressLog;
import io.github.jmecn.minecraftwebexport.model.emi.recipe.CardWriteResult;
import io.github.jmecn.minecraftwebexport.model.emi.recipe.LayoutBuildResult;
import io.github.jmecn.minecraftwebexport.model.emi.recipe.TextureWriteResult;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class LayoutBuilder {

    private LayoutBuilder() {
    }

    static int panelMargin() {
        return Constants.PANEL_MARGIN;
    }

    static int progressLogStride(int total) {
        return ProgressLog.stride(total, Constants.PROP_LAYOUT_LOG_STRIDE, 20, 200);
    }


    public static boolean isEnabled() {
        if (Boolean.getBoolean(Constants.PROP_SKIP_EMI_LAYOUT_EXPORT)) {
            return false;
        }
        String prop = System.getProperty(Constants.PROP_EXPORT_EMI_LAYOUT);
        if (prop != null) {
            return !"false".equalsIgnoreCase(prop);
        }
        return true;
    }

    public static int layoutScale() {
        return Math.max(1, Integer.getInteger(Constants.PROP_RECIPE_LAYOUT_SCALE, Constants.DEFAULT_RECIPE_LAYOUT_SCALE));
    }

    public static LayoutBuildResult export(Path outputDir, Minecraft client, Set<String> recipeIds) throws IOException {
        CardWriteResult cards = CardWriter.export(outputDir, client, recipeIds, null);
        return new LayoutBuildResult(
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
                new TextureWriteResult(0, 0, 0, 0),
                BundleMods.empty());
    }

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
        WidgetSerializer.Context context = new WidgetSerializer.Context(
                client,
                chromeRoot,
                chromeHashToRelative,
                chromeWritten,
                chromeDedupedCount,
                referencedItems,
                referencedFluids,
                referencedTags,
                iconVariants);
        WidgetSerializer.serializeWidgets(recipe, widgets, textureIds, context, widgetArray::add);

        JsonObject panel = new JsonObject();
        panel.addProperty("width", displayWidth);
        panel.addProperty("height", displayHeight);
        panel.addProperty("margin", Constants.PANEL_MARGIN);
        panel.addProperty("frameWidth", displayWidth + Constants.PANEL_MARGIN * 2);
        panel.addProperty("frameHeight", displayHeight + Constants.PANEL_MARGIN * 2);

        JsonObject root = new JsonObject();
        root.addProperty("schema", Constants.LAYOUT_PACK_SCHEMA);
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
