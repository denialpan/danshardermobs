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
import net.minecraft.world.effect.MobEffect;

import java.util.Map;

public class HostileEffectLoader extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new Gson();

    public HostileEffectLoader() {
        super(GSON, "effects");
    }

    @Override
    protected void apply(
            Map<ResourceLocation, JsonElement> objects,
            ResourceManager resourceManager,
            ProfilerFiller profiler
    ) {
        HostileEffectData.clear();

        // default json
        JsonElement defaultJson = objects.get(ResourceLocation.fromNamespaceAndPath(danshardermobs.MODID, "default"));
        if (defaultJson != null) {
            HostileEffectData.setDefault(parseDefaults(defaultJson.getAsJsonObject()));
        } else {
            HostileEffectData.setDefault(HostileEffectConfig.DEFAULT);
        }

        // *json
        for (var entry : objects.entrySet()) {

            ResourceLocation id = entry.getKey();

            if (id.getPath().equals("default")) continue;
            ResourceLocation effectId = extractEffectId(id);
            if (effectId == null) continue;

            HostileEffectConfig config = parseConfig(entry.getValue().getAsJsonObject(), HostileEffectData.defaults());
            HostileEffectData.put(effectId, config);

        }
    }

    private static ResourceLocation extractEffectId(ResourceLocation fileId) {
        String path = fileId.getPath();

        if (path.equals("default")) return null;

        // effects/<mod id>/<mob id>.json
        int slash = path.indexOf('/');
        if (slash <= 0) return null;

        String entityNamespace = path.substring(0, slash);
        String entityPath = path.substring(slash + 1);

        return ResourceLocation.tryParse(entityNamespace + ":" + entityPath);
    }

    private static HostileEffectConfig parseDefaults(JsonObject obj) {
        return new HostileEffectConfig(
            obj.has("disabled") ? obj.get("disabled").getAsBoolean() : false,
            obj.has("base_roll_chance") ? obj.get("base_roll_chance").getAsFloat() : 1
        );
    }

    private static HostileEffectConfig parseConfig(JsonObject obj, HostileEffectConfig defaults) {
        return new HostileEffectConfig(
            obj.has("disabled") ? obj.get("disabled").getAsBoolean() : defaults.disabled(),
            obj.has("base_roll_chance") ? obj.get("base_roll_chance").getAsFloat() : 1
        );
    }
}
