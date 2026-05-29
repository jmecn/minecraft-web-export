package io.github.jmecn.minecraftwebexport.export.emi;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.jmecn.minecraftwebexport.export.ExportGson;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.LanguageManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Fills missing {@code item.*}/{@code block.*}/{@code fluid.*} lang entries from in-game hover names.
 *
 * <p>Needed for mods (e.g. AE2) where kubejs lang overrides omit most item keys, and for GregTech-style
 * composed names that are resolved only at runtime.</p>
 */
public final class RegistryLangFillExporter {

    private static final Logger LOGGER = LogManager.getLogger(RegistryLangFillExporter.class);

    private RegistryLangFillExporter() {
    }

    public record Result(int languagesTouched, int keysAdded, int itemsResolved, int fluidsResolved) {
    }

    public static boolean isEnabled() {
        return !Boolean.getBoolean("minecraftWebExport.skipRegistryLangFill");
    }

    public static Result fillMissing(
            Path outputDir,
            Minecraft client,
            Set<String> itemIds,
            Set<String> fluidIds) throws IOException {
        Path langRoot = EmiBundlePaths.resolve(outputDir, EmiBundlePaths.LANG_DIR);
        if (!Files.isDirectory(langRoot)) {
            return new Result(0, 0, 0, 0);
        }

        Set<String> languages = MinecraftWebExportLanguages.resolve();
        if (languages == null) {
            languages = client.getLanguageManager().getLanguages().keySet();
        }

        LanguageManager languageManager = client.getLanguageManager();
        String previous = languageManager.getSelected();

        int languagesTouched = 0;
        int keysAdded = 0;
        int itemsResolved = 0;
        int fluidsResolved = 0;

        try {
            for (String locale : new TreeSet<>(languages)) {
                if (!languageManager.getLanguages().containsKey(locale)) {
                    continue;
                }
                languageManager.setSelected(locale);

                Path langFile = langRoot.resolve(locale + ".json");
                Map<String, String> table = readLangTable(langFile);
                int before = table.size();
                int itemHits = fillItems(client, itemIds, table);
                int fluidHits = fillFluids(client, fluidIds, table);
                int added = table.size() - before;
                if (added > 0) {
                    writeLangTable(langFile, table);
                    languagesTouched++;
                    keysAdded += added;
                    itemsResolved += itemHits;
                    fluidsResolved += fluidHits;
                    LOGGER.info(
                            "{} {} - added {} registry lang keys ({} items, {} fluids)",
                            ExportLog.LANG,
                            locale,
                            added,
                            itemHits,
                            fluidHits);
                }
            }
        } finally {
            languageManager.setSelected(previous);
        }

        if (keysAdded > 0) {
            LOGGER.info(
                    "{} registry lang fill complete: {} keys across {} language(s)",
                    ExportLog.LANG,
                    keysAdded,
                    languagesTouched);
        }

        return new Result(languagesTouched, keysAdded, itemsResolved, fluidsResolved);
    }

    private static int fillItems(Minecraft client, Set<String> itemIds, Map<String, String> table) {
        if (itemIds == null || itemIds.isEmpty()) {
            return 0;
        }
        int resolved = 0;
        for (String itemId : itemIds) {
            ItemStack stack = stackForItemId(itemId);
            if (stack.isEmpty()) {
                continue;
            }
            String label = stack.getHoverName().getString();
            if (label == null || label.isBlank()) {
                continue;
            }
            if (putIfMissing(table, RegistryLangKeys.itemKey(itemId), label)) {
                resolved++;
            }
            Item item = stack.getItem();
            if (item instanceof BucketItem) {
                putIfMissing(table, RegistryLangKeys.blockKey(itemId), label);
            }
        }
        return resolved;
    }

    private static int fillFluids(Minecraft client, Set<String> fluidIds, Map<String, String> table) {
        if (fluidIds == null || fluidIds.isEmpty()) {
            return 0;
        }
        int resolved = 0;
        for (String fluidId : fluidIds) {
            String label = fluidLabel(fluidId);
            if (label == null || label.isBlank()) {
                continue;
            }
            if (putIfMissing(table, RegistryLangKeys.fluidKey(fluidId), label)) {
                resolved++;
            }
        }
        return resolved;
    }

    private static String fluidLabel(String fluidId) {
        ResourceLocation location = ResourceLocation.tryParse(RegistryLangKeys.normalizeRegistryId(fluidId));
        if (location == null) {
            return null;
        }
        Fluid fluid = BuiltInRegistries.FLUID.get(location);
        if (fluid == null || fluid.isSame(Fluids.EMPTY)) {
            return null;
        }
        for (Item item : BuiltInRegistries.ITEM) {
            if (item instanceof BucketItem bucket && bucket.getFluid() == fluid) {
                return new ItemStack(item).getHoverName().getString();
            }
        }
        return fluid.getFluidType().getDescription().getString();
    }

    private static ItemStack stackForItemId(String itemId) {
        ResourceLocation location = ResourceLocation.tryParse(RegistryLangKeys.normalizeRegistryId(itemId));
        if (location == null) {
            return ItemStack.EMPTY;
        }
        Item item = BuiltInRegistries.ITEM.get(location);
        if (item == null || item == Items.AIR) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(item);
    }

    private static boolean putIfMissing(Map<String, String> table, String key, String value) {
        if (key == null || key.isBlank() || value == null || value.isBlank()) {
            return false;
        }
        if (table.containsKey(key)) {
            return false;
        }
        table.put(key, value);
        return true;
    }

    private static Map<String, String> readLangTable(Path langFile) throws IOException {
        Map<String, String> table = new TreeMap<>();
        if (!Files.isRegularFile(langFile)) {
            return table;
        }
        String raw = Files.readString(langFile, StandardCharsets.UTF_8);
        JsonObject object = JsonParser.parseString(raw).getAsJsonObject();
        for (var entry : object.entrySet()) {
            if (entry.getValue().isJsonPrimitive()) {
                table.put(entry.getKey(), entry.getValue().getAsString());
            }
        }
        return table;
    }

    private static void writeLangTable(Path langFile, Map<String, String> table) throws IOException {
        Map<String, String> sorted = new LinkedHashMap<>();
        table.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> sorted.put(entry.getKey(), entry.getValue()));
        Files.createDirectories(langFile.getParent());
        Files.writeString(langFile, ExportGson.GSON.toJson(sorted), StandardCharsets.UTF_8);
    }
}
