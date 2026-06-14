package io.github.jmecn.minecraftwebexport.model.pipeline;

import java.util.Objects;

public record Plan(ExportContext context, Hints hints, Seeds sourceSeeds) {

    public Plan {
        context = Objects.requireNonNull(context, "context");
        hints = Objects.requireNonNull(hints, "hints");
        sourceSeeds = Objects.requireNonNull(sourceSeeds, "sourceSeeds");
    }

    public Mode mode() {
        return context.mode();
    }
}
