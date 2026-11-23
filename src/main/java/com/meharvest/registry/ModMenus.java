package com.meharvest.registry;

import com.meharvest.MEHarvest;
import com.meharvest.menu.HarvestFlagMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, MEHarvest.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<HarvestFlagMenu>> HARVEST_FLAG =
            MENUS.register("me_harvest_flag",
                    () -> IMenuTypeExtension.create(HarvestFlagMenu::new));

    private ModMenus() {
    }
}
