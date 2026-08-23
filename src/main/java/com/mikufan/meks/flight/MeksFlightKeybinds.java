package com.mikufan.meks.flight;

import com.mikufan.meks.MekanismSwitch;
import com.mikufan.meks.flight.api.key.InputContext;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public final class MeksFlightKeybinds {

    public static final KeyMapping PITCH_UP = new KeyMapping(
            "key.meks.flight.pitch_up",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            "key.categories.meks"
    );
    public static final KeyMapping PITCH_DOWN = new KeyMapping(
            "key.meks.flight.pitch_down",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            "key.categories.meks"
    );
    public static final KeyMapping YAW_LEFT = new KeyMapping(
            "key.meks.flight.yaw_left",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_A,
            "key.categories.meks"
    );
    public static final KeyMapping YAW_RIGHT = new KeyMapping(
            "key.meks.flight.yaw_right",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_D,
            "key.categories.meks"
    );
    public static final KeyMapping ROLL_LEFT = new KeyMapping(
            "key.meks.flight.roll_left",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            "key.categories.meks"
    );
    public static final KeyMapping ROLL_RIGHT = new KeyMapping(
            "key.meks.flight.roll_right",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            "key.categories.meks"
    );

    public static final List<KeyMapping> ALL = List.of(
            PITCH_UP,
            PITCH_DOWN,
            YAW_LEFT,
            YAW_RIGHT,
            ROLL_LEFT,
            ROLL_RIGHT
    );

    public static final InputContext CONTEXT = InputContext.of(
            ResourceLocation.fromNamespaceAndPath(MekanismSwitch.MODID, "fall_flying"),
            MeksFlightClient.FALL_FLYING_GROUP
    );

    static {
        CONTEXT.addKeyBinding(PITCH_UP);
        CONTEXT.addKeyBinding(PITCH_DOWN);
        CONTEXT.addKeyBinding(YAW_LEFT);
        CONTEXT.addKeyBinding(YAW_RIGHT);
        CONTEXT.addKeyBinding(ROLL_LEFT);
        CONTEXT.addKeyBinding(ROLL_RIGHT);
    }

    private MeksFlightKeybinds() {
    }

    public static void register(RegisterKeyMappingsEvent event) {
        ALL.forEach(event::register);
    }
}