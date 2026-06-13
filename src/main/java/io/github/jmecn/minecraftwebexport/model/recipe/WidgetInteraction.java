package io.github.jmecn.minecraftwebexport.model.recipe;

import java.util.List;
import java.util.Map;

public record WidgetInteraction(
        String kind,
        String id,
        Integer amount,
        Long amountMb,
        Map<String, Object> nbt,
        String tag,
        String tagKind,
        String displayId,
        List<WidgetInteraction> entries,
        Integer featuredIndex) {
}
