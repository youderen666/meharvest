package com.meharvest.integration;

import appeng.api.integrations.igtooltip.ClientRegistration;
import appeng.api.integrations.igtooltip.CommonRegistration;
import appeng.api.integrations.igtooltip.TooltipProvider;
import appeng.integration.modules.igtooltip.blocks.GridNodeStateDataProvider;
import appeng.integration.modules.igtooltip.blocks.PowerStorageDataProvider;
import com.meharvest.MEHarvest;
import com.meharvest.block.HarvestFlagBlock;
import com.meharvest.blockentity.HarvestFlagBlockEntity;
import net.minecraft.resources.ResourceLocation;

/**
 * 把本机器的方块实体登记给 AE2 的内置悬浮提示（Jade / WTHIT / TOP），
 * 复用 AE2 自己的两个 provider：
 * <ul>
 *   <li>{@link GridNodeStateDataProvider} → 设备在线 / 设备离线 / 缺少频道</li>
 *   <li>{@link PowerStorageDataProvider} → 已存储：X / Y AE</li>
 * </ul>
 * 也就是充能器那种提示样式。
 * <p>
 * 注意：这里必须用 addBlockEntityData / addBlockEntityBody 精确登记本 mod 的类，
 * 不能用 addBaseBlockEntity —— 因为我们的方块实体继承自 AE2 的 AEBaseBlockEntity，
 * 走 base class 注册会把 AE2 自己的默认注册顶掉，导致所有 AE2 机器失去提示。
 * <p>
 * 该实现通过 {@code META-INF/services} 由 AE2 的 ServiceLoader 发现。
 */
public final class MEHarvestTooltips implements TooltipProvider {
    private static final ResourceLocation GRID_NODE_STATE = MEHarvest.id("grid_node_state");
    private static final ResourceLocation POWER_STORAGE = MEHarvest.id("power_storage");

    @Override
    public void registerCommon(CommonRegistration registration) {
        registration.addBlockEntityData(HarvestFlagBlockEntity.class, new GridNodeStateDataProvider());
        registration.addBlockEntityData(HarvestFlagBlockEntity.class, new PowerStorageDataProvider());
        MEHarvest.LOGGER.info("[meharvest] registered in-game tooltip providers for ME harvest machine");
    }

    @Override
    public void registerClient(ClientRegistration registration) {
        registration.addBlockEntityBody(HarvestFlagBlockEntity.class, HarvestFlagBlock.class,
                GRID_NODE_STATE, new GridNodeStateDataProvider());
        registration.addBlockEntityBody(HarvestFlagBlockEntity.class, HarvestFlagBlock.class,
                POWER_STORAGE, new PowerStorageDataProvider());
    }
}
