package io.github.jmecn.minecraftwebexport.emi.icon;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import io.github.jmecn.minecraftwebexport.Constants;
import io.github.jmecn.minecraftwebexport.MweMod;
import io.github.jmecn.minecraftwebexport.emi.bundle.Paths;
import io.github.jmecn.minecraftwebexport.emi.category.IconRenderer;
import io.github.jmecn.minecraftwebexport.emi.category.LangKeys;
import io.github.jmecn.minecraftwebexport.emi.support.Log;
import io.github.jmecn.minecraftwebexport.io.JsonIO;
import io.github.jmecn.minecraftwebexport.model.category.CategoriesIndex;
import io.github.jmecn.minecraftwebexport.model.category.CategoryEntry;
import io.github.jmecn.minecraftwebexport.model.category.CategoryIconSprite;
import io.github.jmecn.minecraftwebexport.model.emi.icon.AtlasPagePlan;
import io.github.jmecn.minecraftwebexport.model.emi.icon.CategoryIconResult;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class CategoryIconWriter {

    private CategoryIconWriter() {
    }

    public static boolean isEnabled() {
        return !Boolean.getBoolean(Constants.PROP_SKIP_CATEGORY_ICON_EXPORT);
    }

    public static CategoryIconResult export(Path outputRoot, Minecraft client) throws IOException {
        var manager = EmiApi.getRecipeManager();
        if (manager == null) {
            MweMod.LOGGER.warn("{} EMI recipe manager unavailable - skipping category icons", Log.EMI);
            return new CategoryIconResult(0, 0, 0, 0, 0);
        }

        List<EmiRecipeCategory> registered = manager.getCategories();
        List<CategoryEntry> categories = new ArrayList<>();
        int order = 0;
        for (EmiRecipeCategory category : registered) {
            categories.add(buildCategoryEntry(category, order++));
        }

        int placed = 0;
        int failures = 0;
        long atlasIndexBytes = 0;

        if (isEnabled() && !categories.isEmpty()) {
            int cell = ExportSizes.categoryIconCellSize();
            int atlasMax = ExportSizes.atlasMaxSize();
            Path iconsRoot = Paths.resolve(outputRoot, Constants.CATEGORY_ICONS_DIR);
            List<AtlasPagePlan> layout = AtlasLayout.plan(categories.size(), cell, atlasMax);

            var bufferSource = client.renderBuffers().bufferSource();
            try (var renderer = new OffScreenRenderer(cell, cell);
                 var atlas = new AtlasBuilder(iconsRoot, cell, atlasMax, "category", layout, null)) {
                GuiGraphics guiGraphics = new GuiGraphics(client, bufferSource);
                PlaceholderRenderer.render(guiGraphics, renderer);
                atlas.place(PlaceholderRenderer.REGISTRY_ID, renderer);

                int index = 0;
                int total = registered.size();
                for (int i = 0; i < registered.size(); i++) {
                    EmiRecipeCategory category = registered.get(i);
                    String categoryId = category.getId().toString();
                    index++;
                    boolean ok;
                    try {
                        ok = IconRenderer.render(client, guiGraphics, renderer, category);
                        if (!ok) {
                            ok = renderWorkstationFallback(client, guiGraphics, renderer, category);
                        }
                        if (ok) {
                            atlas.place(categoryId, renderer);
                            categories.set(i, categories.get(i).withIcon(toSprite(atlas.spriteFor(categoryId))));
                            placed++;
                        } else {
                            failures++;
                        }
                    } catch (Exception e) {
                        failures++;
                        Log.detailFailure(failures,
                                "{} category icon failed {} ({}/{}): {}",
                                Log.ICONS,
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
                AtlasBuilder.AtlasResult atlasResult = atlas.finish();
                atlasIndexBytes = atlasResult.indexBytes();
            }
        }

        int cellSize = ExportSizes.categoryIconCellSize();
        CategoriesIndex index = CategoriesIndex.of(cellSize, Constants.CATEGORY_ICONS_DIR, categories);
        Path indexFile = Paths.resolve(outputRoot, Constants.CATEGORIES_INDEX_FILE);
        JsonIO.write(indexFile, index);
        long indexBytes = JsonIO.toUtf8Bytes(index).length;

        MweMod.LOGGER.info(
                "{} {} categories, {} icons placed, {} failed -> {}",
                Log.EMI,
                categories.size(),
                placed,
                failures,
                Constants.CATEGORIES_INDEX_FILE);

        return new CategoryIconResult(categories.size(), placed, failures, indexBytes, atlasIndexBytes);
    }

    private static CategoryEntry buildCategoryEntry(EmiRecipeCategory category, int order) {
        String categoryId = category.getId().toString();
        int priority = dev.emi.emi.data.EmiRecipeCategoryProperties.getOrder(category);
        Integer priorityField = priority != 0 ? priority : null;
        return new CategoryEntry(categoryId, order, priorityField, LangKeys.resolveNameKey(category), null);
    }

    private static CategoryIconSprite toSprite(Map<String, Object> sprite) {
        if (sprite == null || sprite.isEmpty()) {
            return null;
        }
        Object page = sprite.get("page");
        Object x = sprite.get("x");
        Object y = sprite.get("y");
        if (page instanceof Number pageNumber && x instanceof Number xNumber && y instanceof Number yNumber) {
            return new CategoryIconSprite(pageNumber.intValue(), xNumber.intValue(), yNumber.intValue());
        }
        return null;
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
                ItemStack item = StackKey.toItemStack(stack);
                if (!item.isEmpty() && IconRenderer.renderItemStack(client, guiGraphics, renderer, item)) {
                    return true;
                }
            }
        }
        return false;
    }
}
