package com.meharvest.menu;

import com.meharvest.blockentity.HarvestFlagBlockEntity;
import com.meharvest.registry.ModBlocks;
import com.meharvest.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

public class HarvestFlagMenu extends AbstractContainerMenu {
    /** radius / interval / batch / status / index / buffer / power / maxPower / enabled */
    private static final int DATA_SLOTS = 9;

    private final int[] clientCache = new int[DATA_SLOTS];
    private final HarvestFlagBlockEntity blockEntity;
    private final ContainerData data;

    public HarvestFlagMenu(int id, Inventory inventory, FriendlyByteBuf buf) {
        this(id, inventory, resolve(inventory, buf.readBlockPos()), true);
    }

    public HarvestFlagMenu(int id, Inventory inventory, @Nullable HarvestFlagBlockEntity blockEntity, boolean marker) {
        super(ModMenus.HARVEST_FLAG.get(), id);
        this.blockEntity = blockEntity;
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                if (blockEntity != null && blockEntity.getLevel() != null
                        && !blockEntity.getLevel().isClientSide) {
                    return switch (index) {
                        case 0 -> blockEntity.getRadius();
                        case 1 -> blockEntity.getInterval();
                        case 2 -> blockEntity.getBatch();
                        case 3 -> blockEntity.getStatus();
                        case 4 -> blockEntity.getIndexSize();
                        case 5 -> blockEntity.getBufferCount();
                        case 6 -> blockEntity.getStoredPowerInt();
                        case 7 -> blockEntity.getMaxStoredPowerInt();
                        default -> blockEntity.isEnabled() ? 1 : 0;
                    };
                }
                return clientCache[index];
            }

            @Override
            public void set(int index, int value) {
                if (index >= 0 && index < clientCache.length) {
                    clientCache[index] = value;
                }
            }

            @Override
            public int getCount() {
                return DATA_SLOTS;
            }
        };
        addDataSlots(this.data);
    }

    @Nullable
    private static HarvestFlagBlockEntity resolve(Inventory inventory, BlockPos pos) {
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        return blockEntity instanceof HarvestFlagBlockEntity flag ? flag : null;
    }

    public int get(int index) {
        return data.get(index);
    }

    public int getStoredPower() {
        return data.get(6);
    }

    public int getMaxStoredPower() {
        return Math.max(1, data.get(7));
    }

    public boolean isEnabled() {
        return data.get(8) != 0;
    }

    public BlockPos getPos() {
        return blockEntity != null ? blockEntity.getBlockPos() : BlockPos.ZERO;
    }

    @Override
    public boolean stillValid(Player player) {
        if (blockEntity == null || blockEntity.getLevel() == null) {
            return false;
        }
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
                player, ModBlocks.HARVEST_FLAG.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
