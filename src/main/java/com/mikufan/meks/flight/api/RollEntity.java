package com.mikufan.meks.flight.api;

import com.mikufan.meks.flight.config.Sensitivity;

public interface RollEntity {
    void meksFlight$changeElytraLook(double pitch, double yaw, double roll, Sensitivity sensitivity, double mouseDelta);

    void meksFlight$changeElytraLook(float pitch, float yaw, float roll);

    boolean meksFlight$isRolling();

    void meksFlight$setRolling(boolean rolling);

    float meksFlight$getRoll();

    float meksFlight$getRoll(float tickDelta);

    void meksFlight$setRoll(float roll);
}