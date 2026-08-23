package com.mikufan.meks.flight;

import com.mikufan.meks.MekanismSwitch;
import com.mikufan.meks.flight.api.event.RollEvents;
import com.mikufan.meks.flight.api.event.RollGroup;
import com.mikufan.meks.flight.config.MeksFlightConfig;
import mekanism.common.item.gear.ItemMekaSuitArmor;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.SmoothDouble;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * Client-side init and activation gating for the flight controls.
 *
 * <p>The camera modifier pipeline comes from the reference flight mod (Do a Barrel Roll):
 * keyboard controls and the configured mouse sensitivity run as EARLY_CAMERA_MODIFIERS, and
 * control-surface efficacy, smoothing, banking and automatic righting run as
 * LATE_CAMERA_MODIFIERS, all gated on {@link #isFallFlying()}.
 *
 * <p>On top of the reference behaviour there is no energy cost: activating the controls is
 * free, and with {@code activationMode = GLOBAL} in the config anyone who is fall-flying
 * (not just MekaSuit Elytra Unit users) gets the controls. The enabled flag is read from the
 * config file only — there is no in-game toggle key.
 */
public final class MeksFlightClient {

    public static final SmoothDouble PITCH_SMOOTHER = new SmoothDouble();
    public static final SmoothDouble YAW_SMOOTHER = new SmoothDouble();
    public static final SmoothDouble ROLL_SMOOTHER = new SmoothDouble();
    public static final RollGroup FALL_FLYING_GROUP = RollGroup.of(
            ResourceLocation.fromNamespaceAndPath(MekanismSwitch.MODID, "fall_flying"));

    private MeksFlightClient() {
    }

    public static void init() {
        FALL_FLYING_GROUP.trueIf(MeksFlightClient::isFallFlying);

        // Keyboard modifiers
        RollEvents.EARLY_CAMERA_MODIFIERS.register(context -> context
                .useModifier(RotationModifiers.buttonControls(1800)),
                2000, FALL_FLYING_GROUP);

        // Mouse modifiers, including swapping axes
        RollEvents.EARLY_CAMERA_MODIFIERS.register(context -> context
                .useModifier(MeksFlightConfig::configureRotation),
                1000, FALL_FLYING_GROUP);

        // Generic movement modifiers, banking and such
        RollEvents.LATE_CAMERA_MODIFIERS.register(context -> context
                .useModifier(RotationModifiers::applyControlSurfaceEfficacy, MeksFlightConfig::getSimulateControlSurfaceEfficacy)
                .useModifier(RotationModifiers.smoothing(
                        PITCH_SMOOTHER, YAW_SMOOTHER, ROLL_SMOOTHER,
                        MeksFlightConfig.getSmoothing()
                ))
                .useModifier(RotationModifiers::banking, MeksFlightConfig::getEnableBanking)
                .useModifier(RotationModifiers::reorient, MeksFlightConfig::getAutomaticRighting),
                1000, FALL_FLYING_GROUP);
    }

    public static void clearValues() {
        PITCH_SMOOTHER.reset();
        YAW_SMOOTHER.reset();
        ROLL_SMOOTHER.reset();
    }

    /**
     * Whether the flight control modifier pipeline is enabled: config enabled, not submerged,
     * fall flying, and — in the default {@code activationMode = ELYTRA_UNIT} — wearing a
     * MekaSuit chestplate (the elytra flight itself is provided by Mekanism's Elytra Unit).
     * In {@code activationMode = GLOBAL} the chestplate is not required, so any fall-flying
     * player (e.g. vanilla elytra) gets the controls.
     */
    public static boolean isFallFlying() {
        if (!MeksFlightConfig.getModEnabled()) {
            return false;
        }

        var player = Minecraft.getInstance().player;
        if (player == null) {
            return false;
        }
        if (MeksFlightConfig.getDisableWhenSubmerged() && player.isUnderWater()) {
            return false;
        }
        if (!player.isFallFlying()) {
            return false;
        }
        return MeksFlightConfig.getActivationMode() == MeksFlightConfig.ActivationMode.GLOBAL
                || player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof ItemMekaSuitArmor;
    }
}