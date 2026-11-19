package com.meharvest.logic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public final class HarvestRules {
    /**
     * 补种时需要原样保留的结构属性，避免番茄脱离绳子、水稻丢水等。
     */
    private static final Set<String> STRUCTURAL = Set.of(
            "ropelogged", "waterlogged", "facing", "axis", "attached",
            "up", "down", "north", "south", "east", "west");

    private HarvestRules() {
    }

    public static boolean isCropLike(BlockState state) {
        Block block = state.getBlock();
        CropCompat.Mode mode = CropCompat.modeOf(block);

        if (mode == CropCompat.Mode.SKIP) {
            return false;
        }
        if (mode == CropCompat.Mode.RICE_COLUMN) {
            return isRiceBottom(state);
        }
        if (mode == CropCompat.Mode.HARVEST_ONLY) {
            return ageProperty(state) != null;
        }
        if (mode == CropCompat.Mode.GRAPE) {
            return grapeProperty(state) != null;
        }
        if (mode == CropCompat.Mode.NORMAL) {
            return ageProperty(state) != null || isFruit(state);
        }

        if (isTallPlant(state) || isFruit(state)) {
            return true;
        }
        if (state.is(Blocks.NETHER_WART) || state.is(Blocks.COCOA) || state.is(Blocks.SWEET_BERRY_BUSH)) {
            return true;
        }
        return ageProperty(state) != null
                && (block instanceof CropBlock || block instanceof BonemealableBlock);
    }

    public static boolean isMature(Level level, BlockPos pos, BlockState state) {
        CropCompat.Mode mode = CropCompat.modeOf(state.getBlock());
        if (mode == CropCompat.Mode.SKIP) {
            return false;
        }
        if (mode == CropCompat.Mode.RICE_COLUMN) {
            return isRiceMature(state);
        }
        if (mode == CropCompat.Mode.GRAPE) {
            IntegerProperty grape = grapeProperty(state);
            return grape != null && state.getValue(grape) >= maxAge(grape);
        }

        if (isTallPlant(state)) {
            return isTallTop(level, pos, state);
        }
        if (isFruit(state)) {
            return hasStem(level, pos);
        }

        IntegerProperty age = ageProperty(state);
        return age != null && state.getValue(age) >= maxAge(age);
    }

    public static boolean isTallPlant(BlockState state) {
        Block block = state.getBlock();
        return block instanceof SugarCaneBlock
                || block instanceof CactusBlock
                || block instanceof BambooStalkBlock
                || state.is(Blocks.SUGAR_CANE)
                || state.is(Blocks.CACTUS)
                || state.is(Blocks.BAMBOO);
    }

    public static boolean isFruit(BlockState state) {
        return state.is(Blocks.MELON) || state.is(Blocks.PUMPKIN);
    }

    /**
     * 多格作物的最底下一格：森罗物语水稻看 location=0，农夫乐事水稻看 supporting=false。
     */
    private static boolean isRiceBottom(BlockState state) {
        IntegerProperty location = intProperty(state, "location");
        if (location != null) {
            return state.getValue(location) == 0;
        }
        BooleanProperty supporting = boolProperty(state, "supporting");
        return supporting == null || !state.getValue(supporting);
    }

    private static boolean isRiceMature(BlockState state) {
        IntegerProperty age = ageProperty(state);
        return isRiceBottom(state) && age != null && state.getValue(age) >= maxAge(age);
    }

    /**
     * 判断某格是否属于以 bottom 为底的多格作物（同 ID 或登记过的上半部分）。
     */
    public static boolean isColumnPartner(Block bottom, BlockState above) {
        if (above.is(bottom)) {
            return true;
        }
        ResourceLocation partner = CropCompat.columnPartnerOf(bottom);
        return partner != null && partner.equals(BuiltInRegistries.BLOCK.getKey(above.getBlock()));
    }

    private static boolean isTallTop(Level level, BlockPos pos, BlockState state) {
        Block block = state.getBlock();
        if (level.getBlockState(pos.above()).is(block)) {
            return false;
        }
        return level.getBlockState(pos.below()).is(block)
                && level.getBlockState(pos.below(2)).is(block);
    }

    private static boolean hasStem(Level level, BlockPos pos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            Block block = level.getBlockState(pos.relative(direction)).getBlock();
            if (block instanceof StemBlock || block instanceof AttachedStemBlock) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public static IntegerProperty ageProperty(BlockState state) {
        return intProperty(state, "age");
    }

    @Nullable
    public static IntegerProperty grapeProperty(BlockState state) {
        return CropCompat.grapeProperty(state);
    }

    @Nullable
    public static BooleanProperty boolProperty(BlockState state, String name) {
        for (Property<?> property : state.getProperties()) {
            if (property instanceof BooleanProperty bool && name.equals(property.getName())) {
                return bool;
            }
        }
        return null;
    }

    @Nullable
    public static IntegerProperty intProperty(BlockState state, String name) {
        for (Property<?> property : state.getProperties()) {
            if (property instanceof IntegerProperty integer && name.equals(property.getName())) {
                return integer;
            }
        }
        return null;
    }

    private static int maxAge(IntegerProperty property) {
        int max = 0;
        for (int value : property.getPossibleValues()) {
            max = Math.max(max, value);
        }
        return max;
    }

    public static int minValue(IntegerProperty property) {
        int min = Integer.MAX_VALUE;
        for (int value : property.getPossibleValues()) {
            min = Math.min(min, value);
        }
        return min == Integer.MAX_VALUE ? 0 : min;
    }

    /**
     * 把旧状态里的结构属性复制到新状态，避免补种后改变朝向/绳子/含水状态。
     */
    public static BlockState copyStructural(BlockState from, BlockState to) {
        for (Property<?> property : from.getProperties()) {
            if (!STRUCTURAL.contains(property.getName())) {
                continue;
            }
            Property<?> target = to.getBlock().getStateDefinition().getProperty(property.getName());
            if (target == null) {
                continue;
            }
            to = copyValue(to, target, from, property);
        }
        return to;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState copyValue(BlockState to, Property target, BlockState from, Property source) {
        return to.setValue((Property) target, from.getValue((Property) source));
    }
}
