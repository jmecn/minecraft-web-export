package io.github.jmecn.minecraftwebexport.model.bundle;

import io.github.jmecn.minecraftwebexport.Constants;
import java.util.List;
import java.util.Objects;

public record Bundle(
        int schema,
        int imageScale,
        String recipeImageFormat,
        int recipeCount,
        List<String> languages,
        String missingIconId,
        ItemsLangRef itemsLang) {

    public Bundle {
        schema = schema;
        recipeImageFormat = recipeImageFormat == null ? "png" : recipeImageFormat;
        languages = List.copyOf(languages == null ? List.of() : languages);
        missingIconId = Objects.requireNonNull(missingIconId, "missingIconId");
    }

    public static Bundle of(
            int imageScale,
            int recipeCount,
            List<String> languages,
            String missingIconId,
            ItemsLangRef itemsLang) {
        return new Bundle(
                Constants.BUNDLE_SCHEMA,
                imageScale,
                "png",
                recipeCount,
                languages,
                missingIconId,
                itemsLang);
    }
}
