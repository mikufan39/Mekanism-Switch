package com.mikufan.meks.flight.net;

import com.mikufan.meks.MeksPayloads;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Roll state synchronization for the flight controls.
 *
 * <p>The local player reports its roll state to the server (C2S) and the server broadcasts it
 * to every player tracking the entity (S2C), so other players can see the roll. There is no
 * server-side config handshake; the server relays roll packets as long as its own
 * {@code [flight] enabled} flag (in {@code meks-common.toml}) is true, giving the server
 * owner a master switch for roll relay. Players who start tracking an already-rolling entity
 * receive a one-off initial state push via {@link #sendRollUpdateToPlayer}.
 */
public final class FlightNetworking {

    private FlightNetworking() {
    }

    public static void sendRollUpdateToServer(boolean rolling, float roll) {
        PacketDistributor.sendToServer(new MeksPayloads.RollSyncPayload(rolling, roll));
    }

    public static void broadcastRollUpdate(Entity entity, boolean rolling, float roll) {
        PacketDistributor.sendToPlayersTrackingEntity(
                entity, new MeksPayloads.RollSyncS2CPayload(entity.getId(), rolling, roll));
    }

    /**
     * Send the current roll state to a single player, used when that player starts tracking
     * the entity (first-frame push so a mid-flight joiner can see the current roll).
     */
    public static void sendRollUpdateToPlayer(ServerPlayer player, Entity entity, boolean rolling, float roll) {
        PacketDistributor.sendToPlayer(
                player, new MeksPayloads.RollSyncS2CPayload(entity.getId(), rolling, roll));
    }
}