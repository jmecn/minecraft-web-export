package io.github.jmecn.minecraftwebexport.model.pipeline;

import io.github.jmecn.minecraftwebexport.enums.pipeline.Mode;
import java.util.Objects;

public record Plan(ExportContext context, Hints hints) {

    public Plan {
        context = Objects.requireNonNull(context, "context");
        hints = Objects.requireNonNull(hints, "hints");
    }

    public Mode mode() {
        return context.mode();
    }
}
