package com.mikufan.meks.mixin.flight.roll.entity;

import net.minecraft.Util;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerEntityMixin extends LivingEntityMixin {
    @Unique
    protected boolean isRolling;
    @Unique
    protected float prevRoll;
    @Unique
    protected float roll;

    @Override
    protected void meksFlight$baseTickTail(CallbackInfo ci) {
        meksFlight$baseTickTail2();

        prevRoll = meksFlight$getRoll();

        if (!meksFlight$isRolling()) {
            meksFlight$setRoll(0.0f);
        }
    }

    @Unique
    protected void meksFlight$baseTickTail2() {
    }

    @Override
    public boolean meksFlight$isRolling() {
        return isRolling;
    }

    @Override
    public void meksFlight$setRolling(boolean rolling) {
        isRolling = rolling;
    }

    @Override
    public float meksFlight$getRoll() {
        return roll;
    }

    @Override
    public float meksFlight$getRoll(float tickDelta) {
        if (tickDelta == 1.0f) {
            return meksFlight$getRoll();
        }
        return Mth.lerp(tickDelta, prevRoll, meksFlight$getRoll());
    }

    @Override
    public void meksFlight$setRoll(float roll) {
        if (!Float.isFinite(roll)) {
            Util.logAndPauseIfInIde("Invalid entity rotation: " + roll + ", discarding.");
            return;
        }
        var lastRoll = meksFlight$getRoll();
        this.roll = roll;

        if (roll < -90 && lastRoll > 90) {
            prevRoll -= 360;
        } else if (roll > 90 && lastRoll < -90) {
            prevRoll += 360;
        }
    }
}
