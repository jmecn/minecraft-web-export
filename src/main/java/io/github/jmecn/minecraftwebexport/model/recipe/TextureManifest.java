package io.github.jmecn.minecraftwebexport.model.recipe;

import java.util.Map;

public record TextureManifest(int schema, Map<String, String> textures) {

    public TextureManifest {
        textures = Map.copyOf(textures == null ? Map.of() : textures);
    }

    public static TextureManifest of(Map<String, String> textures) {
        return new TextureManifest(1, textures);
    }
}
