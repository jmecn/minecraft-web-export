package io.github.jmecn.minecraftwebexport.export.emi;

import io.github.jmecn.minecraftwebexport.export.ExportOutputPaths;
import io.github.jmecn.minecraftwebexport.export.RuntimeExportEntrypoint;
import io.github.jmecn.minecraftwebexport.export.ci.ExportCiProperties;
import io.github.jmecn.minecraftwebexport.export.module.ExportCoordinator;
import io.github.jmecn.minecraftwebexport.export.module.ExportResult;
import io.github.jmecn.minecraftwebexport.mod.MinecraftWebExportMod;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.nio.file.Path;
import java.util.Objects;

public final class RuntimeEmiExportEntrypoint {

    private static final int DEFAULT_WARMUP_TICKS = 100;
    private static final int HEARTBEAT_TICKS = 200;

    private final ExportCoordinator coordinator;

    public RuntimeEmiExportEntrypoint() {
        this(new ExportCoordinator());
    }

    RuntimeEmiExportEntrypoint(ExportCoordinator coordinator) {
        this.coordinator = Objects.requireNonNull(coordinator, "coordinator");
    }

    public void armIfEnabled(Path gameDirectory) {
        if (!Boolean.getBoolean(RuntimeExportEntrypoint.ENABLE_PROPERTY)) {
            return;
        }
        if (ExportCiProperties.runExportAndExit()) {
            return;
        }
        MinecraftForge.EVENT_BUS.register(new AutoExportWhenWorldReady(
                gameDirectory,
                System.getProperty(RuntimeExportEntrypoint.OUTPUT_ROOT_PROPERTY),
                coordinator));
    }

    private static int warmupTicks() {
        return Math.max(0, Integer.getInteger("minecraftWebExport.exportWarmupTicks", DEFAULT_WARMUP_TICKS));
    }

    private static final class AutoExportWhenWorldReady {

        private final Path gameDirectory;
        private final String outputRootOverride;
        private final ExportCoordinator coordinator;

        private boolean finished;
        private int readyTicks;
        private int failureCount;

        private AutoExportWhenWorldReady(
                Path gameDirectory,
                String outputRootOverride,
                ExportCoordinator coordinator) {
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
            if (readyTicks == 1 || readyTicks % HEARTBEAT_TICKS == 0) {
                MinecraftWebExportMod.LOGGER.info(
                        "{} world ready, warmup {}/{}",
                        ExportLog.EMI,
                        readyTicks,
                        warmupTicks());
            }
            if (readyTicks < warmupTicks()) {
                return;
            }

            Path outputRoot = ExportOutputPaths.resolve(gameDirectory, outputRootOverride).rootDir();
            try {
                ExportResult result = coordinator.run(outputRoot, gameDirectory, client);
                finished = true;
                MinecraftWebExportMod.LOGGER.info(
                        "{} wrote {} (recipes={}, items={}, tags={}, langs={}, icons={})",
                        ExportLog.EMI,
                        result.outputRoot().toAbsolutePath(),
                        result.recipesWritten(),
                        result.itemIndexCount(),
                        result.tagIndexCount(),
                        result.languagesWritten(),
                        result.iconsWritten());
            } catch (Exception e) {
                failureCount++;
                readyTicks = 0;
                MinecraftWebExportMod.LOGGER.error(
                        "{} export attempt #{} failed for {}",
                        ExportLog.EMI,
                        failureCount,
                        outputRoot.toAbsolutePath(),
                        e);
            }
        }

        private static boolean isReady(Minecraft client) {
            return EmiExportReadiness.isReadyForExport(client);
        }
    }
}
