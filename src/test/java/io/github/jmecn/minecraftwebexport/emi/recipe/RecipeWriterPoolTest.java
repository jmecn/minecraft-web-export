package io.github.jmecn.minecraftwebexport.emi.recipe;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class RecipeWriterPoolTest {

    @Test
    void dimKeyDistinguishesWidthAndHeight() {
        assertNotEquals(RecipeWriter.dimKey(100, 200), RecipeWriter.dimKey(200, 100));
        assertEquals(RecipeWriter.dimKey(176, 64), RecipeWriter.dimKey(176, 64));
    }
}
