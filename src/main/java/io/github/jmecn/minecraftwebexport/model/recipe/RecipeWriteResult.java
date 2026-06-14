package io.github.jmecn.minecraftwebexport.model.recipe;

import java.util.Set;

public record RecipeWriteResult(
        int requested,
        int written,
        int missing,
        int failures,
        long pngBytes,
        long metaBytes,
        int imageScale) {
}
