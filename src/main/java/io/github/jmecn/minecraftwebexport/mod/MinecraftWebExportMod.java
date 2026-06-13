package io.github.jmecn.minecraftwebexport.mod;

import io.github.jmecn.minecraftwebexport.export.RuntimeExportEntrypoint;
import io.github.jmecn.minecraftwebexport.export.ci.ExportCiDriver;
import io.github.jmecn.minecraftwebexport.export.ci.ExportCiProperties;
import net.minecraft.SharedConstants;
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
    private static final RuntimeExportEntrypoint EXPORT_ENTRYPOINT = new RuntimeExportEntrypoint();

    public MinecraftWebExportMod() {
        LOGGER.info("Minecraft Web Export mod initialized");
        boolean ciExport = ExportCiProperties.runExportAndExit();
        if (!ciExport) {
            EXPORT_ENTRYPOINT.runIfEnabled(
                    MOD_ID,
                    SharedConstants.getCurrentVersion().getName(),
                    FMLPaths.GAMEDIR.get());
        } else {
            LOGGER.info("skipping mod-init stub export; CI driver owns the export lifecycle");
        }
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientBootstrap.arm(FMLPaths.GAMEDIR.get());
        }
    }

    private static final class ClientBootstrap {
        private static void arm(java.nio.file.Path gameDirectory) {
            if (ExportCiProperties.runExportAndExit()) {
                new ExportCiDriver(
                        gameDirectory,
                        System.getProperty(RuntimeExportEntrypoint.OUTPUT_ROOT_PROPERTY))
                        .register();
                return;
            }
            io.github.jmecn.minecraftwebexport.export.emi.RuntimeEmiExportEntrypoint entrypoint =
                    new io.github.jmecn.minecraftwebexport.export.emi.RuntimeEmiExportEntrypoint();
            entrypoint.armIfEnabled(gameDirectory);
        }
    }
}
