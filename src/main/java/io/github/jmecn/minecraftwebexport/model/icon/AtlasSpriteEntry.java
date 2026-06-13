package io.github.jmecn.minecraftwebexport.model.icon;


public record AtlasSpriteEntry(int page, int x, int y, Integer usage) {

    public AtlasSpriteEntry(int page, int x, int y) {
        this(page, x, y, null);
    }
}
