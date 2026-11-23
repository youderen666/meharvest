package com.meharvest.registry;

import com.meharvest.MEHarvest;
import com.meharvest.blockentity.HarvestFlagBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MEHarvest.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HarvestFlagBlockEntity>> HARVEST_FLAG =
            BLOCK_ENTITIES.register("me_harvest_flag",
                    () -> BlockEntityType.Builder
                            .of(HarvestFlagBlockEntity::new, ModBlocks.HARVEST_FLAG.get())
                            .build(null));

    private ModBlockEntities() {
    }
}
