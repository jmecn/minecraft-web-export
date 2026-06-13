package io.github.jmecn.minecraftwebexport.cmd;
import io.github.jmecn.minecraftwebexport.Constants;
import io.github.jmecn.minecraftwebexport.emi.pipeline.Orchestrator;
import io.github.jmecn.minecraftwebexport.emi.pipeline.Readiness;
import io.github.jmecn.minecraftwebexport.model.emi.EmiExportReport;
import io.github.jmecn.minecraftwebexport.model.pipeline.Mode;
import io.github.jmecn.minecraftwebexport.model.pipeline.Plan;
import io.github.jmecn.minecraftwebexport.model.pipeline.Scope;
import io.github.jmecn.minecraftwebexport.pipeline.Planner;
import io.github.jmecn.minecraftwebexport.runtime.OutputPaths;

import io.github.jmecn.minecraftwebexport.MweMod;
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
                    source.sendFailure(Component.literal(Constants.COMMAND_LOG_PREFIX + "需要在已加载的单人世界中执行"));
                } else if (Readiness.isReloadFailed()) {
                    source.sendFailure(Component.literal(Constants.COMMAND_LOG_PREFIX + "EMI 重载失败，无法导出"));
                } else {
                    source.sendFailure(Component.literal(
                            Constants.COMMAND_LOG_PREFIX + "EMI 未就绪 (status=" + Readiness.reloadStatusLabel() + ")"));
                }
                return 0;
            }

            Path gameDir = client.gameDirectory.toPath();
            Path outputRoot = OutputPaths.resolve(gameDir, null).rootDir();
            Scope scope = new Scope(outputRoot, gameDir, Mode.FULL);
            Plan plan = Planner.plan(client, Mode.FULL, List.of(), scope);
            EmiExportReport report = new Orchestrator().export(outputRoot, client, plan);

            Component message = Component.literal(String.format(
                    "%s已写入 %s (recipes=%d/%d, items=%d, tags=%d, langs=%d, icons=%d)",
                    Constants.COMMAND_LOG_PREFIX,
                    report.outputRoot().toAbsolutePath(),
                    report.recipesWritten(),
                    report.recipesRequested(),
                    report.itemIndexCount(),
                    report.tagIndexCount(),
                    report.languagesWritten(),
                    report.iconsWritten()));
            source.sendSystemMessage(message);
            MweMod.LOGGER.info(message.getString());
            return 1;
        } catch (IOException e) {
            source.sendFailure(Component.literal(Constants.COMMAND_LOG_PREFIX + "导出失败: " + e.getMessage()));
            MweMod.LOGGER.error(Constants.COMMAND_LOG_PREFIX + "export failed", e);
            return 0;
        } catch (Exception e) {
            source.sendFailure(Component.literal(Constants.COMMAND_LOG_PREFIX + "导出失败: " + e.getMessage()));
            MweMod.LOGGER.error(Constants.COMMAND_LOG_PREFIX + "export failed", e);
            return 0;
        }
    }
}
