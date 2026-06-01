package io.github.jmecn.minecraftwebexport.export.emi;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Exports all EMI recipe categories: translation keys + dedicated 16×16 icon atlas
 * under {@link EmiBundlePaths#CATEGORY_ICONS_DIR}.
 */
public final class EmiCategoryIconsExporter {

    private static final Logger LOGGER = LogManager.getLogger(EmiCategoryIconsExporter.class);

    private EmiCategoryIconsExporter() {
    }

    public record Result(
            int categoryCount,
            int iconsPlaced,
            int iconFailures,
            long categoriesIndexBytes,
            long atlasIndexBytes) {
    }

    public static boolean isEnabled() {
        return !Boolean.getBoolean("minecraftWebExport.skipCategoryIconExport");
    }

    public static Result export(Path outputRoot, Minecraft client) throws IOException {
        var manager = EmiApi.getRecipeManager();
        if (manager == null) {
            LOGGER.warn("{} EMI recipe manager unavailable - skipping category icons", ExportLog.EMI);
            return new Result(0, 0, 0, 0, 0);
        }

        List<EmiRecipeCategory> registered = manager.getCategories();
        List<Map<String, Object>> categories = new ArrayList<>();
        int order = 0;
        for (EmiRecipeCategory category : registered) {
            categories.add(buildCategoryEntry(category, order++));
        }

        int placed = 0;
        int failures = 0;
        long atlasIndexBytes = 0;

        if (isEnabled() && !categories.isEmpty()) {
            int cell = IconExportSizes.categoryIconCellSize();
            int atlasMax = IconExportSizes.atlasMaxSize();
            Path iconsRoot = EmiBundlePaths.resolve(outputRoot, EmiBundlePaths.CATEGORY_ICONS_DIR);
            List<IconAtlasLayout.PagePlan> layout = IconAtlasLayout.plan(categories.size(), cell, atlasMax);

            var bufferSource = client.renderBuffers().bufferSource();
            try (var renderer = new OffScreenRenderer(cell, cell);
                 var atlas = new ItemIconAtlasBuilder(iconsRoot, cell, atlasMax, "category", layout, null)) {
                GuiGraphics guiGraphics = new GuiGraphics(client, bufferSource);
                IconPlaceholderRenderer.render(client, guiGraphics, renderer);
                atlas.place(IconPlaceholderRenderer.REGISTRY_ID, renderer);

                int index = 0;
                int total = registered.size();
                for (int i = 0; i < registered.size(); i++) {
                    EmiRecipeCategory category = registered.get(i);
                    String categoryId = category.getId().toString();
                    Map<String, Object> entry = categories.get(i);
                    index++;
                    boolean ok = false;
                    try {
                        ok = CategoryIconRenderer.render(client, guiGraphics, renderer, category);
                        if (!ok) {
                            ok = renderWorkstationFallback(client, guiGraphics, renderer, category);
                        }
                        if (ok) {
                            atlas.place(categoryId, renderer);
                            attachIconSprite(entry, atlas.spriteFor(categoryId));
                            placed++;
                        } else {
                            failures++;
                        }
                    } catch (Exception e) {
                        failures++;
                        ExportLog.detailFailure(
                                LOGGER,
                                failures,
                                "{} category icon failed {} ({}/{}): {}",
                                ExportLog.ICONS,
                                categoryId,
                                index,
                                total,
                                e.toString());
                    }
                    if (index % 32 == 0) {
                        bufferSource.endBatch();
                    }
                }
                bufferSource.endBatch();
                ItemIconAtlasBuilder.AtlasResult atlasResult = atlas.finish();
                atlasIndexBytes = atlasResult.indexBytes();
            }
        }

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schema", 2);
        root.put("iconCellSize", IconExportSizes.categoryIconCellSize());
        root.put("iconsDir", EmiBundlePaths.CATEGORY_ICONS_DIR);
        root.put("categories", categories);

        long indexBytes = EmiRecipeCategoriesExporter.writeCategoriesIndex(outputRoot, root);
        LOGGER.info(
                "{} {} categories, {} icons placed, {} failed -> {}",
                ExportLog.EMI,
                categories.size(),
                placed,
                failures,
                EmiBundlePaths.CATEGORIES_INDEX_FILE);

        return new Result(categories.size(), placed, failures, indexBytes, atlasIndexBytes);
    }

    private static Map<String, Object> buildCategoryEntry(EmiRecipeCategory category, int order) {
        Map<String, Object> entry = new LinkedHashMap<>();
        String categoryId = category.getId().toString();
        entry.put("id", categoryId);
        entry.put("order", order);
        int priority = dev.emi.emi.data.EmiRecipeCategoryProperties.getOrder(category);
        if (priority != 0) {
            entry.put("priority", priority);
        }
        entry.put("nameKey", CategoryLangKeys.resolveNameKey(category));
        return entry;
    }

    private static void attachIconSprite(Map<String, Object> entry, Map<String, Object> sprite) {
        if (sprite != null && !sprite.isEmpty()) {
            entry.put("icon", sprite);
        }
    }

    private static boolean renderWorkstationFallback(
            Minecraft client,
            GuiGraphics guiGraphics,
            OffScreenRenderer renderer,
            EmiRecipeCategory category) {
        var manager = EmiApi.getRecipeManager();
        if (manager == null) {
            return false;
        }
        for (var workstation : manager.getWorkstations(category)) {
            if (workstation == null) {
                continue;
            }
            for (EmiStack stack : workstation.getEmiStacks()) {
                ItemStack item = IconStackKey.toItemStack(stack);
                if (!item.isEmpty() && CategoryIconRenderer.renderItemStack(client, guiGraphics, renderer, item)) {
                    return true;
                }
            }
        }
        return false;
    }
}
