package io.github.jmecn.minecraftwebexport.emi.icon;
import io.github.jmecn.minecraftwebexport.emi.icon.AtlasLayout;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AtlasLayoutTest {

    @Test
    void plansCompactSquareishPage() {
        List<AtlasLayout.PagePlan> plans = AtlasLayout.plan(797, 16, 2048);

        assertEquals(1, plans.size());
        assertEquals(29, plans.get(0).cols());
        assertEquals(28, plans.get(0).rows());
    }
}
