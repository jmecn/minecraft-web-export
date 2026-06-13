package io.github.jmecn.minecraftwebexport.pipeline;
import io.github.jmecn.minecraftwebexport.pipeline.Hints;
import io.github.jmecn.minecraftwebexport.pipeline.Result;
import io.github.jmecn.minecraftwebexport.pipeline.Scope;
import io.github.jmecn.minecraftwebexport.pipeline.Seeds;

import net.minecraft.client.Minecraft;

import java.io.IOException;

public interface Module {

    String moduleId();

    default void beforeEmiExport(Scope scope, Minecraft client) throws IOException {
    }

    Seeds collectSeeds(Scope scope);

    default Hints buildHints(Scope scope, Seeds mergedSeeds) {
        return Hints.defaults();
    }

    default void exportExtras(Scope scope, Result result) throws IOException {
    }
}
