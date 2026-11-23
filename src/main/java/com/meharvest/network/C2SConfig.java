package com.meharvest.network;

import com.meharvest.MEHarvest;
import com.meharvest.blockentity.HarvestFlagBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record C2SConfig(BlockPos pos, int radius, int interval, int batch) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<C2SConfig> TYPE =
            new CustomPacketPayload.Type<>(MEHarvest.id("config"));

    public static final StreamCodec<FriendlyByteBuf, C2SConfig> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, C2SConfig::pos,
            ByteBufCodecs.VAR_INT, C2SConfig::radius,
            ByteBufCodecs.VAR_INT, C2SConfig::interval,
            ByteBufCodecs.VAR_INT, C2SConfig::batch,
            C2SConfig::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(C2SConfig payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (player.distanceToSqr(Vec3.atCenterOf(payload.pos())) > 64.0D) {
                return;
            }
            if (player.level().getBlockEntity(payload.pos()) instanceof HarvestFlagBlockEntity flag) {
                flag.applyConfig(payload.radius(), payload.interval(), payload.batch());
            }
        });
    }
}
