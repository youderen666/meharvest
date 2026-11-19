package com.meharvest;

import appeng.blockentity.AEBaseBlockEntity;
import com.meharvest.network.Network;
import com.meharvest.registry.ModBlockEntities;
import com.meharvest.registry.ModBlocks;
import com.meharvest.registry.ModItems;
import com.meharvest.registry.ModMenus;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(MEHarvest.MODID)
public final class MEHarvest {
    public static final String MODID = "meharvest";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public MEHarvest() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModMenus.MENUS.register(modBus);

        modBus.addListener(this::addCreative);
        modBus.addListener(this::commonSetup);
        Network.init();
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MODID, path);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // 让 AE2 知道这个方块实体代表哪个物品（节点可视化、名称、提示图标都用得上）
        event.enqueueWork(() -> AEBaseBlockEntity.registerBlockEntityItem(
                ModBlockEntities.HARVEST_FLAG.get(), ModItems.HARVEST_FLAG.get()));
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModItems.HARVEST_FLAG.get());
        }
    }
}
