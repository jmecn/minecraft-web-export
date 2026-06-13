package io.github.jmecn.minecraftwebexport.cmd;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.jmecn.minecraftwebexport.MweMod;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MweMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ExportClientCommands {

    private ExportClientCommands() {}

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("minecraftwebexport");
        root.then(Commands.literal("run").executes(ctx -> Export.run(ctx.getSource())));
        event.getDispatcher().register(root);
    }
}
