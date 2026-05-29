package io.github.jmecn.minecraftwebexport.export.module;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExportModuleRegistryTest {

    @AfterEach
    void cleanup() {
        ExportModuleRegistry.clearForTests();
    }

    @Test
    void registerAndListModules() {
        ExportModuleRegistry.register(new FixedExportModule("demo", ExportSeeds.builder().recipeId("demo:a").build()));
        assertEquals(1, ExportModuleRegistry.modules().size());
        assertEquals("demo", ExportModuleRegistry.modules().get(0).moduleId());
    }

    @Test
    void rejectsDuplicateModuleIds() {
        ExportModuleRegistry.register(new FixedExportModule("demo", ExportSeeds.empty()));
        assertThrows(IllegalStateException.class, () ->
                ExportModuleRegistry.register(new FixedExportModule("demo", ExportSeeds.empty())));
    }

    static final class FixedExportModule implements ExportModule {
        private final String id;
        private final ExportSeeds seeds;

        FixedExportModule(String id, ExportSeeds seeds) {
            this.id = id;
            this.seeds = seeds;
        }

        @Override
        public String moduleId() {
            return id;
        }

        @Override
        public ExportSeeds collectSeeds(ExportScope scope) {
            return seeds;
        }
    }
}
