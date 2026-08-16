package com.mikufan.meks.mixin.client;

import com.mikufan.meks.soul.SoulOutController;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true, remap = false)
    private void meks$soulNoAttack(CallbackInfoReturnable<Boolean> cir) {
        if (SoulOutController.isActive()) {
            cir.cancel();
        }
    }

    @Inject(method = "pickBlock", at = @At("HEAD"), cancellable = true, remap = false)
    private void meks$soulNoPick(CallbackInfo ci) {
        if (SoulOutController.isActive()) {
            ci.cancel();
        }
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true, remap = false)
    private void meks$soulNoBreak(CallbackInfo ci) {
        if (SoulOutController.isActive()) {
            ci.cancel();
        }
    }

    @Inject(method = {"disconnect()V", "disconnect(Lnet/minecraft/client/gui/screens/Screen;)V", "disconnect(Lnet/minecraft/client/gui/screens/Screen;Z)V"},
          at = @At("HEAD"), remap = false)
    private void meks$soulDisconnect(CallbackInfo ci) {
        SoulOutController.onDisconnect();
    }
}
