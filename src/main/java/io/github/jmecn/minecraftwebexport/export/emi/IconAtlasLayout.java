package io.github.jmecn.minecraftwebexport.export.emi;

import java.util.ArrayList;
import java.util.List;

public final class IconAtlasLayout {

    private IconAtlasLayout() {
    }

    public record PagePlan(int cols, int rows) {
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

    public static List<PagePlan> plan(int spriteCount, int cellSize, int maxAtlasSize) {
        if (spriteCount <= 0) {
            return List.of(new PagePlan(1, 1));
        }
        int maxCols = Math.max(1, maxAtlasSize / cellSize);
        int maxRows = Math.max(1, maxAtlasSize / cellSize);
        int maxPerPage = maxCols * maxRows;

        List<PagePlan> pages = new ArrayList<>();
        int remaining = spriteCount;
        while (remaining > 0) {
            int onPage = Math.min(remaining, maxPerPage);
            int cols = (int) Math.ceil(Math.sqrt(onPage));
            int rows = (int) Math.ceil((double) onPage / cols);
            if (cols > maxCols) {
                cols = maxCols;
                rows = (int) Math.ceil((double) onPage / cols);
            }
            if (rows > maxRows) {
                rows = maxRows;
                cols = (int) Math.ceil((double) onPage / rows);
                cols = Math.min(cols, maxCols);
            }
            pages.add(new PagePlan(cols, rows));
            remaining -= cols * rows;
        }
        return pages;
    }
}
