package io.github.jmecn.minecraftwebexport.emi.lang;

import io.github.jmecn.minecraftwebexport.model.recipe.RecipeMeta;
import io.github.jmecn.minecraftwebexport.model.recipe.RecipeWidget;
import io.github.jmecn.minecraftwebexport.model.recipe.WidgetInteraction;
import com.google.gson.JsonElement;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import io.github.jmecn.minecraftwebexport.Constants;

public final class UsedKeysCollector {

    private final Set<String> keys = new TreeSet<>();

    public int size() {
        return keys.size();
    }

    public Set<String> snapshot() {
        return Set.copyOf(keys);
    }

    public void collectMeta(RecipeMeta meta) {
        if (meta == null || meta.widgets() == null) {
            return;
        }
        for (RecipeWidget widget : meta.widgets()) {
            collectInteraction(widget.interaction());
        }
    }

    private void collectInteraction(WidgetInteraction interaction) {
        if (interaction == null) {
            return;
        }
        String kind = interaction.kind();
        if ("item".equals(kind)) {
            if (interaction.id() != null) {
                addRegistryItem(interaction.id());
            }
            if (interaction.nbt() != null) {
                collectFluidFromNbt(interaction.nbt());
            }
            return;
        }
        if ("fluid".equals(kind)) {
            if (interaction.id() != null) {
                addRegistryFluid(interaction.id());
            }
            return;
        }
        if ("tag".equals(kind)) {
            if (interaction.tag() != null) {
                addTag(interaction.tag());
            }
            if (interaction.displayId() != null) {
                addRegistryItem(interaction.displayId());
            }
            return;
        }
        if ("list".equals(kind) && interaction.entries() != null) {
            for (WidgetInteraction entry : interaction.entries()) {
                collectInteraction(entry);
            }
        }
    }

    private void collectFluidFromNbt(JsonElement nbt) {
        String raw = nbt.isJsonPrimitive() ? nbt.getAsString() : nbt.toString();
        Matcher matcher = Constants.FLUID_NBT_NAME_PATTERN.matcher(raw);
        if (matcher.find()) {
            addRegistryFluid(matcher.group(1));
        }
    }

    private void addRegistryItem(String registryId) {
        ClosureKeys.addForItem(keys, registryId);
    }

    private void addRegistryFluid(String registryId) {
        ClosureKeys.addForFluid(keys, registryId);
    }

    private void addTag(String tagId) {
        if (tagId == null || tagId.isEmpty()) {
            return;
        }
        String dotted = tagId.replace('/', '.').replace(':', '.');
        keys.add("tag.item." + dotted);
        keys.add("tag.block." + dotted);
        keys.add("tag.fluid." + dotted);
    }
}
