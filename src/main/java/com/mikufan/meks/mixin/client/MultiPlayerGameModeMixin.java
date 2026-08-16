package com.mikufan.meks.mixin.client;

import com.mikufan.meks.soul.SoulOutController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true, remap = false)
    private void meks$soulNoUseOn(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult,
                                  CallbackInfoReturnable<InteractionResult> cir) {
        if (SoulOutController.isActive()) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }

    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true, remap = false)
    private void meks$soulNoUseItem(Player player, InteractionHand hand,
                                    CallbackInfoReturnable<InteractionResult> cir) {
        if (SoulOutController.isActive()) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true, remap = false)
    private void meks$soulNoInteract(Player player, Entity entity, InteractionHand hand,
                                     CallbackInfoReturnable<InteractionResult> cir) {
        if (SoulOutController.isActive()) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }

    @Inject(method = "interactAt", at = @At("HEAD"), cancellable = true, remap = false)
    private void meks$soulNoInteractAt(Player player, Entity entity, EntityHitResult hitResult, InteractionHand hand,
                                       CallbackInfoReturnable<InteractionResult> cir) {
        if (SoulOutController.isActive()) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true, remap = false)
    private void meks$soulNoAttack(Player player, Entity target, CallbackInfo ci) {
        if (SoulOutController.isActive() || target == Minecraft.getInstance().player) {
            ci.cancel();
        }
    }
}
