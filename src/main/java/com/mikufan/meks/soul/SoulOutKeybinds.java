package com.mikufan.meks.soul;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

public final class SoulOutKeybinds {

    public static final KeyMapping TOGGLE = new KeyMapping(
            "key.meks.soul_out",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F4,
            "key.categories.meks"
    );

    private SoulOutKeybinds() {
    }

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE);
    }
}
