package ravex.modules.combat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.modules.client.Notifications;
import ravex.utility.misc.CombatUtility;
import ravex.utility.misc.LanguageUtility;
import ravex.utility.misc.PhysicUtility;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.SwingUtility;
import ravex.utility.player.rotation.SilentRotationUtility;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Module(name = "BasePlace", category = "Combat")
public class BasePlace {
    @Parameter(name = "Range", min = 1.0, max = 6.0, step = 0.1)
    public double range = 4.5;
    @Parameter(name = "SelfDmgWeight", min = 0.0, max = 5.0, step = 0.1)
    public double selfDamageWeight = 1.2;
    @Parameter(name = "PredictTicks", min = 0.0, max = 4.0, step = 0.1)
    public double predictTicks = 1.0;
    @Parameter(name = "AirPlace")
    public boolean airPlace = false;
    @Parameter(name = "Delay", min = 0.0, max = 1000.0, step = 10.0)
    public double placeDelay = 200.0;
    @Parameter(name = "Rotate", modes = {"Grim", "NCP", "NCPStrict", "None"})
    public String rotate = "Grim";
    @Parameter(name = "Swap", modes = {"Silent", "Normal", "None"})
    public String swapMode = "Silent";
    @Parameter(name = "SwitchBack")
    public boolean swapSwitchBack = true;
    @Parameter(name = "SwapInv")
    public boolean swapInventory = false;
    @Parameter(name = "AutoCrystalSync")
    public boolean autoCrystalSync = true;
    @Parameter(name = "SyncPredict", min = 1.0, max = 10.0, step = 1.0)
    public double syncPredictTicks = 5.0;
    @Parameter(name = "Render")
    public boolean render = true;
    @Parameter(name = "Color", color = true, visible = "render")
    public int color = 0x3F00FF00;

    public static BlockPos lastPlacedBase = null;
    public static long lastPlacedTime = 0L;
    public static double currentTargetDamage = 0.0;
    public static double currentSelfDamage = 0.0;

    private static final Map<BlockPos, Long> placedPositions = new ConcurrentHashMap<>();
    private static final SilentRotationUtility silentRotation = new SilentRotationUtility();
    private static BlockPos simulatedPlacementBlock = null;

    private long lastPlaceTime = 0L;
    private long lastWarnTime = 0L;
    private int noTargetTicks = 0;

    public static boolean hasSilentRotations() {
        return silentRotation.hasRotation;
    }

    public static float getSilentYaw() {
        return silentRotation.yaw;
    }

    public static float getSilentPitch() {
        return silentRotation.pitch;
    }

    public static BlockPos getSimulatedPlacementBlock() {
        return simulatedPlacementBlock;
    }

    public static Map<BlockPos, Long> getRecentPlacedBlocks() {
        return placedPositions;
    }

    public void onEnable() {
        lastPlaceTime = 0L;
        lastWarnTime = 0L;
        noTargetTicks = 0;
        lastPlacedBase = null;
        lastPlacedTime = 0L;
        simulatedPlacementBlock = null;
        placedPositions.clear();
        silentRotation.reset();
    }

    public void onDisable() {
        silentRotation.reset();
        simulatedPlacementBlock = null;
        placedPositions.clear();
    }

    public void onTick() {
        MinecraftWrapper mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getLevel() == null || mc.getGameMode() == null) return;
        silentRotation.hasRotation = false;

        AutoCrystal autoCrystal = Modules.get(AutoCrystal.class);
        if (autoCrystal == null || !Modules.enabled(AutoCrystal.class)) {
            noTargetTicks = 0;
            simulatedPlacementBlock = null;
            disableWithWarning(LanguageUtility.t("baseplace_no_ac"));
            return;
        }

        LivingEntity target = autoCrystal.currentTarget;
        if (target == null || !target.isAlive() || target.isRemoved() || target.level() != mc.getLevel()) {
            simulatedPlacementBlock = null;
            currentTargetDamage = 0.0;
            currentSelfDamage = 0.0;
            if (++noTargetTicks >= 8) {
                noTargetTicks = 0;
                disableWithWarning(LanguageUtility.t("baseplace_no_target"));
            }
            return;
        }
        noTargetTicks = 0;

        double effectiveMinDamage = Math.max(1.0, autoCrystal.minDamage);

        BlockPos activeBase = AutoCrystal.currentPlacementBlock;
        if (activeBase != null && isUsableBase(mc, activeBase)) {
            Vec3 activeCenter = new Vec3(activeBase.getX() + 0.5, activeBase.getY() + 1.0, activeBase.getZ() + 0.5);
            if (autoCrystalCanReach(mc, activeCenter, autoCrystal)
                    && damageToTarget(target, activeCenter, CombatUtility.getEntityStats(target)) >= effectiveMinDamage) {
                simulatedPlacementBlock = null;
                return;
            }
        }

        Vec3 predictedTarget = predictTarget(target);
        if (hasUsableBase(mc, target, predictedTarget, effectiveMinDamage, autoCrystal)) {
            simulatedPlacementBlock = null;
            return;
        }
        if (hasDamagingCrystal(mc, target, effectiveMinDamage)) {
            simulatedPlacementBlock = null;
            return;
        }
        if (findObsidianSlot(mc) == -1) {
            simulatedPlacementBlock = null;
            warnThrottled(LanguageUtility.t("baseplace_no_obsidian"), 0xFFFFAA00);
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastPlaceTime < (long) placeDelay) return;
        if (now - lastPlacedTime < 600L) return;

        placedPositions.entrySet().removeIf(entry -> now - entry.getValue() > 2000L);

        PlaceCandidate candidate = findBestBase(mc, target, predictedTarget, effectiveMinDamage, autoCrystal);
        if (candidate == null) {
            simulatedPlacementBlock = null;
            currentTargetDamage = 0.0;
            currentSelfDamage = 0.0;
            return;
        }

        simulatedPlacementBlock = candidate.targetBlock;
        currentTargetDamage = candidate.targetDamage;
        currentSelfDamage = candidate.selfDamage;

        if (placedPositions.containsKey(candidate.targetBlock)) return;

        int blockSlot = findObsidianSlot(mc);
        if (blockSlot == -1) return;

        lastPlaceTime = now;
        if (!place(mc, candidate, blockSlot)) return;

        placedPositions.put(candidate.targetBlock, now);
        lastPlacedBase = candidate.targetBlock;
        lastPlacedTime = now;
        Notifications.notifyByMode(
                LanguageUtility.t("baseplace_placed", candidate.targetBlock.getX(), candidate.targetBlock.getY(), candidate.targetBlock.getZ()),
                0xFF55FF55);
    }

    private Vec3 predictTarget(LivingEntity target) {
        double vx = target.getX() - target.xOld;
        double vy = target.getY() - target.yOld;
        double vz = target.getZ() - target.zOld;
        if (vx == 0.0 && vy == 0.0 && vz == 0.0 && target.getDeltaMovement() != null) {
            Vec3 motion = target.getDeltaMovement();
            vx = motion.x;
            vy = motion.y;
            vz = motion.z;
        }
        return target.position().add(vx * predictTicks, vy * predictTicks, vz * predictTicks);
    }

    private boolean place(MinecraftWrapper mc, PlaceCandidate candidate, int blockSlot) {
        var player = mc.getPlayer();
        int previousSlot = InventoryUtility.getSelectedSlot(player);
        String swap = swapMode;

        if ("None".equals(swap) && previousSlot != blockSlot) return false;
        if (!"None".equals(swap)) {
            InventoryUtility.selectSlot(player, blockSlot);
            InventoryUtility.silentSelectSlot(player, blockSlot);
        }

        BlockPos clickedPos;
        Direction face;
        Vec3 hitVec;
        if (candidate.airPlace) {
            clickedPos = candidate.targetBlock;
            face = Direction.UP;
            hitVec = PhysicUtility.centerOf(clickedPos).add(0.0, 0.5, 0.0);
        } else {
            clickedPos = candidate.neighbor;
            face = candidate.face;
            hitVec = PhysicUtility.centerOf(clickedPos).add(
                    PhysicUtility.vec3(face.getStepX(), face.getStepY(), face.getStepZ()).scale(0.5));
        }

        if (!"None".equals(rotate)) {
            float speed = switch (rotate) {
                case "NCPStrict" -> 90.0f;
                case "NCP" -> 140.0f;
                default -> 180.0f;
            };
            CombatUtility.rotateTo(mc, hitVec, speed, 0.0f, silentRotation);
        }

        boolean placed = mc.getGameMode().useItemOn(
                player, InteractionHand.MAIN_HAND, new BlockHitResult(hitVec, face, clickedPos, false));
        SwingUtility.swing(player, InteractionHand.MAIN_HAND);

        if (!"None".equals(swap)) {
            InventoryUtility.selectSlot(player, previousSlot);
            if (swapSwitchBack) InventoryUtility.silentSelectSlot(player, previousSlot);
        }

        return placed;
    }

    private PlaceCandidate findBestBase(MinecraftWrapper mc, LivingEntity target, Vec3 predictedTarget,
                                        double effectiveMinDamage, AutoCrystal autoCrystal) {
        BlockPos origin = BlockPos.containing(predictedTarget);
        Vec3 eyePos = mc.getPlayer().getEyePosition();
        double[] targetStats = CombatUtility.getEntityStats(target);
        double[] playerStats = CombatUtility.getEntityStats(mc.getPlayer());

        PlaceCandidate best = null;
        double bestScore = -Double.MAX_VALUE;

        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -2; dy <= 1; dy++) {
                for (int dz = -3; dz <= 3; dz++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    BlockState state = mc.getLevel().getBlockState(pos);
                    if (!state.canBeReplaced()) continue;
                    if (!mc.getLevel().getBlockState(pos.above()).isAir()) continue;
                    if (intersects(mc.getPlayer(), pos) || intersects(target, pos)) continue;

                    Vec3 center = PhysicUtility.centerOf(pos);
                    if (eyePos.distanceTo(center) > range) continue;

                    Vec3 crystalCenter = new Vec3(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
                    if (!autoCrystalCanReach(mc, crystalCenter, autoCrystal)) continue;

                    double targetDamage = damageToTarget(target, crystalCenter, targetStats);
                    if (targetDamage < effectiveMinDamage) continue;

                    double selfDamage = damageToSelf(mc.getPlayer(), crystalCenter, playerStats);

                    Neighbor neighbor = findNeighbor(mc, pos);
                    if (neighbor == null) {
                        if (!airPlace) continue;
                        neighbor = new Neighbor(pos, Direction.UP, true);
                    }

                    double score = targetDamage * 1.5 - selfDamage * selfDamageWeight;
                    double dyToTarget = (pos.getY() + 1.0) - predictedTarget.y;
                    if (dyToTarget <= 0.1 && dyToTarget >= -1.2) {
                        score += 18.0;
                    } else if (dyToTarget < -1.2 && dyToTarget >= -2.5) {
                        score += 8.0;
                    } else if (dyToTarget > 0.1) {
                        score -= 10.0 + dyToTarget * 15.0;
                    }

                    if (score > bestScore) {
                        bestScore = score;
                        best = new PlaceCandidate(pos, neighbor.pos, neighbor.face, neighbor.airPlace, targetDamage, selfDamage);
                    }
                }
            }
        }
        return best;
    }

    private Neighbor findNeighbor(MinecraftWrapper mc, BlockPos pos) {
        Direction[] order = {Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.UP};
        for (Direction dir : order) {
            BlockPos neighbor = pos.relative(dir);
            BlockState state = mc.getLevel().getBlockState(neighbor);
            if (!state.canBeReplaced() && !state.isAir()) {
                return new Neighbor(neighbor, dir.getOpposite(), false);
            }
        }
        return null;
    }

    private boolean hasUsableBase(MinecraftWrapper mc, LivingEntity target, Vec3 predictedTarget,
                                  double effectiveMinDamage, AutoCrystal autoCrystal) {
        BlockPos origin = BlockPos.containing(predictedTarget);
        double[] targetStats = CombatUtility.getEntityStats(target);
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -2; dy <= 1; dy++) {
                for (int dz = -3; dz <= 3; dz++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    if (!isUsableBase(mc, pos)) continue;
                    Vec3 crystalCenter = new Vec3(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
                    if (!autoCrystalCanReach(mc, crystalCenter, autoCrystal)) continue;
                    if (damageToTarget(target, crystalCenter, targetStats) >= effectiveMinDamage) return true;
                }
            }
        }
        return false;
    }

    private boolean autoCrystalCanReach(MinecraftWrapper mc, Vec3 crystalCenter, AutoCrystal autoCrystal) {
        double maxRange = autoCrystal.placeRange;
        if (PhysicUtility.raycast(mc.getPlayer().getEyePosition(), crystalCenter) != null) {
            maxRange = Math.min(maxRange, autoCrystal.placeWallRange);
        }
        return mc.getPlayer().position().distanceTo(crystalCenter) <= maxRange;
    }

    private boolean isUsableBase(MinecraftWrapper mc, BlockPos pos) {
        BlockState state = mc.getLevel().getBlockState(pos);
        if (!state.is(Blocks.OBSIDIAN) && !state.is(Blocks.BEDROCK)) return false;
        return mc.getLevel().getBlockState(pos.above()).isAir();
    }

    private boolean hasDamagingCrystal(MinecraftWrapper mc, LivingEntity target, double effectiveMinDamage) {
        double[] targetStats = CombatUtility.getEntityStats(target);
        for (var entity : mc.getLevel().entitiesForRendering()) {
            if (!(entity instanceof EndCrystal crystal) || !crystal.isAlive() || crystal.isRemoved()) continue;
            if (damageToTarget(target, crystal.position(), targetStats) >= effectiveMinDamage) return true;
        }
        return false;
    }

    private double damageToTarget(LivingEntity target, Vec3 crystalCenter, double[] stats) {
        double dist = crystalCenter.distanceTo(target.position().add(0.0, 1.0, 0.0));
        return applyArmor(rawCrystalDamage(dist), stats);
    }

    private double damageToSelf(LivingEntity player, Vec3 crystalCenter, double[] stats) {
        double raw = rawCrystalDamage(crystalCenter.distanceTo(player.position()));
        if (PhysicUtility.raycast(crystalCenter.add(0.0, 0.2, 0.0), player.getEyePosition()) != null) {
            raw *= 0.45;
        }
        return applyArmor(raw, stats);
    }

    private double rawCrystalDamage(double distance) {
        if (distance > 12.0) return 0.0;
        double impact = Math.max(0.0, 1.0 - distance / 12.0);
        return (impact * impact + impact) / 2.0 * 84.0 + 1.0;
    }

    private double applyArmor(double raw, double[] stats) {
        if (stats == null || stats.length < 10) return raw;
        double armor = stats[0];
        double toughness = stats[1];
        double blastEpf = stats[2];
        double protEpf = stats[3];
        double resistanceLevel = stats[4];

        double armorFactor = Math.max(armor / 5.0, armor - raw / (2.0 + toughness / 4.0));
        armorFactor = Math.min(20.0, armorFactor);
        double afterArmor = raw * (1.0 - armorFactor / 25.0);
        double totalEpf = Math.min(20.0, blastEpf + protEpf);
        double afterEnchant = afterArmor * (1.0 - totalEpf * 0.04);
        double resistance = Math.min(1.0, resistanceLevel * 0.2);
        return afterEnchant * (1.0 - resistance);
    }

    private int findObsidianSlot(MinecraftWrapper mc) {
        for (int slot = 0; slot < 9; slot++) {
            if (isObsidian(InventoryUtility.getItem(mc.getPlayer(), slot))) return slot;
        }
        if (!swapInventory) return -1;
        if (mc.getPlayer().containerMenu != mc.getPlayer().inventoryMenu) return -1;

        int hotbarSlot = InventoryUtility.getSelectedSlot(mc.getPlayer());
        for (int slot = 9; slot < 36; slot++) {
            if (isObsidian(InventoryUtility.getItem(mc.getPlayer(), slot))) {
                InventoryUtility.handleInventoryClick(mc, mc.getPlayer(), slot, hotbarSlot, InventoryUtility.SWAP);
                return isObsidian(InventoryUtility.getItem(mc.getPlayer(), hotbarSlot)) ? hotbarSlot : -1;
            }
        }
        return -1;
    }

    private boolean isObsidian(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() == Blocks.OBSIDIAN;
    }

    private boolean intersects(LivingEntity entity, BlockPos pos) {
        return entity.getBoundingBox().intersects(new net.minecraft.world.phys.AABB(pos));
    }

    private void disableWithWarning(String text) {
        Notifications.notifyByMode(text, 0xFFFF5555);
        Modules.setEnabled(BasePlace.class, false);
    }

    private void warnThrottled(String text, int color) {
        long now = System.currentTimeMillis();
        if (now - lastWarnTime < 3000L) return;
        lastWarnTime = now;
        Notifications.notifyByMode(text, color);
    }

    private record Neighbor(BlockPos pos, Direction face, boolean airPlace) {}

    private record PlaceCandidate(BlockPos targetBlock, BlockPos neighbor, Direction face, boolean airPlace,
                                  double targetDamage, double selfDamage) {}
}
