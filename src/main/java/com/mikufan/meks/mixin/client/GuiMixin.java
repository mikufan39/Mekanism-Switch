package com.mikufan.meks.mixin.client;

import com.mikufan.meks.soul.SoulOutController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Gui.class)
public abstract class GuiMixin {

    // The HUD should always describe the real player, not the soul camera.
    @Inject(method = "getCameraPlayer", at = @At("HEAD"), cancellable = true, remap = false)
    private void meks$soulHudPlayer(CallbackInfoReturnable<Player> cir) {
        if (SoulOutController.isActive()) {
            cir.setReturnValue(Minecraft.getInstance().player);
        }
    }

    // Suppress pumpkin/blur overlays tied to the old camera entity.
    @Inject(method = "renderTextureOverlay", at = @At("HEAD"), cancellable = true, remap = false)
    private void meks$soulNoOverlay(GuiGraphics guiGraphics, ResourceLocation texture, float alpha, CallbackInfo ci) {
        if (SoulOutController.isActive()) {
            ci.cancel();
        }
    }
}
