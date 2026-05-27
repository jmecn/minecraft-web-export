package io.github.jmecn.minecraftwebexport.export.emi;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IconAtlasLayoutTest {

    @Test
    void plansCompactSquareishPage() {
        List<IconAtlasLayout.PagePlan> plans = IconAtlasLayout.plan(797, 16, 2048);

        assertEquals(1, plans.size());
        assertEquals(29, plans.get(0).cols());
        assertEquals(28, plans.get(0).rows());
    }
}
