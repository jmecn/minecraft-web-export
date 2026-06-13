package io.github.jmecn.minecraftwebexport.emi.item;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.jmecn.minecraftwebexport.Constants;
import io.github.jmecn.minecraftwebexport.MweMod;
import io.github.jmecn.minecraftwebexport.emi.bundle.Paths;
import io.github.jmecn.minecraftwebexport.emi.lang.RegistryKeys;
import io.github.jmecn.minecraftwebexport.emi.support.Log;
import io.github.jmecn.minecraftwebexport.io.JsonIO;
import io.github.jmecn.minecraftwebexport.model.emi.item.NameKeysResult;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

public final class NameKeysWriter {

    private NameKeysWriter() {
    }


    public static boolean isEnabled() {
        return !Boolean.getBoolean(Constants.PROP_SKIP_ITEM_NAME_KEYS_EXPORT);
    }

    public static NameKeysResult export(Path outputDir, Minecraft client) throws IOException {
        if (client == null || client.level == null) {
            MweMod.LOGGER.warn("{} skipped: no client level", Log.ITEM_NAME_KEYS);
            return NameKeysResult.EMPTY;
        }

        Path bundleRoot = Paths.resolve(outputDir, "");
        Path indexPath = bundleRoot.resolve(Constants.ITEMS_INDEX_FILE);
        if (!Files.isRegularFile(indexPath)) {
            MweMod.LOGGER.warn("{} skipped: missing {}", Log.ITEM_NAME_KEYS, Constants.ITEMS_INDEX_FILE);
            return NameKeysResult.EMPTY;
        }

        JsonObject index = JsonParser.parseString(Files.readString(indexPath)).getAsJsonObject();
        Map<String, String> items = new TreeMap<>();
        int fluidCount = 0;

        for (Map.Entry<String, JsonElement> entry : index.entrySet()) {
            String namespace = entry.getKey();
            if ("schema".equals(namespace)) {
                continue;
            }
            if ("fluid".equals(namespace) && entry.getValue().isJsonArray()) {
                for (JsonElement fluidEl : entry.getValue().getAsJsonArray()) {
                    if (!fluidEl.isJsonPrimitive()) {
                        continue;
                    }
                    String path = fluidEl.getAsString();
                    if (path == null || path.isEmpty()) {
                        continue;
                    }
                    String registryId = path.contains(":") ? path : "minecraft:" + path;
                    items.put(registryId, RegistryKeys.resolveFluidDescriptionKey(client, registryId));
                    fluidCount++;
                }
                continue;
            }
            if (!entry.getValue().isJsonArray()) {
                continue;
            }
            for (JsonElement pathEl : entry.getValue().getAsJsonArray()) {
                if (!pathEl.isJsonPrimitive()) {
                    continue;
                }
                String path = pathEl.getAsString();
                if (path == null || path.isEmpty()) {
                    continue;
                }
                String registryId = path.contains(":") ? path : namespace + ":" + path;
                items.put(registryId, RegistryKeys.resolveItemDescriptionKey(client, registryId));
            }
        }

        if (items.isEmpty()) {
            return NameKeysResult.EMPTY;
        }

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schema", 1);
        root.put("items", items);

        Path out = bundleRoot.resolve(Constants.ITEM_NAME_KEYS_FILE);
        Files.createDirectories(out.getParent());
        JsonIO.writeLine(out, root);

        MweMod.LOGGER.info(
                "{} {} registry ids ({} fluids) -> {}",
                Log.ITEM_NAME_KEYS,
                items.size(),
                fluidCount,
                out);
        return new NameKeysResult(items.size() - fluidCount, fluidCount);
    }

    public static Map<String, String> readNameKeys(Path outputDir) throws IOException {
        Path file = Paths.resolve(outputDir, Constants.ITEM_NAME_KEYS_FILE);
        if (!Files.isRegularFile(file)) {
            return Map.of();
        }
        JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        JsonElement items = root.get("items");
        if (items == null || !items.isJsonObject()) {
            return Map.of();
        }
        Map<String, String> map = new TreeMap<>();
        for (Map.Entry<String, JsonElement> entry : items.getAsJsonObject().entrySet()) {
            if (entry.getValue().isJsonPrimitive()) {
                map.put(entry.getKey(), entry.getValue().getAsString());
            }
        }
        return map;
    }
}
