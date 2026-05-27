package io.github.jmecn.minecraftwebexport.mod;

import io.github.jmecn.minecraftwebexport.export.RuntimeExportEntrypoint;
import net.minecraft.SharedConstants;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Minimal Forge entrypoint for the extracted web export runtime.
 *
 * <p>This scaffold intentionally mirrors the structure of {@code Field-Guide-Modern/forge}
 * while keeping the first cut small enough to evolve into a standalone export mod.</p>
 */
@Mod(MinecraftWebExportMod.MOD_ID)
public final class MinecraftWebExportMod {

    public static final String MOD_ID = "minecraft_web_export";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    private static final RuntimeExportEntrypoint EXPORT_ENTRYPOINT = new RuntimeExportEntrypoint();

    public MinecraftWebExportMod() {
        LOGGER.info("Minecraft Web Export mod initialized");
        EXPORT_ENTRYPOINT.runIfEnabled(
                MOD_ID,
                SharedConstants.getCurrentVersion().getName(),
                FMLPaths.GAMEDIR.get(),
                LOGGER);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientBootstrap.arm(FMLPaths.GAMEDIR.get(), LOGGER);
        }
    }

    private static final class ClientBootstrap {
        private static void arm(java.nio.file.Path gameDirectory, Logger logger) {
            io.github.jmecn.minecraftwebexport.export.emi.RuntimeEmiExportEntrypoint entrypoint =
                    new io.github.jmecn.minecraftwebexport.export.emi.RuntimeEmiExportEntrypoint();
            entrypoint.armIfEnabled(gameDirectory, logger);
        }
    }
}
