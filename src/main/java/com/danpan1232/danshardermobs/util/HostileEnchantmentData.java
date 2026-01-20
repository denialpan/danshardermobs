package com.danpan1232.danshardermobs.util;


import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public final class HostileEnchantmentData {

    private static final Map<ResourceLocation, HostileEnchantmentConfig> ENTRIES = new HashMap<>();
    private static HostileEnchantmentConfig DEFAULT = HostileEnchantmentConfig.DEFAULT;

    public static void clear() {
        ENTRIES.clear();
    }

    public static void setDefault(HostileEnchantmentConfig cfg) {
        DEFAULT = cfg;
    }

    public static HostileEnchantmentConfig defaults() {
        return DEFAULT;
    }

    public static void put(ResourceLocation id, HostileEnchantmentConfig cfg) {
        ENTRIES.put(id, cfg);
    }

    public static HostileEnchantmentConfig get(ResourceLocation id) {
        return ENTRIES.getOrDefault(id, DEFAULT);
    }

    public static Map<ResourceLocation, HostileEnchantmentConfig> getAll() {
        return ENTRIES;
    }
}