package io.github.jmecn.minecraftwebexport.cmd;

import io.github.jmecn.minecraftwebexport.Constants;
import io.github.jmecn.minecraftwebexport.MweMod;
import io.github.jmecn.minecraftwebexport.emi.pipeline.Orchestrator;
import io.github.jmecn.minecraftwebexport.emi.pipeline.Readiness;
import io.github.jmecn.minecraftwebexport.model.emi.EmiExportReport;
import io.github.jmecn.minecraftwebexport.model.pipeline.Mode;
import io.github.jmecn.minecraftwebexport.model.pipeline.Plan;
import io.github.jmecn.minecraftwebexport.model.pipeline.Scope;
import io.github.jmecn.minecraftwebexport.pipeline.Planner;
import io.github.jmecn.minecraftwebexport.runtime.OutputPaths;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class Export {

    private Export() {}

    public static int run(CommandSourceStack source) {
        try {
            Minecraft client = Minecraft.getInstance();
            if (!Readiness.isReadyForExport(client)) {
                if (client.player == null || client.level == null || client.getSingleplayerServer() == null) {
                    source.sendFailure(Component.literal(
                            Constants.COMMAND_LOG_PREFIX + "must run in a loaded singleplayer world"));
                } else if (Readiness.isReloadFailed()) {
                    source.sendFailure(Component.literal(
                            Constants.COMMAND_LOG_PREFIX + "EMI reload failed; export unavailable"));
                } else {
                    source.sendFailure(Component.literal(
                            Constants.COMMAND_LOG_PREFIX + "EMI not ready (status="
                                    + Readiness.reloadStatusLabel() + ")"));
                }
                return 0;
            }

            Path gameDir = client.gameDirectory.toPath();
            Path outputRoot = OutputPaths.resolve(gameDir, null).rootDir();
            Scope scope = new Scope(outputRoot, gameDir, Mode.FULL);
            Plan plan = Planner.plan(client, Mode.FULL, List.of(), scope);
            EmiExportReport report = new Orchestrator().export(outputRoot, client, plan);

            String summary = String.format(
                    "%swrote %s (recipes=%d/%d, items=%d, tags=%d, langs=%d, icons=%d)",
                    Constants.COMMAND_LOG_PREFIX,
                    report.outputRoot().toAbsolutePath(),
                    report.recipesWritten(),
                    report.recipesRequested(),
                    report.itemIndexCount(),
                    report.tagIndexCount(),
                    report.languagesWritten(),
                    report.iconsWritten());
            source.sendSystemMessage(Component.literal(summary));
            MweMod.LOGGER.info(summary);
            return 1;
        } catch (IOException e) {
            source.sendFailure(Component.literal(
                    Constants.COMMAND_LOG_PREFIX + "export failed: " + e.getMessage()));
            MweMod.LOGGER.error(Constants.COMMAND_LOG_PREFIX + "export failed", e);
            return 0;
        } catch (Exception e) {
            source.sendFailure(Component.literal(
                    Constants.COMMAND_LOG_PREFIX + "export failed: " + e.getMessage()));
            MweMod.LOGGER.error(Constants.COMMAND_LOG_PREFIX + "export failed", e);
            return 0;
        }
    }
}
