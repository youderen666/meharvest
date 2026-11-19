package com.meharvest.network;

import com.meharvest.blockentity.HarvestFlagBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record C2SConfig(BlockPos pos, int radius, int interval, int batch) {
    public static void encode(C2SConfig message, FriendlyByteBuf buf) {
        buf.writeBlockPos(message.pos);
        buf.writeVarInt(message.radius);
        buf.writeVarInt(message.interval);
        buf.writeVarInt(message.batch);
    }

    public static C2SConfig decode(FriendlyByteBuf buf) {
        return new C2SConfig(buf.readBlockPos(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(C2SConfig message, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            if (player.distanceToSqr(Vec3.atCenterOf(message.pos)) > 64.0D) {
                return;
            }
            if (player.level().getBlockEntity(message.pos) instanceof HarvestFlagBlockEntity flag) {
                flag.applyConfig(message.radius, message.interval, message.batch);
            }
        });
        context.setPacketHandled(true);
    }
}
