package io.github.jmecn.minecraftwebexport.config;

import io.github.jmecn.minecraftwebexport.emi.lang.Languages;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MweJvmCompatTest {

    @AfterEach
    void clearState() {
        System.clearProperty(MweConfig.JVM_EXPORT_ENABLED);
        System.clearProperty(MweConfig.JVM_EXPORT_OUTPUT_DIR);
        System.clearProperty(MweConfig.JVM_EXPORT_MODE);
        System.clearProperty(MweConfig.JVM_EXPORT_LANGUAGES);
        MweConfig.clearForTests();
    }

    @Test
    void exportEnabledJvmFlagArmsCiMode() {
        MweConfig.ensureForTests();
        System.setProperty(MweConfig.JVM_EXPORT_ENABLED, "true");
        System.setProperty(MweConfig.JVM_EXPORT_OUTPUT_DIR, "/tmp/mwe-export");
        System.setProperty(MweConfig.JVM_EXPORT_MODE, "scoped");
        System.setProperty(MweConfig.JVM_EXPORT_LANGUAGES, "en_us,zh_cn");

        assertTrue(MweConfig.exportEnabled());
        assertEquals("/tmp/mwe-export", MweConfig.exportOutputDir());
        assertEquals("scoped", MweConfig.exportMode());
        assertEquals(Set.of("en_us", "zh_cn"), Languages.resolve());
    }

    @Test
    void exportEnabledFalseMeansCommandOnly() {
        MweConfig.ensureForTests();
        assertFalse(MweConfig.exportEnabled());
    }
}
