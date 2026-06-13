package io.github.jmecn.minecraftwebexport.export.ci;

import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorPresets;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import io.github.jmecn.minecraftwebexport.mod.MinecraftWebExportMod;

public final class ExportWorldCreator {

    private ExportWorldCreator() {}

    public static String saveName() {
        return ExportCiProperties.exportWorldName();
    }

    public static boolean saveExists(Minecraft mc) {
        try {
            return mc.getLevelSource().levelExists(saveName());
        } catch (Exception e) {
            MinecraftWebExportMod.LOGGER.warn("levelExists({}) threw; assuming missing", saveName(), e);
            return false;
        }
    }

    public static void openExisting(Minecraft mc) {
        MinecraftWebExportMod.LOGGER.info("opening existing world '{}'", saveName());
        mc.createWorldOpenFlows().loadLevel(mc.screen, saveName());
    }

    public static void createAndLoad(Minecraft mc) {
        MinecraftWebExportMod.LOGGER.info("creating fresh void creative world '{}' (seed=0, peaceful, cheats=on)", saveName());

        GameRules rules = buildHeadlessGameRules();

        LevelSettings settings = new LevelSettings(
                saveName(),
                GameType.CREATIVE,
                false,
                Difficulty.PEACEFUL,
                true,
                rules,
                WorldDataConfiguration.DEFAULT
        );

        WorldOptions worldOptions = new WorldOptions(0L, false, false);

        mc.createWorldOpenFlows().createFreshLevel(
                saveName(),
                settings,
                worldOptions,
                ExportWorldCreator::buildVoidDimensions
        );
    }

    private static GameRules buildHeadlessGameRules() {
        GameRules rules = new GameRules();
        setBool(rules, GameRules.RULE_DAYLIGHT, false);
        setBool(rules, GameRules.RULE_WEATHER_CYCLE, false);
        setBool(rules, GameRules.RULE_DOMOBSPAWNING, false);
        setBool(rules, GameRules.RULE_DOFIRETICK, false);
        setBool(rules, GameRules.RULE_MOBGRIEFING, false);
        setBool(rules, GameRules.RULE_KEEPINVENTORY, true);
        setInt(rules, GameRules.RULE_RANDOMTICKING, 0);
        return rules;
    }

    private static void setBool(GameRules rules, GameRules.Key<GameRules.BooleanValue> key, boolean value) {
        rules.getRule(key).set(value, null);
    }

    private static void setInt(GameRules rules, GameRules.Key<GameRules.IntegerValue> key, int value) {
        rules.getRule(key).set(value, null);
    }

    private static WorldDimensions buildVoidDimensions(RegistryAccess registries) {
        FlatLevelGeneratorSettings voidSettings = registries
                .registryOrThrow(Registries.FLAT_LEVEL_GENERATOR_PRESET)
                .getHolderOrThrow(FlatLevelGeneratorPresets.THE_VOID)
                .value()
                .settings();

        ChunkGenerator voidGen = new FlatLevelSource(voidSettings);

        WorldPreset normal = registries
                .registryOrThrow(Registries.WORLD_PRESET)
                .getHolderOrThrow(WorldPresets.NORMAL)
                .value();
        WorldDimensions normalDims = normal.createWorldDimensions();

        return normalDims.replaceOverworldGenerator(registries, voidGen);
    }
}
