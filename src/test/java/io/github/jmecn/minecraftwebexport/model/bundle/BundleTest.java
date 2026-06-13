package io.github.jmecn.minecraftwebexport.model.bundle;

import io.github.jmecn.minecraftwebexport.Constants;
import io.github.jmecn.minecraftwebexport.model.Json;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BundleTest {

    @Test
    void serializesSchema2Fields() {
        Bundle document = Bundle.of(
                2,
                100,
                List.of("en_us", "zh_cn"),
                "minecraft_web_export:missing_icon",
                new ItemsLangRef("items-lang", List.of("en_us")));

        String json = Json.GSON.toJson(document);

        assertTrue(json.contains("\"schema\":" + Constants.BUNDLE_SCHEMA));
        assertTrue(json.contains("\"imageScale\":2"));
        assertTrue(json.contains("\"itemsLang\""));
    }
}
