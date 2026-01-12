package com.danpan1232.danshardermobs.util;

public record HostileArmorConfig(
        boolean disabled,
        float baseRollChance,
        int tier
) {
    public static final HostileArmorConfig DEFAULT = new HostileArmorConfig(false, 0.5f, 1);
}