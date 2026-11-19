package com.meharvest.registry;

import com.meharvest.MEHarvest;
import com.meharvest.menu.HarvestFlagMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, MEHarvest.MODID);

    public static final RegistryObject<MenuType<HarvestFlagMenu>> HARVEST_FLAG =
            MENUS.register("me_harvest_flag",
                    () -> IForgeMenuType.create(HarvestFlagMenu::new));

    private ModMenus() {
    }
}
