package com.mikufan.meks.soul;

import com.mikufan.meks.Config;
import mekanism.common.item.gear.ItemMekaSuitArmor;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * State machine for the soul out-of-body feature. Client-side only; the fake
 * camera never sends packets, so the server never sees the soul.
 */
public final class SoulOutController {

    private static final Minecraft MC = Minecraft.getInstance();

    private static SoulCamera camera;
    private static boolean active;
    private static boolean disableNextTick;
    private static CameraType rememberedCameraType;

    private SoulOutController() {
    }

    public static boolean isActive() {
        return active && camera != null && MC.player != null;
    }

    public static SoulCamera getCamera() {
        return camera;
    }

    public static void disableNextTick() {
        if (active) {
            disableNextTick = true;
        }
    }

    public static void onClientTickPre(ClientTickEvent.Pre event) {
        if (disableNextTick && active) {
            disable(Component.translatable("msg.meks.soul.auto_return"));
        }
        disableNextTick = false;

        if (!isActive()) {
            return;
        }

        if (!Config.isLoaded() || !Config.SOUL_OUT_ENABLED.get() || MC.player == null || MC.level == null) {
            disable(Component.translatable("msg.meks.soul.auto_return"));
            return;
        }

        ItemStack helmet = MC.player.getItemBySlot(EquipmentSlot.HEAD);
        if (!(helmet.getItem() instanceof ItemMekaSuitArmor)) {
            disable(Component.translatable("msg.meks.soul.requires_helmet"));
            return;
        }

        MC.player.input = new Input();
    }

    public static void onClientTickPost(ClientTickEvent.Post event) {
        if (SoulOutKeybinds.TOGGLE.consumeClick()) {
            toggle();
        }
    }

    public static void toggle() {
        if (isActive()) {
            disable(Component.translatable("msg.meks.soul.disabled"));
        } else {
            enable();
        }
    }

    public static void onDisconnect() {
        if (active) {
            disable(null);
        }
    }

    public static void onRespawn() {
        if (active) {
            disable(Component.translatable("msg.meks.soul.auto_return"));
        }
    }

    private static void enable() {
        if (!Config.isLoaded() || !Config.SOUL_OUT_ENABLED.get() || MC.player == null || MC.level == null) {
            return;
        }

        ItemStack helmet = MC.player.getItemBySlot(EquipmentSlot.HEAD);
        if (!(helmet.getItem() instanceof ItemMekaSuitArmor)) {
            sendMessage("msg.meks.soul.requires_helmet");
            return;
        }

        camera = new SoulCamera();
        camera.moveToPlayer();
        camera.spawn();
        MC.setCameraEntity(camera);
        MC.smartCull = false;

        rememberedCameraType = MC.options.getCameraType();
        if (MC.gameRenderer.getMainCamera().isDetached()) {
            MC.options.setCameraType(CameraType.FIRST_PERSON);
        }

        MC.player.input = new Input();
        active = true;
        sendMessage("msg.meks.soul.enabled");
    }

    private static void disable(Component message) {
        if (camera != null) {
            camera.despawn();
            camera = null;
        }
        MC.smartCull = true;
        if (MC.player != null) {
            MC.setCameraEntity(MC.player);
            MC.player.input = new KeyboardInput(MC.options);
        }
        active = false;
        disableNextTick = false;
        if (rememberedCameraType != null) {
            MC.options.setCameraType(rememberedCameraType);
            rememberedCameraType = null;
        }
        if (message != null && MC.player != null) {
            MC.player.displayClientMessage(message, true);
        }
    }

    private static void sendMessage(String key) {
        if (MC.player != null) {
            MC.player.displayClientMessage(Component.translatable(key), true);
        }
    }
}
