package io.github.jmecn.minecraftwebexport.runtime;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipeManager;
import dev.emi.emi.runtime.EmiReloadManager;
import io.github.jmecn.minecraftwebexport.Constants;
import io.github.jmecn.minecraftwebexport.MweMod;
import io.github.jmecn.minecraftwebexport.config.MweConfig;
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

    public static boolean isReloadFailed() {
        return EmiReloadManager.getStatus() == -1;
    }

    public static boolean isReloadInProgress() {
        int status = EmiReloadManager.getStatus();
        return status == 1 || (status == 2 && !EmiReloadManager.isLoaded());
    }

    public static String reloadStatusLabel() {
        return switch (EmiReloadManager.getStatus()) {
            case -1 -> "error";
            case 0 -> "idle";
            case 1 -> "reloading";
            case 2 -> EmiReloadManager.isLoaded() ? "loaded" : "finishing";
            default -> "unknown(" + EmiReloadManager.getStatus() + ")";
        };
    }

    public static boolean isReadyForExport(Minecraft client) {
        if (client.player == null || client.level == null || client.getSingleplayerServer() == null) {
            return false;
        }
        if (!EmiReloadManager.isLoaded()) {
            return false;
        }
        EmiRecipeManager manager = EmiApi.getRecipeManager();
        return manager != null && !manager.getRecipes().isEmpty();
    }

    public void register() {
        MweMod.LOGGER.info(
                "mode=ci-export, world={}, output={}, timeoutSeconds={}",
                WorldCreator.saveName(),
                OutputPaths.resolveForRun(gameDirectory, outputRootOverride).rootDir(),
                MweConfig.timeoutSeconds());
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
        return isReadyForExport(client);
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

        private enum Phase {ARMED, WORLD_OPENING, WAITING_FOR_EMI, DONE}

        private final Path gameDirectory;
        private final String outputRootOverride;
        private final StateLogger stateLog;

        private Phase phase = Phase.ARMED;
        private boolean worldRequestSent;
        private int worldDelayTicks;
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

            Minecraft client = Minecraft.getInstance();

            if (phase == Phase.ARMED) {
                startNanos = System.nanoTime();
                phase = Phase.WORLD_OPENING;
                MweMod.LOGGER.info(
                        "CI export: armed (timeout={}s), waiting for idle menu...",
                        MweConfig.timeoutSeconds());
            }

            if (isFatalMenuScreen(client)) {
                phase = Phase.DONE;
                MweMod.LOGGER.error(
                        "fatal menu screen ({}); aborting export",
                        client.screen.getClass().getName());
                System.exit(1);
                return;
            }
            if (MweConfig.timedOut(startNanos)) {
                String phaseAtTimeout = phase.name();
                phase = Phase.DONE;
                String screen = client.screen == null ? "null" : client.screen.getClass().getName();
                MweMod.LOGGER.error(
                        "export timed out after {}s (phase={}, player={}, level={}, screen={})",
                        MweConfig.timeoutSeconds(),
                        phaseAtTimeout,
                        client.player != null,
                        client.level != null,
                        screen);
                System.exit(1);
                return;
            }

            switch (phase) {
                case WORLD_OPENING -> tickWorldOpening(client);
                case WAITING_FOR_EMI -> tickWaitingForEmi(client);
                default -> {}
            }
        }

        private void tickWorldOpening(Minecraft client) {
            if (client.player != null && client.level != null) {
                phase = Phase.WAITING_FOR_EMI;
                MweMod.LOGGER.info(
                        "player + level present (player={}, dim={}), waiting for EMI ready",
                        client.player.getName().getString(),
                        client.level.dimension().location());
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
                int delayTarget = reuseSave ? 0 : MweConfig.worldDelayTicks();
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

        private void tickWaitingForEmi(Minecraft client) {
            if (client.player == null || client.level == null) {
                stateLog.tick("warning: lost player/level while waiting for EMI");
                phase = Phase.WORLD_OPENING;
                worldRequestSent = true;
                worldDelayTicks = 0;
                return;
            }

            if (isReloadFailed()) {
                phase = Phase.DONE;
                MweMod.LOGGER.error("EMI reload failed (status=-1); aborting export");
                System.exit(1);
                return;
            }
            if (!isEmiReady(client)) {
                stateLog.tick("waiting: EMI not ready (status=" + reloadStatusLabel() + ")");
                return;
            }

            phase = Phase.DONE;
            runCiExport(client);
        }

        private void runCiExport(Minecraft client) {
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
