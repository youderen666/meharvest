package com.meharvest.network;

import com.meharvest.blockentity.HarvestFlagBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record C2SToggle(BlockPos pos, boolean enabled) {
    public static void encode(C2SToggle message, FriendlyByteBuf buf) {
        buf.writeBlockPos(message.pos);
        buf.writeBoolean(message.enabled);
    }

    public static C2SToggle decode(FriendlyByteBuf buf) {
        return new C2SToggle(buf.readBlockPos(), buf.readBoolean());
    }

    public static void handle(C2SToggle message, Supplier<NetworkEvent.Context> supplier) {
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
                flag.setEnabled(message.enabled);
            }
        });
        context.setPacketHandled(true);
    }
}
