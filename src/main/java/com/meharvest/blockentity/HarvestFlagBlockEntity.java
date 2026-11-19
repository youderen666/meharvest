package com.meharvest.blockentity;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGrid;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.MEStorage;
import appeng.blockentity.grid.AENetworkPowerBlockEntity;
import appeng.capabilities.Capabilities;
import appeng.me.helpers.MachineSource;
import com.meharvest.logic.CropCompat;
import com.meharvest.logic.HarvestRules;
import com.meharvest.logic.ReplantHelper;
import com.meharvest.menu.HarvestFlagMenu;
import com.meharvest.registry.ModBlockEntities;
import com.meharvest.registry.ModItems;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * ME 农收机。
 * <p>
 * 继承 AE2 的 {@link AENetworkPowerBlockEntity}：它同时提供
 * <ul>
 *   <li>世界内网格节点（{@code setInWorldNode(true)}，线缆/控制器才认得出它）</li>
 *   <li>内置 AE 电量缓存（{@code IAEPowerStorage}），因此悬停时能显示“已存储 / 设备在线”</li>
 *   <li>Forge Energy 输入（{@code IEnergyStorage}），可用 FE 线缆直接充电</li>
 * </ul>
 */
public class HarvestFlagBlockEntity extends AENetworkPowerBlockEntity implements MenuProvider {
    public static final int MIN_RADIUS = 3;
    public static final int MAX_RADIUS = 16;
    public static final int MIN_INTERVAL = 20;
    public static final int MAX_INTERVAL = 400;
    public static final int MAX_BATCH = 16;

    /** 内置缓存上限（AE）。必须 <= 32767，因为菜单数据槽走 short 同步。 */
    public static final int MAX_STORED_POWER = 10000;
    /** 每 tick 最多从 ME 网络抽取多少 AE 充进缓存。 */
    private static final double RECHARGE_RATE = 500.0;

    private static final int MAX_BUFFER = 9;
    private static final double HARVEST_COST = 6.0;
    private static final double IDLE_POWER = 0.2;
    private static final int REBUILD_PERIOD = 1200;
    private static final int VERTICAL_RANGE = 3;

    private final IActionSource actionSource = new MachineSource(this);
    private final LongArrayList positions = new LongArrayList();
    private final List<ItemStack> buffer = new ArrayList<>();
    private final BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();

    private int radius = 9;
    private int baseInterval = 40;
    private int batch = 1;
    private int cooldown = 20;
    private int misses;
    private int scanCursor;
    private long lastRebuild = -REBUILD_PERIOD;
    private boolean rebuildPending = true;
    private boolean enabled = true;

    public HarvestFlagBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.HARVEST_FLAG.get(), pos, state);
    }

    public HarvestFlagBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        // 内置电量缓存：机器的“油箱”
        setInternalMaxPower(MAX_STORED_POWER);
        getMainNode().setVisualRepresentation(AEItemKey.of(ModItems.HARVEST_FLAG.get()));
    }

    @Override
    protected IManagedGridNode createMainNode() {
        // 注意：父类会补上 setInWorldNode(true) / addService(IAEPowerStorage) / setTagName
        return super.createMainNode()
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .setIdlePowerUsage(IDLE_POWER);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        if (capability == Capabilities.IN_WORLD_GRID_NODE_HOST) {
            return LazyOptional.of(() -> this).cast();
        }
        return super.getCapability(capability, side);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability) {
        if (capability == Capabilities.IN_WORLD_GRID_NODE_HOST) {
            return LazyOptional.of(() -> this).cast();
        }
        return super.getCapability(capability);
    }

    @Override
    public InternalInventory getInternalInventory() {
        return InternalInventory.empty();
    }

    @Override
    public void onChangeInventory(InternalInventory inv, int slot) {
        setChanged();
    }

    // ------------------------------------------------------------------
    // 存档
    // ------------------------------------------------------------------

    @Override
    public void loadTag(CompoundTag tag) {
        super.loadTag(tag);

        radius = Mth.clamp(tag.getInt("Radius"), MIN_RADIUS, MAX_RADIUS);
        baseInterval = Mth.clamp(tag.getInt("Interval"), MIN_INTERVAL, MAX_INTERVAL);
        batch = Mth.clamp(tag.getInt("Batch"), 1, MAX_BATCH);
        // 老存档没有这个字段，默认开着
        enabled = !tag.contains("Enabled") || tag.getBoolean("Enabled");

        buffer.clear();
        ListTag stored = tag.getList("Buffer", Tag.TAG_COMPOUND);
        for (int i = 0; i < stored.size() && i < MAX_BUFFER; i++) {
            ItemStack stack = ItemStack.of(stored.getCompound(i));
            if (!stack.isEmpty()) {
                buffer.add(stack);
            }
        }
        rebuildPending = true;
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        tag.putInt("Radius", radius);
        tag.putInt("Interval", baseInterval);
        tag.putInt("Batch", batch);
        tag.putBoolean("Enabled", enabled);

        ListTag stored = new ListTag();
        for (ItemStack stack : buffer) {
            stored.add(stack.save(new CompoundTag()));
        }
        tag.put("Buffer", stored);
    }

    // ------------------------------------------------------------------
    // 每 tick
    // ------------------------------------------------------------------

    public void serverTick() {
        if (level == null || level.isClientSide) {
            return;
        }

        long now = level.getGameTime();

        rechargeFromGrid();

        if (now % 20 == 0) {
            flushBuffer();
        }

        if (rebuildPending) {
            rebuildPending = false;
            if (level.hasChunkAt(worldPosition)) {
                rebuildIndex();
            }
        }

        // 关机：仍然充电、仍然把缓冲区里的产物送进网络，但不干活
        if (!enabled) {
            cooldown = 20;
            return;
        }

        if (!getMainNode().isActive()) {
            cooldown = 40;
            return;
        }

        if (--cooldown > 0) {
            return;
        }
        runCycle(now);
    }

    /** 从 ME 网络往内置缓存里充电（缓存只进不出，不会反过来被网络抽干）。 */
    private void rechargeFromGrid() {
        double missing = getInternalMaxPower() - getInternalCurrentPower();
        if (missing <= 0.01D) {
            return;
        }
        IGrid grid = getMainNode().getGrid();
        if (grid == null) {
            return;
        }
        double pulled = grid.getEnergyService()
                .extractAEPower(Math.min(missing, RECHARGE_RATE), Actionable.MODULATE, PowerMultiplier.CONFIG);
        if (pulled > 0) {
            setInternalCurrentPower(Math.min(getInternalMaxPower(), getInternalCurrentPower() + pulled));
        }
    }

    private void runCycle(long now) {
        if (now - lastRebuild > REBUILD_PERIOD) {
            rebuildIndex();
        }

        int size = positions.size();
        if (size == 0) {
            cooldown = MAX_INTERVAL;
            return;
        }

        int harvested = 0;
        int budget = Math.min(size, Math.max(64, size / 10));

        for (int step = 0; step < budget && !positions.isEmpty(); step++) {
            if (scanCursor >= positions.size()) {
                scanCursor = 0;
            }

            long packed = positions.getLong(scanCursor);
            probe.set(BlockPos.getX(packed), BlockPos.getY(packed), BlockPos.getZ(packed));

            if (!level.hasChunkAt(probe)) {
                scanCursor++;
                continue;
            }

            BlockState state = level.getBlockState(probe);
            if (!HarvestRules.isCropLike(state)) {
                positions.removeLong(scanCursor);
                continue;
            }

            if (harvested < batch && HarvestRules.isMature(level, probe, state)
                    && harvest(probe.immutable(), state)) {
                harvested++;
            }
            scanCursor++;
        }

        if (harvested > 0) {
            misses = 0;
            cooldown = baseInterval;
        } else {
            misses = Math.min(misses + 1, 3);
            cooldown = Math.min(baseInterval << misses, MAX_INTERVAL);
        }
    }

    // ------------------------------------------------------------------
    // 收割
    // ------------------------------------------------------------------

    /** 假玩家交互收获的结果。 */
    private enum Probe {
        /** 没反应，交给原来的拆方块 + 补种逻辑 */
        NONE,
        /** 收获成功且方块被保留（多收作物），已经处理完 */
        KEPT,
        /** 交互把方块收掉了，需要由调用方补种 */
        DESTROYED
    }

    /**
     * 用假玩家试一次「右键收获」。
     * <p>
     * 走 {@code ServerPlayerGameMode#useItemOn}，所以方块自己的 {@code use()} 和 Forge 的
     * {@code PlayerInteractEvent.RightClickBlock} 都会触发 —— 「右键收获」「作物可重复收获」
     * 这一类 mod 基本都挂在其中一条路上。
     * <p>
     * 只有真的掉出了物品、或者方块状态发生了变化，才算有反应；这样即使某个 mod 对任何右键
     * 都返回 SUCCESS，也不会被误判成"收完了"而白扣电。
     * <p>
     * 关键区分：方块**还在**（多收作物，自己会接着长）还是**没了**（一次性作物被交互收走）。
     * 后者必须回到补种流程，否则地里就空了。
     */
    private Probe harvestByInteraction(BlockPos pos, BlockState state, List<ItemStack> outDrops) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return Probe.NONE;
        }

        FakePlayer player = FakePlayerFactory.getMinecraft(serverLevel);
        AABB area = new AABB(pos).inflate(1.5D);

        Set<Integer> existing = new HashSet<>();
        for (ItemEntity entity : serverLevel.getEntitiesOfClass(ItemEntity.class, area)) {
            existing.add(entity.getId());
        }

        Vec3 center = Vec3.atCenterOf(pos);
        ItemStack previous = player.getMainHandItem();
        try {
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            player.moveTo(center.x, center.y, center.z, 0.0F, 0.0F);
            BlockHitResult hit = new BlockHitResult(center, Direction.UP, pos, false);
            player.gameMode.useItemOn(player, serverLevel, ItemStack.EMPTY,
                    InteractionHand.MAIN_HAND, hit);
        } catch (Throwable ignored) {
            return Probe.NONE;
        } finally {
            player.setItemInHand(InteractionHand.MAIN_HAND, previous);
        }

        // 收获产生的新掉落物：收走，不让它们躺在地上
        List<ItemStack> gained = new ArrayList<>();
        for (ItemEntity entity : serverLevel.getEntitiesOfClass(ItemEntity.class, area)) {
            if (!existing.contains(entity.getId())) {
                gained.add(entity.getItem().copy());
                entity.discard();
            }
        }

        BlockState after = serverLevel.getBlockState(pos);
        boolean changed = after != state;
        if (gained.isEmpty() && !changed) {
            return Probe.NONE;
        }

        if (after.is(state.getBlock())) {
            // 作物还在：多收作物，产物直接入库，收工
            for (ItemStack stack : gained) {
                insertOrBuffer(stack);
            }
            consumePower();
            return Probe.KEPT;
        }

        // 作物被交互拆掉了：产物交给调用方，补种后再入库
        outDrops.addAll(gained);
        return Probe.DESTROYED;
    }

    /**
     * 方块已经被假玩家的交互收掉（一次性作物），这里只负责补种 + 入库。
     */
    private boolean replantAfterInteraction(BlockPos pos, BlockState state, List<ItemStack> drops) {
        Block block = state.getBlock();
        CropCompat.Mode mode = CropCompat.modeOf(block);

        if (mode != CropCompat.Mode.HARVEST_ONLY) {
            Item seed = ReplantHelper.seedFor(block, CropCompat.seedOf(block));
            if (seed != null) {
                ItemStack fromDrops = null;
                for (ItemStack stack : drops) {
                    if (stack.is(seed)) {
                        fromDrops = stack;
                        break;
                    }
                }

                boolean replanted;
                if (fromDrops != null) {
                    fromDrops.shrink(1);
                    replanted = true;
                } else {
                    replanted = extractSeed(seed);
                }

                if (replanted) {
                    BlockState fresh = HarvestRules.copyStructural(state, block.defaultBlockState());
                    if (fresh.canSurvive(level, pos)) {
                        level.setBlock(pos, fresh, Block.UPDATE_ALL);
                    }
                }
            }
        }

        drops.removeIf(ItemStack::isEmpty);
        for (ItemStack stack : drops) {
            insertOrBuffer(stack);
        }

        consumePower();
        return true;
    }

    private boolean harvest(BlockPos pos, BlockState state) {
        if (getInternalCurrentPower() < HARVEST_COST) {
            return false;
        }

        // 先问游戏自己：这块能不能「不破坏方块」地收（右键收获类 mod、浆果丛等）。
        // 能的话就用它自己的收获结果，不要去拆方块——很多把作物改成可重复收获的 mod
        // 都是挂在这个交互上的，拆方块会绕过它们的逻辑。
        List<ItemStack> interactionDrops = new ArrayList<>();
        Probe probe = harvestByInteraction(pos, state, interactionDrops);
        if (probe == Probe.KEPT) {
            return true;
        }
        if (probe == Probe.DESTROYED) {
            // 交互已经把作物收走了，这里只需要补种
            return replantAfterInteraction(pos, state, interactionDrops);
        }

        List<ItemStack> drops = Block.getDrops(state, (ServerLevel) level, pos, null, null, ItemStack.EMPTY);
        if (drops.isEmpty()) {
            return false;
        }

        Block block = state.getBlock();
        CropCompat.Mode mode = CropCompat.modeOf(block);

        if (mode == CropCompat.Mode.RICE_COLUMN) {
            return harvestRiceColumn(pos, state, block, drops);
        }
        if (mode == CropCompat.Mode.GRAPE) {
            return harvestVine(pos, state, block, drops);
        }
        if (mode == CropCompat.Mode.REGROW) {
            return harvestRegrow(pos, state, drops);
        }

        boolean replanted = false;
        if (mode != CropCompat.Mode.HARVEST_ONLY) {
            Item seed = ReplantHelper.seedFor(block, CropCompat.seedOf(block));
            if (seed != null) {
                ItemStack fromDrops = null;
                for (ItemStack stack : drops) {
                    if (stack.is(seed)) {
                        fromDrops = stack;
                        break;
                    }
                }
                if (fromDrops != null) {
                    fromDrops.shrink(1);
                    replanted = true;
                } else if (extractSeed(seed)) {
                    replanted = true;
                }
                // 认得出种子但拿不到：宁可不收，也不要把作物拆了不补
                if (!replanted) {
                    return false;
                }
            }
        }

        if (replanted) {
            BlockState fresh = HarvestRules.copyStructural(state, block.defaultBlockState());
            if (fresh.canSurvive(level, pos)) {
                level.setBlock(pos, fresh, Block.UPDATE_ALL);
            } else {
                level.removeBlock(pos, false);
            }
        } else {
            level.removeBlock(pos, false);
        }

        drops.removeIf(ItemStack::isEmpty);
        for (ItemStack stack : drops) {
            insertOrBuffer(stack);
        }

        consumePower();
        return true;
    }

    /**
     * 多格作物：只认最下面一格，从顶部往下拆掉整列后补回底部一格。
     * 森罗物语水稻是三格同 ID（location 0/1/2），农夫乐事水稻是 rice + rice_panicles。
     */
    private boolean harvestRiceColumn(BlockPos pos, BlockState state, Block block, List<ItemStack> drops) {
        boolean replanted = false;
        Item seed = ReplantHelper.seedFor(block, CropCompat.seedOf(block));
        if (seed != null) {
            ItemStack fromDrops = null;
            for (ItemStack stack : drops) {
                if (stack.is(seed)) {
                    fromDrops = stack;
                    break;
                }
            }
            if (fromDrops != null) {
                fromDrops.shrink(1);
                replanted = true;
            } else if (extractSeed(seed)) {
                replanted = true;
            }
            if (!replanted) {
                return false;
            }
        }

        for (int i = 2; i >= 1; i--) {
            BlockPos above = pos.above(i);
            BlockState aboveState = level.getBlockState(above);
            if (aboveState.is(block)) {
                // 同 ID 的上半段（水稻未抽穗时的 supporting=true、森罗物语的 location=1/2）不掉落
                level.removeBlock(above, false);
            } else if (HarvestRules.isColumnPartner(block, aboveState)) {
                // 登记过的上半部分（农夫乐事的稻穗）按正常掉落结算
                drops.addAll(Block.getDrops(aboveState, (ServerLevel) level, above,
                        null, null, ItemStack.EMPTY));
                level.removeBlock(above, false);
            }
        }
        level.removeBlock(pos, false);

        if (replanted) {
            BlockState fresh = HarvestRules.copyStructural(state, block.defaultBlockState());
            IntegerProperty location = HarvestRules.intProperty(fresh, "location");
            if (location != null) {
                fresh = fresh.setValue(location, 0);
            }
            BooleanProperty supporting = HarvestRules.boolProperty(fresh, "supporting");
            if (supporting != null) {
                fresh = fresh.setValue(supporting, false);
            }
            if (fresh.canSurvive(level, pos)) {
                level.setBlock(pos, fresh, Block.UPDATE_ALL);
            }
        }

        drops.removeIf(ItemStack::isEmpty);
        for (ItemStack stack : drops) {
            insertOrBuffer(stack);
        }

        consumePower();
        return true;
    }

    /**
     * 葡萄藤一类：成熟看 grape 属性，收完把属性归零，不需要种子。
     */
    private boolean harvestVine(BlockPos pos, BlockState state, Block block, List<ItemStack> drops) {
        level.removeBlock(pos, false);

        BlockState fresh = HarvestRules.copyStructural(state, block.defaultBlockState());
        IntegerProperty grape = HarvestRules.grapeProperty(fresh);
        if (grape != null) {
            fresh = fresh.setValue(grape, HarvestRules.minValue(grape));
        }
        if (fresh.canSurvive(level, pos)) {
            level.setBlock(pos, fresh, Block.UPDATE_ALL);
        }

        drops.removeIf(ItemStack::isEmpty);
        for (ItemStack stack : drops) {
            insertOrBuffer(stack);
        }

        consumePower();
        return true;
    }

    /** 从机器内置缓存扣电（不是从整个网络扣）。 */
    private void consumePower() {
        extractAEPower(HARVEST_COST, Actionable.MODULATE, PowerMultiplier.CONFIG);
    }

    /**
     * 只摘果、不动本体：把生长状态回退，作物自己接着长。
     * 甜浆果丛、洞穴藤蔓这类方块拆掉反而损失本体，还要额外消耗一个果实补种。
     */
    private boolean harvestRegrow(BlockPos pos, BlockState state, List<ItemStack> drops) {
        BlockState fresh = state;

        BooleanProperty fruit = HarvestRules.fruitProperty(state);
        if (fruit != null) {
            fresh = fresh.setValue(fruit, false);
        } else {
            IntegerProperty age = HarvestRules.ageProperty(state);
            int reset = CropCompat.resetAgeOf(state.getBlock());
            if (age != null && reset >= 0) {
                fresh = fresh.setValue(age,
                        Mth.clamp(reset, HarvestRules.minValue(age), HarvestRules.maxValue(age)));
            }
        }

        if (fresh == state) {
            return false;
        }

        // 保留原方块，只改状态：不会掉落本体，也不需要补种
        level.setBlock(pos, fresh, Block.UPDATE_ALL);

        drops.removeIf(ItemStack::isEmpty);
        for (ItemStack stack : drops) {
            insertOrBuffer(stack);
        }

        consumePower();
        return true;
    }

    private boolean extractSeed(Item seed) {
        IGrid grid = getMainNode().getGrid();
        if (grid == null) {
            return false;
        }
        AEItemKey key = AEItemKey.of(new ItemStack(seed));
        if (key == null) {
            return false;
        }
        return grid.getStorageService().getInventory()
                .extract(key, 1, Actionable.MODULATE, actionSource) > 0;
    }

    private void insertOrBuffer(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        long inserted = 0;
        IGrid grid = getMainNode().getGrid();
        if (grid != null) {
            AEItemKey key = AEItemKey.of(stack);
            if (key != null) {
                inserted = grid.getStorageService().getInventory()
                        .insert(key, stack.getCount(), Actionable.MODULATE, actionSource);
            }
        }

        if (inserted >= stack.getCount()) {
            return;
        }

        ItemStack rest = stack.copyWithCount(stack.getCount() - (int) inserted);
        for (ItemStack buffered : buffer) {
            if (ItemStack.isSameItemSameTags(buffered, rest)
                    && buffered.getCount() + rest.getCount() <= buffered.getMaxStackSize()) {
                buffered.grow(rest.getCount());
                return;
            }
        }

        if (buffer.size() < MAX_BUFFER) {
            buffer.add(rest);
        } else {
            Block.popResource(level, worldPosition, rest);
        }
    }

    private void flushBuffer() {
        if (buffer.isEmpty()) {
            return;
        }
        IGrid grid = getMainNode().getGrid();
        if (grid == null) {
            return;
        }

        MEStorage storage = grid.getStorageService().getInventory();
        Iterator<ItemStack> iterator = buffer.iterator();
        while (iterator.hasNext()) {
            ItemStack stack = iterator.next();
            AEItemKey key = AEItemKey.of(stack);
            if (key == null) {
                iterator.remove();
                continue;
            }
            long inserted = storage.insert(key, stack.getCount(), Actionable.MODULATE, actionSource);
            if (inserted >= stack.getCount()) {
                iterator.remove();
            } else {
                stack.setCount(stack.getCount() - (int) inserted);
            }
        }
    }

    private void rebuildIndex() {
        positions.clear();
        scanCursor = 0;
        lastRebuild = level.getGameTime();

        int r = radius;
        int minY = Math.max(level.getMinBuildHeight(), worldPosition.getY() - VERTICAL_RANGE);
        int maxY = Math.min(level.getMaxBuildHeight() - 1, worldPosition.getY() + VERTICAL_RANGE);
        int centerX = worldPosition.getX();
        int centerZ = worldPosition.getZ();

        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                for (int y = minY; y <= maxY; y++) {
                    probe.set(centerX + dx, y, centerZ + dz);
                    if (!level.hasChunkAt(probe)) {
                        continue;
                    }
                    if (HarvestRules.isCropLike(level.getBlockState(probe))) {
                        positions.add(probe.asLong());
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // 配置 / 状态
    // ------------------------------------------------------------------

    public void applyConfig(int newRadius, int newInterval, int newBatch) {
        int r = Mth.clamp(newRadius, MIN_RADIUS, MAX_RADIUS);
        int interval = Mth.clamp(newInterval, MIN_INTERVAL, MAX_INTERVAL);
        int b = Mth.clamp(newBatch, 1, MAX_BATCH);

        boolean radiusChanged = r != radius;
        radius = r;
        baseInterval = interval;
        batch = b;

        if (radiusChanged) {
            rebuildPending = true;
        }
        if (cooldown > baseInterval) {
            cooldown = baseInterval;
        }
        setChanged();
    }

    public int getRadius() {
        return radius;
    }

    public int getInterval() {
        return baseInterval;
    }

    public int getBatch() {
        return batch;
    }

    public int getIndexSize() {
        return positions.size();
    }

    public int getBufferCount() {
        return buffer.size();
    }

    /** 运行开关：关掉后不再收割，但仍然会充电、仍然会把缓冲区里的产物送进网络。 */
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean value) {
        if (enabled != value) {
            enabled = value;
            if (enabled) {
                cooldown = 0;
            }
            setChanged();
        }
    }

    /** 0 = 未接入网络，1 = 缺频道，2 = 在线，3 = 电量不足，4 = 已停止。 */
    public int getStatus() {
        if (!enabled) {
            return 4;
        }
        if (getMainNode().getGrid() == null) {
            return 0;
        }
        if (!getMainNode().isActive()) {
            return 1;
        }
        return getInternalCurrentPower() < HARVEST_COST ? 3 : 2;
    }

    public int getStoredPowerInt() {
        return (int) Math.round(getInternalCurrentPower());
    }

    public int getMaxStoredPowerInt() {
        return (int) Math.round(getInternalMaxPower());
    }

    public double getStoredPower() {
        return getInternalCurrentPower();
    }

    public double getMaxPower() {
        return getInternalMaxPower();
    }

    public boolean isDeviceOnline() {
        return getMainNode().isActive();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.meharvest.me_harvest_flag");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new HarvestFlagMenu(id, inventory, this, true);
    }
}
