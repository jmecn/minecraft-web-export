package io.github.jmecn.minecraftwebexport.runtime;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OutputPathsTest {

    @Test
    void resolvesDefaultRootUnderGameDirectory() {
        OutputPaths paths = OutputPaths.resolve(Path.of("/tmp/game"), null);
        assertEquals(Path.of("/tmp/game/export/minecraft-web-export"), paths.rootDir());
    }

    @Test
    void usesExplicitOutputRootWhenProvided() {
        OutputPaths paths = OutputPaths.resolve(Path.of("/tmp/game"), "/tmp/custom-export");
        assertEquals(Path.of("/tmp/custom-export"), paths.rootDir());
    }
}
