package com.danpan1232.danshardermobs.util;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.Set;

public record HostileWeaponVariantConfig(
    boolean disabled,
    float baseRollChance,
    float dropRate,
    float damagePercentage,
    boolean enchantable,
    float playerLevelPercentMin,
    float playerLevelPercentMax,
    int enchantmentMinLevel,
    int enchantmentMaxLevel,
    boolean ignoreEnchantmentCompatibility,
    Set<ResourceKey<Enchantment>> blacklistEnchantments,
    int tier
) {

    public static final HostileWeaponVariantConfig DEFAULT = new HostileWeaponVariantConfig(false, 0.5f, 0.1f, 0.9f, true, -1, -1, -1, -1, false, Set.of(), 1);

}