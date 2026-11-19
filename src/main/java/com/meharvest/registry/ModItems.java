package com.meharvest.registry;

import com.meharvest.MEHarvest;
import com.meharvest.item.HarvestFlagItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MEHarvest.MODID);

    public static final RegistryObject<Item> HARVEST_FLAG =
            ITEMS.register("me_harvest_flag",
                    () -> new HarvestFlagItem(ModBlocks.HARVEST_FLAG.get(), new Item.Properties()));

    private ModItems() {
    }
}
