package io.github.jmecn.minecraftwebexport.mod;
import io.github.jmecn.minecraftwebexport.runtime.CiDriver;
import io.github.jmecn.minecraftwebexport.runtime.CiProperties;
import io.github.jmecn.minecraftwebexport.runtime.ClientEntrypoint;
import io.github.jmecn.minecraftwebexport.runtime.ExportProperties;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(MinecraftWebExportMod.MOD_ID)
public final class MinecraftWebExportMod {

    public static final String MOD_ID = "minecraft_web_export";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public MinecraftWebExportMod() {
        LOGGER.info("Minecraft Web Export mod initialized");
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientBootstrap.arm(FMLPaths.GAMEDIR.get());
        }
    }

    private static final class ClientBootstrap {
        private static void arm(java.nio.file.Path gameDirectory) {
            if (CiProperties.runExportAndExit()) {
                new CiDriver(
                        gameDirectory,
                        System.getProperty(ExportProperties.OUTPUT_ROOT_PROPERTY))
                        .register();
                return;
            }
            ClientEntrypoint entrypoint = new ClientEntrypoint();
            entrypoint.armIfEnabled(gameDirectory);
        }
    }
}
