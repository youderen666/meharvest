package com.meharvest.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class HarvestFlagItem extends BlockItem {
    public HarvestFlagItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.meharvest.me_harvest_flag.1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.meharvest.me_harvest_flag.2").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.meharvest.me_harvest_flag.3").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.meharvest.me_harvest_flag.4").withStyle(ChatFormatting.DARK_GRAY));
    }
}
