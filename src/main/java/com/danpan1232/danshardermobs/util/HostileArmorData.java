package com.danpan1232.danshardermobs.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

public final class HostileArmorData {

    private static final Map<Item, HostileArmorStackConfig> ENTRIES = new HashMap<>();
    private static HostileArmorVariantConfig DEFAULT = HostileArmorVariantConfig.DEFAULT;

    public static void clear() {
        ENTRIES.clear();
    }

    public static void setDefaults(HostileArmorVariantConfig config) {
        DEFAULT = config;
    }

    public static void put(Item item, HostileArmorStackConfig entry) {
        ENTRIES.put(item, entry);
    }

    public static HostileArmorVariantConfig defaults() {
        return DEFAULT;
    }

    public static HostileArmorStackConfig get(ResourceLocation id) {
        return ENTRIES.get(id);
    }

    public static boolean has(ResourceLocation id) {
        return ENTRIES.containsKey(id);
    }

    public static Map<Item, HostileArmorStackConfig> getAll() {
        return ENTRIES;
    }

    public static int getTier(ItemStack stack) {
        if (stack.isEmpty()) return 0;

        Item item = stack.getItem();
        HostileArmorStackConfig cfg = HostileArmorData.get(BuiltInRegistries.ITEM.getKey(item));
        if (cfg == null || cfg.variants().isEmpty()) return 0;

        // TODO: assume first variant represents the equipped tier, likely change though
        return cfg.variants().get(0).tier();
    }

}
