package com.danpan1232.danshardermobs.util;

import com.danpan1232.danshardermobs.danshardermobs;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HostileWeaponLoader extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new Gson();

    public HostileWeaponLoader() {
        super(GSON, "danshardermobs");
    }

    @Override
    protected void apply(
            Map<ResourceLocation, JsonElement> objects,
            ResourceManager resourceManager,
            ProfilerFiller profiler
    ) {
        HostileWeaponData.clear();

        for (var entry : objects.entrySet()) {
            JsonObject root = entry.getValue().getAsJsonObject();
            JsonObject weapons = root.getAsJsonObject("weapons");
            if (weapons == null) continue;

            if (weapons.has("default")) {
                HostileWeaponData.setDefaults(parseDefaults(weapons.getAsJsonObject("default")));
            }

            for (var weaponEntry : weapons.entrySet()) {
                if (weaponEntry.getKey().equals("default")) continue;

                ResourceLocation id = ResourceLocation.tryParse(weaponEntry.getKey());
                if (id == null) continue;

                JsonObject obj = weaponEntry.getValue().getAsJsonObject();
                HostileWeaponStackConfig config = parseWeaponEntry(obj, HostileWeaponData.defaults());

                Item item = BuiltInRegistries.ITEM.get(id);
                if (item == Items.AIR) {
                    danshardermobs.LOGGER.warn("Unknown weapon '{}'", id);
                    continue;
                }


                HostileWeaponData.put(item, config);
            }
        }
    }

    private HostileWeaponVariantConfig parseDefaults(JsonObject obj) {
        return new HostileWeaponVariantConfig(
            obj.has("disabled") ? obj.get("disabled").getAsBoolean() : false,
            obj.has("base_roll_chance") ? obj.get("base_roll_chance").getAsFloat() : 0.5f,
            obj.has("enchantable") ? obj.get("enchantable").getAsBoolean() : true,
            obj.has("ignore_enchantment_compatibility") ? obj.get("ignore_enchantment_compatibility").getAsBoolean() : true,
            obj.has("tier") ? obj.get("tier").getAsInt() : 1
        );
    }

    private HostileWeaponStackConfig parseWeaponEntry(JsonObject obj, HostileWeaponVariantConfig defaults) {
        List<HostileWeaponVariantConfig> variants = new ArrayList<>();

        if (obj.has("variants")) {
            JsonArray arr = obj.getAsJsonArray("variants");

            for (JsonElement e : arr) {
                variants.add(parseVariant(e.getAsJsonObject(), defaults));
            }
        }

        return new HostileWeaponStackConfig(variants);
    }

    private HostileWeaponVariantConfig parseVariant(JsonObject obj, HostileWeaponVariantConfig d) {
        return new HostileWeaponVariantConfig(
            obj.has("disabled") ? obj.get("disabled").getAsBoolean() : d.disabled(),
            obj.has("base_roll_chance") ? obj.get("base_roll_chance").getAsFloat() : d.baseRollChance(),
            obj.has("enchantable") ? obj.get("enchantable").getAsBoolean() : d.enchantable(),
            obj.has("ignore_enchantment_compatibility") ? obj.get("ignore_enchantment_compatibility").getAsBoolean() : d.ignoreEnchantmentCompatibility(),
            obj.has("tier") ? obj.get("tier").getAsInt() : d.tier()
        );
    }
}