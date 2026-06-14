package io.github.jmecn.minecraftwebexport.emi.icon;

import java.util.HashMap;
import java.util.Map;

public final class OffScreenRendererPool implements AutoCloseable {

    private final Map<Long, OffScreenRenderer> renderers = new HashMap<>();

    public OffScreenRenderer borrow(int width, int height) {
        if (width < 1 || height < 1) {
            throw new IllegalArgumentException("width and height must be positive");
        }
        return renderers.computeIfAbsent(dimKey(width, height), ignored -> new OffScreenRenderer(width, height));
    }

    @Override
    public void close() {
        for (OffScreenRenderer renderer : renderers.values()) {
            renderer.close();
        }
        renderers.clear();
    }

    static long dimKey(int width, int height) {
        return ((long) width << 32) | (height & 0xFFFFFFFFL);
    }
}
