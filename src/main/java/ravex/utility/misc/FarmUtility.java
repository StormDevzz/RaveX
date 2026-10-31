package ravex.utility.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.utility.misc.block.BlockUtility;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.SwingUtility;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// crop harvesting and replanting for the AutoFarm module.
public final class FarmUtility {
    private FarmUtility() {}

    private enum Plant {
        WHEAT("Wheat", "wheat_seeds"),
        CARROTS("Carrots", "carrot"),
        POTATOES("Potatoes", "potato"),
        BEETROOTS("Beetroots", "beetroot_seeds"),
        NETHER_WART("NetherWart", "nether_wart"),
        PUMPKIN("Pumpkin", "pumpkin_seeds"),
        MELON("Melon", "melon_seeds"),
        SUGAR_CANE("SugarCane", "sugar_cane"),
        CACTUS("Cactus", "cactus"),
        BAMBOO("Bamboo", "bamboo"),
        BERRIES("Berries", "sweet_berries");

        final String display;
        final String seedItem;
        Plant(String display, String seedItem) {
            this.display = display;
            this.seedItem = seedItem;
        }
    }

    private static final Plant[] SEED_PLANTS = {
        Plant.WHEAT, Plant.CARROTS, Plant.POTATOES, Plant.BEETROOTS, Plant.NETHER_WART
    };

    private static String targetMode = "Range";
    private static double range = 4.5;
    private static boolean harvest = true;
    private static boolean replant = true;
    private static double delay = 150;
    private static boolean silentSwap = true;
    private static List<String> plants = new ArrayList<>();

    private static final Map<Long, Plant> replantSpots = new HashMap<>();
    private static final EnumSet<Plant> availableSeeds = EnumSet.noneOf(Plant.class);
    private static BlockPos mining;
    private static int miningPrevSlot = -1;
    private static int miningToolSlot = -1;
    private static long lastActionTime = 0;
    private static BlockPos lastReplantPos;
    private static long lastReplantAt = 0;
    private static BlockPos renderTarget;
    private static final Map<Long, Long> highlightTimes = new HashMap<>();
    private static final int MAX_HIGHLIGHTS = 10;

    public static void highlightBlock(BlockPos pos) {
        long now = System.currentTimeMillis();
        highlightTimes.put(pos.asLong(), now);
        cleanupOldHighlights(now);
        if (highlightTimes.size() > MAX_HIGHLIGHTS) {
            highlightTimes.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .ifPresent(e -> highlightTimes.remove(e.getKey()));
        }
    }

    public static void clearHighlights() {
        highlightTimes.clear();
    }

    public static Map<Long, Long> getHighlightTimes() {
        return highlightTimes;
    }

    private static void cleanupOldHighlights(long now) {
        highlightTimes.entrySet().removeIf(e -> now - e.getValue() > 5000);
    }

    public static void configure(String mode, double rangeValue, boolean harvestValue, boolean replantValue,
                                 double delayValue, boolean silent, List<String> plantList) {
        targetMode = mode;
        range = rangeValue;
        harvest = harvestValue;
        replant = replantValue;
        delay = delayValue;
        silentSwap = silent;
        plants = plantList;
    }

    public static void reset() {
        releaseMining();
        replantSpots.clear();
        availableSeeds.clear();
        renderTarget = null;
        lastActionTime = 0;
        lastReplantPos = null;
        lastReplantAt = 0;
    }

    @Nullable
    public static BlockPos getCurrentTarget() {
        return renderTarget;
    }

    public static void tick() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getLevel() == null || mc.getGameMode() == null) return;
        if (mc.getCurrentScreen() != null) return;

        long now = System.currentTimeMillis();
        if (mining != null) {
            var state = mc.getLevel().getBlockState(mining);
            if (state.isAir() || plantOf(state) == null) {
                highlightBlock(mining);
                releaseMining();
            } else {
                renderTarget = mining;
                if (now - lastActionTime >= delay) {
                    lastActionTime = now;
                    var cfg = new BlockUtility.BreakConfig();
                    cfg.swapMode = swapMode();
                    BlockUtility.continueBreak(mc, mining, cfg);
                }
                return;
            }
        }
        if (now - lastActionTime < delay) return;

        refreshAvailableSeeds(mc);
        if ("Aim".equals(targetMode)) tickAim(mc, now);
        else tickRange(mc, now);
    }

    private static String swapMode() {
        return silentSwap ? "Silent" : "Normal";
    }

    private static void refreshAvailableSeeds(MinecraftWrapper mc) {
        availableSeeds.clear();
        var player = mc.getPlayer();
        if (player == null) return;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = InventoryUtility.getItem(player, i);
            if (stack.isEmpty()) continue;
            for (Plant plant : SEED_PLANTS) {
                if (InventoryUtility.isItem(stack, plant.seedItem)) availableSeeds.add(plant);
            }
        }
    }

    private static void tickAim(MinecraftWrapper mc, long now) {
        HitResult hit = mc.getHitResult();
        if (!(hit instanceof BlockHitResult blockHit)) {
            renderTarget = null;
            return;
        }
        BlockPos pointed = blockHit.getBlockPos();
        var player = mc.getPlayer();

        if (harvest && pointed.distToCenterSqr(player.getEyePosition()) <= range * range) {
            BlockPos target = findAimHarvest(mc, pointed);
            if (target != null) {
                startHarvest(mc, target, now);
                return;
            }
        }
        if (replant) {
            BlockPos spot = findAimReplantSpot(mc, pointed);
            if (spot != null) {
                startReplant(mc, spot, now);
                return;
            }
        }
        renderTarget = null;
    }

    @Nullable
    private static BlockPos findAimHarvest(MinecraftWrapper mc, BlockPos pointed) {
        for (BlockPos candidate : new BlockPos[]{pointed, pointed.above()}) {
            Plant plant = harvestPlantFor(mc.getLevel(), candidate);
            if (plant != null && isPlantEnabled(plant)) return candidate;
        }
        return null;
    }

    @Nullable
    private static BlockPos findAimReplantSpot(MinecraftWrapper mc, BlockPos pointed) {
        Level level = mc.getLevel();
        BlockPos spot = null;
        if (canPlantAt(level, pointed)) spot = pointed.above();
        else if (level.getBlockState(pointed).isAir() && canPlantAt(level, pointed.below())) spot = pointed;
        if (spot == null) return null;
        if (spot.distToCenterSqr(mc.getPlayer().getEyePosition()) > range * range) return null;
        Plant plant = replantPlantFor(level, spot);
        if (plant == null || !isPlantEnabled(plant)) return null;
        return spot;
    }

    private static boolean canPlantAt(Level level, BlockPos soilPos) {
        BlockState soil = level.getBlockState(soilPos);
        if (!soil.is(Blocks.FARMLAND) && !soil.is(Blocks.SOUL_SAND)) return false;
        return level.getBlockState(soilPos.above()).isAir();
    }

    private static void tickRange(MinecraftWrapper mc, long now) {
        var player = mc.getPlayer();
        var eye = player.getEyePosition();
        int r = (int) Math.ceil(range);
        double rangeSq = range * range;
        BlockPos base = player.blockPosition();
        BlockPos bestHarvest = null;
        BlockPos bestReplant = null;
        double bestHarvestDist = Double.MAX_VALUE;
        double bestReplantDist = Double.MAX_VALUE;

        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    BlockPos pos = base.offset(dx, dy, dz);
                    double dist = pos.distToCenterSqr(eye);
                    if (dist > rangeSq) continue;
                    if (harvest && dist < bestHarvestDist) {
                        Plant plant = harvestPlantFor(mc.getLevel(), pos);
                        if (plant != null && isPlantEnabled(plant)) {
                            bestHarvest = pos;
                            bestHarvestDist = dist;
                        }
                    }
                    if (replant && dist < bestReplantDist) {
                        Plant plant = replantPlantFor(mc.getLevel(), pos);
                        if (plant != null && isPlantEnabled(plant)) {
                            bestReplant = pos;
                            bestReplantDist = dist;
                        }
                    }
                }
            }
        }
        if (bestHarvest != null) {
            startHarvest(mc, bestHarvest, now);
            return;
        }
        if (bestReplant != null) {
            startReplant(mc, bestReplant, now);
            return;
        }
        renderTarget = null;
    }

    private static void startHarvest(MinecraftWrapper mc, BlockPos pos, long now) {
        var state = mc.getLevel().getBlockState(pos);
        Plant plant = plantOf(state);
        if (plant == null) return;
        renderTarget = pos;
        highlightBlock(pos);
        lastActionTime = now;

        if (plant == Plant.WHEAT || plant == Plant.CARROTS || plant == Plant.POTATOES
            || plant == Plant.BEETROOTS || plant == Plant.NETHER_WART) {
            replantSpots.put(pos.asLong(), plant);
        }

        if (plant == Plant.BERRIES) {
            var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
            mc.getGameMode().useItemOn(mc.getPlayer(), InteractionHand.MAIN_HAND, hit);
            SwingUtility.swingMainHand(mc.getPlayer());
            return;
        }

        boolean progressive = plant == Plant.PUMPKIN || plant == Plant.MELON;
        if (!progressive) {
            mc.getGameMode().startDestroyBlock(pos, Direction.UP);
            SwingUtility.swingMainHand(mc.getPlayer());
            return;
        }

        var cfg = new BlockUtility.BreakConfig();
        cfg.swapMode = swapMode();
        miningPrevSlot = InventoryUtility.getSelectedSlot(mc.getPlayer());
        miningToolSlot = BlockUtility.startBreak(mc, pos, cfg);
        mining = pos;
    }

    private static void startReplant(MinecraftWrapper mc, BlockPos spot, long now) {
        Plant plant = replantPlantFor(mc.getLevel(), spot);
        if (plant == null) return;
        if (spot.equals(lastReplantPos) && now - lastReplantAt < 500) return;
        int seedSlot = findSeedSlot(mc, plant);
        if (seedSlot == -1) return;

        BlockPos support = spot.below();
        int prev = InventoryUtility.getSelectedSlot(mc.getPlayer());
        InventoryUtility.swapToSlot(mc.getPlayer(), seedSlot, swapMode());
        var hit = new BlockHitResult(Vec3.atCenterOf(support), Direction.UP, support, false);
        mc.getGameMode().useItemOn(mc.getPlayer(), InteractionHand.MAIN_HAND, hit);
        SwingUtility.swingMainHand(mc.getPlayer());
        if (seedSlot != prev) InventoryUtility.swapBackSlot(mc.getPlayer(), prev, swapMode());
        renderTarget = spot;
        highlightBlock(spot);
        replantSpots.remove(spot.asLong());
        lastReplantPos = spot;
        lastReplantAt = now;
        lastActionTime = now;
    }

    private static void releaseMining() {
        var mc = MinecraftWrapper.getWrapper();
        if (mining != null && mc.getGameMode() != null) mc.getGameMode().stopDestroyBlock();
        if (miningToolSlot != -1 && miningPrevSlot != -1 && mc.getPlayer() != null && miningToolSlot != miningPrevSlot) {
            InventoryUtility.swapBackSlot(mc.getPlayer(), miningPrevSlot, swapMode());
        }
        mining = null;
        miningPrevSlot = -1;
        miningToolSlot = -1;
    }

    @Nullable
    private static Plant plantOf(BlockState state) {
        var block = state.getBlock();
        if (block == Blocks.WHEAT) return Plant.WHEAT;
        if (block == Blocks.CARROTS) return Plant.CARROTS;
        if (block == Blocks.POTATOES) return Plant.POTATOES;
        if (block == Blocks.BEETROOTS) return Plant.BEETROOTS;
        if (block == Blocks.NETHER_WART) return Plant.NETHER_WART;
        if (block == Blocks.PUMPKIN) return Plant.PUMPKIN;
        if (block == Blocks.MELON) return Plant.MELON;
        if (block == Blocks.SUGAR_CANE) return Plant.SUGAR_CANE;
        if (block == Blocks.CACTUS) return Plant.CACTUS;
        if (block == Blocks.BAMBOO) return Plant.BAMBOO;
        if (block == Blocks.SWEET_BERRY_BUSH) return Plant.BERRIES;
        return null;
    }

    @Nullable
    private static Plant harvestPlantFor(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Plant plant = plantOf(state);
        if (plant == null) return null;
        return switch (plant) {
            case WHEAT, CARROTS, POTATOES, BEETROOTS ->
                state.getValue(BlockStateProperties.AGE_7) >= 7 ? plant : null;
            case NETHER_WART -> state.getValue(BlockStateProperties.AGE_3) >= 3 ? plant : null;
            case BERRIES -> state.getValue(BlockStateProperties.AGE_3) >= 2 ? plant : null;
            case PUMPKIN, MELON -> plant;
            case SUGAR_CANE, CACTUS ->
                level.getBlockState(pos.below()).getBlock() == state.getBlock() ? plant : null;
            case BAMBOO -> {
                var below = level.getBlockState(pos.below()).getBlock();
                yield (below == Blocks.BAMBOO || below == Blocks.BAMBOO_SAPLING) ? plant : null;
            }
        };
    }

    @Nullable
    private static Plant replantPlantFor(Level level, BlockPos spot) {
        if (!level.getBlockState(spot).isAir()) return null;
        BlockState soil = level.getBlockState(spot.below());
        Plant remembered = replantSpots.get(spot.asLong());
        if (remembered != null && matchesSoil(remembered, soil)) return remembered;

        if (soil.is(Blocks.FARMLAND)) {
            Plant neighbour = cropNeighbour(level, spot);
            if (neighbour != null) return neighbour;
            if (availableSeeds.contains(Plant.WHEAT)) return Plant.WHEAT;
            if (availableSeeds.contains(Plant.CARROTS)) return Plant.CARROTS;
            if (availableSeeds.contains(Plant.POTATOES)) return Plant.POTATOES;
            if (availableSeeds.contains(Plant.BEETROOTS)) return Plant.BEETROOTS;
            return null;
        }
        if (soil.is(Blocks.SOUL_SAND)) {
            return availableSeeds.contains(Plant.NETHER_WART) ? Plant.NETHER_WART : null;
        }
        return null;
    }

    private static boolean matchesSoil(Plant plant, BlockState soil) {
        return switch (plant) {
            case WHEAT, CARROTS, POTATOES, BEETROOTS -> soil.is(Blocks.FARMLAND);
            case NETHER_WART -> soil.is(Blocks.SOUL_SAND);
            default -> false;
        };
    }

    @Nullable
    private static Plant cropNeighbour(Level level, BlockPos spot) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            Plant plant = plantOf(level.getBlockState(spot.relative(dir)));
            if (plant == Plant.WHEAT || plant == Plant.CARROTS || plant == Plant.POTATOES || plant == Plant.BEETROOTS) {
                return plant;
            }
        }
        return null;
    }

    private static boolean isPlantEnabled(Plant plant) {
        return plants != null && plants.contains(plant.display);
    }

    private static int findSeedSlot(MinecraftWrapper mc, Plant plant) {
        var player = mc.getPlayer();
        if (player == null) return -1;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = InventoryUtility.getItem(player, i);
            if (stack.isEmpty() || !InventoryUtility.isItem(stack, plant.seedItem)) continue;
            if (i < 9) return i;
            int free = InventoryUtility.findEmptyHotbarSlot(player);
            if (free == -1) return -1;
            InventoryUtility.handleInventoryClick(mc, player, i, free, InventoryUtility.SWAP);
            return free;
        }
        return -1;
    }
}
