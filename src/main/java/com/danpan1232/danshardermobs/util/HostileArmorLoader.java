package com.danpan1232.danshardermobs.util;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.Map;

public class HostileArmorLoader extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new Gson();

    public HostileArmorLoader() {
        super(GSON, "danshardermobs");
    }

    @Override
    protected void apply(
            Map<ResourceLocation, JsonElement> objects,
            ResourceManager resourceManager,
            ProfilerFiller profiler
    ) {
        HostileArmorData.clear();

        for (JsonElement element : objects.values()) {
            JsonObject root = element.getAsJsonObject();
            JsonObject armor = root.getAsJsonObject("armor");
            if (armor == null) continue;

            HostileArmorConfig defaultConfig = HostileArmorConfig.DEFAULT;

            if (armor.has("default")) {
                defaultConfig = parseConfig(
                        armor.getAsJsonObject("default"),
                        HostileArmorConfig.DEFAULT
                );
            }

            HostileArmorData.setDefault(defaultConfig);

            for (var entry : armor.entrySet()) {
                String key = entry.getKey();
                if (key.equals("default")) continue;

                ResourceLocation id = ResourceLocation.tryParse(key);
                if (id == null) continue;

                Item item = BuiltInRegistries.ITEM.get(id);
                if (item == Items.AIR) continue;

                if (!(item instanceof ArmorItem)) continue;

                JsonObject obj = entry.getValue().getAsJsonObject();

                HostileArmorConfig config =
                        parseConfig(obj, defaultConfig);

                HostileArmorData.put(item, config);
            }
        }
    }

    private static HostileArmorConfig parseConfig(JsonObject obj, HostileArmorConfig base) {
        return new HostileArmorConfig(
                obj.has("base_roll_chance") ? obj.get("base_roll_chance").getAsFloat() : base.baseRollChance(),
                obj.has("tier") ? obj.get("tier").getAsInt() : base.tier()
        );
    }
}
