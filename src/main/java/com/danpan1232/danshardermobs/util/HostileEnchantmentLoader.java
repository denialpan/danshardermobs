package com.danpan1232.danshardermobs.util;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

public class HostileEnchantmentLoader extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new Gson();

    public HostileEnchantmentLoader() {
        super(GSON, "danshardermobs");
    }

    @Override
    protected void apply(
            Map<ResourceLocation, JsonElement> objects,
            ResourceManager resourceManager,
            ProfilerFiller profiler
    ) {
        HostileEnchantmentData.clear();

        for (var entry : objects.entrySet()) {
            JsonObject root = entry.getValue().getAsJsonObject();
            JsonObject enchants = root.getAsJsonObject("enchantments");
            if (enchants == null) continue;

            if (enchants.has("default")) {
                HostileEnchantmentData.setDefault(parse(enchants.getAsJsonObject("default")));
            }

            for (var e : enchants.entrySet()) {
                if (e.getKey().equals("default")) continue;

                ResourceLocation id = ResourceLocation.tryParse(e.getKey());
                if (id == null) continue;

                HostileEnchantmentData.put(id, parse(e.getValue().getAsJsonObject()));
            }
        }
    }

    private static HostileEnchantmentConfig parse(JsonObject obj) {
        return new HostileEnchantmentConfig(
                obj.has("disabled") ? obj.get("disabled").getAsBoolean() : false,
                obj.has("base_roll_chance") ? obj.get("base_roll_chance").getAsFloat() : HostileEnchantmentConfig.DEFAULT.baseRollChance()
        );
    }

}
