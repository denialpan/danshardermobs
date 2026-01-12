package com.danpan1232.danshardermobs.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

public final class HostileWeaponData {

    private static final Map<Item, HostileWeaponStackConfig> ENTRIES = new HashMap<>();
    private static HostileWeaponVariantConfig DEFAULT = HostileWeaponVariantConfig.DEFAULT;

    public static void clear() {
        ENTRIES.clear();
    }

    public static void setDefaults(HostileWeaponVariantConfig config) {
        DEFAULT = config;
    }

    public static void put(Item item, HostileWeaponStackConfig entry) {
        ENTRIES.put(item, entry);
    }

    public static HostileWeaponVariantConfig defaults() {
        return DEFAULT;
    }

    public static HostileWeaponStackConfig get(ResourceLocation id) {
        return ENTRIES.get(id);
    }

    public static boolean has(ResourceLocation id) {
        return ENTRIES.containsKey(id);
    }

    public static Map<Item, HostileWeaponStackConfig> getAll() {
        return ENTRIES;
    }

    public static int getTier(ItemStack stack) {
        if (stack.isEmpty()) return 0;

        Item item = stack.getItem();
        HostileWeaponStackConfig cfg = HostileWeaponData.get(BuiltInRegistries.ITEM.getKey(item));
        if (cfg == null || cfg.variants().isEmpty()) return 0;

        // TODO: assume first variant represents the equipped tier, likely change though
        return cfg.variants().get(0).tier();
    }
}
