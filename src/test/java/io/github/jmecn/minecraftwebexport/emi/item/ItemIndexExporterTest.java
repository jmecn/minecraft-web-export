package io.github.jmecn.minecraftwebexport.emi.item;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.jmecn.minecraftwebexport.Constants;
import io.github.jmecn.minecraftwebexport.emi.EmiPaths;
import io.github.jmecn.minecraftwebexport.model.pipeline.ExportContext;
import io.github.jmecn.minecraftwebexport.model.pipeline.Mode;
import io.github.jmecn.minecraftwebexport.io.ExportWriteQueue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemIndexExporterTest {

    @TempDir
    Path tempDir;

    @Test
    void collectRegistryTagIdsReturnsEmptyWithoutServer() {
        assertTrue(ItemIndexExporter.collectRegistryTagIds(null, Set.of("minecraft:stick")).isEmpty());
    }

    @Test
    void mergesSeedItemIdsWithoutRecipeRefs() throws IOException {
        ExportContext context = ExportContext.builder(Mode.FULL)
                .itemIds(Set.of("minecraft:oak_log"))
                .build();
        ItemIndexExporter.accumulateRecipeRefsFromLayout(
                "minecraft:stick",
                layout("""
                        {
                          "schema": 2,
                          "id": "minecraft:stick",
                          "category": "minecraft:crafting",
                          "widgets": [
                            {
                              "type": "slot",
                              "role": "output",
                              "ingredient": "item:minecraft:stick"
                            }
                          ]
                        }
                        """),
                context.inputs(),
                context.outputs(),
                context.fluidRegistryIds());

        ExportWriteQueue.drain(writes -> ItemIndexExporter.export(tempDir, null, context, writes));

        Path oakFile = EmiPaths.resolve(tempDir, "items/minecraft/oak_log.json");
        Path itemsIndexFile = EmiPaths.resolve(tempDir, Constants.ITEMS_INDEX_FILE);
        JsonObject index = JsonParser.parseString(Files.readString(itemsIndexFile)).getAsJsonObject();

        assertTrue(Files.exists(oakFile));
        assertTrue(index.getAsJsonArray("minecraft").contains(JsonParser.parseString("\"oak_log\"")));
        JsonObject oak = JsonParser.parseString(Files.readString(oakFile)).getAsJsonObject();
        assertFalse(oak.has("inputs"));
        assertFalse(oak.has("outputs"));
    }

    private static JsonObject layout(String json) {
        return JsonParser.parseString(json).getAsJsonObject();
    }
}
