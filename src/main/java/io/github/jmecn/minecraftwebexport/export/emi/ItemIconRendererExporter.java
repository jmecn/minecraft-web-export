package io.github.jmecn.minecraftwebexport.export.emi;

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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

public final class ItemIconRendererExporter {

    private static final Logger LOGGER = LogManager.getLogger(ItemIconRendererExporter.class);
    private static final String ICON_LOG_STRIDE_PROPERTY = "minecraftWebExport.iconLogStride";
    private static final int FLUSH_RENDER_EVERY = 256;

    private ItemIconRendererExporter() {
    }

    public record Result(
            int itemsWritten,
            int fluidsWritten,
            int atlasPages,
            int itemFailures,
            int fluidFailures,
            int fluidsSkippedNoStack,
            long atlasPngBytes,
            long indexBytes,
            long cssBytes) {

        public int totalSpritesWritten() {
            return itemsWritten + fluidsWritten;
        }

        public int failures() {
            return itemFailures + fluidFailures;
        }
    }

    public static boolean isEnabled() {
        return !Boolean.getBoolean("minecraftWebExport.skipItemIconExport");
    }

    public static boolean fluidsEnabled() {
        return !Boolean.getBoolean("minecraftWebExport.skipFluidIconExport");
    }

    public static Result export(Path outputDir, Minecraft client) {
        return export(outputDir, client, null, null, null);
    }

    public static Result export(Path outputDir, Minecraft client, Set<String> onlyItemIds) {
        return export(outputDir, client, onlyItemIds, null, null);
    }

    public static Result export(
            Path outputDir,
            Minecraft client,
            Set<String> onlyItemIds,
            Map<String, Integer> usageWeights) {
        return export(outputDir, client, onlyItemIds, null, usageWeights);
    }

    public static Result export(
            Path outputDir,
            Minecraft client,
            Set<String> onlyItemIds,
            Set<String> onlyFluidIds,
            Map<String, Integer> usageWeights) {
        return export(outputDir, client, onlyItemIds, onlyFluidIds, usageWeights, Map.of());
    }

    public static Result export(
            Path outputDir,
            Minecraft client,
            Set<String> onlyItemIds,
            Set<String> onlyFluidIds,
            Map<String, Integer> usageWeights,
            Map<String, ItemStack> iconVariants) {
        Path iconsRoot = EmiBundlePaths.resolve(outputDir, EmiBundlePaths.ICONS_DIR);
        clearLegacyDirs(outputDir);
        clearDir(iconsRoot);
        return exportImpl(iconsRoot, client, onlyItemIds, onlyFluidIds, usageWeights, iconVariants);
    }

    /** Icon atlas at an explicit directory (e.g. {@code guide-export/assets/icons}). */
    public static Result exportAtRoot(
            Path iconsRoot,
            Minecraft client,
            Set<String> onlyItemIds,
            Map<String, Integer> usageWeights) {
        clearDir(iconsRoot);
        return exportImpl(iconsRoot, client, onlyItemIds, null, usageWeights, Map.of());
    }

    private static Result exportImpl(
            Path iconsRoot,
            Minecraft client,
            Set<String> onlyItemIds,
            Set<String> onlyFluidIds,
            Map<String, Integer> usageWeights,
            Map<String, ItemStack> iconVariants) {
        int cell = IconExportSizes.iconCellSize();
        int atlasMax = IconExportSizes.atlasMaxSize();
        boolean exportFluids = fluidsEnabled() && (onlyFluidIds == null || !onlyFluidIds.isEmpty());

        List<ResourceLocation> itemOrder = IconItemOrdering.orderedForPass(onlyItemIds, item -> true, usageWeights);
        List<String> fluidOrder = exportFluids ? orderedFluidIds(onlyFluidIds) : List.of();
        Map<String, ItemStack> variants = iconVariants != null ? iconVariants : Map.of();

        if (onlyItemIds != null) {
            LOGGER.info(
                    "{} closure: {} items, {} fluids, {} nbt variants at {}px -> {}",
                    ExportLog.ICONS,
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
            LOGGER.info(
                    "{} full export: {} registry items, {} fluids at {}px (max atlas {}px) -> {}",
                    ExportLog.ICONS,
                    totalItems,
                    fluidOrder.size(),
                    cell,
                    atlasMax,
                    iconsRoot);
            logRegistryItemCountsByNamespace();
        }

        int totalSprites = itemOrder.size() + fluidOrder.size() + variants.size() + 1;
        List<IconAtlasLayout.PagePlan> layout = IconAtlasLayout.plan(totalSprites, cell, atlasMax);
        if (!layout.isEmpty()) {
            IconAtlasLayout.PagePlan first = layout.get(0);
            LOGGER.info(
                    "{} {} sprites, planned {} page(s), first page {}x{} cells ({}x{}px)",
                    ExportLog.ICONS,
                    totalSprites,
                    layout.size(),
                    first.cols(),
                    first.rows(),
                    first.widthPx(cell),
                    first.heightPx(cell));
        }

        int itemLogStride = ExportProgressLog.stride(itemOrder.size(), ICON_LOG_STRIDE_PROPERTY, 50, 500);
        int variantLogStride = ExportProgressLog.stride(variants.size(), ICON_LOG_STRIDE_PROPERTY, 20, 200);

        int itemsPlaced = 0;
        int itemFailures = 0;
        int fluidsPlaced = 0;
        int fluidFailures = 0;
        int fluidsSkipped = 0;
        int variantsPlaced = 0;
        int variantFailures = 0;

        ItemIconAtlasBuilder.AtlasResult atlasResult;
        try (var renderer = new OffScreenRenderer(cell, cell);
             var atlas = new ItemIconAtlasBuilder(iconsRoot, cell, atlasMax, "icon", layout, usageWeights)) {
            var bufferSource = client.renderBuffers().bufferSource();
            var guiGraphics = new GuiGraphics(client, bufferSource);
            IconPlaceholderRenderer.render(client, guiGraphics, renderer);
            atlas.place(IconPlaceholderRenderer.REGISTRY_ID, renderer);

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
                    if (index % FLUSH_RENDER_EVERY == 0) {
                        bufferSource.endBatch();
                    }
                    if (ExportProgressLog.shouldLog(index, itemTotal, itemLogStride)) {
                        LOGGER.info(
                                "{} items: {}% {}/{} ({} ok, {} fail)",
                                ExportLog.ICONS,
                                ExportProgressLog.percent(index, itemTotal),
                                index,
                                itemTotal,
                                itemsPlaced,
                                itemFailures);
                    }
                } catch (Exception e) {
                    itemFailures++;
                    ExportLog.detailFailure(
                            LOGGER,
                            itemFailures,
                            "{} item failed {} ({}/{}): {}",
                            ExportLog.ICONS,
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
                    if (!FluidStillIconRenderer.render(client, guiGraphics, renderer, fluid)) {
                        fluidsSkipped++;
                        continue;
                    }
                    atlas.place(fluidIdStr, renderer);
                    fluidsPlaced++;
                } catch (Exception e) {
                    fluidFailures++;
                    ExportLog.detailFailure(
                            LOGGER,
                            fluidFailures,
                            "{} fluid failed {}: {}",
                            ExportLog.ICONS,
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
                        if (ExportProgressLog.shouldLog(variantIndex, variantTotal, variantLogStride)) {
                            LOGGER.info(
                                    "{} nbt variants: {}% {}/{} ({} ok, {} fail)",
                                    ExportLog.ICONS,
                                    ExportProgressLog.percent(variantIndex, variantTotal),
                                    variantIndex,
                                    variantTotal,
                                    variantsPlaced,
                                    variantFailures);
                        }
                    } catch (Exception e) {
                        variantFailures++;
                        ExportLog.detailFailure(
                                LOGGER,
                                variantFailures,
                                "{} nbt variant failed {} ({}/{}): {}",
                                ExportLog.ICONS,
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
        LOGGER.info(
                "{} done: {} items, {} nbt variants, {} fluids ({} skipped no still), {} pages, {} failures at {}",
                ExportLog.ICONS,
                itemsPlaced,
                variantsPlaced,
                fluidsPlaced,
                fluidsSkipped,
                atlasResult.pageCount(),
                totalFailures,
                iconsRoot);
        if (totalFailures > ExportLog.DETAIL_FAILURE_LIMIT) {
            LOGGER.warn(
                    "{} {} icon failures (first {} at DEBUG; -D{}=true or enable DEBUG on export.emi)",
                    ExportLog.ICONS,
                    totalFailures,
                    ExportLog.DETAIL_FAILURE_LIMIT,
                    "minecraftWebExport.export.logDetailFailures");
        }

        return new Result(
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
            LOGGER.warn("{} could not clear {}: {}", ExportLog.ICONS, dir, e.getMessage());
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
        LOGGER.debug("{} block-items in registry: {}", ExportLog.ICONS, blockItems);
        for (String ns : new String[]{"minecraft_web_export", "minecraft"}) {
            LOGGER.debug("{} {}: {} items in registry", ExportLog.ICONS, ns, byNs.getOrDefault(ns, 0));
        }
    }

    private static void renderItemStackIcon(
            Minecraft client,
            GuiGraphics guiGraphics,
            OffScreenRenderer renderer,
            ItemStack stack) {
        var sprites = collectSprites(client, stack);
        if (renderer.isAnimated(sprites)) {
            renderer.uploadAnimatedFirstFrame(sprites);
        }
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
        if (renderer.isAnimated(sprites)) {
            renderer.uploadAnimatedFirstFrame(sprites);
        }
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
