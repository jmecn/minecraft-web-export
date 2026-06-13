package io.github.jmecn.minecraftwebexport.runtime;

import io.github.jmecn.minecraftwebexport.MweMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MweMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WorldServerEvents {

    public static final BlockPos BEDROCK_POS = new BlockPos(0, 64, 0);
    public static final BlockPos PLAYER_SPAWN_POS = new BlockPos(0, 65, 0);

    private WorldServerEvents() {}

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        if (!CiProperties.exportEnabled()) {
            return;
        }
        ServerLevel overworld = event.getServer().overworld();
        if (overworld == null) {
            MweMod.LOGGER.warn("ServerStartedEvent: overworld is null, skipping bedrock setup");
            return;
        }

        overworld.setBlock(BEDROCK_POS, Blocks.BEDROCK.defaultBlockState(), 3);
        overworld.setDefaultSpawnPos(PLAYER_SPAWN_POS, 0f);

        MweMod.LOGGER.info("placed bedrock at {} and set spawn at {}", BEDROCK_POS, PLAYER_SPAWN_POS);
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!CiProperties.exportEnabled()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ServerLevel overworld = player.server.overworld();
        if (overworld == null) {
            MweMod.LOGGER.warn("PlayerLoggedInEvent: overworld is null, skipping teleport");
            return;
        }

        BlockPos spawn = PLAYER_SPAWN_POS;
        player.teleportTo(overworld, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, 0f, 0f);
        player.getAbilities().mayfly = true;
        player.getAbilities().flying = true;
        player.getAbilities().invulnerable = true;
        player.onUpdateAbilities();

        MweMod.LOGGER.info("teleported {} to {} with creative flight", player.getName().getString(), spawn);
    }
}
