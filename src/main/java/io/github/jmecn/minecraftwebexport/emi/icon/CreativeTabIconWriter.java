package io.github.jmecn.minecraftwebexport.emi.icon;

import io.github.jmecn.minecraftwebexport.Constants;
import io.github.jmecn.minecraftwebexport.MweMod;
import io.github.jmecn.minecraftwebexport.emi.EmiPaths;
import io.github.jmecn.minecraftwebexport.emi.support.Log;
import io.github.jmecn.minecraftwebexport.io.ExportWriteQueue;
import io.github.jmecn.minecraftwebexport.model.creativetab.CreativeTabEntry;
import io.github.jmecn.minecraftwebexport.model.creativetab.CreativeTabIconsIndex;
import io.github.jmecn.minecraftwebexport.model.creativetab.CreativeTabsIndex;
import io.github.jmecn.minecraftwebexport.model.creativetab.ParsedCreativeTabId;
import io.github.jmecn.minecraftwebexport.model.icon.AtlasPagePlan;
import io.github.jmecn.minecraftwebexport.model.pipeline.CreativeTabExportResult;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public final class CreativeTabIconWriter {

    private CreativeTabIconWriter() {
    }

    public static int export(
            Path outputRoot,
            Minecraft client,
            CreativeTabExportResult creativeTabs,
            ExportWriteQueue writes) throws IOException {
        Objects.requireNonNull(writes, "writes");
        List<CreativeTabEntry> tabs = creativeTabs == null ? List.of() : creativeTabs.tabs();
        if (tabs.isEmpty() || client == null || client.level == null) {
            return 0;
        }

        Registry<CreativeModeTab> registry = client.level.registryAccess().registryOrThrow(Registries.CREATIVE_MODE_TAB);
        int cell = AtlasBuilder.categoryIconCellSize();
        int atlasMax = AtlasBuilder.atlasMaxSize();
        Path iconsRoot = EmiPaths.resolve(outputRoot, Constants.CREATIVE_TABS_ICONS_DIR);
        List<AtlasPagePlan> layout = AtlasBuilder.planPages(tabs.size(), cell, atlasMax);

        int placed = 0;
        int failures = 0;
        MultiBufferSource.BufferSource bufferSource = client.renderBuffers().bufferSource();
        try (OffScreenRenderer renderer = new OffScreenRenderer(cell, cell);
                AtlasBuilder atlas = new AtlasBuilder(iconsRoot, cell, atlasMax, "creative-tab", layout, null, writes)) {
            GuiGraphics guiGraphics = new GuiGraphics(client, bufferSource);
            AtlasBuilder.renderPlaceholder(guiGraphics, renderer);
            atlas.place(Constants.MISSING_ICON_REGISTRY_ID, renderer);

            int index = 0;
            for (CreativeTabEntry entry : tabs) {
                index++;
                CreativeModeTab tab = resolveTab(registry, entry.id());
                if (tab == null) {
                    failures++;
                    continue;
                }
                ItemStack icon = tab.getIconItem();
                if (icon.isEmpty() || !renderItemStack(client, guiGraphics, renderer, icon)) {
                    failures++;
                    continue;
                }
                atlas.place(entry.id(), renderer);
                placed++;
                if (index % 32 == 0) {
                    bufferSource.endBatch();
                }
            }
            bufferSource.endBatch();

            AtlasBuilder.AtlasFinishResult built = atlas.finishCreativeTabIcons();
            writes.submitJson(
                    iconsRoot.resolve("index.json"),
                    CreativeTabIconsIndex.of(cell, built.pages(), built.sprites()));
        }

        writes.submitJson(
                EmiPaths.resolve(outputRoot, Constants.CREATIVE_TABS_INDEX_FILE),
                CreativeTabsIndex.of(cell, Constants.CREATIVE_TABS_ICONS_DIR, tabs));

        MweMod.LOGGER.info(
                "{} {} tabs, {} icons placed, {} failed",
                Log.CREATIVE_TABS,
                tabs.size(),
                placed,
                failures);
        return tabs.size();
    }

    private static CreativeModeTab resolveTab(Registry<CreativeModeTab> registry, String tabId) {
        ParsedCreativeTabId parsed = ParsedCreativeTabId.parse(tabId);
        if (parsed == null) {
            return null;
        }
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(parsed.namespace(), parsed.path());
        ResourceKey<CreativeModeTab> key = ResourceKey.create(Registries.CREATIVE_MODE_TAB, location);
        return registry.get(key);
    }

    private static boolean renderItemStack(
            Minecraft client, GuiGraphics guiGraphics, OffScreenRenderer renderer, ItemStack stack) {
        try {
            renderer.setupItemRendering();
            Runnable draw = () -> {
                guiGraphics.renderItem(stack, 0, 0);
                guiGraphics.renderItemDecorations(client.font, stack, 0, 0, "");
            };
            renderer.captureAsPng(draw);
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }
}
