package io.github.jmecn.minecraftwebexport.pipeline.strategy;

import io.github.jmecn.minecraftwebexport.model.pipeline.ExportContext;
import io.github.jmecn.minecraftwebexport.model.pipeline.Seeds;
import net.minecraft.client.Minecraft;

public interface ExportStrategy {

    ExportContext planScope(Minecraft client, Seeds seeds);
}
