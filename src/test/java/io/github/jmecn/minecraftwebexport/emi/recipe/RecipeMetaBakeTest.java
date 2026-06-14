package io.github.jmecn.minecraftwebexport.emi.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.jmecn.minecraftwebexport.model.recipe.RecipeMeta;
import io.github.jmecn.minecraftwebexport.model.recipe.RecipeWidget;
import io.github.jmecn.minecraftwebexport.model.recipe.WidgetInteraction;
import io.github.jmecn.minecraftwebexport.model.recipe.RecipeIdParts;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipeMetaBakeTest {

    @Test
    void listMapsFluidTagPrimitiveToTagInteraction() {
        JsonObject layout = JsonParser.parseString("""
                {
                  "id": "tfg:ae_transform/water_test",
                  "panel": { "width": 150, "height": 72 },
                  "category": "ae2:item_transformation",
                  "widgets": [
                    {
                      "type": "slot",
                      "role": "input",
                      "x": 64,
                      "y": 27,
                      "w": 18,
                      "h": 18,
                      "ingredient": [
                        "fluid:tfg:muddy_water",
                        "#fluid:tfc:water"
                      ]
                    }
                  ]
                }
                """).getAsJsonObject();

        RecipeMeta meta = bakeLayout(layout);
        WidgetInteraction list = meta.widgets().get(0).interaction();

        assertEquals("list", list.kind());
        assertEquals("fluid", list.entries().get(0).kind());
        assertEquals("tfg:muddy_water", list.entries().get(0).id());
        assertEquals("tag", list.entries().get(1).kind());
        assertEquals("tfc:water", list.entries().get(1).tag());
        assertEquals("fluid", list.entries().get(1).tagKind());
    }

    @Test
    void bakesTagAndListInteractions() {
        JsonObject layout = JsonParser.parseString("""
                {
                  "id": "gtceu:assembler/iron_plate",
                  "panel": { "width": 126, "height": 72 },
                  "category": "gtceu:assembler",
                  "widgets": [
                    {
                      "type": "slot",
                      "role": "input",
                      "x": 18,
                      "y": 27,
                      "w": 18,
                      "h": 18,
                      "ingredient": "#item:c:iron_plates",
                      "tagDisplayItem": "gtceu:iron_plate"
                    },
                    {
                      "type": "slot",
                      "x": 36,
                      "y": 27,
                      "w": 18,
                      "h": 18,
                      "ingredient": [
                        { "type": "item", "id": "gtceu:copper_ingot", "amount": 1 },
                        { "type": "item", "id": "gtceu:tin_ingot", "amount": 1 }
                      ]
                    }
                  ]
                }
                """).getAsJsonObject();

        RecipeMeta meta = bakeLayout(layout);

        assertEquals(1, meta.schema());
        assertEquals("gtceu:assembler/iron_plate", meta.id());
        assertEquals(126, meta.width());
        assertEquals(4, meta.margin());
        assertEquals(2, meta.widgets().size());

        RecipeWidget tagWidget = meta.widgets().get(0);
        WidgetInteraction tag = tagWidget.interaction();
        assertEquals("tag", tag.kind());
        assertEquals("c:iron_plates", tag.tag());
        assertEquals("gtceu:iron_plate", tag.displayId());

        RecipeWidget listWidget = meta.widgets().get(1);
        WidgetInteraction list = listWidget.interaction();
        assertEquals("list", list.kind());
        assertEquals(2, list.entries().size());
    }

    @Test
    void pathSafeEncodesSlashes() {
        assertEquals("assembler_iron_plate", new RecipeIdParts("gtceu", "assembler/iron_plate").pathSafe());
        assertTrue(new RecipeIdParts("emi", "/emi/tag").pathSafe().startsWith("_"));
    }

    private static RecipeMeta bakeLayout(JsonObject layout) {
        JsonObject panel = layout.getAsJsonObject("panel");
        int width = panel != null && panel.has("width") ? panel.get("width").getAsInt() : 1;
        int height = panel != null && panel.has("height") ? panel.get("height").getAsInt() : 1;
        int margin = panel != null && panel.has("margin")
                ? panel.get("margin").getAsInt()
                : LayoutBuilder.panelMargin();
        String id = stringProp(layout, "id");
        String category = stringProp(layout, "category");

        List<RecipeWidget> widgets = new ArrayList<>();
        JsonElement layoutWidgets = layout.get("widgets");
        if (layoutWidgets != null && layoutWidgets.isJsonArray()) {
            for (JsonElement element : layoutWidgets.getAsJsonArray()) {
                if (!element.isJsonObject()) {
                    continue;
                }
                RecipeWidget baked = RecipeInteractionMapper.widgetFromSlotJson(element.getAsJsonObject());
                if (baked != null) {
                    widgets.add(baked);
                }
            }
        }
        return RecipeMeta.of(id, width, height, margin, category, widgets);
    }

    private static String stringProp(JsonObject object, String key) {
        if (object.has(key) && object.get(key).isJsonPrimitive()) {
            return object.get(key).getAsString();
        }
        return null;
    }
}
