package com.meharvest.registry;

import com.meharvest.MEHarvest;
import com.meharvest.blockentity.HarvestFlagBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MEHarvest.MODID);

    public static final RegistryObject<BlockEntityType<HarvestFlagBlockEntity>> HARVEST_FLAG =
            BLOCK_ENTITIES.register("me_harvest_flag",
                    () -> BlockEntityType.Builder
                            .of(HarvestFlagBlockEntity::new, ModBlocks.HARVEST_FLAG.get())
                            .build(null));

    private ModBlockEntities() {
    }
}
