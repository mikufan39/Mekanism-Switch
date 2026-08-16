package com.mikufan.meks.soul;

import com.mikufan.meks.Config;
import com.mojang.authlib.GameProfile;
import java.util.Collections;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.CommonListenerCookie;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.ServerLinks;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.Vec3;

/**
 * Client-only camera entity used while the soul is outside the body.
 * Ported from Freecam by hashalite (MIT): the camera is a fake LocalPlayer that
 * never talks to the server and moves without colliding with blocks.
 */
public class SoulCamera extends LocalPlayer {

    public SoulCamera() {
        super(Minecraft.getInstance(), Minecraft.getInstance().level, createNetworkHandler(),
              Minecraft.getInstance().player.getStats(), Minecraft.getInstance().player.getRecipeBook(),
              false, false);
        setId(-420);
        setPose(Pose.SWIMMING);
        getAbilities().flying = true;
        input = new KeyboardInput(Minecraft.getInstance().options);
    }

    private static ClientPacketListener createNetworkHandler() {
        Minecraft mc = Minecraft.getInstance();
        return new ClientPacketListener(
                mc,
                new Connection(PacketFlow.CLIENTBOUND),
                new CommonListenerCookie(
                        new GameProfile(UUID.randomUUID(), "SoulCamera"),
                        mc.getTelemetryManager().createWorldSessionManager(false, null, null),
                        mc.player.registryAccess().freeze(),
                        FeatureFlagSet.of(),
                        null,
                        null,
                        null,
                        Collections.emptyMap(),
                        null,
                        false,
                        Collections.emptyMap(),
                        ServerLinks.EMPTY
                )) {
            @Override
            public void send(Packet<?> packet) {
            }
        };
    }

    /**
     * Starts at the player's eye position so the viewpoint is not offset by the
     * fake camera's swimming pose eye height.
     */
    public void moveToPlayer() {
        Player player = Minecraft.getInstance().player;
        double y = player.getY() - player.getEyeHeight(Pose.SWIMMING) + player.getEyeHeight(player.getPose());
        moveTo(player.getX(), y, player.getZ(), player.getYRot(), player.getXRot());
    }

    public void spawn() {
        ((ClientLevel) level()).addEntity(this);
    }

    public void despawn() {
        if (level() instanceof ClientLevel clientLevel) {
            clientLevel.removeEntity(getId(), RemovalReason.DISCARDED);
        }
    }

    @Override
    public void aiStep() {
        Vec3 motion = SoulMotion.compute(this, Config.SOUL_HORIZONTAL_SPEED.get(), Config.SOUL_VERTICAL_SPEED.get());
        setDeltaMovement(motion);
        move(MoverType.SELF, motion);
        setDeltaMovement(Vec3.ZERO);
        setOnGround(false);
        getAbilities().flying = true;
    }

    // Prevents fall damage sound when the soul camera touches ground.
    @Override
    protected void checkFallDamage(double heightDifference, boolean onGround, BlockState landedState, BlockPos landedPosition) {
    }

    // Hand swing animations follow the real player.
    @Override
    public float getAttackAnim(float tickDelta) {
        return Minecraft.getInstance().player.getAttackAnim(tickDelta);
    }

    // Item use animations follow the real player.
    @Override
    public int getUseItemRemainingTicks() {
        return Minecraft.getInstance().player.getUseItemRemainingTicks();
    }

    @Override
    public boolean isUsingItem() {
        return Minecraft.getInstance().player.isUsingItem();
    }

    // Prevents slow down from ladders/vines.
    @Override
    public boolean onClimbable() {
        return false;
    }

    // Prevents slow down from water.
    @Override
    public boolean isInWater() {
        return false;
    }

    // Night vision and other effects follow the real player.
    @Override
    public MobEffectInstance getEffect(Holder<MobEffect> holder) {
        return Minecraft.getInstance().player.getEffect(holder);
    }

    // Prevents pistons from moving the soul camera.
    @Override
    public PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE;
    }

    // Prevents collision with solid entities such as shulkers and boats.
    @Override
    public boolean canCollideWith(Entity other) {
        return false;
    }

    // Keeps the fake camera in the swimming pose so the hitbox stays tiny.
    @Override
    public void setPose(Pose pose) {
        super.setPose(Pose.SWIMMING);
    }

    // Prevents slow down from the swimming pose.
    @Override
    public boolean isMovingSlowly() {
        return false;
    }

    // Prevents water splash sounds.
    @Override
    protected void doWaterSplashEffect() {
    }
}
