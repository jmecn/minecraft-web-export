package io.github.jmecn.minecraftwebexport.model.emi.icon;


public record AtlasPagePlan(int cols, int rows) {

    public int widthPx(int cellSize) {
        return cols * cellSize;
    }

    public int heightPx(int cellSize) {
        return rows * cellSize;
    }

    public int capacity() {
        return cols * rows;
    }
}
