package io.github.jmecn.minecraftwebexport.emi.bundle;

import io.github.jmecn.minecraftwebexport.Constants;
import io.github.jmecn.minecraftwebexport.emi.icon.PlaceholderRenderer;
import io.github.jmecn.minecraftwebexport.io.JsonIO;
import io.github.jmecn.minecraftwebexport.model.bundle.Bundle;
import io.github.jmecn.minecraftwebexport.model.bundle.ItemsLangRef;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class ManifestWriter {

    private ManifestWriter() {
    }

    public static void write(
            Path outputDir,
            List<String> languages,
            int imageScale,
            int recipeCount) throws IOException {
        write(outputDir, languages, imageScale, recipeCount, null);
    }

    public static void write(
            Path outputDir,
            List<String> languages,
            int imageScale,
            int recipeCount,
            List<String> itemsLangLocales) throws IOException {
        ItemsLangRef itemsLang = itemsLangLocales == null || itemsLangLocales.isEmpty()
                ? null
                : new ItemsLangRef(Constants.ITEMS_LANG_DIR, List.copyOf(itemsLangLocales));
        Bundle document = Bundle.of(
                imageScale,
                recipeCount,
                languages,
                PlaceholderRenderer.REGISTRY_ID,
                itemsLang);
        JsonIO.write(Paths.resolve(outputDir, Constants.BUNDLE_FILE), document);
    }
}
