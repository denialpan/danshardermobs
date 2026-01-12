package com.danpan1232.danshardermobs.util;

public record HostileEffectConfig(
        boolean disabled,
        float baseRollChance
) {
    public static final HostileEffectConfig DEFAULT =
            new HostileEffectConfig(false,1.0F);
}
