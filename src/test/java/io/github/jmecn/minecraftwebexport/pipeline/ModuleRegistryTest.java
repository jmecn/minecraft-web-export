package io.github.jmecn.minecraftwebexport.pipeline;

import io.github.jmecn.minecraftwebexport.model.pipeline.Scope;
import io.github.jmecn.minecraftwebexport.model.pipeline.Seeds;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ModuleRegistryTest {

    @AfterEach
    void cleanup() {
        ModuleRegistry.clearForTests();
    }

    @Test
    void registerAndListModules() {
        ModuleRegistry.register(new FixedExportModule("demo", Seeds.builder().recipeId("demo:a").build()));
        assertEquals(1, ModuleRegistry.modules().size());
        assertEquals("demo", ModuleRegistry.modules().get(0).moduleId());
    }

    @Test
    void rejectsDuplicateModuleIds() {
        ModuleRegistry.register(new FixedExportModule("demo", Seeds.empty()));
        assertThrows(IllegalStateException.class, () ->
                ModuleRegistry.register(new FixedExportModule("demo", Seeds.empty())));
    }

    static final class FixedExportModule implements Module {
        private final String id;
        private final Seeds seeds;

        FixedExportModule(String id, Seeds seeds) {
            this.id = id;
            this.seeds = seeds;
        }

        @Override
        public String moduleId() {
            return id;
        }

        @Override
        public Seeds collectSeeds(Scope scope) {
            return seeds;
        }
    }
}
