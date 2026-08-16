package com.mikufan.meks.mixin.client;

import com.mikufan.meks.soul.SoulOutController;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Options.class)
public abstract class OptionsMixin {

    // Perspective switching is managed by the soul feature while it is active.
    @Inject(method = "setCameraType", at = @At("HEAD"), cancellable = true, remap = false)
    private void meks$soulLockPerspective(CallbackInfo ci) {
        if (SoulOutController.isActive()) {
            ci.cancel();
        }
    }
}
