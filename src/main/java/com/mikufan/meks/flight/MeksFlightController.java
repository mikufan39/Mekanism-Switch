package com.mikufan.meks.flight;

import com.mikufan.meks.Config;
import mekanism.api.Action;
import mekanism.api.energy.IStrictEnergyHandler;
import mekanism.common.capabilities.Capabilities;
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

    /** Extra multiplier applied on top of the vanilla mouse sensitivity for roll input. */
    private static final float ROLL_SENSITIVITY = 1.6F;

    /** Degrees per frame added while holding A or D. */
    private static final float KEY_YAW_STEP = 2.5F;

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
        // Mouse X feeds the roll target; Entity.turn applies a 0.15 factor, so match that scale.
        state.meks$setTargetRoll(state.meks$getTargetRoll() + (float) (deltaYaw * 0.15F * ROLL_SENSITIVITY));

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
        IStrictEnergyHandler handler = Capabilities.STRICT_ENERGY.item().getCapability(chest, null);
        if (handler == null || handler.extractEnergy(0, amount, Action.SIMULATE) < amount) {
            return false;
        }
        handler.extractEnergy(0, amount, Action.EXECUTE);
        return true;
    }
}
