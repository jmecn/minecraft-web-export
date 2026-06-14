package io.github.jmecn.minecraftwebexport.config;

import io.github.jmecn.minecraftwebexport.Constants;
import io.github.jmecn.minecraftwebexport.runtime.CiDriver;
import io.github.jmecn.minecraftwebexport.config.MweConfig;
import java.nio.file.Path;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@Mod.EventBusSubscriber(modid = Constants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class MweClientBootstrap {

    private static boolean armed;

    private MweClientBootstrap() {}

    @SubscribeEvent
    public static void onClientConfigLoad(ModConfigEvent.Loading event) {
        if (!Constants.MOD_ID.equals(event.getConfig().getModId())) {
            return;
        }
        if (event.getConfig().getType() != ModConfig.Type.CLIENT) {
            return;
        }
        tryArm(FMLPaths.GAMEDIR.get());
    }

    static void tryArm(Path gameDirectory) {
        if (armed) {
            return;
        }
        if (!MweConfig.exportEnabled()) {
            return;
        }
        armed = true;
        new CiDriver(gameDirectory, MweConfig.exportOutputDir()).register();
    }

    static void clearForTests() {
        armed = false;
    }
}
