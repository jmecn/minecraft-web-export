package io.github.jmecn.minecraftwebexport.emi.item;

import io.github.jmecn.minecraftwebexport.Constants;
import io.github.jmecn.minecraftwebexport.MweMod;
import io.github.jmecn.minecraftwebexport.emi.EmiPaths;
import io.github.jmecn.minecraftwebexport.emi.lang.RegistryKeys;
import io.github.jmecn.minecraftwebexport.emi.support.Log;
import io.github.jmecn.minecraftwebexport.io.JsonIO;
import io.github.jmecn.minecraftwebexport.model.emi.item.NameKeysResult;
import io.github.jmecn.minecraftwebexport.model.item.ItemIndex;
import io.github.jmecn.minecraftwebexport.model.item.NameKeys;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class NameKeysExporter {

    private NameKeysExporter() {
    }

    public static boolean isEnabled() {
        return !Boolean.getBoolean(Constants.PROP_SKIP_ITEM_NAME_KEYS_EXPORT);
    }

    public static NameKeysResult export(Path outputDir, Minecraft client) throws IOException {
        if (client == null || client.level == null) {
            MweMod.LOGGER.warn("{} skipped: no client level", Log.ITEM_NAME_KEYS);
            return NameKeysResult.EMPTY;
        }

        Path bundleRoot = EmiPaths.resolve(outputDir, "");
        Path indexPath = bundleRoot.resolve(Constants.ITEMS_INDEX_FILE);
        if (!Files.isRegularFile(indexPath)) {
            MweMod.LOGGER.warn("{} skipped: missing {}", Log.ITEM_NAME_KEYS, Constants.ITEMS_INDEX_FILE);
            return NameKeysResult.EMPTY;
        }

        ItemIndex index = JsonIO.read(indexPath, ItemIndex.class);
        Map<String, String> items = new TreeMap<>();
        int fluidCount = 0;

        for (Map.Entry<String, List<String>> entry : index.namespacePaths().entrySet()) {
            String namespace = entry.getKey();
            for (String path : entry.getValue()) {
                if (path == null || path.isEmpty()) {
                    continue;
                }
                String registryId = path.contains(":") ? path : namespace + ":" + path;
                items.put(registryId, RegistryKeys.resolveItemDescriptionKey(client, registryId));
            }
        }
        for (String fluidPath : index.fluidRegistryIds()) {
            if (fluidPath == null || fluidPath.isEmpty()) {
                continue;
            }
            String registryId = fluidPath.contains(":") ? fluidPath : "minecraft:" + fluidPath;
            items.put(registryId, RegistryKeys.resolveFluidDescriptionKey(client, registryId));
            fluidCount++;
        }

        if (items.isEmpty()) {
            return NameKeysResult.EMPTY;
        }

        Path out = bundleRoot.resolve(Constants.ITEM_NAME_KEYS_FILE);
        Files.createDirectories(out.getParent());
        JsonIO.writeLine(out, NameKeys.of(items));

        MweMod.LOGGER.info(
                "{} {} registry ids ({} fluids) -> {}",
                Log.ITEM_NAME_KEYS,
                items.size(),
                fluidCount,
                out);
        return new NameKeysResult(items.size() - fluidCount, fluidCount);
    }

    public static Map<String, String> readNameKeys(Path outputDir) throws IOException {
        Path file = EmiPaths.resolve(outputDir, Constants.ITEM_NAME_KEYS_FILE);
        if (!Files.isRegularFile(file)) {
            return Map.of();
        }
        NameKeys document = JsonIO.read(file, NameKeys.class);
        return document.items();
    }
}
