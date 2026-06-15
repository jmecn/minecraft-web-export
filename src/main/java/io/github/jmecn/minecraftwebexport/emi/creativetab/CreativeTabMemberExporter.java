package io.github.jmecn.minecraftwebexport.emi.creativetab;

import io.github.jmecn.minecraftwebexport.Constants;
import io.github.jmecn.minecraftwebexport.MweMod;
import io.github.jmecn.minecraftwebexport.emi.EmiPaths;
import io.github.jmecn.minecraftwebexport.emi.Visibility;
import io.github.jmecn.minecraftwebexport.emi.support.Log;
import io.github.jmecn.minecraftwebexport.io.ExportWriteQueue;
import io.github.jmecn.minecraftwebexport.model.creativetab.CreativeTabEntry;
import io.github.jmecn.minecraftwebexport.model.creativetab.CreativeTabMembers;
import io.github.jmecn.minecraftwebexport.model.creativetab.ParsedCreativeTabId;
import io.github.jmecn.minecraftwebexport.model.pipeline.CreativeTabExportResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.CreativeModeTab;

public final class CreativeTabMemberExporter {

    private CreativeTabMemberExporter() {
    }

    public static CreativeTabExportResult export(
            Path outputDir,
            MinecraftServer server,
            Set<String> indexedItemIds,
            ExportWriteQueue writes) throws IOException {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(writes, "writes");
        if (indexedItemIds == null || indexedItemIds.isEmpty()) {
            MweMod.LOGGER.warn("{} skipped: no indexed items", Log.CREATIVE_TABS);
            return CreativeTabExportResult.empty();
        }

        Path tabsRoot = EmiPaths.resolve(outputDir, Constants.CREATIVE_TABS_DIR);
        Files.createDirectories(tabsRoot);

        Registry<CreativeModeTab> registry = server.registryAccess().registryOrThrow(Registries.CREATIVE_MODE_TAB);
        CreativeModeTab.ItemDisplayParameters params = new CreativeModeTab.ItemDisplayParameters(
                FeatureFlags.DEFAULT_FLAGS, true, server.registryAccess());

        List<CreativeTabEntry> catalog = new ArrayList<>();
        Set<String> langKeys = new TreeSet<>();
        int memberFiles = 0;
        int totalMemberRefs = 0;

        List<ResourceKey<CreativeModeTab>> tabKeys = new ArrayList<>(registry.registryKeySet());
        tabKeys.sort(Comparator.comparing(ResourceKey::location));

        for (ResourceKey<CreativeModeTab> tabKey : tabKeys) {
            if (!CreativeTabSupport.shouldExportTab(tabKey)) {
                continue;
            }
            CreativeModeTab tab = registry.get(tabKey);
            if (tab == null) {
                continue;
            }
            tab.buildContents(params);

            Set<String> members = new TreeSet<>();
            for (String itemId : CreativeTabSupport.collectMemberItemIds(tab.getDisplayItems())) {
                if (!indexedItemIds.contains(itemId)) {
                    continue;
                }
                if (!Visibility.shouldExportRegistryId(server, itemId)) {
                    continue;
                }
                members.add(itemId);
            }
            if (members.isEmpty()) {
                continue;
            }

            String tabId = CreativeTabSupport.tabId(tabKey);
            ParsedCreativeTabId parsed = ParsedCreativeTabId.parse(tabId);
            if (parsed == null) {
                continue;
            }

            String nameKey = CreativeTabSupport.resolveNameKey(tab);
            langKeys.add(nameKey);
            catalog.add(new CreativeTabEntry(tabId, CreativeTabSupport.tabOrder(tab), nameKey));

            writes.submitJson(parsed.toMemberFilePath(outputDir), CreativeTabMembers.of(tabId, List.copyOf(members)));
            memberFiles++;
            totalMemberRefs += members.size();
        }

        catalog.sort(Comparator.comparingInt(CreativeTabEntry::order).thenComparing(CreativeTabEntry::id));

        MweMod.LOGGER.info(
                "{} {} tabs, {} member files, {} item refs",
                Log.CREATIVE_TABS,
                catalog.size(),
                memberFiles,
                totalMemberRefs);

        return new CreativeTabExportResult(catalog.size(), List.copyOf(catalog), Set.copyOf(langKeys));
    }
}
