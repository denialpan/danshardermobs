package com.danpan1232.danshardermobs.util;

import net.minecraft.world.effect.MobEffect;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class HostileEffectData {

    private static final Map<MobEffect, HostileEffectConfig> EFFECTS = new HashMap<>();
    private static HostileEffectConfig DEFAULT = HostileEffectConfig.DEFAULT;

    public static void clear() {
        EFFECTS.clear();
        DEFAULT = HostileEffectConfig.DEFAULT;
    }

    public static void setDefault(HostileEffectConfig config) {
        DEFAULT = config;
    }

    public static void put(MobEffect effect, HostileEffectConfig config) {
        EFFECTS.put(effect, config);
    }

    public static Map<MobEffect, HostileEffectConfig> getAll() {
        return EFFECTS;
    }
}
