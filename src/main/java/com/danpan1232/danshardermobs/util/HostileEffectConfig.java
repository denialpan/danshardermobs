package com.danpan1232.danshardermobs.util;

public record HostileEffectConfig(
        float baseRollChance
) {
    public static final HostileEffectConfig DEFAULT =
            new HostileEffectConfig(1.0F);
}
