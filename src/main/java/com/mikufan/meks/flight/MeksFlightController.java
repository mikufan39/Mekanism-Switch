package com.mikufan.meks.flight;

import com.mikufan.meks.Config;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.energy.IEnergyContainer;
import mekanism.common.util.StorageUtils;
import mekanism.common.item.gear.ItemMekaSuitArmor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * Original three-axis flight controls for elytra flight with a MekaSuit chestplate.
 *
 * <p>Mouse X controls roll, mouse Y keeps vanilla pitch, and A/D steer yaw while the controls
 * are active. The feature is implemented from scratch for this mod and only runs on the client;
 * other players will not see the local player's roll.
 */
public final class MeksFlightController {

    /** Energy drained from the MekaSuit chestplate every tick while the controls are active. */
    public static final long ENERGY_PER_TICK = 100L;

    /** Roll sensitivity multiplier, matching the fixed feel used by the reference flight mod. */
    private static final float ROLL_SENSITIVITY = 1.0F;

    /** Degrees per frame added while holding A or D. */
    private static final float KEY_YAW_STEP = 1.8F;

    /** How quickly the roll follows the mouse target. */
    private static final float ROLL_LERP = 0.45F;

    /** How quickly the roll returns to level when the controls are inactive. */
    private static final float RIGHTING_LERP = 0.22F;

    private MeksFlightController() {
    }

    public static void tick(LocalPlayer player) {
        if (!(player instanceof MeksRollState state)) {
            return;
        }
        boolean active = Config.MEKA_SUIT_FLIGHT_CONTROLS.get()
              && player.isFallFlying()
              && isMekaSuitChest(player);
        state.meks$setPrevRoll(state.meks$getRoll());
        if (active && drainEnergy(player, ENERGY_PER_TICK)) {
            state.meks$setRolling(true);
            float target = state.meks$getTargetRoll();
            float roll = state.meks$getRoll();
            state.meks$setRoll(roll + (target - roll) * ROLL_LERP);
        } else {
            state.meks$setRolling(false);
            state.meks$setTargetRoll(0.0F);
            float roll = state.meks$getRoll() + (0.0F - state.meks$getRoll()) * RIGHTING_LERP;
            state.meks$setRoll(Math.abs(roll) < 0.05F ? 0.0F : roll);
        }
    }

    /**
     * Handles mouse input while the flight controls are active.
     *
     * @return true if the input was consumed by the flight controls, false for vanilla handling
     */
    public static boolean handleMouse(LocalPlayer player, double deltaYaw, double deltaPitch) {
        if (!(player instanceof MeksRollState state) || !state.meks$isRolling()) {
            return false;
        }
        // Undo vanilla's sensitivity scaling so roll speed stays fixed, like the reference flight mod.
        // MouseHandler.turnPlayer scales raw deltas by 8 * (0.6 * sensitivity + 0.2)^3 (or the square when scoping).
        double sensitivity = Minecraft.getInstance().options.sensitivity().get();
        double base = 0.6D * sensitivity + 0.2D;
        double scale = player.isScoping() ? base * base : 8.0D * base * base * base;
        double rawYaw = scale <= 0.0D ? 0.0D : deltaYaw / scale;
        // Entity.turn applies a 0.15 factor, so keep the same scale for the roll input.
        state.meks$setTargetRoll(state.meks$getTargetRoll() + (float) (rawYaw * 0.15F * ROLL_SENSITIVITY));

        // Mouse Y keeps vanilla pitch control, and A/D replaces mouse X as the yaw input.
        player.turn(0.0D, deltaPitch);
        int keyYaw = 0;
        var options = Minecraft.getInstance().options;
        if (options.keyRight.isDown()) {
            keyYaw++;
        }
        if (options.keyLeft.isDown()) {
            keyYaw--;
        }
        if (keyYaw != 0) {
            player.turn(keyYaw * KEY_YAW_STEP / 0.15F, 0.0D);
        }
        return true;
    }

    private static boolean isMekaSuitChest(LocalPlayer player) {
        return player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof ItemMekaSuitArmor;
    }

    private static boolean drainEnergy(LocalPlayer player, long amount) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        IEnergyContainer container = StorageUtils.getEnergyContainer(chest, 0);
        if (container == null || container.extract(amount, Action.SIMULATE, AutomationType.MANUAL) < amount) {
            return false;
        }
        container.extract(amount, Action.EXECUTE, AutomationType.MANUAL);
        return true;
    }
}
