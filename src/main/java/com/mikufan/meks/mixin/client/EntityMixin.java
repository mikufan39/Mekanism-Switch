package com.mikufan.meks.mixin.client;

import com.mikufan.meks.Config;
import com.mikufan.meks.soul.SoulOutController;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "turn", at = @At("HEAD"), cancellable = true, remap = false)
    private void meks$soulTurn(double yaw, double pitch, CallbackInfo ci) {
        if (SoulOutController.isActive() && (Object) this == Minecraft.getInstance().player) {
            SoulOutController.getCamera().turn(yaw, pitch);
            ci.cancel();
        }
    }

    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void meks$soulNoPush(Entity other, CallbackInfo ci) {
        if (SoulOutController.isActive() && ((Object) this == SoulOutController.getCamera() || other == SoulOutController.getCamera())) {
            ci.cancel();
        }
    }

    @Inject(method = "setPos(DDD)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void meks$soulFreezeSetPos(double x, double y, double z, CallbackInfo ci) {
        if (meks$shouldFreezeBody()) {
            ci.cancel();
        }
    }

    @Inject(method = "setPos(Lnet/minecraft/world/phys/Vec3;)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void meks$soulFreezeSetPosVec(Vec3 position, CallbackInfo ci) {
        if (meks$shouldFreezeBody()) {
            ci.cancel();
        }
    }

    @Inject(method = "setPosRaw(DDD)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void meks$soulFreezeSetPosRaw(double x, double y, double z, CallbackInfo ci) {
        if (meks$shouldFreezeBody()) {
            ci.cancel();
        }
    }

    @Inject(method = "setDeltaMovement(DDD)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void meks$soulFreezeDelta(double x, double y, double z, CallbackInfo ci) {
        if (meks$shouldFreezeBody()) {
            ci.cancel();
        }
    }

    @Inject(method = "setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void meks$soulFreezeDeltaVec(Vec3 motion, CallbackInfo ci) {
        if (meks$shouldFreezeBody()) {
            ci.cancel();
        }
    }

    @Inject(method = "moveRelative(FLnet/minecraft/world/phys/Vec3;)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void meks$soulFreezeMoveRelative(float speed, Vec3 relative, CallbackInfo ci) {
        if (meks$shouldFreezeBody()) {
            ci.cancel();
        }
    }

    private boolean meks$shouldFreezeBody() {
        return SoulOutController.isActive()
              && (Object) this == Minecraft.getInstance().player
              && Config.SOUL_FREEZE_BODY.get();
    }
}
