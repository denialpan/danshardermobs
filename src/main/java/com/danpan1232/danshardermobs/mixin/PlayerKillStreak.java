package com.danpan1232.danshardermobs.mixin;


import com.danpan1232.danshardermobs.danshardermobs;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerKillStreak {

    private static final String KILL_STREAK_TAG = "killstreak";

    @Inject(
            method = "killedEntity",
            at = @At("HEAD")
    )
    private void onKill(ServerLevel level, LivingEntity entity, CallbackInfoReturnable<Boolean> cir){

        if (!(entity instanceof Monster)) return;

        Player player = (Player)(Object)this;
        CompoundTag data = player.getPersistentData();
        int kills = data.getInt(KILL_STREAK_TAG);
        danshardermobs.LOGGER.info("kills: {}", kills);
        data.putInt(KILL_STREAK_TAG, kills + 1);


    }

    @Inject(
            method = "die",
            at = @At("HEAD")
    )
    private void onDeath(DamageSource cause, CallbackInfo ci) {
        Player player = (Player)(Object)this;
        CompoundTag data = player.getPersistentData();
        int kills = data.getInt(KILL_STREAK_TAG);
        player.getPersistentData().putInt(KILL_STREAK_TAG, kills - 5);
    }

}
