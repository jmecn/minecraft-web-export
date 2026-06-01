package io.github.jmecn.minecraftwebexport.export.emi;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.jmecn.minecraftwebexport.export.ExportGson;
import net.minecraft.client.Minecraft;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Writes {@code items/name-keys.json}: registry id → in-game {@code getDescriptionId()} translation key.
 */
public final class ItemNameKeysExporter {

    private static final Logger LOGGER = LogManager.getLogger(ItemNameKeysExporter.class);
    private static final Gson GSON = ExportGson.GSON;

    private ItemNameKeysExporter() {
    }

    public record Result(int itemCount, int fluidCount) {
        static final Result EMPTY = new Result(0, 0);
    }

    public static boolean isEnabled() {
        return !Boolean.getBoolean("minecraftWebExport.skipItemNameKeysExport");
    }

    public static Result export(Path outputDir, Minecraft client) throws IOException {
        if (client == null || client.level == null) {
            LOGGER.warn("{} skipped: no client level", ExportLog.ITEM_NAME_KEYS);
            return Result.EMPTY;
        }

        Path bundleRoot = EmiBundlePaths.resolve(outputDir, "");
        Path indexPath = bundleRoot.resolve(EmiBundlePaths.ITEMS_INDEX_FILE);
        if (!Files.isRegularFile(indexPath)) {
            LOGGER.warn("{} skipped: missing {}", ExportLog.ITEM_NAME_KEYS, EmiBundlePaths.ITEMS_INDEX_FILE);
            return Result.EMPTY;
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
                    items.put(registryId, RegistryLangKeys.resolveFluidDescriptionKey(client, registryId));
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
                items.put(registryId, RegistryLangKeys.resolveItemDescriptionKey(client, registryId));
            }
        }

        if (items.isEmpty()) {
            return Result.EMPTY;
        }

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schema", 1);
        root.put("items", items);

        Path out = bundleRoot.resolve(EmiBundlePaths.ITEM_NAME_KEYS_FILE);
        Files.createDirectories(out.getParent());
        Files.writeString(out, GSON.toJson(root) + "\n", StandardCharsets.UTF_8);

        LOGGER.info(
                "{} {} registry ids ({} fluids) -> {}",
                ExportLog.ITEM_NAME_KEYS,
                items.size(),
                fluidCount,
                out);
        return new Result(items.size() - fluidCount, fluidCount);
    }

    /** Reads {@code items/name-keys.json} into a map (empty if missing). */
    public static Map<String, String> readNameKeys(Path outputDir) throws IOException {
        Path file = EmiBundlePaths.resolve(outputDir, EmiBundlePaths.ITEM_NAME_KEYS_FILE);
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
