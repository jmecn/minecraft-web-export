package io.github.jmecn.minecraftwebexport.export.emi;

import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.ItemEmiStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class IconStackKey {

    private static final HexFormat HEX = HexFormat.of();
    public static final int HASH_LEN = 16;

    private IconStackKey() {
    }

    public static String forEmiStack(EmiStack stack) {
        String itemId = itemIdForEmiStack(stack);
        if (itemId != null) {
            String fingerprint = itemStackFingerprint(toItemStack(stack));
            return forItemIdAndNbtSnbt(itemId, fingerprint);
        }

        ResourceLocation id = stack.getId();
        if (id == null) {
            return null;
        }
        if (!stack.hasNbt()) {
            return id.toString();
        }
        return forItemIdAndNbtSnbt(id, nbtSnbt(stack.getNbt()));
    }

    public static String forItemIdAndNbtSnbt(ResourceLocation id, String nbtSnbt) {
        if (id == null) {
            return null;
        }
        return forItemIdAndNbtSnbt(id.toString(), nbtSnbt);
    }

    public static String forItemIdAndNbtSnbt(String itemId, String nbtSnbt) {
        if (itemId == null || itemId.isBlank()) {
            return null;
        }
        if (nbtSnbt == null || nbtSnbt.isEmpty()) {
            return itemId;
        }
        return itemId + "@" + hashString(nbtSnbt);
    }

    public static boolean isVariantKey(String key) {
        return key != null && key.indexOf('@') > 0;
    }

    public static String hashString(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HEX.formatHex(hash, 0, HASH_LEN / 2);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private static String nbtSnbt(CompoundTag nbt) {
        return nbt == null || nbt.isEmpty() ? "" : nbt.toString();
    }

    public static String itemIdForEmiStack(EmiStack emiStack) {
        ItemStack stack = toItemStack(emiStack);
        if (!stack.isEmpty() && stack.getItem() != Items.AIR) {
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (itemId != null) {
                return itemId.toString();
            }
        }
        ResourceLocation rawId = emiStack.getId();
        return rawId != null ? rawId.toString() : null;
    }

    private static String itemStackFingerprint(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTag()) {
            return "";
        }
        CompoundTag exported = stack.save(new CompoundTag());
        return exported.isEmpty() ? "" : exported.toString();
    }

    public static ItemStack toItemStack(EmiStack emiStack) {
        if (emiStack instanceof ItemEmiStack itemEmi) {
            ItemStack stack = itemEmi.getItemStack();
            return stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
        }
        if (emiStack.getKey() instanceof Item item && item != Items.AIR) {
            ItemStack stack = new ItemStack(item, Math.max(1, (int) emiStack.getAmount()));
            if (emiStack.hasNbt()) {
                stack.setTag(emiStack.getNbt().copy());
            }
            return stack;
        }
        return ItemStack.EMPTY;
    }
}
