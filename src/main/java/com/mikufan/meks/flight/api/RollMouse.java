package com.mikufan.meks.flight.api;

import net.minecraft.client.player.LocalPlayer;

public interface RollMouse {
    boolean meksFlight$updateMouse(LocalPlayer player, double cursorDeltaX, double cursorDeltaY, double mouseDelta);
}