package com.meharvest.registry;

import com.meharvest.MEHarvest;
import com.meharvest.block.HarvestFlagBlock;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, MEHarvest.MODID);

    public static final RegistryObject<Block> HARVEST_FLAG =
            BLOCKS.register("me_harvest_flag", HarvestFlagBlock::new);

    private ModBlocks() {
    }
}
