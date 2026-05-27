package io.github.jmecn.minecraftwebexport.export.emi;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EmiWidgetSerializerTest {

    @Test
    void collectsStructuredTagRefsFromSerializedIngredient() {
        var tags = new TreeSet<String>();
        var ingredient = JsonParser.parseString("""
                {
                  "entries": [
                    { "tag": "forge:ingots/iron" },
                    { "entries": [ { "tag": "minecraft:planks" } ] }
                  ]
                }
                """);

        EmiWidgetSerializer.collectSerializedTagRefs(ingredient, tags);

        assertEquals(Set.of("forge:ingots/iron", "minecraft:planks"), tags);
    }
}
