package io.github.jmecn.minecraftwebexport.emi.creativetab;

import io.github.jmecn.minecraftwebexport.emi.lang.LangDerivation;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

final class CreativeTabSupport {

    private CreativeTabSupport() {
    }

    static boolean shouldExportTab(ResourceKey<CreativeModeTab> tabKey) {
        if (tabKey == null) {
            return false;
        }
        return tabKey != CreativeModeTabs.SEARCH
                && tabKey != CreativeModeTabs.HOTBAR
                && tabKey != CreativeModeTabs.INVENTORY;
    }

    static String tabId(ResourceKey<CreativeModeTab> tabKey) {
        return tabKey.location().toString();
    }

    static int tabOrder(CreativeModeTab tab) {
        return tab.row().ordinal() * 1_000 + tab.column();
    }

    static String resolveNameKey(CreativeModeTab tab) {
        String fromComponent = LangDerivation.translationKeyFromComponent(tab.getDisplayName());
        if (fromComponent != null) {
            return fromComponent;
        }
        ResourceLocation id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
        if (id == null) {
            return "emi.creative_tab.unknown";
        }
        return "emi.creative_tab." + id.getNamespace() + "." + id.getPath().replace('/', '.');
    }

    static Set<String> collectMemberItemIds(Iterable<ItemStack> stacks) {
        Set<String> itemIds = new TreeSet<>();
        if (stacks == null) {
            return itemIds;
        }
        for (ItemStack stack : stacks) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            Item item = stack.getItem();
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
            if (id == null) {
                continue;
            }
            String itemId = id.toString();
            if (isExportableRegistryId(itemId)) {
                itemIds.add(itemId);
            }
        }
        return itemIds;
    }

    static boolean isExportableRegistryId(String id) {
        if (id == null || id.isBlank() || id.startsWith("#")) {
            return false;
        }
        int colon = id.indexOf(':');
        if (colon <= 0 || colon >= id.length() - 1) {
            return false;
        }
        return !id.substring(0, colon).startsWith("#");
    }
}
