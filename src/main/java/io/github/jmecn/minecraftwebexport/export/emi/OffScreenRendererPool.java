package io.github.jmecn.minecraftwebexport.export.emi;

import java.util.HashMap;
import java.util.Map;

final class OffScreenRendererPool implements AutoCloseable {

    private final Map<Long, OffScreenRenderer> renderers = new HashMap<>();

    OffScreenRenderer borrow(int width, int height) {
        if (width < 1 || height < 1) {
            throw new IllegalArgumentException("width and height must be positive");
        }
        return renderers.computeIfAbsent(dimKey(width, height), ignored -> new OffScreenRenderer(width, height));
    }

    int pooledCount() {
        return renderers.size();
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
