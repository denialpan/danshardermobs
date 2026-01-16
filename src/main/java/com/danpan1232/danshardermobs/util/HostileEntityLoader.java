package com.danpan1232.danshardermobs.util;

import com.danpan1232.danshardermobs.danshardermobs;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

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
            obj.has("base_level") ? obj.get("base_level").getAsInt() : 1,
            obj.has("max_health") ? obj.get("max_health").getAsInt() : -1,
            obj.has("health_scaling_multiplier") ? obj.get("health_scaling_multiplier").getAsFloat() : 1.0F,
            obj.has("health_scaling_flat_max_gain") ? obj.get("health_scaling_flat_max_gain").getAsInt() : 50,
            obj.has("level_cap") ? obj.get("level_cap").getAsInt() : -1,
            obj.has("base_scaling_level_kill_reward") ? obj.get("base_scaling_level_kill_reward").getAsFloat() : 1.0F,
            obj.has("xp_reward_multiplier") ? obj.get("xp_reward_multiplier").getAsFloat() : 1.5F,
            obj.has("is_boss") ? obj.get("is_boss").getAsBoolean() : false
        );
    }

    private static HostileEntityConfig parseConfig(JsonObject obj, HostileEntityConfig defaults) {
        return new HostileEntityConfig(
            obj.has("disabled") ? obj.get("disabled").getAsBoolean() : defaults.disabled(),
            obj.has("base_level") ? obj.get("base_level").getAsInt() : defaults.baseLevel(),
            obj.has("max_health") ? obj.get("max_health").getAsInt() : defaults.maxHealth(),
            obj.has("health_scaling_multiplier") ? obj.get("health_scaling_multiplier").getAsFloat() : defaults.healthScalingMultiplier(),
            obj.has("health_scaling_flat_max_gain") ? obj.get("health_scaling_flat_max_gain").getAsInt() : defaults.healthScalingFlatMaxGain(),
            obj.has("level_cap") ? obj.get("level_cap").getAsInt() : defaults.levelCap(),
            obj.has("base_scaling_level_kill_reward") ? obj.get("base_scaling_level_kill_reward").getAsFloat() : defaults.baseScalingLevelKillReward(),
            obj.has("xp_reward_multiplier") ? obj.get("xp_reward_multiplier").getAsFloat() : defaults.xpRewardMultiplier(),
            obj.has("is_boss") ? obj.get("is_boss").getAsBoolean() : defaults.isBoss()
        );
    }

}
