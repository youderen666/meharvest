package com.meharvest;

import appeng.api.AECapabilities;
import appeng.blockentity.AEBaseBlockEntity;
import com.meharvest.network.Network;
import com.meharvest.registry.ModBlockEntities;
import com.meharvest.registry.ModBlocks;
import com.meharvest.registry.ModItems;
import com.meharvest.registry.ModMenus;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(MEHarvest.MODID)
public final class MEHarvest {
    public static final String MODID = "meharvest";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public MEHarvest(IEventBus modBus) {
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModMenus.MENUS.register(modBus);

        modBus.addListener(this::commonSetup);
        modBus.addListener(this::registerCapabilities);
        modBus.addListener(Network::register);
        modBus.addListener(this::addCreative);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // 让 AE2 知道这个方块实体代表哪个物品（节点可视化、名称、提示图标都用得上）
        event.enqueueWork(() -> AEBaseBlockEntity.registerBlockEntityItem(
                ModBlockEntities.HARVEST_FLAG.get(), ModItems.HARVEST_FLAG.get()));
    }

    /**
     * NeoForge 1.21 的能力不再走 BlockEntity#getCapability，必须在这里集中登记。
     * 少了 IN_WORLD_GRID_NODE_HOST 这条，线缆就找不到机器；少了 EnergyStorage 就没有 FE 输入。
     */
    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(AECapabilities.IN_WORLD_GRID_NODE_HOST,
                ModBlockEntities.HARVEST_FLAG.get(), (be, context) -> be);

        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.HARVEST_FLAG.get(), (be, side) -> be.getEnergyStorage(side));
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModItems.HARVEST_FLAG.get());
        }
    }
}
