package io.github.jmecn.minecraftwebexport;

import io.github.jmecn.minecraftwebexport.runtime.CiDriver;
import io.github.jmecn.minecraftwebexport.runtime.CiProperties;
import io.github.jmecn.minecraftwebexport.runtime.ClientEntrypoint;
import java.nio.file.Path;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(Constants.MOD_ID)
public final class MweMod {

    public static final String MOD_ID = Constants.MOD_ID;
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public MweMod() {
        LOGGER.info("Minecraft Web Export mod initialized");
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientBootstrap.arm(FMLPaths.GAMEDIR.get());
        }
    }

    private static final class ClientBootstrap {
        private static void arm(Path gameDirectory) {
            if (CiProperties.runExportAndExit()) {
                new CiDriver(
                        gameDirectory,
                        System.getProperty(Constants.PROP_EXPORT_OUTPUT_DIR))
                        .register();
                return;
            }
            ClientEntrypoint entrypoint = new ClientEntrypoint();
            entrypoint.armIfEnabled(gameDirectory);
        }
    }
}
