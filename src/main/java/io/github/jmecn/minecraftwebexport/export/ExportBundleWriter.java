package io.github.jmecn.minecraftwebexport.export;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public class ExportBundleWriter {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public Path write(ExportOutputPaths paths, ExportRequest request, Instant generatedAt) throws IOException {
        Map<String, Object> bundle = new LinkedHashMap<>();
        bundle.put("kind", "minecraft-web-export.phase1.bundle");
        bundle.put("generatedAt", generatedAt.toString());

        Map<String, Object> contents = new LinkedHashMap<>();
        contents.put("modId", request.modId());
        contents.put("trigger", request.trigger());
        contents.put("message", "Phase 1 minimal export pipeline is active");
        contents.put("minecraftVersion", request.minecraftVersion());
        bundle.put("contents", contents);

        Files.writeString(paths.bundleFile(), GSON.toJson(bundle));
        return paths.bundleFile();
    }
}
