package com.danpan1232.danshardermobs.util;

import com.danpan1232.danshardermobs.danshardermobs;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.RegisterEvent;

public class ModDataComponents {

    public static final DataComponentType<Float> DAMAGE_PERCENT = DataComponentType.<Float>builder()
            .persistent((Codec.FLOAT))
            .build();

    public static final DataComponentType<Float> DROP_RATE = DataComponentType.<Float>builder()
            .persistent((Codec.FLOAT))
            .build();

    public static void register(RegisterEvent event) {
        event.register(
            Registries.DATA_COMPONENT_TYPE, helper ->
            {
                helper.register(
                        ResourceLocation.fromNamespaceAndPath(danshardermobs.MODID, "damage_percent"),
                        ModDataComponents.DAMAGE_PERCENT
                );
                helper.register(
                        ResourceLocation.fromNamespaceAndPath(danshardermobs.MODID, "drop_rate"),
                        ModDataComponents.DROP_RATE
                );
            }

        );
    }

}
