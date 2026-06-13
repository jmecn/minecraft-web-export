package io.github.jmecn.minecraftwebexport.emi.icon;
import io.github.jmecn.minecraftwebexport.Constants;
import io.github.jmecn.minecraftwebexport.model.emi.icon.ItemIconResult;
import io.github.jmecn.minecraftwebexport.model.emi.icon.AtlasPagePlan;
import io.github.jmecn.minecraftwebexport.emi.bundle.Paths;
import io.github.jmecn.minecraftwebexport.emi.support.Log;
import io.github.jmecn.minecraftwebexport.emi.support.ProgressLog;

import com.google.common.io.MoreFiles;
import com.google.common.io.RecursiveDeleteOption;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import io.github.jmecn.minecraftwebexport.MweMod;

public final class ItemIconWriter {

    private ItemIconWriter() {
    }


    public static boolean isEnabled() {
        return !Boolean.getBoolean(Constants.PROP_SKIP_ITEM_ICON_EXPORT);
    }

    public static boolean fluidsEnabled() {
        return !Boolean.getBoolean(Constants.PROP_SKIP_FLUID_ICON_EXPORT);
    }

    public static ItemIconResult export(Path outputDir, Minecraft client) {
        return export(outputDir, client, null, null, null);
    }

    public static ItemIconResult export(Path outputDir, Minecraft client, Set<String> onlyItemIds) {
        return export(outputDir, client, onlyItemIds, null, null);
    }

    public static ItemIconResult export(
            Path outputDir,
            Minecraft client,
            Set<String> onlyItemIds,
            Map<String, Integer> usageWeights) {
        return export(outputDir, client, onlyItemIds, null, usageWeights);
    }

    public static ItemIconResult export(
            Path outputDir,
            Minecraft client,
            Set<String> onlyItemIds,
            Set<String> onlyFluidIds,
            Map<String, Integer> usageWeights) {
        return export(outputDir, client, onlyItemIds, onlyFluidIds, usageWeights, Map.of());
    }

    public static ItemIconResult export(
            Path outputDir,
            Minecraft client,
            Set<String> onlyItemIds,
            Set<String> onlyFluidIds,
            Map<String, Integer> usageWeights,
            Map<String, ItemStack> iconVariants) {
        Path iconsRoot = Paths.resolve(outputDir, Constants.ICONS_DIR);
        clearLegacyDirs(outputDir);
        clearDir(iconsRoot);
        return exportImpl(iconsRoot, client, onlyItemIds, onlyFluidIds, usageWeights, iconVariants);
    }

    public static ItemIconResult exportAtRoot(
            Path iconsRoot,
            Minecraft client,
            Set<String> onlyItemIds,
            Map<String, Integer> usageWeights) {
        clearDir(iconsRoot);
        return exportImpl(iconsRoot, client, onlyItemIds, null, usageWeights, Map.of());
    }

    private static ItemIconResult exportImpl(
            Path iconsRoot,
            Minecraft client,
            Set<String> onlyItemIds,
            Set<String> onlyFluidIds,
            Map<String, Integer> usageWeights,
            Map<String, ItemStack> iconVariants) {
        int cell = ExportSizes.iconCellSize();
        int atlasMax = ExportSizes.atlasMaxSize();
        boolean exportFluids = fluidsEnabled() && (onlyFluidIds == null || !onlyFluidIds.isEmpty());

        List<ResourceLocation> itemOrder = ItemOrdering.orderedForPass(onlyItemIds, item -> true, usageWeights);
        List<String> fluidOrder = exportFluids ? orderedFluidIds(onlyFluidIds) : List.of();
        Map<String, ItemStack> variants = iconVariants != null ? iconVariants : Map.of();

        if (onlyItemIds != null) {
            MweMod.LOGGER.info(
                    "{} closure: {} items, {} fluids, {} nbt variants at {}px -> {}",
                    Log.ICONS,
                    onlyItemIds.size(),
                    fluidOrder.size(),
                    variants.size(),
                    cell,
                    iconsRoot);
        } else {
            int totalItems = 0;
            for (Item ignored : ForgeRegistries.ITEMS) {
                totalItems++;
            }
            MweMod.LOGGER.info(
                    "{} full export: {} registry items, {} fluids at {}px (max atlas {}px) -> {}",
                    Log.ICONS,
                    totalItems,
                    fluidOrder.size(),
                    cell,
                    atlasMax,
                    iconsRoot);
            logRegistryItemCountsByNamespace();
        }

        int totalSprites = itemOrder.size() + fluidOrder.size() + variants.size() + 1;
        List<AtlasPagePlan> layout = AtlasLayout.plan(totalSprites, cell, atlasMax);
        if (!layout.isEmpty()) {
            AtlasPagePlan first = layout.get(0);
            MweMod.LOGGER.info(
                    "{} {} sprites, planned {} page(s), first page {}x{} cells ({}x{}px)",
                    Log.ICONS,
                    totalSprites,
                    layout.size(),
                    first.cols(),
                    first.rows(),
                    first.widthPx(cell),
                    first.heightPx(cell));
        }

        int itemLogStride = ProgressLog.stride(itemOrder.size(), Constants.PROP_ICON_LOG_STRIDE, 50, 500);
        int variantLogStride = ProgressLog.stride(variants.size(), Constants.PROP_ICON_LOG_STRIDE, 20, 200);

        int itemsPlaced = 0;
        int itemFailures = 0;
        int fluidsPlaced = 0;
        int fluidFailures = 0;
        int fluidsSkipped = 0;
        int variantsPlaced = 0;
        int variantFailures = 0;

        AtlasBuilder.AtlasResult atlasResult;
        try (var renderer = new OffScreenRenderer(cell, cell);
             var atlas = new AtlasBuilder(iconsRoot, cell, atlasMax, "icon", layout, usageWeights)) {
            var bufferSource = client.renderBuffers().bufferSource();
            var guiGraphics = new GuiGraphics(client, bufferSource);
            PlaceholderRenderer.render(guiGraphics, renderer);
            atlas.place(PlaceholderRenderer.REGISTRY_ID, renderer);

            renderer.setupItemRendering();

            int index = 0;
            int itemTotal = itemOrder.size();
            for (ResourceLocation itemId : itemOrder) {
                index++;
                Item item = ForgeRegistries.ITEMS.getValue(itemId);
                if (item == null || item == Items.AIR) {
                    continue;
                }

                try {
                    renderItemIcon(client, guiGraphics, renderer, item);
                    atlas.place(itemId.toString(), renderer);
                    itemsPlaced++;
                    if (index % Constants.ICON_FLUSH_RENDER_EVERY == 0) {
                        bufferSource.endBatch();
                    }
                    if (ProgressLog.shouldLog(index, itemTotal, itemLogStride)) {
                        MweMod.LOGGER.info(
                                "{} items: {}% {}/{} ({} ok, {} fail)",
                                Log.ICONS,
                                ProgressLog.percent(index, itemTotal),
                                index,
                                itemTotal,
                                itemsPlaced,
                                itemFailures);
                    }
                } catch (Exception e) {
                    itemFailures++;
                    Log.detailFailure(itemFailures,
                            "{} item failed {} ({}/{}): {}",
                            Log.ICONS,
                            itemId,
                            index,
                            itemTotal,
                            failureSummary(e));
                }
            }

            renderer.setupFlatGuiRendering();
            for (String fluidIdStr : fluidOrder) {
                Fluid fluid = ForgeRegistries.FLUIDS.getValue(ResourceLocation.parse(fluidIdStr));
                if (fluid == null || fluid.isSame(Fluids.EMPTY)) {
                    continue;
                }

                try {
                    if (!FluidStillRenderer.render(client, guiGraphics, renderer, fluid)) {
                        fluidsSkipped++;
                        continue;
                    }
                    atlas.place(fluidIdStr, renderer);
                    fluidsPlaced++;
                } catch (Exception e) {
                    fluidFailures++;
                    Log.detailFailure(fluidFailures,
                            "{} fluid failed {}: {}",
                            Log.ICONS,
                            fluidIdStr,
                            failureSummary(e));
                }
            }

            bufferSource.endBatch();

            if (!variants.isEmpty()) {
                renderer.setupItemRendering();
                int variantIndex = 0;
                int variantTotal = variants.size();
                for (var entry : variants.entrySet()) {
                    variantIndex++;
                    try {
                        renderItemStackIcon(client, guiGraphics, renderer, entry.getValue());
                        atlas.place(entry.getKey(), renderer);
                        variantsPlaced++;
                        if (ProgressLog.shouldLog(variantIndex, variantTotal, variantLogStride)) {
                            MweMod.LOGGER.info(
                                    "{} nbt variants: {}% {}/{} ({} ok, {} fail)",
                                    Log.ICONS,
                                    ProgressLog.percent(variantIndex, variantTotal),
                                    variantIndex,
                                    variantTotal,
                                    variantsPlaced,
                                    variantFailures);
                        }
                    } catch (Exception e) {
                        variantFailures++;
                        Log.detailFailure(variantFailures,
                                "{} nbt variant failed {} ({}/{}): {}",
                                Log.ICONS,
                                entry.getKey(),
                                variantIndex,
                                variantTotal,
                                failureSummary(e));
                    }
                }
            }

            atlasResult = atlas.finish();
        } catch (IOException e) {
            throw new RuntimeException("icon atlas export failed", e);
        }

        int totalFailures = itemFailures + fluidFailures + variantFailures;
        MweMod.LOGGER.info(
                "{} done: {} items, {} nbt variants, {} fluids ({} skipped no still), {} pages, {} failures at {}",
                Log.ICONS,
                itemsPlaced,
                variantsPlaced,
                fluidsPlaced,
                fluidsSkipped,
                atlasResult.pageCount(),
                totalFailures,
                iconsRoot);
        if (totalFailures > Log.DETAIL_FAILURE_LIMIT) {
            MweMod.LOGGER.warn(
                    "{} {} icon failures (first {} at DEBUG; -D{}=true or enable DEBUG on export.emi)",
                    Log.ICONS,
                    totalFailures,
                    Log.DETAIL_FAILURE_LIMIT,
                    Constants.PROP_LOG_DETAIL_FAILURES);
        }

        return new ItemIconResult(
                itemsPlaced + variantsPlaced,
                fluidsPlaced,
                atlasResult.pageCount(),
                itemFailures + variantFailures,
                fluidFailures,
                fluidsSkipped,
                atlasResult.pngBytes(),
                atlasResult.indexBytes(),
                atlasResult.cssBytes());
    }

    private static List<String> orderedFluidIds(Set<String> onlyFluidIds) {
        if (onlyFluidIds != null && onlyFluidIds.isEmpty()) {
            return List.of();
        }
        TreeSet<String> ids = new TreeSet<>();
        for (Fluid fluid : ForgeRegistries.FLUIDS) {
            if (fluid == null || fluid.isSame(Fluids.EMPTY)) {
                continue;
            }
            ResourceLocation fluidId = ForgeRegistries.FLUIDS.getKey(fluid);
            if (fluidId == null) {
                continue;
            }
            String idStr = fluidId.toString();
            if (onlyFluidIds == null || onlyFluidIds.contains(idStr)) {
                ids.add(idStr);
            }
        }
        return new ArrayList<>(ids);
    }

    private static void clearLegacyDirs(Path outputDir) {
        Path generated = outputDir.resolve("generated");
        for (String legacy : new String[]{"items", "block-items", "fluids"}) {
            clearDir(generated.resolve(legacy));
        }
    }

    private static void clearDir(Path dir) {
        if (!Files.exists(dir)) {
            return;
        }
        try {
            MoreFiles.deleteRecursively(dir, RecursiveDeleteOption.ALLOW_INSECURE);
        } catch (IOException e) {
            MweMod.LOGGER.warn("{} could not clear {}: {}", Log.ICONS, dir, e.getMessage());
        }
    }

    private static void logRegistryItemCountsByNamespace() {
        Map<String, Integer> byNs = new LinkedHashMap<>();
        int blockItems = 0;
        for (Item item : ForgeRegistries.ITEMS) {
            if (item == null || item == Items.AIR) {
                continue;
            }
            if (item instanceof net.minecraft.world.item.BlockItem) {
                blockItems++;
            }
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
            if (id == null) {
                continue;
            }
            byNs.merge(id.getNamespace(), 1, Integer::sum);
        }
        MweMod.LOGGER.debug("{} block-items in registry: {}", Log.ICONS, blockItems);
        for (String ns : new String[]{"minecraft_web_export", "minecraft"}) {
            MweMod.LOGGER.debug("{} {}: {} items in registry", Log.ICONS, ns, byNs.getOrDefault(ns, 0));
        }
    }

    private static void renderItemStackIcon(
            Minecraft client,
            GuiGraphics guiGraphics,
            OffScreenRenderer renderer,
            ItemStack stack) {
        var sprites = collectSprites(client, stack);
        Runnable draw = () -> {
            guiGraphics.renderItem(stack, 0, 0);
            guiGraphics.renderItemDecorations(client.font, stack, 0, 0, "");
        };
        renderer.captureAsPng(draw);
    }

    private static void renderItemIcon(
            Minecraft client,
            GuiGraphics guiGraphics,
            OffScreenRenderer renderer,
            Item item) {
        ItemStack stack = new ItemStack(item);
        var sprites = collectSprites(client, item);
        Runnable draw = () -> {
            guiGraphics.renderItem(stack, 0, 0);
            guiGraphics.renderItemDecorations(client.font, stack, 0, 0, "");
        };
        renderer.captureAsPng(draw);
    }

    private static Set<TextureAtlasSprite> collectSprites(Minecraft client, ItemStack stack) {
        BakedModel model = client.getItemRenderer().getModel(stack, null, null, 0);
        return guessSprites(Set.of(model));
    }

    private static Set<TextureAtlasSprite> collectSprites(Minecraft client, Item item) {
        return collectSprites(client, new ItemStack(item));
    }

    private static String failureSummary(Throwable error) {
        String msg = error.getMessage();
        if (msg != null && !msg.isBlank()) {
            return error.getClass().getSimpleName() + ": " + msg;
        }
        return error.getClass().getSimpleName();
    }

    private static Set<TextureAtlasSprite> guessSprites(Collection<BakedModel> models) {
        var result = Collections.newSetFromMap(new IdentityHashMap<TextureAtlasSprite, Boolean>());
        var random = RandomSource.create(0);
        for (var model : models) {
            for (var quad : model.getQuads(null, null, random, ModelData.EMPTY, null)) {
                result.add(quad.getSprite());
            }
        }
        return result;
    }
}
