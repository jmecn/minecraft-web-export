package io.github.jmecn.minecraftwebexport.emi.recipe;

import com.google.gson.JsonArray;
import com.google.common.collect.Iterables;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.serializer.EmiIngredientSerializer;
import dev.emi.emi.api.widget.AnimatedTextureWidget;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.DrawableWidget;
import dev.emi.emi.api.widget.FillingArrowWidget;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.TankWidget;
import dev.emi.emi.api.widget.TextWidget;
import dev.emi.emi.api.widget.TextureWidget;
import dev.emi.emi.api.widget.TooltipWidget;
import dev.emi.emi.api.widget.Widget;
import dev.emi.emi.widget.RecipeButtonWidget;
import io.github.jmecn.minecraftwebexport.Constants;
import io.github.jmecn.minecraftwebexport.emi.icon.OffScreenRenderer;
import io.github.jmecn.minecraftwebexport.emi.icon.StackKey;
import io.github.jmecn.minecraftwebexport.emi.support.Log;
import io.github.jmecn.minecraftwebexport.model.emi.recipe.ChromeAsset;
import java.lang.reflect.Field;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

public final class WidgetSerializer {

    record Context(
            Minecraft client,
            Path chromeRoot,
            Map<String, String> chromeHashToRelative,
            int[] chromeWritten,
            int[] chromeDeduped,
            Set<String> referencedItems,
            Set<String> referencedFluids,
            Set<String> referencedTags,
            Map<String, ItemStack> iconVariants) {
    }

    private WidgetSerializer() {
    }

    public static void collectFromRecipe(
            EmiRecipe recipe,
            Set<String> itemIds,
            Set<String> fluidIds,
            Set<String> tagIds,
            Set<String> categoryIds) {
        if (recipe == null) {
            return;
        }
        if (recipe.getCategory() != null && recipe.getCategory().getId() != null) {
            categoryIds.add(recipe.getCategory().getId().toString());
        }
        for (EmiIngredient ingredient : Iterables.concat(
                recipe.getInputs(), recipe.getOutputs(), recipe.getCatalysts())) {
            collectFromIngredient(ingredient, itemIds, fluidIds, tagIds, null);
        }
    }

    public static void collectFromIngredient(
            EmiIngredient ingredient,
            Set<String> itemIds,
            Set<String> fluidIds,
            Set<String> tagIds,
            Map<String, ItemStack> iconVariants) {
        if (ingredient == null || ingredient.isEmpty()) {
            return;
        }
        JsonElement serialized = EmiIngredientSerializer.getSerialized(ingredient);
        collectSerializedTagRefs(normalizeIngredientJson(serialized), tagIds);
        collectReferencedStacks(ingredient, itemIds, fluidIds, iconVariants);
    }

    static void serializeWidgets(
            EmiRecipe recipe,
            List<Widget> widgets,
            Set<String> textureIds,
            Context ctx,
            Consumer<JsonObject> sink) {
        for (Widget widget : widgets) {
            if (shouldSkipWidget(widget)) {
                continue;
            }
            JsonObject json = serialize(recipe, widget, textureIds, ctx);
            if (json != null) {
                sink.accept(json);
            }
        }
    }

    private static boolean shouldSkipWidget(Widget widget) {
        return widget instanceof RecipeButtonWidget;
    }

    private static JsonObject serialize(EmiRecipe recipe, Widget widget, Set<String> textureIds, Context ctx) {
        try {
            if (ctx.chromeRoot() == null) {
                if (isRootWidget(widget)
                        || isDrawableWidget(widget)) {
                    return null;
                }
                if (!(widget instanceof SlotWidget)
                        && !(widget instanceof TankWidget)
                        && !(widget instanceof TooltipWidget)) {
                    return null;
                }
            }
            if (isRootWidget(widget)) {
                return rootChrome(widget, ctx);
            }
            if (isDrawableWidget(widget)) {
                return drawableChrome(widget, ctx);
            }
            if (widget instanceof TankWidget tank) {
                return slotLike(recipe, tank, "tank", textureIds, true, ctx);
            }
            if (widget instanceof SlotWidget slot) {
                return slotLike(recipe, slot, "slot", textureIds, false, ctx);
            }
            if (widget instanceof FillingArrowWidget arrow) {
                return ctx.chromeRoot() == null ? null : fillingArrow(arrow, textureIds);
            }
            if (widget instanceof AnimatedTextureWidget animated) {
                return ctx.chromeRoot() == null ? null : animatedTexture(animated, textureIds);
            }
            if (widget instanceof TextureWidget texture) {
                return ctx.chromeRoot() == null ? null : texture(texture, textureIds);
            }
            if (widget instanceof TextWidget text) {
                return ctx.chromeRoot() == null ? null : textWidget(text);
            }
            if (widget instanceof TooltipWidget tooltip) {
                return tooltip(tooltip);
            }
            if (ctx.chromeRoot() == null) {
                return null;
            }
            return rasterChrome(widget, ctx, "raster");
        } catch (Exception e) {
            Log.detailFailure(1,
                    "{} widget {} failed: {}",
                    Log.EMI_LAYOUT,
                    widget.getClass().getName(),
                    e);
            throw new RuntimeException("widget export failed: " + widget.getClass().getName(), e);
        }
    }

    private static JsonObject rootChrome(Widget widget, Context ctx) throws Exception {
        JsonObject object = boundsObject(widget.getBounds());
        object.addProperty("type", "root_chrome");
        attachChrome(object, widget, ctx);
        object.addProperty("javaClass", widget.getClass().getName());
        return object;
    }

    private static JsonObject drawableChrome(Widget widget, Context ctx) throws Exception {
        JsonObject object = boundsObject(widget.getBounds());
        object.addProperty("type", "drawable_raster");
        attachChrome(object, widget, ctx);
        object.addProperty("javaClass", widget.getClass().getName());
        return object;
    }

    private static JsonObject rasterChrome(Widget widget, Context ctx, String type) throws Exception {
        Bounds bounds = widget.getBounds();
        if (bounds == null || bounds.empty()) {
            return null;
        }
        JsonObject object = boundsObject(bounds);
        object.addProperty("type", type);
        attachChrome(object, widget, ctx);
        object.addProperty("javaClass", widget.getClass().getName());
        return object;
    }

    private static void attachChrome(JsonObject object, Widget widget, Context ctx) throws Exception {
        if (ctx.chromeRoot() == null) {
            return;
        }
        ChromeAsset asset = rasterizeWidget(
                ctx.client(), widget, ctx.chromeRoot(), ctx.chromeHashToRelative());
        object.addProperty("chrome", asset.exportPath());
        ctx.chromeWritten()[0]++;
        if (asset.deduplicated()) {
            ctx.chromeDeduped()[0]++;
        }
    }

    private static JsonObject slotLike(
            EmiRecipe recipe,
            SlotWidget slot,
            String type,
            Set<String> textureIds,
            boolean tank,
            Context ctx) {
        Bounds bounds = slot.getBounds();
        JsonObject object = boundsObject(bounds);
        object.addProperty("type", type);
        object.addProperty("role", inferSlotRole(slot));
        object.addProperty("large", readBooleanField(slot, "output"));
        object.addProperty("catalyst", readBooleanField(slot, "catalyst"));
        object.addProperty("drawBack", readBooleanField(slot, "drawBack"));
        object.addProperty("custom", readBooleanField(slot, "custom"));

        ResourceLocation customTexture = readField(slot, "textureId", ResourceLocation.class);
        if (customTexture != null) {
            object.addProperty("backgroundTexture", customTexture.toString());
            textureIds.add(customTexture.toString());
            Integer u = readField(slot, "u", Integer.class);
            Integer v = readField(slot, "v", Integer.class);
            if (u != null) {
                object.addProperty("backgroundU", u);
            }
            if (v != null) {
                object.addProperty("backgroundV", v);
            }
        }

        if (tank) {
            Long capacity = readField(slot, "capacity", Long.class);
            if (capacity != null) {
                object.addProperty("capacity", capacity);
            }
        }

        EmiIngredient stack = slot.getStack();
        if (stack != null && !stack.isEmpty()) {
            JsonElement ingredient = normalizeIngredientJson(EmiIngredientSerializer.getSerialized(stack));
            enrichIngredientIconKeys(ingredient, stack);
            object.add("ingredient", ingredient);
            collectSerializedTagRefs(ingredient, ctx.referencedTags());
            collectReferenced(stack, ctx.referencedItems(), ctx.referencedFluids(), ctx.iconVariants());
            attachRemainderHint(object, stack);
            attachTagDisplayItem(object, stack);
        }
        return object;
    }

    private static void attachRemainderHint(JsonObject object, EmiIngredient ingredient) {
        for (EmiStack stack : ingredient.getEmiStacks()) {
            EmiStack remainder = stack.getRemainder();
            if (!remainder.isEmpty()) {
                object.addProperty("remainderIcon", remainder.equals(stack) ? "self" : "other");
                return;
            }
        }
    }

    private static void attachTagDisplayItem(JsonObject object, EmiIngredient ingredient) {
        for (EmiStack stack : ingredient.getEmiStacks()) {
            String key = StackKey.forEmiStack(stack);
            if (key != null) {
                object.addProperty("tagDisplayItem", key);
                return;
            }
        }
    }

    private static void enrichIngredientIconKeys(JsonElement ingredientJson, EmiIngredient ingredient) {
        if (ingredientJson == null || ingredient == null) {
            return;
        }
        List<EmiStack> stacks = ingredient.getEmiStacks();
        if (ingredientJson.isJsonObject()) {
            attachIconKeyToObject(ingredientJson.getAsJsonObject(), stacks.isEmpty() ? null : stacks.get(0));
        } else if (ingredientJson.isJsonArray()) {
            JsonArray array = ingredientJson.getAsJsonArray();
            for (int i = 0; i < array.size() && i < stacks.size(); i++) {
                if (array.get(i).isJsonObject()) {
                    attachIconKeyToObject(array.get(i).getAsJsonObject(), stacks.get(i));
                }
            }
        }
    }

    private static void attachIconKeyToObject(JsonObject object, EmiStack stack) {
        if (stack == null) {
            return;
        }
        String key = StackKey.forEmiStack(stack);
        if (key != null && shouldAttachIconKey(object, key)) {
            object.addProperty("iconKey", key);
        }
    }

    private static boolean shouldAttachIconKey(JsonObject ingredientObject, String key) {
        if (StackKey.isVariantKey(key)) {
            return true;
        }
        if (ingredientObject == null || !ingredientObject.has("id") || !ingredientObject.get("id").isJsonPrimitive()) {
            return false;
        }
        return !key.equals(ingredientObject.get("id").getAsString());
    }

    private static void collectReferenced(
            EmiIngredient ingredient,
            Set<String> items,
            Set<String> fluids,
            Map<String, ItemStack> iconVariants) {
        for (EmiStack emiStack : ingredient.getEmiStacks()) {
            ResourceLocation id = emiStack.getId();
            if (id == null) {
                continue;
            }
            if (emiStack.getKey() instanceof Fluid) {
                fluids.add(id.toString());
            } else {
                String itemId = StackKey.itemIdForEmiStack(emiStack);
                if (itemId != null && !itemId.isBlank()) {
                    items.add(itemId);
                } else {
                    items.add(id.toString());
                }
                String iconKey = StackKey.forEmiStack(emiStack);
                if (iconVariants != null && StackKey.isVariantKey(iconKey)) {
                    ItemStack stack = StackKey.toItemStack(emiStack);
                    if (!stack.isEmpty()) {
                        iconVariants.put(iconKey, stack);
                    }
                }
            }
        }
    }

    static void collectReferencedStacks(
            EmiIngredient ingredient,
            Set<String> items,
            Set<String> fluids,
            Map<String, ItemStack> iconVariants) {
        collectReferenced(ingredient, items, fluids, iconVariants);
    }

    static JsonElement normalizeIngredientJson(JsonElement ingredient) {
        if (ingredient == null || !ingredient.isJsonObject()) {
            return ingredient;
        }
        JsonObject object = ingredient.getAsJsonObject();
        if (!object.has("type") || !object.get("type").isJsonPrimitive()) {
            return ingredient;
        }
        if (!object.has("id") || !object.get("id").isJsonPrimitive()) {
            return ingredient;
        }
        if (!"tag".equals(object.get("type").getAsString())) {
            return ingredient;
        }
        String id = object.get("id").getAsString();
        String registry = object.has("registry") && object.get("registry").isJsonPrimitive()
                ? object.get("registry").getAsString()
                : "minecraft:item";
        if (registry.contains("fluid")) {
            return new JsonPrimitive("#fluid:" + id);
        }
        if (registry.contains("block")) {
            return new JsonPrimitive("#block:" + id);
        }
        return new JsonPrimitive("#item:" + id);
    }

    static void collectSerializedTagRefs(JsonElement ingredient, Set<String> tags) {
        if (ingredient == null || ingredient.isJsonNull()) {
            return;
        }
        if (ingredient.isJsonPrimitive()) {
            String raw = ingredient.getAsString();
            if (raw.startsWith("#item:")) {
                tags.add(raw.substring(6));
            } else if (raw.startsWith("#block:")) {
                tags.add(raw.substring(7));
            } else if (raw.startsWith("#fluid:")) {
                tags.add(raw.substring(7));
            }
            return;
        }
        if (ingredient.isJsonArray()) {
            for (JsonElement child : ingredient.getAsJsonArray()) {
                collectSerializedTagRefs(child, tags);
            }
            return;
        }
        if (!ingredient.isJsonObject()) {
            return;
        }

        JsonObject object = ingredient.getAsJsonObject();
        if (object.has("type")
                && object.get("type").isJsonPrimitive()
                && "tag".equals(object.get("type").getAsString())
                && object.has("id")
                && object.get("id").isJsonPrimitive()) {
            tags.add(object.get("id").getAsString());
        }
        if (object.has("tag") && object.get("tag").isJsonPrimitive()) {
            tags.add(object.get("tag").getAsString());
        }
        if (object.has("entries") && object.get("entries").isJsonArray()) {
            for (JsonElement child : object.getAsJsonArray("entries")) {
                collectSerializedTagRefs(child, tags);
            }
        }
    }

    private static String inferSlotRole(SlotWidget slot) {
        if (readBooleanField(slot, "catalyst")) {
            return "catalyst";
        }
        if (slot.getRecipe() != null) {
            return "output";
        }
        return "input";
    }

    private static JsonObject texture(TextureWidget widget, Set<String> textureIds) {
        ResourceLocation texture = readField(widget, "texture", ResourceLocation.class);
        if (texture != null) {
            textureIds.add(texture.toString());
        }
        JsonObject object = boundsObject(widget.getBounds());
        object.addProperty("type", "texture");
        if (texture != null) {
            object.addProperty("texture", texture.toString());
        }
        putTextureFields(widget, object);
        return object;
    }

    private static JsonObject animatedTexture(AnimatedTextureWidget widget, Set<String> textureIds) {
        ResourceLocation texture = readField(widget, "texture", ResourceLocation.class);
        if (texture != null) {
            textureIds.add(texture.toString());
        }
        JsonObject object = boundsObject(widget.getBounds());
        object.addProperty("type", "animated_texture");
        if (texture != null) {
            object.addProperty("texture", texture.toString());
        }
        putTextureFields(widget, object);
        Integer time = readField(widget, "time", Integer.class);
        if (time != null) {
            object.addProperty("time", time);
        }
        object.addProperty("horizontal", readBooleanField(widget, "horizontal"));
        object.addProperty("endToStart", readBooleanField(widget, "endToStart"));
        object.addProperty("fullToEmpty", readBooleanField(widget, "fullToEmpty"));
        return object;
    }

    private static JsonObject fillingArrow(FillingArrowWidget widget, Set<String> textureIds) {
        textureIds.add("emi:textures/gui/widgets.png");
        JsonObject object = boundsObject(widget.getBounds());
        object.addProperty("type", "filling_arrow");
        Integer time = readField(widget, "time", Integer.class);
        if (time != null) {
            object.addProperty("time", time);
        }
        return object;
    }

    private static JsonObject textWidget(TextWidget widget) {
        JsonObject object = boundsObject(widget.getBounds());
        object.addProperty("type", "text");
        FormattedCharSequence text = readField(widget, "text", FormattedCharSequence.class);
        if (text != null) {
            String plain = formattedCharSequenceToString(text);
            if (!plain.isEmpty()) {
                object.addProperty("text", plain);
            }
        }
        Integer color = readField(widget, "color", Integer.class);
        if (color != null) {
            object.addProperty("color", color);
        }
        object.addProperty("shadow", readBooleanField(widget, "shadow"));
        TextWidget.Alignment horizontal = readField(widget, "horizontalAlignment", TextWidget.Alignment.class);
        TextWidget.Alignment vertical = readField(widget, "verticalAlignment", TextWidget.Alignment.class);
        if (horizontal != null) {
            object.addProperty("horizontalAlign", horizontal.name());
        }
        if (vertical != null) {
            object.addProperty("verticalAlign", vertical.name());
        }
        Integer baseX = readField(widget, "x", Integer.class);
        Integer baseY = readField(widget, "y", Integer.class);
        if (baseX != null) {
            object.addProperty("baseX", baseX);
        }
        if (baseY != null) {
            object.addProperty("baseY", baseY);
        }
        return object;
    }

    private static JsonObject tooltip(TooltipWidget widget) {
        JsonObject object = boundsObject(widget.getBounds());
        object.addProperty("type", "tooltip");
        return object;
    }

    private static void putTextureFields(TextureWidget widget, JsonObject object) {
        Integer u = readField(widget, "u", Integer.class);
        Integer v = readField(widget, "v", Integer.class);
        Integer textureWidth = readField(widget, "textureWidth", Integer.class);
        Integer textureHeight = readField(widget, "textureHeight", Integer.class);
        Integer regionWidth = readField(widget, "regionWidth", Integer.class);
        Integer regionHeight = readField(widget, "regionHeight", Integer.class);
        if (u != null) {
            object.addProperty("u", u);
        }
        if (v != null) {
            object.addProperty("v", v);
        }
        if (textureWidth != null) {
            object.addProperty("texW", textureWidth);
        }
        if (textureHeight != null) {
            object.addProperty("texH", textureHeight);
        }
        if (regionWidth != null) {
            object.addProperty("regionW", regionWidth);
        }
        if (regionHeight != null) {
            object.addProperty("regionH", regionHeight);
        }
    }

    private static JsonObject boundsObject(Bounds bounds) {
        JsonObject object = new JsonObject();
        object.addProperty("x", bounds.x());
        object.addProperty("y", bounds.y());
        object.addProperty("w", bounds.width());
        object.addProperty("h", bounds.height());
        return object;
    }

    private static String formattedCharSequenceToString(FormattedCharSequence text) {
        StringBuilder builder = new StringBuilder();
        text.accept((index, style, codePoint) -> {
            builder.appendCodePoint(codePoint);
            return true;
        });
        return builder.toString();
    }

    private static boolean readBooleanField(Object target, String name) {
        Boolean value = readField(target, name, Boolean.class);
        return value != null && value;
    }

    @SuppressWarnings("unchecked")
    private static <T> T readField(Object target, String name, Class<T> type) {
        Class<?> current = target.getClass();
        while (current != null) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                Object value = field.get(target);
                if (value == null) {
                    return null;
                }
                return type.cast(value);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private static ChromeAsset rasterizeWidget(
            Minecraft client,
            Widget widget,
            Path chromeRoot,
            Map<String, String> hashToRelative) throws IOException {
        Bounds bounds = widget.getBounds();
        if (bounds == null || bounds.empty()) {
            throw new IOException("widget bounds empty: " + widget.getClass().getName());
        }
        int width = Math.max(1, bounds.width());
        int height = Math.max(1, bounds.height());
        int scale = LayoutBuilder.layoutScale();
        int pixelWidth = width * scale;
        int pixelHeight = height * scale;

        try (OffScreenRenderer offScreen = new OffScreenRenderer(pixelWidth, pixelHeight)) {
            GuiGraphics graphics = new GuiGraphics(client, client.renderBuffers().bufferSource());
            byte[] png = offScreen.captureAsPng(() -> offScreen.runWithEmiRecipeMatrices(width, height, () -> {
                graphics.pose().pushPose();
                graphics.pose().translate(-bounds.x(), -bounds.y(), 0);
                widget.render(graphics, -10_000, -10_000, 0);
                graphics.flush();
                graphics.pose().popPose();
            }));

            String hash = sha256(png);
            String relative = hashToRelative.get(hash);
            boolean deduped = relative != null;
            if (relative == null) {
                relative = "sh/" + hash.substring(0, 2) + "/" + hash + ".png";
                Path out = chromeRoot.resolve(relative);
                Files.createDirectories(out.getParent());
                Files.write(out, png);
                hashToRelative.put(hash, relative);
            }
            String exportPath = Constants.CHROME_DIR + "/" + relative.replace('\\', '/');
            return new ChromeAsset(exportPath, deduped);
        }
    }

    private static boolean isRootWidget(Widget widget) {
        return widget.getClass().getName().endsWith("RootWidget");
    }

    private static boolean isDrawableWidget(Widget widget) {
        if (widget instanceof DrawableWidget) {
            return true;
        }
        return widget.getClass().getName().endsWith(".DrawableWidget");
    }

    private static String sha256(byte[] data) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(data);
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 unavailable", e);
        }
    }
}
