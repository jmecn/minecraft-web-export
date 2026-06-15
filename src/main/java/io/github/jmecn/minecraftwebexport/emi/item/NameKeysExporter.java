package io.github.jmecn.minecraftwebexport.emi.item;

import io.github.jmecn.minecraftwebexport.Constants;
import io.github.jmecn.minecraftwebexport.MweMod;
import io.github.jmecn.minecraftwebexport.emi.EmiPaths;
import io.github.jmecn.minecraftwebexport.emi.lang.RegistryKeys;
import io.github.jmecn.minecraftwebexport.emi.support.Log;
import io.github.jmecn.minecraftwebexport.io.ExportWriteQueue;
import io.github.jmecn.minecraftwebexport.model.item.ItemIndex;
import io.github.jmecn.minecraftwebexport.model.item.NameKeys;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public final class NameKeysExporter {

    private NameKeysExporter() {
    }

    public static Map<String, String> export(
            Path outputDir, Minecraft client, ItemIndex index, ExportWriteQueue writes) throws IOException {
        Objects.requireNonNull(writes, "writes");
        if (client == null || client.level == null) {
            MweMod.LOGGER.warn("{} skipped: no client level", Log.ITEM_NAME_KEYS);
            return Map.of();
        }
        if (index == null || index.namespacePaths().isEmpty() && index.fluidRegistryIds().isEmpty()) {
            MweMod.LOGGER.warn("{} skipped: empty item index", Log.ITEM_NAME_KEYS);
            return Map.of();
        }

        Map<String, String> items = resolveNameKeys(client, index);
        if (items.isEmpty()) {
            return Map.of();
        }

        Path out = EmiPaths.resolve(outputDir, Constants.ITEM_NAME_KEYS_FILE);
        writes.submitJsonLine(out, NameKeys.of(items));

        int fluidCount = (int) items.keySet().stream().filter(id -> index.fluidRegistryIds().contains(id)).count();
        MweMod.LOGGER.info(
                "{} {} registry ids ({} fluids) -> {}",
                Log.ITEM_NAME_KEYS,
                items.size(),
                fluidCount,
                out);
        return Map.copyOf(items);
    }

    static Map<String, String> resolveNameKeys(Minecraft client, ItemIndex index) {
        Map<String, String> items = new TreeMap<>();
        int total = 0;
        for (List<String> paths : index.namespacePaths().values()) {
            total += paths.size();
        }
        total += index.fluidRegistryIds().size();
        int progress = 0;
        int logStride = 5_000;

        MweMod.LOGGER.info("{} resolving name keys for {} registry ids ...", Log.ITEM_NAME_KEYS, total);

        for (Map.Entry<String, List<String>> entry : index.namespacePaths().entrySet()) {
            String namespace = entry.getKey();
            for (String path : entry.getValue()) {
                if (path == null || path.isEmpty()) {
                    continue;
                }
                progress++;
                String registryId = path.contains(":") ? path : namespace + ":" + path;
                items.put(registryId, RegistryKeys.resolveItemDescriptionKey(client, registryId));
                if (progress % logStride == 0) {
                    MweMod.LOGGER.info("{} {}/{} name keys resolved", Log.ITEM_NAME_KEYS, progress, total);
                }
            }
        }
        for (String fluidPath : index.fluidRegistryIds()) {
            if (fluidPath == null || fluidPath.isEmpty()) {
                continue;
            }
            progress++;
            String registryId = fluidPath.contains(":") ? fluidPath : "minecraft:" + fluidPath;
            items.put(registryId, RegistryKeys.resolveFluidDescriptionKey(client, registryId));
            if (progress % logStride == 0) {
                MweMod.LOGGER.info("{} {}/{} name keys resolved", Log.ITEM_NAME_KEYS, progress, total);
            }
        }
        return items;
    }
}
