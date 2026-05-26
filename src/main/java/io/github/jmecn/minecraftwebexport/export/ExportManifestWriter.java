package io.github.jmecn.minecraftwebexport.export;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ExportManifestWriter {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public Path write(
            ExportOutputPaths paths,
            ExportRequest request,
            ExportStatus status,
            List<Path> filesWritten,
            List<ExportIssue> errors,
            long durationMillis,
            Instant exportedAt) throws IOException {
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("status", status.wireValue());
        manifest.put("exportedAt", exportedAt.toString());
        manifest.put("modId", request.modId());
        manifest.put("minecraftVersion", request.minecraftVersion());
        manifest.put("schemaVersion", request.schemaVersion());
        manifest.put("bundlePath", paths.relativeBundlePath());
        manifest.put("outputRoot", paths.rootDir().toString());
        manifest.put("durationMillis", durationMillis);
        manifest.put("filesWritten", filesWritten.stream()
                .map(path -> paths.rootDir().relativize(path).toString().replace('\\', '/'))
                .toList());
        manifest.put("errors", errors.stream().map(ExportIssue::toMap).toList());

        Files.writeString(paths.manifestFile(), GSON.toJson(manifest));
        return paths.manifestFile();
    }
}
