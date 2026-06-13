package io.github.jmecn.minecraftwebexport.export.emi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class OffScreenRendererPoolTest {

    @Test
    void dimKeyDistinguishesWidthAndHeight() {
        assertNotEquals(OffScreenRendererPool.dimKey(100, 200), OffScreenRendererPool.dimKey(200, 100));
        assertEquals(OffScreenRendererPool.dimKey(176, 64), OffScreenRendererPool.dimKey(176, 64));
    }
}
