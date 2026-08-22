package com.mikufan.meks.flight.api;

import net.minecraft.client.player.LocalPlayer;
import org.joml.Vector2d;

public interface RollMouse {
    boolean meksFlight$updateMouse(LocalPlayer player, double cursorDeltaX, double cursorDeltaY, double mouseDelta);

    Vector2d meksFlight$getMouseTurnVec();
}