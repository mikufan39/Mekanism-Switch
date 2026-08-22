package com.mikufan.meks.mixin.client;

import com.mikufan.meks.soul.SoulOutController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {

    // The soul camera replaces the controlled camera, but rendering/HUD helpers
    // should still treat the real player as the controlled camera.
    @Inject(method = "isControlledCamera", at = @At("HEAD"), cancellable = true, remap = false)
    private void meks$soulControlledCamera(CallbackInfoReturnable<Boolean> cir) {
        if (SoulOutController.isActive() && (Object) this == Minecraft.getInstance().player) {
            cir.setReturnValue(true);
        }
    }
}
