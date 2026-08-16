package com.mikufan.meks.flight;

/**
 * Client-side roll state kept on the local player while the MekaSuit flight controls are active.
 */
public interface MeksRollState {

    float meks$getRoll();

    float meks$getPrevRoll();

    /**
     * Roll interpolated between the previous and current tick for smooth rendering.
     */
    float meks$getRoll(float partialTick);

    void meks$setRoll(float roll);

    void meks$setPrevRoll(float prevRoll);

    float meks$getTargetRoll();

    void meks$setTargetRoll(float targetRoll);

    boolean meks$isRolling();

    void meks$setRolling(boolean rolling);
}
