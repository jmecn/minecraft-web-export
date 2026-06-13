package io.github.jmecn.minecraftwebexport.emi.icon;

import io.github.jmecn.minecraftwebexport.model.emi.icon.AtlasPagePlan;
import java.util.ArrayList;
import java.util.List;

public final class AtlasLayout {

    private AtlasLayout() {
    }

    public static List<AtlasPagePlan> plan(int spriteCount, int cellSize, int maxAtlasSize) {
        if (spriteCount <= 0) {
            return List.of(new AtlasPagePlan(1, 1));
        }
        int maxCols = Math.max(1, maxAtlasSize / cellSize);
        int maxRows = Math.max(1, maxAtlasSize / cellSize);
        int maxPerPage = maxCols * maxRows;

        List<AtlasPagePlan> pages = new ArrayList<>();
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
            pages.add(new AtlasPagePlan(cols, rows));
            remaining -= cols * rows;
        }
        return pages;
    }
}
