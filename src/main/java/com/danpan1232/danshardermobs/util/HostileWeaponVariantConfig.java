package com.danpan1232.danshardermobs.util;

public record HostileWeaponVariantConfig(
    boolean disabled,
    float baseRollChance,
    boolean enchantable,
    boolean ignoreEnchantmentCompatibility,
    int tier
) {

    public static final HostileWeaponVariantConfig DEFAULT = new HostileWeaponVariantConfig(false, 0.5F, true, false, 1);

}