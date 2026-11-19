package com.meharvest.logic;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 按方块注册名做软兼容，不引用任何可选模组的类，避免编译期依赖。
 */
public final class CropCompat {
    public enum Mode {
        /** 普通作物：收完原地补种 */
        NORMAL,
        /** 只收割，不补种 */
        HARVEST_ONLY,
        /** 不是可收割目标（例如番茄的基部、水稻的下段） */
        SKIP,
        /** 多格作物：只认最下面一格，整列收割后补种 */
        RICE_COLUMN,
        /** 藤蔓类：靠 grape 类属性判断成熟，收完把属性归零，不消耗种子 */
        GRAPE
    }

    private record Rule(Mode mode, @Nullable ResourceLocation seed) {
    }

    private static final Rule UNKNOWN = new Rule(null, null);
    private static final Map<ResourceLocation, Rule> RULES = new HashMap<>();
    private static final Map<ResourceLocation, ResourceLocation> COLUMN_PARTNERS = new HashMap<>();
    private static final Map<Block, Rule> CACHE = new HashMap<>();

    static {
        // Farmer's Delight / 农夫乐事
        rule("farmersdelight:cabbages", Mode.NORMAL, "farmersdelight:cabbage_seeds");
        rule("farmersdelight:onions", Mode.NORMAL, "farmersdelight:onion");
        rule("farmersdelight:tomatoes", Mode.NORMAL, "farmersdelight:tomato_seeds");
        rule("farmersdelight:tomatoes_on_rope", Mode.NORMAL, "farmersdelight:tomato_seeds");
        rule("farmersdelight:budding_tomatoes", Mode.SKIP, null);
        // 水稻是两格：下面 farmersdelight:rice，上面 farmersdelight:rice_panicles
        rule("farmersdelight:rice", Mode.RICE_COLUMN, "farmersdelight:rice");
        rule("farmersdelight:rice_panicles", Mode.SKIP, null);
        columnPartner("farmersdelight:rice", "farmersdelight:rice_panicles");

        // Kaleidoscope Cookery / 森罗物语：厨房
        rule("kaleidoscope_cookery:chili_crop", Mode.NORMAL, "kaleidoscope_cookery:chili_seed");
        rule("kaleidoscope_cookery:lettuce_crop", Mode.NORMAL, "kaleidoscope_cookery:lettuce_seed");
        rule("kaleidoscope_cookery:tomato_crop", Mode.NORMAL, "kaleidoscope_cookery:tomato_seed");
        // 三格同 ID，只有 location=0 掉产物；每次收割固定返 1 野生稻米，正好拿来补种
        rule("kaleidoscope_cookery:rice_crop", Mode.RICE_COLUMN, "kaleidoscope_cookery:wild_rice");
    }

    private CropCompat() {
    }

    private static void rule(String blockId, Mode mode, @Nullable String seedId) {
        RULES.put(new ResourceLocation(blockId),
                new Rule(mode, seedId == null ? null : new ResourceLocation(seedId)));
    }

    private static void columnPartner(String bottomId, String upperId) {
        COLUMN_PARTNERS.put(new ResourceLocation(bottomId), new ResourceLocation(upperId));
    }

    @Nullable
    public static Mode modeOf(Block block) {
        return rule(block).mode();
    }

    @Nullable
    public static ResourceLocation seedOf(Block block) {
        return rule(block).seed();
    }

    /**
     * 多格作物的上半部分（例如农夫乐事的水稻稻穗），收割整列时一并清除。
     */
    @Nullable
    public static ResourceLocation columnPartnerOf(Block block) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        return id == null ? null : COLUMN_PARTNERS.get(id);
    }

    private static Rule rule(Block block) {
        return CACHE.computeIfAbsent(block, b -> {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(b);
            Rule known = id == null ? null : RULES.get(id);
            if (known != null) {
                return known;
            }
            // 未登记但带 grape 属性的藤蔓（葡萄藤之类），按藤蔓作物处理
            if (grapeProperty(b.defaultBlockState()) != null) {
                return new Rule(Mode.GRAPE, null);
            }
            // 名字里带 grape 但属性名不同的，退化成普通作物
            if (id != null && id.getPath().toLowerCase(Locale.ROOT).contains("grape")) {
                return new Rule(Mode.NORMAL, null);
            }
            return UNKNOWN;
        });
    }

    /**
     * 查找名为 grape / grapes 的整型属性，用来兼容各类葡萄藤。
     */
    @Nullable
    public static IntegerProperty grapeProperty(BlockState state) {
        for (Property<?> property : state.getProperties()) {
            if (property instanceof IntegerProperty integer) {
                String name = property.getName().toLowerCase(Locale.ROOT);
                if (name.equals("grape") || name.equals("grapes")) {
                    return integer;
                }
            }
        }
        return null;
    }
}
