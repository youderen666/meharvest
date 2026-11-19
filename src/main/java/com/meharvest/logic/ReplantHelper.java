package com.meharvest.logic;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ReplantHelper {
    private static final Map<Block, Item> SEED_CACHE = new ConcurrentHashMap<>();

    private ReplantHelper() {
    }

    /**
     * 优先用兼容表里登记的种子；没有登记或物品不存在时，退回扫描 ItemNameBlockItem。
     */
    public static Item seedFor(Block crop, @Nullable ResourceLocation override) {
        if (override != null) {
            Item item = BuiltInRegistries.ITEM.get(override);
            if (item != Items.AIR) {
                return item;
            }
        }
        return seedFor(crop);
    }

    public static Item seedFor(Block crop) {
        Item cached = SEED_CACHE.computeIfAbsent(crop, block -> {
            for (Item item : BuiltInRegistries.ITEM) {
                if (item instanceof ItemNameBlockItem named && named.getBlock() == block) {
                    return item;
                }
            }
            return Items.AIR;
        });
        return cached == Items.AIR ? null : cached;
    }
}
