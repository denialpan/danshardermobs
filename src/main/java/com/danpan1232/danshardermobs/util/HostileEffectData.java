package com.danpan1232.danshardermobs.util;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public final class HostileEffectData {

    private static final Map<ResourceLocation, HostileEffectConfig> EFFECTS = new HashMap<>();
    private static HostileEffectConfig DEFAULT = HostileEffectConfig.DEFAULT;

    public static void clear() {
        EFFECTS.clear();
        DEFAULT = HostileEffectConfig.DEFAULT;
    }

    public static void setDefault(HostileEffectConfig config) {
        DEFAULT = config;
    }

    public static HostileEffectConfig defaults() {
        return DEFAULT;
    }

    public static void put(ResourceLocation effect, HostileEffectConfig config) {
        EFFECTS.put(effect, config);
    }

    public static Map<ResourceLocation, HostileEffectConfig> getAll() {
        return EFFECTS;
    }
}
