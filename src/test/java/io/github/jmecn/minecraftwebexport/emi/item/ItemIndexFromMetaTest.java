package io.github.jmecn.minecraftwebexport.emi.item;

import io.github.jmecn.minecraftwebexport.Constants;
import io.github.jmecn.minecraftwebexport.emi.EmiPaths;
import io.github.jmecn.minecraftwebexport.enums.pipeline.Mode;
import io.github.jmecn.minecraftwebexport.io.ExportWriteQueue;
import io.github.jmecn.minecraftwebexport.model.pipeline.ExportContext;
import io.github.jmecn.minecraftwebexport.model.recipe.RecipeMeta;
import io.github.jmecn.minecraftwebexport.model.recipe.RecipeWidget;
import io.github.jmecn.minecraftwebexport.model.recipe.WidgetInteraction;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemIndexFromMetaTest {

    @TempDir
    Path tempDir;

    @Test
    void tagInputAddsDisplayItemToInputs() {
        Map<String, Map<String, Set<String>>> inputs = new TreeMap<>();
        Map<String, Map<String, Set<String>>> outputs = new TreeMap<>();
        Set<String> fluids = new TreeSet<>();

        RecipeMeta meta = RecipeMeta.of(
                "gtceu:assembler/iron_plate",
                126,
                72,
                4,
                "gtceu:assembler",
                List.of(RecipeWidget.of(
                        18,
                        27,
                        18,
                        18,
                        "input",
                        WidgetInteraction.tag("c:iron_plates", "item", "gtceu:iron_plate"))));

        ItemIndexExporter.accumulateRecipeRefsFromMeta(
                "gtceu:assembler/iron_plate", meta, inputs, outputs, fluids);

        assertTrue(inputs.containsKey("gtceu:iron_plate"));
        assertEquals(
                Set.of("gtceu:assembler/iron_plate"),
                inputs.get("gtceu:iron_plate").get("gtceu:assembler"));
        assertTrue(outputs.isEmpty());
    }

    @Test
    void outputItemAddsToOutputs() {
        Map<String, Map<String, Set<String>>> inputs = new TreeMap<>();
        Map<String, Map<String, Set<String>>> outputs = new TreeMap<>();
        Set<String> fluids = new TreeSet<>();

        RecipeMeta meta = RecipeMeta.of(
                "minecraft:stick",
                72,
                72,
                4,
                "minecraft:crafting",
                List.of(RecipeWidget.of(
                        0, 0, 18, 18, "output", WidgetInteraction.item("minecraft:stick", null, null))));

        ItemIndexExporter.accumulateRecipeRefsFromMeta("minecraft:stick", meta, inputs, outputs, fluids);

        assertTrue(outputs.containsKey("minecraft:stick"));
        assertEquals(Set.of("minecraft:stick"), outputs.get("minecraft:stick").get("minecraft:crafting"));
        assertTrue(inputs.isEmpty());
    }

    @Test
    void catalystGoesToInputs() {
        Map<String, Map<String, Set<String>>> inputs = new TreeMap<>();
        Map<String, Map<String, Set<String>>> outputs = new TreeMap<>();
        Set<String> fluids = new TreeSet<>();

        RecipeMeta meta = RecipeMeta.of(
                "minecraft:furnace",
                72,
                72,
                4,
                "minecraft:smelting",
                List.of(RecipeWidget.of(
                        0, 0, 18, 18, "catalyst", WidgetInteraction.item("minecraft:coal", null, null))));

        ItemIndexExporter.accumulateRecipeRefsFromMeta("minecraft:furnace", meta, inputs, outputs, fluids);

        assertTrue(inputs.containsKey("minecraft:coal"));
        assertFalse(outputs.containsKey("minecraft:coal"));
    }

    @Test
    void listAddsAllEntries() {
        Map<String, Map<String, Set<String>>> inputs = new TreeMap<>();
        Map<String, Map<String, Set<String>>> outputs = new TreeMap<>();
        Set<String> fluids = new TreeSet<>();

        RecipeMeta meta = RecipeMeta.of(
                "gtceu:assembler/mixed",
                126,
                72,
                4,
                "gtceu:assembler",
                List.of(RecipeWidget.of(
                        36,
                        27,
                        18,
                        18,
                        "input",
                        WidgetInteraction.list(
                                List.of(
                                        WidgetInteraction.item("gtceu:copper_ingot", 1, null),
                                        WidgetInteraction.item("gtceu:tin_ingot", 1, null)),
                                0))));

        ItemIndexExporter.accumulateRecipeRefsFromMeta("gtceu:assembler/mixed", meta, inputs, outputs, fluids);

        assertTrue(inputs.containsKey("gtceu:copper_ingot"));
        assertTrue(inputs.containsKey("gtceu:tin_ingot"));
    }

    @Test
    void fluidAddsToBucketAndFluidRegistryIds() {
        Map<String, Map<String, Set<String>>> inputs = new TreeMap<>();
        Map<String, Map<String, Set<String>>> outputs = new TreeMap<>();
        Set<String> fluids = new TreeSet<>();

        RecipeMeta meta = RecipeMeta.of(
                "gtceu:mixer/water",
                126,
                72,
                4,
                "gtceu:mixer",
                List.of(RecipeWidget.of(
                        18, 27, 18, 18, "input", WidgetInteraction.fluid("minecraft:water", 1000L))));

        ItemIndexExporter.accumulateRecipeRefsFromMeta("gtceu:mixer/water", meta, inputs, outputs, fluids);

        assertTrue(inputs.containsKey("minecraft:water"));
        assertEquals(Set.of("minecraft:water"), fluids);
    }

    @Test
    void skipsEmiTagDisplayRecipe() {
        Map<String, Map<String, Set<String>>> inputs = new TreeMap<>();
        Map<String, Map<String, Set<String>>> outputs = new TreeMap<>();
        Set<String> fluids = new TreeSet<>();

        RecipeMeta meta = RecipeMeta.of(
                "emi:/tag/item/minecraft:planks",
                1,
                1,
                4,
                "emi:anvil",
                List.of(RecipeWidget.of(
                        0, 0, 18, 18, "output", WidgetInteraction.item("minecraft:oak_planks", null, null))));

        ItemIndexExporter.accumulateRecipeRefsFromMeta(
                "emi:/tag/item/minecraft:planks", meta, inputs, outputs, fluids);

        assertTrue(inputs.isEmpty());
        assertTrue(outputs.isEmpty());
    }

    @Test
    void skipsWidgetWithoutRole() {
        Map<String, Map<String, Set<String>>> inputs = new TreeMap<>();
        Map<String, Map<String, Set<String>>> outputs = new TreeMap<>();
        Set<String> fluids = new TreeSet<>();

        RecipeMeta meta = RecipeMeta.of(
                "minecraft:crafting_table",
                72,
                72,
                4,
                "minecraft:crafting",
                List.of(RecipeWidget.of(
                        0, 0, 18, 18, null, WidgetInteraction.item("minecraft:stick", null, null))));

        ItemIndexExporter.accumulateRecipeRefsFromMeta("minecraft:crafting_table", meta, inputs, outputs, fluids);

        assertTrue(inputs.isEmpty());
        assertTrue(outputs.isEmpty());
    }

    @Test
    void mergesSeedItemIdsWithoutRecipeRefs() throws IOException {
        ExportContext context = ExportContext.builder(Mode.FULL)
                .itemIds(Set.of("minecraft:oak_log"))
                .build();

        ItemIndexExporter.accumulateRecipeRefsFromMeta(
                "minecraft:stick",
                RecipeMeta.of(
                        "minecraft:stick",
                        72,
                        72,
                        4,
                        "minecraft:crafting",
                        List.of(RecipeWidget.of(
                                0, 0, 18, 18, "output", WidgetInteraction.item("minecraft:stick", null, null)))),
                context.inputs(),
                context.outputs(),
                context.fluidRegistryIds());

        ExportWriteQueue.drain(writes -> ItemIndexExporter.export(tempDir, null, context, writes));

        Path oakFile = EmiPaths.resolve(tempDir, "items/minecraft/oak_log.json");
        Path itemsIndexFile = EmiPaths.resolve(tempDir, Constants.ITEMS_INDEX_FILE);

        assertTrue(Files.exists(oakFile));
        assertTrue(Files.readString(itemsIndexFile).contains("oak_log"));
        assertFalse(Files.readString(oakFile).contains("\"inputs\""));
        assertFalse(Files.readString(oakFile).contains("\"outputs\""));
    }
}
