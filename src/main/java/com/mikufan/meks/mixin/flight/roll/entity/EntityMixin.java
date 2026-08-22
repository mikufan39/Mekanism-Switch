package com.mikufan.meks.mixin.flight.roll.entity;

import com.mikufan.meks.flight.api.RollEntity;
import com.mikufan.meks.flight.config.Sensitivity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Entity.class)
public abstract class EntityMixin implements RollEntity {
    @Shadow(remap = false)
    public abstract float getXRot();

    @Shadow(remap = false)
    public abstract float getYRot();

    @Shadow(remap = false)
    public abstract void setXRot(float pitch);

    @Shadow(remap = false)
    public abstract void setYRot(float yaw);

    @Shadow(remap = false)
    public abstract void turn(double cursorDeltaX, double cursorDeltaY);

    @Shadow(remap = false)
    public abstract Vec3 getViewVector(float partialTick);

    @Override
    public void meksFlight$changeElytraLook(double pitch, double yaw, double roll, Sensitivity sensitivity, double mouseDelta) {
    }

    @Override
    public void meksFlight$changeElytraLook(float pitch, float yaw, float roll) {
    }

    @Override
    public boolean meksFlight$isRolling() {
        return false;
    }

    @Override
    public void meksFlight$setRolling(boolean rolling) {
    }

    @Override
    public float meksFlight$getRoll() {
        return 0;
    }

    @Override
    public float meksFlight$getRoll(float tickDelta) {
        return 0;
    }

    @Override
    public void meksFlight$setRoll(float roll) {
    }
}
