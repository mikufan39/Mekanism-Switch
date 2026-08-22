package com.mikufan.meks;

import java.util.List;
import com.mikufan.meks.flight.api.RollEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class MeksPayloads {

    private MeksPayloads() {
    }

    public record StartExchangePayload(BlockPos pos, ResourceLocation target, int count, boolean forget, int slot) implements CustomPacketPayload {

        public static final Type<StartExchangePayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(MekanismSwitch.MODID, "start_exchange"));

        public static final StreamCodec<RegistryFriendlyByteBuf, StartExchangePayload> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, StartExchangePayload::pos,
                ResourceLocation.STREAM_CODEC, StartExchangePayload::target,
                ByteBufCodecs.VAR_INT, StartExchangePayload::count,
                ByteBufCodecs.BOOL, StartExchangePayload::forget,
                ByteBufCodecs.VAR_INT, StartExchangePayload::slot,
                StartExchangePayload::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record SyncExchangePayload(List<ResourceLocation> knowledge, long sv) implements CustomPacketPayload {

        public static final Type<SyncExchangePayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(MekanismSwitch.MODID, "sync_exchange"));

        public static final StreamCodec<RegistryFriendlyByteBuf, SyncExchangePayload> STREAM_CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()), SyncExchangePayload::knowledge,
                ByteBufCodecs.VAR_LONG, SyncExchangePayload::sv,
                SyncExchangePayload::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record RequestExchangeSyncPayload(BlockPos pos) implements CustomPacketPayload {

        public static final Type<RequestExchangeSyncPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(MekanismSwitch.MODID, "request_sync"));

        public static final StreamCodec<RegistryFriendlyByteBuf, RequestExchangeSyncPayload> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, RequestExchangeSyncPayload::pos,
                RequestExchangeSyncPayload::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record CancelExchangePayload(BlockPos pos, int slot) implements CustomPacketPayload {

        public static final Type<CancelExchangePayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(MekanismSwitch.MODID, "cancel_exchange"));

        public static final StreamCodec<RegistryFriendlyByteBuf, CancelExchangePayload> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, CancelExchangePayload::pos,
                ByteBufCodecs.VAR_INT, CancelExchangePayload::slot,
                CancelExchangePayload::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record CancelRepairPayload(BlockPos pos) implements CustomPacketPayload {

        public static final Type<CancelRepairPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(MekanismSwitch.MODID, "cancel_repair"));

        public static final StreamCodec<RegistryFriendlyByteBuf, CancelRepairPayload> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, CancelRepairPayload::pos,
                CancelRepairPayload::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record RollSyncPayload(boolean rolling, float roll) implements CustomPacketPayload {

        public static final Type<RollSyncPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(MekanismSwitch.MODID, "roll_sync"));

        public static final StreamCodec<RegistryFriendlyByteBuf, RollSyncPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, RollSyncPayload::rolling,
                ByteBufCodecs.FLOAT, RollSyncPayload::roll,
                RollSyncPayload::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record RollSyncS2CPayload(int entityId, boolean rolling, float roll) implements CustomPacketPayload {

        public static final Type<RollSyncS2CPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(MekanismSwitch.MODID, "roll_sync_s2c"));

        public static final StreamCodec<RegistryFriendlyByteBuf, RollSyncS2CPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, RollSyncS2CPayload::entityId,
                ByteBufCodecs.BOOL, RollSyncS2CPayload::rolling,
                ByteBufCodecs.FLOAT, RollSyncS2CPayload::roll,
                RollSyncS2CPayload::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(MekanismSwitch.MODID).versioned("2").optional();
        registrar.playToServer(StartExchangePayload.TYPE, StartExchangePayload.STREAM_CODEC, MeksPayloads::handleStartExchange);
        registrar.playToServer(RequestExchangeSyncPayload.TYPE, RequestExchangeSyncPayload.STREAM_CODEC, MeksPayloads::handleRequestSync);
        registrar.playToServer(CancelExchangePayload.TYPE, CancelExchangePayload.STREAM_CODEC, MeksPayloads::handleCancelExchange);
        registrar.playToServer(CancelRepairPayload.TYPE, CancelRepairPayload.STREAM_CODEC, MeksPayloads::handleCancelRepair);
        registrar.playToServer(RollSyncPayload.TYPE, RollSyncPayload.STREAM_CODEC, MeksPayloads::handleRollSync);
        registrar.playToClient(SyncExchangePayload.TYPE, SyncExchangePayload.STREAM_CODEC, MeksPayloads::handleSyncExchange);
        registrar.playToClient(RollSyncS2CPayload.TYPE, RollSyncS2CPayload.STREAM_CODEC, MeksPayloads::handleRollSyncS2C);
    }

    private static void handleStartExchange(StartExchangePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player
                  && player.level().getBlockEntity(payload.pos()) instanceof ExchangeSwitchTile tile) {
                tile.startExchange(player, payload.target(), payload.count(), payload.forget(), payload.slot());
            }
        });
    }

    private static void handleSyncExchange(SyncExchangePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof ExchangeSwitchContainer container) {
                container.receiveSync(payload.knowledge(), payload.sv());
            }
        });
    }

    private static void handleRequestSync(RequestExchangeSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player
                  && player.level().getBlockEntity(payload.pos()) instanceof ExchangeSwitchTile tile) {
                tile.syncToOwner(player);
            }
        });
    }

    private static void handleCancelExchange(CancelExchangePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player
                  && player.level().getBlockEntity(payload.pos()) instanceof ExchangeSwitchTile tile) {
                tile.cancelOperation(player, payload.slot());
            }
        });
    }

    private static void handleCancelRepair(CancelRepairPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player
                  && player.level().getBlockEntity(payload.pos()) instanceof RestorationSwitchTile tile) {
                tile.cancelRepair();
            }
        });
    }

    private static void handleRollSync(RollSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && player instanceof RollEntity rollPlayer) {
                rollPlayer.meksFlight$setRolling(payload.rolling());
                rollPlayer.meksFlight$setRoll(payload.rolling() ? Mth.wrapDegrees(payload.roll()) : 0.0F);
            }
        });
    }

    private static void handleRollSyncS2C(RollSyncS2CPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() != null) {
                var entity = context.player().level().getEntity(payload.entityId());
                if (entity instanceof RollEntity rollEntity) {
                    rollEntity.meksFlight$setRolling(payload.rolling());
                    rollEntity.meksFlight$setRoll(payload.rolling() ? Mth.wrapDegrees(payload.roll()) : 0.0F);
                }
            }
        });
    }

    public static void sendStartExchange(BlockPos pos, ResourceLocation target, int count, boolean forget, int slot) {
        PacketDistributor.sendToServer(new StartExchangePayload(pos, target, count, forget, slot));
    }

    public static void sendRequestSync(BlockPos pos) {
        PacketDistributor.sendToServer(new RequestExchangeSyncPayload(pos));
    }

    public static void sendCancelExchange(BlockPos pos, int slot) {
        PacketDistributor.sendToServer(new CancelExchangePayload(pos, slot));
    }

    public static void sendCancelRepair(BlockPos pos) {
        PacketDistributor.sendToServer(new CancelRepairPayload(pos));
    }
}
