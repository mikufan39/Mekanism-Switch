package com.mikufan.meks.mixin.flight.client.roll;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import com.mikufan.meks.flight.api.RollCamera;
import com.mikufan.meks.flight.api.RollEntity;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin implements RollCamera {
    @Shadow(remap = false)
    private Entity entity;

    @Shadow(remap = false)
    private float roll;

    @Unique
    private boolean isRolling;
    @Unique
    private float lastRollBack;
    @Unique
    private float rollBack;

    @Inject(
            method = "setup",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/Camera;eyeHeight:F",
                    ordinal = 0
            ),
            remap = false
    )
    private void meksFlight$interpolateRollnt(CallbackInfo ci) {
        if (!((RollEntity) entity).meksFlight$isRolling()) {
            lastRollBack = rollBack;
            rollBack -= rollBack * 0.5f;
        }
    }

    @Inject(
            method = "setup",
            at = @At("HEAD"),
            remap = false
    )
    private void meksFlight$captureTickDeltaAndUpdate(BlockGetter area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci, @Share("meksFlight$tickDelta") LocalFloatRef tickDeltaRef) {
        tickDeltaRef.set(tickDelta);
        isRolling = ((RollEntity) focusedEntity).meksFlight$isRolling();
    }

    @Inject(
            method = "setup",
            at = @At("TAIL"),
            remap = false
    )
    private void meksFlight$updateRollBack(BlockGetter area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        if (isRolling) {
            rollBack = roll;
            lastRollBack = roll;
        }
    }

    @ModifyArg(
            method = "setup",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Camera;setRotation(FFF)V",
                    ordinal = 0
            ),
            index = 2,
            remap = false
    )
    private float meksFlight$addRoll2(float original, @Share("meksFlight$tickDelta") LocalFloatRef tickDelta) {
        if (isRolling) {
            return original + ((RollEntity) entity).meksFlight$getRoll(tickDelta.get());
        } else {
            return original + Mth.lerp(tickDelta.get(), lastRollBack, rollBack);
        }
    }

    @ModifyArg(
            method = "setup",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Camera;setRotation(FFF)V",
                    ordinal = 1
            ),
            index = 2,
            remap = false
    )
    private float meksFlight$addRoll3(float original, @Share("meksFlight$tickDelta") LocalFloatRef tickDelta) {
        if (isRolling) {
            return original - ((RollEntity) entity).meksFlight$getRoll(tickDelta.get());
        } else {
            return original - Mth.lerp(tickDelta.get(), lastRollBack, rollBack);
        }
    }

    @Override
    public float meksFlight$getRoll() {
        return roll;
    }
}
