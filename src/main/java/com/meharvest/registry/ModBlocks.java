package com.meharvest.registry;

import com.meharvest.MEHarvest;
import com.meharvest.block.HarvestFlagBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MEHarvest.MODID);

    public static final DeferredBlock<HarvestFlagBlock> HARVEST_FLAG =
            BLOCKS.register("me_harvest_flag", () -> new HarvestFlagBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0F, 6.0F)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops()));

    private ModBlocks() {
    }
}
