package com.danpan1232.danshardermobs.util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.effect.MobEffect;

import java.util.Map;

public class HostileEffectLoader extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new Gson();

    public HostileEffectLoader() {
        super(GSON, "danshardermobs");
    }

    @Override
    protected void apply(
            Map<ResourceLocation, JsonElement> objects,
            ResourceManager resourceManager,
            ProfilerFiller profiler
    ) {
        HostileEffectData.clear();

        for (JsonElement element : objects.values()) {
            JsonObject root = element.getAsJsonObject();
            JsonObject effects = root.getAsJsonObject("effects");
            if (effects == null) continue;

            HostileEffectConfig defaultConfig = HostileEffectConfig.DEFAULT;

            if (effects.has("default")) {
                defaultConfig = parseConfig(
                        effects.getAsJsonObject("default"),
                        HostileEffectConfig.DEFAULT
                );
            }

            HostileEffectData.setDefault(defaultConfig);

            for (var entry : effects.entrySet()) {
                String key = entry.getKey();
                if (key.equals("default")) continue;

                ResourceLocation id = ResourceLocation.tryParse(key);
                if (id == null) continue;

                MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(id);
                if (effect == null) continue;

                JsonObject obj = entry.getValue().getAsJsonObject();

                HostileEffectConfig config =
                        parseConfig(obj, defaultConfig);

                HostileEffectData.put(effect, config);
            }
        }
    }

    private static HostileEffectConfig parseConfig(JsonObject obj, HostileEffectConfig base) {
        return new HostileEffectConfig(
                obj.has("disabled") ? obj.get("disabled").getAsBoolean() : false,
                obj.has("base_roll_chance") ? obj.get("base_roll_chance").getAsFloat() : base.baseRollChance()
        );
    }
}
