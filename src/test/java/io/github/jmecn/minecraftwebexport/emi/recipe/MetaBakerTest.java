package io.github.jmecn.minecraftwebexport.emi.recipe;
import io.github.jmecn.minecraftwebexport.emi.recipe.CardPaths;
import io.github.jmecn.minecraftwebexport.emi.recipe.MetaBaker;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetaBakerTest {

    @Test
    void bakesTagAndListInteractions() {
        JsonObject layout = JsonParser.parseString("""
                {
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

        JsonObject meta = MetaBaker.bake(layout);
        assertEquals(1, meta.get("schema").getAsInt());
        assertEquals(126, meta.get("width").getAsInt());
        assertEquals(4, meta.get("margin").getAsInt());
        assertEquals(2, meta.getAsJsonArray("widgets").size());
        JsonObject tag = meta.getAsJsonArray("widgets").get(0).getAsJsonObject();
        assertEquals("tag", tag.getAsJsonObject("interaction").get("kind").getAsString());
        assertEquals("c:iron_plates", tag.getAsJsonObject("interaction").get("tag").getAsString());
        JsonObject list = meta.getAsJsonArray("widgets").get(1).getAsJsonObject();
        assertEquals("list", list.getAsJsonObject("interaction").get("kind").getAsString());
        assertEquals(2, list.getAsJsonObject("interaction").getAsJsonArray("entries").size());
    }

    @Test
    void pathSafeEncodesSlashes() {
        assertEquals("assembler_iron_plate", CardPaths.pathSafe("assembler/iron_plate"));
        assertTrue(CardPaths.pathSafe("/emi/tag").startsWith("_"));
    }
}
