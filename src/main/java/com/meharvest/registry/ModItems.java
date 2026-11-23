package com.meharvest.registry;

import com.meharvest.MEHarvest;
import com.meharvest.item.HarvestFlagItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MEHarvest.MODID);

    public static final DeferredItem<HarvestFlagItem> HARVEST_FLAG =
            ITEMS.register("me_harvest_flag",
                    () -> new HarvestFlagItem(ModBlocks.HARVEST_FLAG.get(), new Item.Properties()));

    private ModItems() {
    }
}
