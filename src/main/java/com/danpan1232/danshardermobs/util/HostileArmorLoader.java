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
import net.minecraft.server.packs.resources.Resource;
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
        super(GSON, "armor");
    }

    @Override
    protected void apply(
            Map<ResourceLocation, JsonElement> objects,
            ResourceManager resourceManager,
            ProfilerFiller profiler
    ) {

        HostileArmorData.clear();

        // default json
        JsonElement defaultJson = objects.get(
                ResourceLocation.fromNamespaceAndPath(danshardermobs.MODID, "default")
        );

        HostileArmorVariantConfig defaults = defaultJson != null ? parseDefaults(defaultJson.getAsJsonObject()) : HostileArmorVariantConfig.DEFAULT;
        HostileArmorData.setDefaults(defaults);

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
                danshardermobs.LOGGER.warn("Unknown armor '{}'", itemId);
                continue;
            }

            HostileArmorStackConfig stack = parseArmorFile(entry.getValue().getAsJsonObject(), defaults);
            HostileArmorData.put(item, stack);

        }

    }

    private HostileArmorVariantConfig parseDefaults(JsonObject obj) {
        return new HostileArmorVariantConfig(
            obj.has("disabled") ? obj.get("disabled").getAsBoolean() : false,
            obj.has("base_roll_chance") ? obj.get("base_roll_chance").getAsFloat() : 0.5f,
            obj.has("drop_rate") ? obj.get("drop_rate").getAsFloat() : 0.1f,
            obj.has("damage_percentage") ? obj.get("damage_percentage").getAsFloat() : 0.9f,
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

    private HostileArmorStackConfig parseArmorFile(JsonObject obj, HostileArmorVariantConfig defaults) {

        if (!obj.has("variants")) {
            return new HostileArmorStackConfig(List.of());
        }

        List<HostileArmorVariantConfig> variants = new ArrayList<>();
        JsonArray arr = obj.getAsJsonArray("variants");

        for (JsonElement e : arr) {
            variants.add(parseVariant(e.getAsJsonObject(), defaults));
        }

        return new HostileArmorStackConfig(variants);
    }

    private HostileArmorVariantConfig parseVariant(JsonObject obj, HostileArmorVariantConfig defaults) {
        return new HostileArmorVariantConfig(
            obj.has("disabled") ? obj.get("disabled").getAsBoolean() : defaults.disabled(),
            obj.has("base_roll_chance") ? obj.get("base_roll_chance").getAsFloat() : defaults.baseRollChance(),
            obj.has("drop_rate") ? obj.get("drop_rate").getAsFloat() : defaults.dropRate(),
            obj.has("damage_percentage") ? obj.get("damage_percentage").getAsFloat() : defaults.damagePercentage(),
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
