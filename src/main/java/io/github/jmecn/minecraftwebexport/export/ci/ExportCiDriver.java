package io.github.jmecn.minecraftwebexport.export.ci;

import io.github.jmecn.minecraftwebexport.export.ExportOutputPaths;
import io.github.jmecn.minecraftwebexport.export.RuntimeExportEntrypoint;
import io.github.jmecn.minecraftwebexport.export.emi.EmiExportReadiness;
import io.github.jmecn.minecraftwebexport.export.module.ExportCoordinator;
import io.github.jmecn.minecraftwebexport.export.module.ExportResult;
import io.github.jmecn.minecraftwebexport.mod.MinecraftWebExportMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.nio.file.Path;

public final class ExportCiDriver {

    private static final int HEARTBEAT_TICKS = 200;

    private final Path gameDirectory;
    private final String outputRootOverride;
    private final ExportCoordinator coordinator;

    public ExportCiDriver(Path gameDirectory, String outputRootOverride) {
        this(gameDirectory, outputRootOverride, new ExportCoordinator());
    }

    ExportCiDriver(Path gameDirectory, String outputRootOverride, ExportCoordinator coordinator) {
        this.gameDirectory = gameDirectory;
        this.outputRootOverride = outputRootOverride;
        this.coordinator = coordinator;
    }

    public void register() {
        MinecraftWebExportMod.LOGGER.info(
                "mode=runExportAndExit, world={}, output={}, warmupTicks={}, timeoutSeconds={}",
                ExportWorldCreator.saveName(),
                ExportOutputPaths.resolveForRun(gameDirectory, outputRootOverride).rootDir(),
                ExportCiProperties.exportWarmupTicks(),
                ExportCiProperties.exportTimeoutSeconds());
        MinecraftForge.EVENT_BUS.register(new AutoExportHandler(gameDirectory, outputRootOverride, coordinator));
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
        return EmiExportReadiness.isReadyForExport(client);
    }

    private static final class StateLogger {
        private String state = "";
        private int sameStateTicks;

        void tick(String newState) {
            if (!newState.equals(state)) {
                state = newState;
                sameStateTicks = 0;
                MinecraftWebExportMod.LOGGER.info(newState);
            } else if (++sameStateTicks % HEARTBEAT_TICKS == 0) {
                MinecraftWebExportMod.LOGGER.info("{} (still waiting, {} ticks)", newState, sameStateTicks);
            }
        }
    }

    private static final class AutoExportHandler {

        private enum Phase {ARMED, WORLD_OPENING, WARMUP, DONE}

        private final Path gameDirectory;
        private final String outputRootOverride;
        private final ExportCoordinator coordinator;
        private final StateLogger stateLog;

        private Phase phase = Phase.ARMED;
        private boolean worldRequestSent;
        private int worldDelayTicks;
        private int warmupTicks;
        private long startNanos;

        AutoExportHandler(Path gameDirectory, String outputRootOverride, ExportCoordinator coordinator) {
            this.gameDirectory = gameDirectory;
            this.outputRootOverride = outputRootOverride;
            this.coordinator = coordinator;
            this.stateLog = new StateLogger();
        }

        @SubscribeEvent
        public void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END || phase == Phase.DONE) {
                return;
            }
            if (!Boolean.getBoolean(RuntimeExportEntrypoint.ENABLE_PROPERTY)) {
                return;
            }

            Minecraft client = Minecraft.getInstance();

            if (phase == Phase.ARMED) {
                startNanos = System.nanoTime();
                phase = Phase.WORLD_OPENING;
                MinecraftWebExportMod.LOGGER.info(
                        "runExportAndExit: armed (timeout={}s), waiting for idle menu...",
                        ExportCiProperties.exportTimeoutSeconds());
            }

            if (isFatalMenuScreen(client)) {
                phase = Phase.DONE;
                MinecraftWebExportMod.LOGGER.error(
                        "fatal menu screen ({}); aborting export",
                        client.screen.getClass().getName());
                System.exit(1);
                return;
            }
            if (ExportCiProperties.timedOut(startNanos)) {
                String phaseAtTimeout = phase.name();
                phase = Phase.DONE;
                String screen = client.screen == null ? "null" : client.screen.getClass().getName();
                MinecraftWebExportMod.LOGGER.error(
                        "export timed out after {}s (phase={}, player={}, level={}, screen={})",
                        ExportCiProperties.exportTimeoutSeconds(),
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
                MinecraftWebExportMod.LOGGER.info(
                        "player + level present (player={}, dim={}), warming up {} ticks",
                        client.player.getName().getString(),
                        client.level.dimension().location(),
                        ExportCiProperties.exportWarmupTicks());
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
                boolean reuseSave = ExportWorldCreator.saveExists(client);
                int delayTarget = reuseSave ? 0 : ExportCiProperties.exportWorldDelayTicks();
                if (delayTarget > 0 && worldDelayTicks < delayTarget) {
                    worldDelayTicks++;
                    if (worldDelayTicks == 1 || worldDelayTicks % HEARTBEAT_TICKS == 0
                            || worldDelayTicks == delayTarget) {
                        MinecraftWebExportMod.LOGGER.info(
                                "world create delay {}/{} ticks (screen={})",
                                worldDelayTicks,
                                delayTarget,
                                client.screen.getClass().getSimpleName());
                    }
                    return;
                }
                worldRequestSent = true;
                if (reuseSave) {
                    MinecraftWebExportMod.LOGGER.info(
                            "save '{}' exists, opening cached world",
                            ExportWorldCreator.saveName());
                    ExportWorldCreator.openExisting(client);
                } else {
                    MinecraftWebExportMod.LOGGER.info(
                            "save '{}' missing, creating void world",
                            ExportWorldCreator.saveName());
                    ExportWorldCreator.createAndLoad(client);
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

            if (EmiExportReadiness.isReloadFailed()) {
                phase = Phase.DONE;
                MinecraftWebExportMod.LOGGER.error("EMI reload failed (status=-1); aborting export");
                System.exit(1);
                return;
            }
            if (!isEmiReady(client)) {
                if (warmupTicks % HEARTBEAT_TICKS == 0) {
                    stateLog.tick("waiting: EMI not ready (status=" + EmiExportReadiness.reloadStatusLabel() + ")");
                }
                warmupTicks = 0;
                return;
            }

            if (warmupTicks < ExportCiProperties.exportWarmupTicks()) {
                warmupTicks++;
                if (warmupTicks % HEARTBEAT_TICKS == 0 || warmupTicks == ExportCiProperties.exportWarmupTicks()) {
                    MinecraftWebExportMod.LOGGER.info(
                            "warmup {}/{}",
                            warmupTicks,
                            ExportCiProperties.exportWarmupTicks());
                }
                return;
            }

            phase = Phase.DONE;
            Path outputRoot = ExportOutputPaths.resolveForRun(gameDirectory, outputRootOverride).rootDir();
            MinecraftWebExportMod.LOGGER.info("running EMI export to {} ...", outputRoot.toAbsolutePath());
            try {
                ExportResult result = coordinator.run(outputRoot, gameDirectory, client);
                MinecraftWebExportMod.LOGGER.info(
                        "EMI export finished (recipes={}/{}, items={}, tags={}, langs={}, icons={}), exiting 0",
                        result.recipesWritten(),
                        result.recipesRequested(),
                        result.itemIndexCount(),
                        result.tagIndexCount(),
                        result.languagesWritten(),
                        result.iconsWritten());
                System.exit(0);
            } catch (Exception e) {
                MinecraftWebExportMod.LOGGER.error("EMI export failed for {}", outputRoot.toAbsolutePath(), e);
                System.exit(1);
            }
        }
    }
}
