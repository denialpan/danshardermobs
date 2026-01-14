package com.danpan1232.danshardermobs.util;

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

    public HostileEntityLoader() {
        super(GSON, "danshardermobs");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager resourceManager, ProfilerFiller profiler) {

        HostileEntityData.clear();
        for (var entry : objects.entrySet()) {
            JsonObject root = entry.getValue().getAsJsonObject();
            JsonObject entities = root.getAsJsonObject("entities");
            if (entities == null) continue;

            if (entities.has("default")) {
                HostileEntityData.setDefault(parseConfig(entities.getAsJsonObject("default")));
            }

            for (var entityEntry : entities.entrySet()) {
                String key = entityEntry.getKey();
                if (key.equals("default")) continue;

                ResourceLocation id = ResourceLocation.tryParse(key);
                if (id == null) {
                    continue;
                }

                HostileEntityConfig config =
                        parseConfig(entityEntry.getValue().getAsJsonObject());

                HostileEntityData.put(id, config);
            }
        }
    }

    private static HostileEntityConfig parseConfig(JsonObject obj) {
        return new HostileEntityConfig(
                obj.has("disabled") ? obj.get("disabled").getAsBoolean() : false,
                obj.has("base_level") ? obj.get("base_level").getAsInt() : 1,
                obj.has("max_health") ? obj.get("max_health").getAsInt() : -1,
                obj.has("health_scaling_multiplier") ? obj.get("health_scaling_multiplier").getAsFloat() : 1.0f,
                obj.has("health_scaling_flat_max_gain") ? obj.get("health_scaling_flat_max_gain").getAsInt() : 50,
                obj.has("level_cap") ? obj.get("level_cap").getAsInt() : -1,
                obj.has("base_scaling_level_kill_reward") ? obj.get("base_scaling_level_kill_reward").getAsFloat() : 1.0f,
                obj.has("is_boss") ? obj.get("is_boss").getAsBoolean() : false
        );
    }

}
