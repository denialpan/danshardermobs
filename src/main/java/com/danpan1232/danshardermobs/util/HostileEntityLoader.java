package com.danpan1232.danshardermobs.util;

import com.danpan1232.danshardermobs.danshardermobs;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

public class HostileEntityLoader extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new Gson();

    public HostileEntityLoader() {super(GSON, "hostile");}

    @Override
    protected void apply(
        Map<ResourceLocation, JsonElement> objects,
        ResourceManager resourceManager,
        ProfilerFiller profiler
    ) {
        HostileEntityData.clear();

        // default json
        JsonElement defaultJson = objects.get(ResourceLocation.fromNamespaceAndPath(danshardermobs.MODID, "default"));
        if (defaultJson != null) {
            HostileEntityData.setDefault(parseDefaults(defaultJson.getAsJsonObject()));
        } else {
            HostileEntityData.setDefault(HostileEntityConfig.DEFAULT);
        }

        // *json
        for (var entry : objects.entrySet()) {

            ResourceLocation id = entry.getKey();

            if (id.getPath().equals("default")) continue;
            ResourceLocation entityId = extractEntityId(id);
            if (entityId == null) continue;

            HostileEntityConfig config = parseConfig(entry.getValue().getAsJsonObject(), HostileEntityData.defaults());
            HostileEntityData.put(entityId, config);

        }
    }

    private static ResourceLocation extractEntityId(ResourceLocation fileId) {
        String path = fileId.getPath();

        if (path.equals("default")) return null;

        // hostile/<mod id>/<mob id>.json
        int slash = path.indexOf('/');
        if (slash <= 0) return null;

        String entityNamespace = path.substring(0, slash);
        String entityPath = path.substring(slash + 1);

        return ResourceLocation.tryParse(entityNamespace + ":" + entityPath);
    }

    private static HostileEntityConfig parseDefaults(JsonObject obj) {
        return new HostileEntityConfig(
            obj.has("disabled") ? obj.get("disabled").getAsBoolean() : false,
            obj.has("is_boss") ? obj.get("is_boss").getAsBoolean() : false,
            obj.has("boss_chance") ? obj.get("boss_chance").getAsFloat() : 0.01f,
            obj.has("boss_health_multiplier") ? obj.get("boss_health_multiplier").getAsFloat() : 2,
            obj.has("player_level_percent_min") ? obj.get("player_level_percent_min").getAsFloat() : -1F,
            obj.has("player_level_percent_max") ? obj.get("player_level_percent_max").getAsFloat() : -1F,
            obj.has("player_level_scaling_kill_reward") ? obj.get("player_level_scaling_kill_reward").getAsFloat() : 1F,
            obj.has("mob_min_health") ? obj.get("mob_min_health").getAsInt() : -1,
            obj.has("mob_max_health") ? obj.get("mob_max_health").getAsInt() : -1,
            obj.has("mob_effect_min_amplifier") ? obj.get("mob_effect_min_amplifier").getAsInt() : -1,
            obj.has("mob_effect_max_amplifier") ? obj.get("mob_effect_max_amplifier").getAsInt() : -1,
            obj.has("mob_health_increments") ? obj.get("mob_health_increments").getAsFloat() : 1.0F,
            obj.has("mob_xp_reward_multiplier") ? obj.get("mob_xp_reward_multiplier").getAsFloat() : 1.0F,
            obj.has("mob_can_have_armor") ? obj.get("mob_can_have_armor").getAsBoolean() : false,
            obj.has("mob_can_have_weapons") ? obj.get("mob_can_have_weapons").getAsBoolean() : false,
            obj.has("mob_can_have_effects") ? obj.get("mob_can_have_effects").getAsBoolean() : false,
            obj.has("mob_can_have_enchantments") ? obj.get("mob_can_have_enchantments").getAsBoolean() : false
        );
    }

    private static HostileEntityConfig parseConfig(JsonObject obj, HostileEntityConfig defaults) {
        return new HostileEntityConfig(
            obj.has("disabled") ? obj.get("disabled").getAsBoolean() : defaults.disabled(),
            obj.has("is_boss") ? obj.get("is_boss").getAsBoolean() : defaults.isBoss(),
            obj.has("boss_chance") ? obj.get("boss_chance").getAsFloat() : defaults.bossChance(),
            obj.has("boss_health_multiplier") ? obj.get("boss_health_multiplier").getAsFloat() : defaults.bossHealthMultiplier(),
            obj.has("player_level_percent_min") ? obj.get("player_level_percent_min").getAsFloat() : defaults.playerLevelPercentMin(),
            obj.has("player_level_percent_max") ? obj.get("player_level_percent_max").getAsFloat() : defaults.playerLevelPercentMax(),
            obj.has("player_level_scaling_kill_reward") ? obj.get("player_level_scaling_kill_reward").getAsFloat() : defaults.playerLevelScalingKillReward(),
            obj.has("mob_min_health") ? obj.get("mob_min_health").getAsInt() : defaults.mobMinHealth(),
            obj.has("mob_max_health") ? obj.get("mob_max_health").getAsInt() : defaults.mobMaxHealth(),
            obj.has("mob_effect_min_amplifier") ? obj.get("mob_effect_min_amplifier").getAsInt() : defaults.mobEffectMinAmplifier(),
            obj.has("mob_effect_max_amplifier") ? obj.get("mob_effect_max_amplifier").getAsInt() : defaults.mobEffectMaxAmplifier(),
            obj.has("mob_health_increments") ? obj.get("mob_health_increments").getAsFloat() : defaults.mobHealthIncrements(),
        obj.has("mob_xp_reward_multiplier") ? obj.get("mob_xp_reward_multiplier").getAsFloat() : defaults.mobXpRewardMultiplier(),
            obj.has("mob_can_have_armor") ? obj.get("mob_can_have_armor").getAsBoolean() : defaults.mobCanHaveArmor(),
            obj.has("mob_can_have_weapons") ? obj.get("mob_can_have_weapons").getAsBoolean() : defaults.mobCanHaveWeapons(),
            obj.has("mob_can_have_effects") ? obj.get("mob_can_have_effects").getAsBoolean() : defaults.mobCanHaveEffects(),
            obj.has("mob_can_have_enchantments") ? obj.get("mob_can_have_enchantments").getAsBoolean() : defaults.mobCanHaveEnchantments()
        );
    }

}
