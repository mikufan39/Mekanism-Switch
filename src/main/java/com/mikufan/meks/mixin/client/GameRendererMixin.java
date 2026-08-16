package com.mikufan.meks.mixin.client;

import com.mikufan.meks.soul.SoulOutController;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    // Hide the hand while the soul camera is active.
    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true, remap = false)
    private void meks$soulNoHand(CallbackInfo ci) {
        if (SoulOutController.isActive()) {
            ci.cancel();
        }
    }

    // Hide block outlines while interactions are disabled.
    @Inject(method = "shouldRenderBlockOutline", at = @At("HEAD"), cancellable = true, remap = false)
    private void meks$soulNoOutline(CallbackInfoReturnable<Boolean> cir) {
        if (SoulOutController.isActive()) {
            cir.setReturnValue(false);
        }
    }
}
