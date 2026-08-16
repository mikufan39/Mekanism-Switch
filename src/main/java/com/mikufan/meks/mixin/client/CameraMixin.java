package com.mikufan.meks.mixin.client;

import com.mikufan.meks.flight.MeksRollState;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @Inject(method = "setup", at = @At("TAIL"), remap = false)
    private void meks$applyFlightRoll(BlockGetter area, Entity focusedEntity, boolean detached,
                                      boolean inverted, float partialTick, CallbackInfo ci) {
        if (focusedEntity instanceof MeksRollState state) {
            float roll = state.meks$getRoll(partialTick);
            if (Math.abs(roll) > 0.001F) {
                // First-person views need the inverse roll; inverted third-person front view is mirrored.
                float sign = detached && inverted ? 1.0F : -1.0F;
                ((Camera) (Object) this).rotation()
                      .rotateLocalZ(sign * (float) Math.toRadians(roll));
            }
        }
    }
}
