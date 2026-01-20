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

public class HostileEnchantmentLoader extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new Gson();

    public HostileEnchantmentLoader() {
        super(GSON, "enchantments");
    }

    @Override
    protected void apply(
        Map<ResourceLocation, JsonElement> objects,
        ResourceManager resourceManager,
        ProfilerFiller profiler
    ) {

        HostileEnchantmentData.clear();

        // default json
        JsonElement defaultJson = objects.get(ResourceLocation.fromNamespaceAndPath(danshardermobs.MODID, "default"));
        if (defaultJson != null) {
            HostileEnchantmentData.setDefault(parseDefaults(defaultJson.getAsJsonObject()));
        } else {
            HostileEnchantmentData.setDefault(HostileEnchantmentConfig.DEFAULT);
        }

        // *json
        for (var entry : objects.entrySet()) {

            ResourceLocation id = entry.getKey();

            if (id.getPath().equals("default")) continue;
            ResourceLocation enchantmentId = extractEnchantmentId(id);
            if (enchantmentId == null) continue;

            HostileEnchantmentConfig config = parseConfig(entry.getValue().getAsJsonObject(), HostileEnchantmentData.defaults());
            HostileEnchantmentData.put(enchantmentId, config);

        }

    }

    private static ResourceLocation extractEnchantmentId(ResourceLocation fileId) {
        String path = fileId.getPath();

        if (path.equals("default")) return null;

        // enchantments/<mod id>/<mob id>.json
        int slash = path.indexOf('/');
        if (slash <= 0) return null;

        String enchantmentNamespace = path.substring(0, slash);
        String enchantmentPath = path.substring(slash + 1);

        return ResourceLocation.tryParse(enchantmentNamespace + ":" + enchantmentPath);

    }

    private static HostileEnchantmentConfig parseDefaults(JsonObject obj) {
        return new HostileEnchantmentConfig(
            obj.has("disabled") ? obj.get("disabled").getAsBoolean() : false,
            obj.has("base_roll_chance") ? obj.get("base_roll_chance").getAsFloat() : 0.15F
        );
    }

    private static HostileEnchantmentConfig parseConfig(JsonObject obj, HostileEnchantmentConfig defaults) {
        return new HostileEnchantmentConfig(
            obj.has("disabled") ? obj.get("disabled").getAsBoolean() : defaults.disabled(),
            obj.has("base_roll_chance") ? obj.get("base_roll_chance").getAsFloat() : defaults.baseRollChance()
        );
    }

}
