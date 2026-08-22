package com.mikufan.meks.flight.net;

import com.mikufan.meks.MeksPayloads;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Roll state synchronization for the MekaSuit flight controls.
 *
 * <p>The local player reports its roll state to the server (C2S) and the server broadcasts it
 * to every player tracking the entity (S2C), so other players can see the roll. Unlike the
 * reference mod, there is no server-side config handshake: the server accepts roll packets
 * unconditionally.
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
}
