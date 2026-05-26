package io.github.jmecn.minecraftwebexport.export;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record ExportIssue(String stage, String message) {

    public ExportIssue {
        stage = Objects.requireNonNull(stage, "stage");
        message = Objects.requireNonNull(message, "message");
    }

    public Map<String, String> toMap() {
        Map<String, String> out = new LinkedHashMap<>();
        out.put("stage", stage);
        out.put("message", message);
        return out;
    }
}
