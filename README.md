# Minecraft Web Export

Forge mod that exports EMI recipe bundles for web viewers (e.g. [emi-recipe-renderer](https://github.com/jmecn/emi-recipe-renderer), TFG Recipe Viewer).

## Build

```bash
./gradlew jar
# output: build/libs/minecraft-web-export-<version>.jar
```

## Local dev runs

| Gradle run | Purpose |
|------------|---------|
| `runClient` | Normal client |
| `runExportClient` | Export when already in a world (`export.enabled=true`) |
| `runExportCiClient` | Headless-style CI driver (short warmup for local smoke) |

## CI / HeadlessMC JVM properties

Use with a full modpack + HeadlessMC + xvfb (see TFG-Recipe-Viewer workflow).

| Property | CI example | Description |
|----------|------------|-------------|
| `minecraftWebExport.export.enabled` | `true` | Enable export pipeline |
| `minecraftWebExport.runExportAndExit` | `true` | Auto world → EMI export → `System.exit` |
| `minecraftWebExport.export.outputDir` | `/path/to/export-raw` | Output directory (absolute) |
| `minecraftWebExport.exportWarmupTicks` | `2400` | In-world ticks after EMI is ready (~2 min @ 20 TPS) |
| `minecraftWebExport.exportWorldDelayTicks` | `600` | Menu ticks before creating a **new** world (skipped when reusing save) |
| `minecraftWebExport.exportTimeoutSeconds` | `7200` | Hard timeout for entire run (`<=0` disables) |
| `minecraftWebExport.exportWorldName` | `emi-export` | Save folder under `saves/` |

Recommended extra JVM flags (modpack stability):

```
-Djava.awt.headless=false
-Dmodernfix.config.mixin.feature.integrated_server_watchdog=false
```

**Do not** pass `--quickPlaySingleplayer` when using `runExportAndExit`; the mod owns world loading.
