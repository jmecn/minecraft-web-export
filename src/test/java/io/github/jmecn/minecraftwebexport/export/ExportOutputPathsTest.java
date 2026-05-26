package io.github.jmecn.minecraftwebexport.export;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExportOutputPathsTest {

    @Test
    void resolvesDefaultRootUnderGameDirectory() {
        ExportOutputPaths paths = ExportOutputPaths.resolve(Path.of("/tmp/game"), null);

        assertEquals(Path.of("/tmp/game/export/minecraft-web-export"), paths.rootDir());
        assertEquals(Path.of("/tmp/game/export/minecraft-web-export/manifest.json"), paths.manifestFile());
        assertEquals(Path.of("/tmp/game/export/minecraft-web-export/bundle.json"), paths.bundleFile());
    }

    @Test
    void usesExplicitOutputRootWhenProvided() {
        ExportOutputPaths paths = ExportOutputPaths.resolve(
                Path.of("/tmp/game"),
                "/tmp/custom-export");

        assertEquals(Path.of("/tmp/custom-export"), paths.rootDir());
        assertEquals(Path.of("/tmp/custom-export/manifest.json"), paths.manifestFile());
        assertEquals(Path.of("/tmp/custom-export/bundle.json"), paths.bundleFile());
    }
}
