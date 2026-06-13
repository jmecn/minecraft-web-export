package io.github.jmecn.minecraftwebexport.model.recipe;

import com.google.gson.JsonObject;

import java.util.List;

public record WidgetInteraction(
        String kind,
        String id,
        Integer amount,
        Long amountMb,
        JsonObject nbt,
        String tag,
        String tagKind,
        String displayId,
        List<WidgetInteraction> entries,
        Integer featuredIndex) {

    public WidgetInteraction {
        entries = entries == null ? null : List.copyOf(entries);
    }

    public static WidgetInteraction item(String id, Integer amount, JsonObject nbt) {
        return new WidgetInteraction("item", id, amount, null, nbt, null, null, null, null, null);
    }

    public static WidgetInteraction fluid(String id, Long amountMb) {
        return new WidgetInteraction("fluid", id, null, amountMb, null, null, null, null, null, null);
    }

    public static WidgetInteraction tag(String tag, String tagKind, String displayId) {
        return new WidgetInteraction("tag", null, null, null, null, tag, tagKind, displayId, null, null);
    }

    public static WidgetInteraction list(List<WidgetInteraction> entries, int featuredIndex) {
        return new WidgetInteraction("list", null, null, null, null, null, null, null, entries, featuredIndex);
    }
}
