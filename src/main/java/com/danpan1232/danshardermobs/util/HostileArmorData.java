package com.danpan1232.danshardermobs.util;

import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.Map;

public final class HostileArmorData {

    private static final Map<Item, HostileArmorConfig> ARMOR = new HashMap<>();
    private static HostileArmorConfig DEFAULT = HostileArmorConfig.DEFAULT;

    public static void clear() {
        ARMOR.clear();
        DEFAULT = HostileArmorConfig.DEFAULT;
    }

    public static void setDefault(HostileArmorConfig config) {
        DEFAULT = config;
    }

    public static void put(Item item, HostileArmorConfig config) {
        ARMOR.put(item, config);
    }

    public static HostileArmorConfig get(Item item) {
        return ARMOR.getOrDefault(item, DEFAULT);
    }

    public static Map<Item, HostileArmorConfig> getAll() {
        return ARMOR;
    }
}
