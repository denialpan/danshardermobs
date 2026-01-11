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
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager resourceManager, ProfilerFiller profiler) {

        HostileEffectData.clear();

        for (var entry : objects.entrySet()) {
            JsonObject root = entry.getValue().getAsJsonObject();
            JsonArray effects = root.getAsJsonArray("effects");
            if (effects == null) continue;

            for (JsonElement e : effects) {
                ResourceLocation id = ResourceLocation.tryParse(e.getAsString());
                if (id == null) continue;

                MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(id);
                if (effect == null) {
                    continue;
                }

                HostileEffectData.add(effect);
            }
        }
    }
}
