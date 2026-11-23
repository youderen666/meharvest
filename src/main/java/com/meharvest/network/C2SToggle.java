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

public record C2SToggle(BlockPos pos, boolean enabled) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<C2SToggle> TYPE =
            new CustomPacketPayload.Type<>(MEHarvest.id("toggle"));

    public static final StreamCodec<FriendlyByteBuf, C2SToggle> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, C2SToggle::pos,
            ByteBufCodecs.BOOL, C2SToggle::enabled,
            C2SToggle::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(C2SToggle payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (player.distanceToSqr(Vec3.atCenterOf(payload.pos())) > 64.0D) {
                return;
            }
            if (player.level().getBlockEntity(payload.pos()) instanceof HarvestFlagBlockEntity flag) {
                flag.setEnabled(payload.enabled());
            }
        });
    }
}
