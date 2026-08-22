package com.mikufan.meks.mixin.client;

import com.mikufan.meks.soul.SoulCamera;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow(remap = false)
    private Entity entity;

    @Shadow(remap = false)
    private float eyeHeightOld;

    @Shadow(remap = false)
    private float eyeHeight;

    // When switching between the player and the soul camera, align the eye height instantly.
    @Inject(method = "setup", at = @At("HEAD"), remap = false)
    private void meks$soulEyeHeight(BlockGetter area, Entity focusedEntity, boolean detached,
                                    boolean inverted, float partialTick, CallbackInfo ci) {
        if (focusedEntity == null || this.entity == null || focusedEntity == this.entity) {
            return;
        }
        if (focusedEntity instanceof SoulCamera || this.entity instanceof SoulCamera) {
            this.eyeHeightOld = this.eyeHeight = focusedEntity.getEyeHeight();
        }
    }
}
