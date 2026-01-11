package com.danpan1232.danshardermobs.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class HostileEntityData {

    private static final Map<ResourceLocation, HostileEntityConfig> ENTITY_CONFIGS = new HashMap<>();
    private static HostileEntityConfig DEFAULT = new HostileEntityConfig(1, 1.0f, 50, -1, 1.0F, false);

    public static void clear() {
        ENTITY_CONFIGS.clear();
    }

    public static void setDefault(HostileEntityConfig config) {
        DEFAULT = config;
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
