package io.github.jmecn.minecraftwebexport.export.emi;

import dev.emi.emi.api.EmiApi;
import io.github.jmecn.minecraftwebexport.export.ExportOutputPaths;
import io.github.jmecn.minecraftwebexport.export.RuntimeExportEntrypoint;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.Objects;

public final class RuntimeEmiExportEntrypoint {

    private static final int DEFAULT_WARMUP_TICKS = 100;
    private static final int HEARTBEAT_TICKS = 200;

    private final EmiRuntimeExportOrchestrator orchestrator;

    public RuntimeEmiExportEntrypoint() {
        this(new EmiRuntimeExportOrchestrator());
    }

    RuntimeEmiExportEntrypoint(EmiRuntimeExportOrchestrator orchestrator) {
        this.orchestrator = Objects.requireNonNull(orchestrator, "orchestrator");
    }

    public void armIfEnabled(Path gameDirectory, Logger logger) {
        Objects.requireNonNull(logger, "logger");
        if (!Boolean.getBoolean(RuntimeExportEntrypoint.ENABLE_PROPERTY)) {
            return;
        }
        MinecraftForge.EVENT_BUS.register(new AutoExportWhenWorldReady(
                gameDirectory,
                System.getProperty(RuntimeExportEntrypoint.OUTPUT_ROOT_PROPERTY),
                logger,
                orchestrator));
    }

    private static int warmupTicks() {
        return Math.max(0, Integer.getInteger("minecraftWebExport.exportWarmupTicks", DEFAULT_WARMUP_TICKS));
    }

    private static final class AutoExportWhenWorldReady {

        private final Path gameDirectory;
        private final String outputRootOverride;
        private final Logger logger;
        private final EmiRuntimeExportOrchestrator orchestrator;

        private boolean finished;
        private int readyTicks;
        private int failureCount;

        private AutoExportWhenWorldReady(
                Path gameDirectory,
                String outputRootOverride,
                Logger logger,
                EmiRuntimeExportOrchestrator orchestrator) {
            this.gameDirectory = gameDirectory;
            this.outputRootOverride = outputRootOverride;
            this.logger = logger;
            this.orchestrator = orchestrator;
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
                logger.info("[emi] world ready, warmup {}/{}", readyTicks, warmupTicks());
            }
            if (readyTicks < warmupTicks()) {
                return;
            }

            Path outputRoot = ExportOutputPaths.resolve(gameDirectory, outputRootOverride).rootDir();
            try {
                EmiRuntimeExportOrchestrator.Report report = orchestrator.export(outputRoot, client);
                finished = true;
                logger.info("[emi] wrote {} (recipes={}, items={}, tags={}, langs={}, icons={})",
                        report.outputRoot().toAbsolutePath(),
                        report.recipesWritten(),
                        report.itemIndexCount(),
                        report.tagIndexCount(),
                        report.languagesWritten(),
                        report.iconsWritten());
            } catch (Exception e) {
                failureCount++;
                readyTicks = 0;
                logger.error("[emi] export attempt #{} failed for {}", failureCount, outputRoot.toAbsolutePath(), e);
            }
        }

        private static boolean isReady(Minecraft client) {
            var manager = EmiApi.getRecipeManager();
            return client.player != null
                    && client.level != null
                    && client.getSingleplayerServer() != null
                    && manager != null
                    && !manager.getRecipes().isEmpty();
        }
    }
}
