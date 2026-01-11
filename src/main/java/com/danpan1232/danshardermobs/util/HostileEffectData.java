package com.danpan1232.danshardermobs.util;

import net.minecraft.world.effect.MobEffect;

import java.util.HashSet;
import java.util.Set;

public final class HostileEffectData {

    private static final Set<MobEffect> EFFECTS = new HashSet<>();

    public static void clear() {
        EFFECTS.clear();
    }

    public static void add(MobEffect effect) {
        EFFECTS.add(effect);
    }

    public static boolean isEmpty() {
        return EFFECTS.isEmpty();
    }

    public static Set<MobEffect> getAll() {
        return EFFECTS;
    }

}
