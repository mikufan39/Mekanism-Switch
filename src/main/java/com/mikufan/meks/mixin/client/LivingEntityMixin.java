package com.mikufan.meks.mixin.client;

import com.mikufan.meks.Config;
import com.mikufan.meks.soul.SoulOutController;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Shadow(remap = false)
    public abstract float getHealth();

    @Inject(method = "setHealth", at = @At("HEAD"), remap = false)
    private void meks$soulDamageExit(float health, CallbackInfo ci) {
        if (SoulOutController.isActive() && Config.SOUL_DISABLE_ON_DAMAGE.get()
              && (Object) this == Minecraft.getInstance().player
              && !Minecraft.getInstance().player.isCreative()
              && getHealth() > health) {
            SoulOutController.disableNextTick();
        }
    }
}
