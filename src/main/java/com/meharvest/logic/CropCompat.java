package com.meharvest.logic;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
        GRAPE,
        /** 只摘果、不动本体：把生长状态回退，作物自己继续长（甜浆果丛、洞穴藤蔓等） */
        REGROW
    }

    /** 用来判断“果还在不在”的布尔属性名，兼容各模组的命名习惯。 */
    private static final Set<String> FRUIT_FLAGS = Set.of(
            "berries", "has_berries", "has_fruit", "fruiting", "fruit", "ripe", "fruited");

    private record Rule(Mode mode,
                        @Nullable ResourceLocation seed,
                        int resetAge,
                        @Nullable String fruitFlag) {

        static Rule of(Mode mode, @Nullable ResourceLocation seed) {
            return new Rule(mode, seed, -1, null);
        }

        static Rule regrow(int resetAge, @Nullable String fruitFlag) {
            return new Rule(Mode.REGROW, null, resetAge, fruitFlag);
        }
    }

    private static final Rule UNKNOWN = Rule.of(null, null);
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

        // 原版：只摘果、本体留着继续长
        // 甜浆果丛：age 3 摘完回到 1（和原版右键一致），不需要重新种
        regrowRule("minecraft:sweet_berry_bush", 1, null);
        // 洞穴藤蔓：berries=true → false
        regrowRule("minecraft:cave_vines", -1, "berries");
        regrowRule("minecraft:cave_vines_plant", -1, "berries");

        // 西瓜/南瓜的茎：本身不产东西，拆了反而毁掉结果能力，直接跳过
        rule("minecraft:melon_stem", Mode.SKIP, null);
        rule("minecraft:pumpkin_stem", Mode.SKIP, null);
        rule("minecraft:attached_melon_stem", Mode.SKIP, null);
        rule("minecraft:attached_pumpkin_stem", Mode.SKIP, null);
    }

    private CropCompat() {
    }

    private static void rule(String blockId, Mode mode, @Nullable String seedId) {
        RULES.put(new ResourceLocation(blockId),
                Rule.of(mode, seedId == null ? null : new ResourceLocation(seedId)));
    }

    private static void regrowRule(String blockId, int resetAge, @Nullable String fruitFlag) {
        RULES.put(new ResourceLocation(blockId), Rule.regrow(resetAge, fruitFlag));
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

    /** REGROW 模式下摘完果要把 age 回退到几；-1 表示不动 age。 */
    public static int resetAgeOf(Block block) {
        return rule(block).resetAge();
    }

    /** REGROW 模式下代表“果还在”的布尔属性名；null 表示靠 age 判断。 */
    @Nullable
    public static String fruitFlagOf(Block block) {
        return rule(block).fruitFlag();
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

            BlockState defaultState = b.defaultBlockState();

            // 未登记但带 grape 属性的藤蔓（葡萄藤之类），按藤蔓作物处理
            if (grapeProperty(defaultState) != null) {
                return Rule.of(Mode.GRAPE, null);
            }

            // 带 berries / has_fruit 这类布尔属性的，一律当“只摘果”处理，本体留着继续长
            String flag = fruitFlagName(defaultState);
            if (flag != null) {
                return Rule.regrow(-1, flag);
            }

            // 名字里带 berry 的灌木/丛，按甜浆果丛的逻辑：摘完回退到 age 1
            if (id != null && hasAge(defaultState)) {
                String path = id.getPath().toLowerCase(Locale.ROOT);
                if (path.contains("berry") || path.contains("berries")) {
                    return Rule.regrow(1, null);
                }
            }

            // 名字里带 grape 但属性名不同的，退化成普通作物
            if (id != null && id.getPath().toLowerCase(Locale.ROOT).contains("grape")) {
                return Rule.of(Mode.NORMAL, null);
            }
            return UNKNOWN;
        });
    }

    private static boolean hasAge(BlockState state) {
        for (Property<?> property : state.getProperties()) {
            if (property instanceof IntegerProperty integer && "age".equals(property.getName())) {
                return integer.getPossibleValues().size() > 1;
            }
        }
        return false;
    }

    @Nullable
    private static String fruitFlagName(BlockState state) {
        for (Property<?> property : state.getProperties()) {
            if (property instanceof BooleanProperty) {
                String name = property.getName().toLowerCase(Locale.ROOT);
                if (FRUIT_FLAGS.contains(name)) {
                    return property.getName();
                }
            }
        }
        return null;
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
