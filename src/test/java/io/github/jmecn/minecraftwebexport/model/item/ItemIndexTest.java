package io.github.jmecn.minecraftwebexport.model.item;

import io.github.jmecn.minecraftwebexport.io.JsonIO;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ItemIndexTest {

    @Test
    void roundTripsJson() {
        Map<String, List<String>> namespaces = new LinkedHashMap<>();
        namespaces.put("minecraft", List.of("iron_ingot"));
        namespaces.put("gtceu", List.of("assembler/iron_plate"));
        ItemIndex original = new ItemIndex(namespaces, List.of("gtceu:oxygen"));

        ItemIndex restored = JsonIO.fromJson(JsonIO.toJson(original), ItemIndex.class);

        assertEquals(original.namespacePaths(), restored.namespacePaths());
        assertEquals(original.fluidRegistryIds(), restored.fluidRegistryIds());
    }
}
