package io.github.jmecn.minecraftwebexport.runtime;
import io.github.jmecn.minecraftwebexport.Constants;
import io.github.jmecn.minecraftwebexport.emi.pipeline.Readiness;
import io.github.jmecn.minecraftwebexport.emi.support.Log;
import io.github.jmecn.minecraftwebexport.pipeline.Coordinator;
import io.github.jmecn.minecraftwebexport.model.pipeline.ExportResult;

import io.github.jmecn.minecraftwebexport.MweMod;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.nio.file.Path;
import java.util.Objects;

public final class ClientEntrypoint {

    private final Coordinator coordinator;

    public ClientEntrypoint() {
        this(new Coordinator());
    }

    ClientEntrypoint(Coordinator coordinator) {
        this.coordinator = Objects.requireNonNull(coordinator, "coordinator");
    }

    public void armIfEnabled(Path gameDirectory) {
        if (!Boolean.getBoolean(Constants.PROP_EXPORT_ENABLED)) {
            return;
        }
        if (CiProperties.runExportAndExit()) {
            return;
        }
        MinecraftForge.EVENT_BUS.register(new AutoExportWhenWorldReady(
                gameDirectory,
                System.getProperty(Constants.PROP_EXPORT_OUTPUT_DIR),
                coordinator));
    }

    private static int warmupTicks() {
        return Math.max(0, Integer.getInteger(Constants.PROP_EXPORT_WARMUP_TICKS, Constants.CLIENT_DEFAULT_WARMUP_TICKS));
    }

    private static final class AutoExportWhenWorldReady {

        private final Path gameDirectory;
        private final String outputRootOverride;
        private final Coordinator coordinator;

        private boolean finished;
        private int readyTicks;
        private int failureCount;

        private AutoExportWhenWorldReady(
                Path gameDirectory,
                String outputRootOverride,
                Coordinator coordinator) {
            this.gameDirectory = gameDirectory;
            this.outputRootOverride = outputRootOverride;
            this.coordinator = coordinator;
        }

        @SubscribeEvent
        public void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END || finished) {
                return;
            }
            Minecraft client = Minecraft.getInstance();
            if (!isReady(client)) {
                readyTicks = 0;
                return;
            }

            readyTicks++;
            if (readyTicks == 1 || readyTicks % Constants.HEARTBEAT_TICKS == 0) {
                MweMod.LOGGER.info(
                        "{} world ready, warmup {}/{}",
                        Log.EMI,
                        readyTicks,
                        warmupTicks());
            }
            if (readyTicks < warmupTicks()) {
                return;
            }

            Path outputRoot = OutputPaths.resolve(gameDirectory, outputRootOverride).rootDir();
            try {
                ExportResult result = coordinator.run(outputRoot, gameDirectory, client);
                finished = true;
                MweMod.LOGGER.info(
                        "{} wrote {} (recipes={}, items={}, tags={}, langs={}, icons={})",
                        Log.EMI,
                        result.outputRoot().toAbsolutePath(),
                        result.recipesWritten(),
                        result.itemIndexCount(),
                        result.tagIndexCount(),
                        result.languagesWritten(),
                        result.iconsWritten());
            } catch (Exception e) {
                failureCount++;
                readyTicks = 0;
                MweMod.LOGGER.error(
                        "{} export attempt #{} failed for {}",
                        Log.EMI,
                        failureCount,
                        outputRoot.toAbsolutePath(),
                        e);
            }
        }

        private static boolean isReady(Minecraft client) {
            return Readiness.isReadyForExport(client);
        }
    }
}
