package io.github.jmecn.minecraftwebexport;

import io.github.jmecn.minecraftwebexport.config.MweConfig;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(Constants.MOD_ID)
public final class MweMod {

    public static final String MOD_ID = Constants.MOD_ID;
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public MweMod() {
        MweConfig.register();
        LOGGER.info("Minecraft Web Export mod initialized");
    }
}
