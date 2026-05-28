# Minecraft Web Export

Forge mod that exports EMI recipe bundles for web viewers (e.g. [emi-recipe-renderer](https://github.com/jmecn/emi-recipe-renderer), TFG Recipe Viewer).

## Build

```bash
./gradlew jar
# output: build/libs/minecraft-web-export-<version>.jar
```

## Release

Push a semver tag (`v0.2.0`, …); [`.github/workflows/release.yml`](.github/workflows/release.yml) builds the jar and attaches it to the GitHub Release. Downstream CI (e.g. TFG-Recipe-Viewer) downloads that asset instead of compiling here.

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
| `minecraftWebExport.exportWarmupTicks` | `100` | Extra ticks **after** EMI recipes are non-empty (~5 s @ 20 TPS); not the FG 2400 spawn delay |
| `minecraftWebExport.layoutLogStride` | *(adaptive)* | Layout export progress log interval; default ~30 lines for 100k+ recipes |
| `minecraftWebExport.exportWorldDelayTicks` | `600` | Menu ticks before creating a **new** world (skipped when reusing save) |
| `minecraftWebExport.exportTimeoutSeconds` | `7200` | Hard timeout for entire run (`<=0` disables) |
| `minecraftWebExport.exportWorldName` | `emi-export` | Save folder under `saves/` |

Recommended extra JVM flags (modpack stability):

```
-Djava.awt.headless=false
-Dmodernfix.config.mixin.feature.integrated_server_watchdog=false
```

**Do not** pass `--quickPlaySingleplayer` when using `runExportAndExit`; the mod owns world loading.
