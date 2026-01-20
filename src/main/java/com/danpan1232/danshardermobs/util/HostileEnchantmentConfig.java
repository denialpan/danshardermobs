package com.danpan1232.danshardermobs.util;

public record HostileEnchantmentConfig(
    boolean disabled,
    float baseRollChance
) {
    public static final HostileEnchantmentConfig DEFAULT = new HostileEnchantmentConfig(false, 0.5F);
}
