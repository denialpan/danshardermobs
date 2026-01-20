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

public class HostileWeaponLoader extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new Gson();

    public HostileWeaponLoader() {
        super(GSON, "weapons");
    }

    @Override
    protected void apply(
            Map<ResourceLocation, JsonElement> objects,
            ResourceManager resourceManager,
            ProfilerFiller profiler
    ) {
        HostileWeaponData.clear();

        // default json
        JsonElement defaultJson = objects.get(
                ResourceLocation.fromNamespaceAndPath(danshardermobs.MODID, "default")
        );

        HostileWeaponVariantConfig defaults = defaultJson != null ? parseDefaults(defaultJson.getAsJsonObject()) : HostileWeaponVariantConfig.DEFAULT;
        HostileWeaponData.setDefaults(defaults);

        // <item>/*json
        for (var entry : objects.entrySet()) {
            ResourceLocation fileId = entry.getKey();
            String path = fileId.getPath();

            if (path.equals("default")) continue;

            // <namespace>/<item>
            int slash = path.indexOf('/');
            if (slash <= 0) continue;

            String itemNamespace = path.substring(0, slash);
            String itemPath = path.substring(slash + 1);

            ResourceLocation itemId = ResourceLocation.tryParse(itemNamespace + ":" + itemPath);

            if (itemId == null) continue;

            Item item = BuiltInRegistries.ITEM.get(itemId);
            if (item == Items.AIR) {
                danshardermobs.LOGGER.warn("Unknown weapon '{}'", itemId);
                continue;
            }

            HostileWeaponStackConfig stack = parseWeaponFile(entry.getValue().getAsJsonObject(), defaults);
            HostileWeaponData.put(item, stack);
        }

    }

    private HostileWeaponVariantConfig parseDefaults(JsonObject obj) {
        return new HostileWeaponVariantConfig(
            obj.has("disabled") ? obj.get("disabled").getAsBoolean() : false,
            obj.has("base_roll_chance") ? obj.get("base_roll_chance").getAsFloat() : 0.5f,
            obj.has("drop_rate") ? obj.get("drop_rate").getAsFloat() : 0.1f,
            obj.has("damage_percentage") ? obj.get("damage_percentage").getAsFloat() : 0.9f,
            obj.has("enchantable") ? obj.get("enchantable").getAsBoolean() : true,
            obj.has("player_level_percent_min") ? obj.get("player_level_percent_min").getAsFloat() : -1,
            obj.has("player_level_percent_max") ? obj.get("player_level_percent_max").getAsFloat() : -1,
            obj.has("enchantment_min_level") ? obj.get("enchantment_min_level").getAsInt() : -1,
            obj.has("enchantment_max_level") ? obj.get("enchantment_max_level").getAsInt() : -1,
            obj.has("ignore_enchantment_compatibility") ? obj.get("ignore_enchantment_compatibility").getAsBoolean() : false,
            obj.has("blacklist_enchantments") ? parseEnchantmentBlacklist(obj) : Set.of(),
            obj.has("tier") ? obj.get("tier").getAsInt() : 1
        );
    }

    private HostileWeaponStackConfig parseWeaponFile(JsonObject obj, HostileWeaponVariantConfig defaults) {

        if (!obj.has("variants")) {
            return new HostileWeaponStackConfig(List.of());
        }

        List<HostileWeaponVariantConfig> variants = new ArrayList<>();
        JsonArray arr = obj.getAsJsonArray("variants");

        for (JsonElement e : arr) {
            variants.add(parseVariant(e.getAsJsonObject(), defaults));
        }

        return new HostileWeaponStackConfig(variants);
    }

    private HostileWeaponVariantConfig parseVariant(JsonObject obj, HostileWeaponVariantConfig defaults) {
        return new HostileWeaponVariantConfig(
            obj.has("disabled") ? obj.get("disabled").getAsBoolean() : defaults.disabled(),
            obj.has("base_roll_chance") ? obj.get("base_roll_chance").getAsFloat() : defaults.baseRollChance(),
            obj.has("drop_rate") ? obj.get("drop_rate").getAsFloat() : defaults.dropRate(),
            obj.has("damage_percentage") ? obj.get("damage_percentage").getAsFloat() : defaults.damagePercentage(),
            obj.has("enchantable") ? obj.get("enchantable").getAsBoolean() : defaults.enchantable(),
            obj.has("player_level_percent_min") ? obj.get("player_level_percent_min").getAsFloat() : defaults.playerLevelPercentMin(),
            obj.has("player_level_percent_max") ? obj.get("player_level_percent_max").getAsFloat() : defaults.playerLevelPercentMax(),
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