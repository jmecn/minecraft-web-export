package io.github.jmecn.minecraftwebexport.runtime;

import io.github.jmecn.minecraftwebexport.Constants;
import io.github.jmecn.minecraftwebexport.MweMod;
import io.github.jmecn.minecraftwebexport.emi.pipeline.Readiness;
import io.github.jmecn.minecraftwebexport.model.pipeline.ExportResult;
import io.github.jmecn.minecraftwebexport.pipeline.Pipeline;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class CiDriver {

    private final Path gameDirectory;
    private final String outputRootOverride;

    public CiDriver(Path gameDirectory, String outputRootOverride) {
        this.gameDirectory = gameDirectory;
        this.outputRootOverride = outputRootOverride;
    }

    public void register() {
        MweMod.LOGGER.info(
                "mode=runExportAndExit, world={}, output={}, warmupTicks={}, timeoutSeconds={}",
                WorldCreator.saveName(),
                OutputPaths.resolveForRun(gameDirectory, outputRootOverride).rootDir(),
                CiProperties.exportWarmupTicks(),
                CiProperties.exportTimeoutSeconds());
        MinecraftForge.EVENT_BUS.register(new AutoExportHandler(gameDirectory, outputRootOverride));
    }

    static boolean isFatalMenuScreen(Minecraft client) {
        if (client.screen == null) {
            return false;
        }
        String simple = client.screen.getClass().getSimpleName();
        return switch (simple) {
            case "LoadingErrorScreen", "ErrorScreen", "KubeJSErrorScreen", "DisconnectedScreen" -> true;
            default -> false;
        };
    }

    static boolean isIdleMenuReady(Minecraft client) {
        if (client.getOverlay() instanceof LoadingOverlay) {
            return false;
        }
        if (client.screen == null || client.level != null || client.player != null) {
            return false;
        }
        return !isFatalMenuScreen(client);
    }

    static boolean isEmiReady(Minecraft client) {
        return Readiness.isReadyForExport(client);
    }

    private static final class StateLogger {
        private String state = "";
        private int sameStateTicks;

        void tick(String newState) {
            if (!newState.equals(state)) {
                state = newState;
                sameStateTicks = 0;
                MweMod.LOGGER.info(newState);
            } else if (++sameStateTicks % Constants.HEARTBEAT_TICKS == 0) {
                MweMod.LOGGER.info("{} (still waiting, {} ticks)", newState, sameStateTicks);
            }
        }
    }

    private static final class AutoExportHandler {

        private enum Phase {ARMED, WORLD_OPENING, WARMUP, DONE}

        private final Path gameDirectory;
        private final String outputRootOverride;
        private final StateLogger stateLog;

        private Phase phase = Phase.ARMED;
        private boolean worldRequestSent;
        private int worldDelayTicks;
        private int warmupTicks;
        private long startNanos;

        AutoExportHandler(Path gameDirectory, String outputRootOverride) {
            this.gameDirectory = gameDirectory;
            this.outputRootOverride = outputRootOverride;
            this.stateLog = new StateLogger();
        }

        @SubscribeEvent
        public void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END || phase == Phase.DONE) {
                return;
            }
            if (!Boolean.getBoolean(Constants.PROP_EXPORT_ENABLED)) {
                return;
            }

            Minecraft client = Minecraft.getInstance();

            if (phase == Phase.ARMED) {
                startNanos = System.nanoTime();
                phase = Phase.WORLD_OPENING;
                MweMod.LOGGER.info(
                        "runExportAndExit: armed (timeout={}s), waiting for idle menu...",
                        CiProperties.exportTimeoutSeconds());
            }

            if (isFatalMenuScreen(client)) {
                phase = Phase.DONE;
                MweMod.LOGGER.error(
                        "fatal menu screen ({}); aborting export",
                        client.screen.getClass().getName());
                System.exit(1);
                return;
            }
            if (CiProperties.timedOut(startNanos)) {
                String phaseAtTimeout = phase.name();
                phase = Phase.DONE;
                String screen = client.screen == null ? "null" : client.screen.getClass().getName();
                MweMod.LOGGER.error(
                        "export timed out after {}s (phase={}, player={}, level={}, screen={})",
                        CiProperties.exportTimeoutSeconds(),
                        phaseAtTimeout,
                        client.player != null,
                        client.level != null,
                        screen);
                System.exit(1);
                return;
            }

            switch (phase) {
                case WORLD_OPENING -> tickWorldOpening(client);
                case WARMUP -> tickWarmup(client);
                default -> {}
            }
        }

        private void tickWorldOpening(Minecraft client) {
            if (client.player != null && client.level != null) {
                phase = Phase.WARMUP;
                warmupTicks = 0;
                MweMod.LOGGER.info(
                        "player + level present (player={}, dim={}), warming up {} ticks",
                        client.player.getName().getString(),
                        client.level.dimension().location(),
                        CiProperties.exportWarmupTicks());
                return;
            }

            if (!worldRequestSent) {
                if (!isIdleMenuReady(client)) {
                    worldDelayTicks = 0;
                    if (client.getOverlay() instanceof LoadingOverlay) {
                        stateLog.tick("waiting: resource reload (LoadingOverlay)");
                    } else if (client.screen == null) {
                        stateLog.tick("waiting: no screen yet");
                    } else {
                        stateLog.tick("waiting: menu not ready (screen="
                                + client.screen.getClass().getSimpleName() + ")");
                    }
                    return;
                }
                boolean reuseSave = WorldCreator.saveExists(client);
                int delayTarget = reuseSave ? 0 : CiProperties.exportWorldDelayTicks();
                if (delayTarget > 0 && worldDelayTicks < delayTarget) {
                    worldDelayTicks++;
                    if (worldDelayTicks == 1 || worldDelayTicks % Constants.HEARTBEAT_TICKS == 0
                            || worldDelayTicks == delayTarget) {
                        MweMod.LOGGER.info(
                                "world create delay {}/{} ticks (screen={})",
                                worldDelayTicks,
                                delayTarget,
                                client.screen.getClass().getSimpleName());
                    }
                    return;
                }
                worldRequestSent = true;
                if (reuseSave) {
                    MweMod.LOGGER.info(
                            "save '{}' exists, opening cached world",
                            WorldCreator.saveName());
                    WorldCreator.openExisting(client);
                } else {
                    MweMod.LOGGER.info(
                            "save '{}' missing, creating void world",
                            WorldCreator.saveName());
                    WorldCreator.createAndLoad(client);
                }
                return;
            }

            String screen = client.screen == null ? "null" : client.screen.getClass().getSimpleName();
            stateLog.tick("waiting: world loading (screen=" + screen + ")");
        }

        private void tickWarmup(Minecraft client) {
            if (client.player == null || client.level == null) {
                stateLog.tick("warning: lost player/level during warmup");
                phase = Phase.WORLD_OPENING;
                worldRequestSent = true;
                return;
            }

            if (Readiness.isReloadFailed()) {
                phase = Phase.DONE;
                MweMod.LOGGER.error("EMI reload failed (status=-1); aborting export");
                System.exit(1);
                return;
            }
            if (!isEmiReady(client)) {
                if (warmupTicks % Constants.HEARTBEAT_TICKS == 0) {
                    stateLog.tick("waiting: EMI not ready (status=" + Readiness.reloadStatusLabel() + ")");
                }
                warmupTicks = 0;
                return;
            }

            if (warmupTicks < CiProperties.exportWarmupTicks()) {
                warmupTicks++;
                if (warmupTicks % Constants.HEARTBEAT_TICKS == 0 || warmupTicks == CiProperties.exportWarmupTicks()) {
                    MweMod.LOGGER.info(
                            "warmup {}/{}",
                            warmupTicks,
                            CiProperties.exportWarmupTicks());
                }
                return;
            }

            phase = Phase.DONE;
            Path outputRoot = OutputPaths.resolveForRun(gameDirectory, outputRootOverride).rootDir();
            MweMod.LOGGER.info("running EMI export to {} ...", outputRoot.toAbsolutePath());
            try {
                ExportResult result = Pipeline.run(outputRoot, gameDirectory, client);
                MweMod.LOGGER.info(
                        "EMI export finished (recipes={}/{}, items={}, tags={}, langs={}, icons={}), exiting 0",
                        result.recipesWritten(),
                        result.recipesRequested(),
                        result.itemIndexCount(),
                        result.tagIndexCount(),
                        result.languagesWritten(),
                        result.iconsWritten());
                System.exit(0);
            } catch (Exception e) {
                MweMod.LOGGER.error("EMI export failed for {}", outputRoot.toAbsolutePath(), e);
                System.exit(1);
            }
        }
    }
}
