package com.danpan1232.danshardermobs.util;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.Set;

public record HostileArmorVariantConfig(
        boolean disabled,
        float baseRollChance,
        float dropRate,
        float damagePercentage,
        boolean enchantable,
        int mobMinLevel,
        int mobMaxLevel,
        int enchantmentMinLevel,
        int enchantmentMaxLevel,
        boolean ignoreEnchantmentCompatibility,
        Set<ResourceKey<Enchantment>> blacklistEnchantments,
        int tier
) {
    public static final HostileArmorVariantConfig DEFAULT = new HostileArmorVariantConfig(false, 0.5f, 0.1F, 0.9f, true, -1, -1, -1, -1, false, Set.of(), 1);
}