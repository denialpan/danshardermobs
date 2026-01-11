package com.danpan1232.danshardermobs.util;

public record HostileArmorConfig(
        float baseRollChance,
        int tier
) {
    public static final HostileArmorConfig DEFAULT = new HostileArmorConfig(0.5f, 1);
}