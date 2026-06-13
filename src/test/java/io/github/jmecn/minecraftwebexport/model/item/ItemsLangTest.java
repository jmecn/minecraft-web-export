package io.github.jmecn.minecraftwebexport.model.item;

import io.github.jmecn.minecraftwebexport.io.JsonIO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ItemsLangTest {

    @Test
    void roundTripsJson() {
        ItemsLang original = ItemsLang.of(
                "en_us",
                List.of(
                        new ItemsLangEntry("minecraft:iron_ingot", "Iron Ingot", "minecraft:iron_ingot iron ingot"),
                        new ItemsLangEntry("gtceu:oxygen", "Oxygen", "gtceu:oxygen oxygen")));

        ItemsLang restored = JsonIO.fromJson(JsonIO.toJson(original), ItemsLang.class);

        assertEquals(original.schema(), restored.schema());
        assertEquals(original.locale(), restored.locale());
        assertEquals(original.itemCount(), restored.itemCount());
        assertEquals(original.items(), restored.items());
    }
}
