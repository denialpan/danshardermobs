package com.danpan1232.danshardermobs.util;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public final class HostileEntityData {

    private static final Map<ResourceLocation, HostileEntityConfig> ENTITY_CONFIGS = new HashMap<>();
    private static HostileEntityConfig DEFAULT = HostileEntityConfig.DEFAULT;

    public static void clear() {
        ENTITY_CONFIGS.clear();
    }

    public static void setDefault(HostileEntityConfig config) {
        DEFAULT = config;
    }

    public static HostileEntityConfig defaults() {
        return DEFAULT;
    }

    public static void put(ResourceLocation id, HostileEntityConfig config) {
        ENTITY_CONFIGS.put(id, config);
    }

    public static boolean isHostile(ResourceLocation id) {
        return ENTITY_CONFIGS.containsKey(id);
    }

    public static HostileEntityConfig get(ResourceLocation id) {
        return ENTITY_CONFIGS.getOrDefault(id, DEFAULT);
    }

}
