package io.github.jmecn.minecraftwebexport.emi.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.jmecn.minecraftwebexport.model.recipe.RecipeMeta;
import io.github.jmecn.minecraftwebexport.model.recipe.RecipeWidget;
import io.github.jmecn.minecraftwebexport.model.recipe.WidgetInteraction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetaBakerTest {

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

        RecipeMeta meta = MetaBaker.bake(layout);
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
        assertEquals("assembler_iron_plate", RecipePaths.pathSafe("assembler/iron_plate"));
        assertTrue(RecipePaths.pathSafe("/emi/tag").startsWith("_"));
    }
}
