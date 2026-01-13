package com.danpan1232.danshardermobs.util;


import com.danpan1232.danshardermobs.danshardermobs;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.*;


public class HostileArmorLoader extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new Gson();

    public HostileArmorLoader() {
        super(GSON, "danshardermobs");
    }

    @Override
    protected void apply(
            Map<ResourceLocation, JsonElement> objects,
            ResourceManager resourceManager,
            ProfilerFiller profiler
    ) {

        HostileArmorData.clear();

        for (var entry : objects.entrySet()) {

            JsonObject root = entry.getValue().getAsJsonObject();
            JsonObject armor = root.getAsJsonObject("armor");

            if (armor == null) continue;

            if (armor.has("default")) {
                HostileArmorData.setDefaults(parseDefaults(armor.getAsJsonObject("default")));
            }

            for (var armorEntry : armor.entrySet()) {

                if (armorEntry.getKey().equals("default")) continue;

                ResourceLocation id = ResourceLocation.tryParse(armorEntry.getKey());
                if (id == null) continue;

                JsonObject obj = armorEntry.getValue().getAsJsonObject();
                HostileArmorStackConfig config = parseArmorEntry(obj, HostileArmorData.defaults());

                Item item = BuiltInRegistries.ITEM.get(id);
                if (item == Items.AIR) {
                    danshardermobs.LOGGER.warn("Unknown armor '{}'", id);
                    continue;
                }

                HostileArmorData.put(item, config);

            }

        }

    }

    private HostileArmorVariantConfig parseDefaults(JsonObject obj) {
        return new HostileArmorVariantConfig(
            obj.has("disabled") ? obj.get("disabled").getAsBoolean() : false,
            obj.has("base_roll_chance") ? obj.get("base_roll_chance").getAsFloat() : 0.5f,
            obj.has("enchantable") ? obj.get("enchantable").getAsBoolean() : true,
            obj.has("mob_min_level") ? obj.get("mob_min_level").getAsInt() : -1,
            obj.has("mob_max_level") ? obj.get("mob_max_level").getAsInt() : -1,
            obj.has("enchantment_min_level") ? obj.get("enchantment_min_level").getAsInt() : -1,
            obj.has("enchantment_max_level") ? obj.get("enchantment_max_level").getAsInt() : -1,
            obj.has("ignore_enchantment_compatibility") ? obj.get("ignore_enchantment_compatibility").getAsBoolean() : false,
            obj.has("blacklist_enchantments") ? parseEnchantmentBlacklist(obj) : Set.of(),
            obj.has("tier") ? obj.get("tier").getAsInt() : 1
        );
    }

    private HostileArmorStackConfig parseArmorEntry(JsonObject obj, HostileArmorVariantConfig defaults) {
        List<HostileArmorVariantConfig> variants = new ArrayList<>();

        if (obj.has("variants")) {
            JsonArray arr = obj.getAsJsonArray("variants");

            for (JsonElement e : arr) {
                variants.add(parseVariant(e.getAsJsonObject(), defaults));
            }
        } else {

            // disable by omission
            return new HostileArmorStackConfig(List.of());
        }

        return new HostileArmorStackConfig(variants);
    }

    private HostileArmorVariantConfig parseVariant(JsonObject obj, HostileArmorVariantConfig defaults) {
        return new HostileArmorVariantConfig(
            obj.has("disabled") ? obj.get("disabled").getAsBoolean() : defaults.disabled(),
            obj.has("base_roll_chance") ? obj.get("base_roll_chance").getAsFloat() : defaults.baseRollChance(),
            obj.has("enchantable") ? obj.get("enchantable").getAsBoolean() : defaults.enchantable(),
            obj.has("mob_min_level") ? obj.get("mob_min_level").getAsInt() : defaults.mobMinLevel(),
            obj.has("mob_max_level") ? obj.get("mob_max_level").getAsInt() : defaults.mobMaxLevel(),
            obj.has("enchantment_min_level") ? obj.get("enchantment_min_level").getAsInt() : defaults.enchantmentMinLevel(),
            obj.has("enchantment_max_level") ? obj.get("enchantment_max_level").getAsInt() : defaults.enchantmentMaxLevel(),
            obj.has("ignore_enchantment_compatibility") ? obj.get("ignore_enchantment_compatibility").getAsBoolean() : defaults.ignoreEnchantmentCompatibility(),
            obj.has("blacklist_enchantments") ? parseEnchantmentBlacklist(obj) : defaults.blacklistEnchantments(),
            obj.has("tier") ? obj.get("tier").getAsInt() : defaults.tier()
        );
    }

    private static Set<ResourceKey<Enchantment>> parseEnchantmentBlacklist(JsonObject obj) {
        Set<ResourceKey<Enchantment>> out = new HashSet<>();

        if (!obj.has("blacklist_enchantments")) return out;

        JsonArray arr = obj.getAsJsonArray("blacklist_enchantments");
        for (JsonElement e : arr) {
            ResourceLocation id = ResourceLocation.tryParse(e.getAsString());
            if (id == null) continue;

            out.add(ResourceKey.create(Registries.ENCHANTMENT, id));
        }

        return out;
    }


}
