package com.mikufan.meks.soul;

import com.mikufan.meks.Config;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.energy.IEnergyContainer;
import mekanism.common.util.StorageUtils;
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
    private static long elapsedTicks;
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

        if (!Config.SOUL_OUT_ENABLED.get() || MC.player == null || MC.level == null) {
            disable(Component.translatable("msg.meks.soul.auto_return"));
            return;
        }

        ItemStack helmet = MC.player.getItemBySlot(EquipmentSlot.HEAD);
        if (!(helmet.getItem() instanceof ItemMekaSuitArmor)) {
            disable(Component.translatable("msg.meks.soul.requires_helmet"));
            return;
        }

        if (!MC.player.isCreative() && !drainEnergy(helmet)) {
            disable(Component.translatable("msg.meks.soul.no_energy"));
            return;
        }

        elapsedTicks++;
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
        if (!Config.SOUL_OUT_ENABLED.get() || MC.player == null || MC.level == null) {
            return;
        }

        ItemStack helmet = MC.player.getItemBySlot(EquipmentSlot.HEAD);
        if (!(helmet.getItem() instanceof ItemMekaSuitArmor)) {
            sendMessage("msg.meks.soul.requires_helmet");
            return;
        }
        if (!MC.player.isCreative() && !canAfford(helmet)) {
            sendMessage("msg.meks.soul.no_energy");
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
        elapsedTicks = 0;
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
        elapsedTicks = 0;
        if (message != null && MC.player != null) {
            MC.player.displayClientMessage(message, true);
        }
    }

    private static boolean canAfford(ItemStack helmet) {
        IEnergyContainer container = StorageUtils.getEnergyContainer(helmet, 0);
        long cost = currentCost();
        return container != null && container.extract(cost, Action.SIMULATE, AutomationType.MANUAL) >= cost;
    }

    private static boolean drainEnergy(ItemStack helmet) {
        IEnergyContainer container = StorageUtils.getEnergyContainer(helmet, 0);
        if (container == null) {
            return false;
        }
        long cost = currentCost();
        if (container.extract(cost, Action.SIMULATE, AutomationType.MANUAL) < cost) {
            return false;
        }
        container.extract(cost, Action.EXECUTE, AutomationType.MANUAL);
        return true;
    }

    private static long currentCost() {
        long base = Config.SOUL_BASE_COST_PER_TICK.get();
        int doublingSeconds = Config.SOUL_COST_DOUBLING_SECONDS.get();
        if (doublingSeconds <= 0) {
            return Math.max(1L, base);
        }
        double seconds = elapsedTicks / 20.0;
        double cost = base * Math.pow(2.0, seconds / doublingSeconds);
        return Math.max(1L, (long) Math.ceil(cost));
    }

    private static void sendMessage(String key) {
        if (MC.player != null) {
            MC.player.displayClientMessage(Component.translatable(key), true);
        }
    }
}
