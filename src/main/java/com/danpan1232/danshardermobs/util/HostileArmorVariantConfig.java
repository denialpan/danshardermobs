package com.danpan1232.danshardermobs.util;

public record HostileArmorVariantConfig(
        boolean disabled,
        float baseRollChance,
        boolean enchantable,
        int tier
) {
    public static final HostileArmorVariantConfig DEFAULT = new HostileArmorVariantConfig(false, 0.5f, true, 1);
}