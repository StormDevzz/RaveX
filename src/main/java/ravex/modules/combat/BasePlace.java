package ravex.modules.combat;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import ravex.utility.misc.MobUtility;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffects;
import ravex.RaveX;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.rotation.RotationUtility;
import ravex.utility.player.rotation.SilentRotation;
import ravex.parameter.ColorParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import ravex.utility.nativelib.NativeLibrary;
public class BasePlace extends Module {
    public static final BasePlace INSTANCE = new BasePlace();
    public final ModeParameter   targetMode      = new ModeParameter("Target", "Closest", List.of("Closest", "LowestHP"));
    public final ModeParameter   targetType      = new ModeParameter("TargetType", "Players", List.of("Players", "Monsters", "Passives", "All"));
    public final NumberParameter range           = new NumberParameter("Range", 4.5, 1.0, 6.0, 0.1);
    public final NumberParameter targetRange     = new NumberParameter("TargetRange", 6.0, 1.0, 10.0, 0.1);
    public final NumberParameter minDamage       = new NumberParameter("MinDamage", 4.0, 1.0, 20.0, 0.5);
    public final NumberParameter maxSelfDmg      = new NumberParameter("MaxSelfDmg", 8.0, 1.0, 20.0, 0.5);
    public final NumberParameter selfDamageWeight = new NumberParameter("SelfDmgWeight", 1.2, 0.0, 5.0, 0.1);
    public final BooleanParameter antiSuicide    = new BooleanParameter("AntiSuicide", true);
    public final NumberParameter antiSuicideMinHp = new NumberParameter("AntiSuicideMinHP", 6.0, 1.0, 20.0, 0.5);
    public final NumberParameter predictTicks    = new NumberParameter("PredictTicks", 1.0, 0.0, 4.0, 0.1);
    public final BooleanParameter airPlace       = new BooleanParameter("AirPlace", false);
    public final NumberParameter placeDelay      = new NumberParameter("Delay", 100.0, 0.0, 1000.0, 10.0);
    public final ModeParameter   rotate          = new ModeParameter("Rotate", "Silent", List.of("Silent", "Normal", "None"));
    public final BooleanParameter strictRotation = new BooleanParameter("StrictRotation", false);
    public final ModeParameter   swapMode        = new ModeParameter("Swap", "Silent", List.of("Silent", "Normal", "None"));
    public final BooleanParameter swapSwitchBack  = new BooleanParameter("SwitchBack", true);
    public final BooleanParameter swapInventory   = new BooleanParameter("SwapInv", true);
    public final BooleanParameter autoCrystalSync = new BooleanParameter("AutoCrystalSync", true);
    public final NumberParameter  syncPredictTicks = new NumberParameter("SyncPredictTicks", 5.0, 1.0, 10.0, 1.0);
    public final BooleanParameter render          = new BooleanParameter("Render", true);
    public final ColorParameter  color           = new ColorParameter("Color", 0x3F00FF00);
    public static BlockPos lastPlacedBase = null;
    public static long lastPlacedTime = 0;
    public static double currentTargetDamage = 0.0;
    public static double currentSelfDamage = 0.0;
    private final java.util.Map<BlockPos, Long> placedPositions = new java.util.concurrent.ConcurrentHashMap<>();
    private static final SilentRotation silentRotation = new SilentRotation();
    private long lastPlaceTime = 0;
    private static BlockPos simulatedPlacementBlock = null;
    private static final NativeLibrary NATIVE = NativeLibrary.of("ravex_baseplace");
    static {
        NATIVE.load();
    }
    private BasePlace() {
        super("BasePlace");
        strictRotation.setVisible(() -> !rotate.getValue().equals("None"));
        swapSwitchBack.setVisible(() -> !swapMode.getValue().equals("None"));
        swapInventory.setVisible(() -> !swapMode.getValue().equals("None"));
        syncPredictTicks.setVisible(autoCrystalSync::getValue);
    }
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
    @Override
    protected void onEnable() {
        lastPlaceTime = 0;
        silentRotation.reset();
        lastPlacedBase = null;
        lastPlacedTime = 0;
        simulatedPlacementBlock = null;
        placedPositions.clear();
    }
    @Override
    protected void onDisable() {
        silentRotation.reset();
        simulatedPlacementBlock = null;
        placedPositions.clear();
    }
    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;
        silentRotation.hasRotation = false;
        if (autoCrystalSync.getValue()) {
            if (!AutoCrystal.INSTANCE.getEnabled()) {
                simulatedPlacementBlock = null;
                return;
            }
            if (AutoCrystal.currentPlacementBlock != null) {
                simulatedPlacementBlock = null;
                return;
            }
            if (!playerHasCrystals(mc)) {
                simulatedPlacementBlock = null;
                return;
            }
        }
        LivingEntity target = findTarget(mc);
        if (target == null) {
            simulatedPlacementBlock = null;
            return;
        }
        double[] solidBlockData = collectSolidBlocks(mc);
        double[] result;
        if (NATIVE.isLoaded()) {
            result = nativeCalculateBasePlace(
                mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                mc.player.getHealth(), mc.player.getAbsorptionAmount(), getEntityStats(mc.player),
                target.getX(), target.getY(), target.getZ(),
                target.getHealth(), target.getAbsorptionAmount(), getEntityStats(target),
                solidBlockData,
                range.getValue(),
                targetRange.getValue(),
                minDamage.getValue(),
                maxSelfDmg.getValue(),
                selfDamageWeight.getValue(),
                antiSuicide.getValue(),
                antiSuicideMinHp.getValue(),
                predictTicks.getValue(),
                airPlace.getValue()
            );
        } else {
            result = javaFallbackCalculate(mc, target, solidBlockData);
        }
        if (result == null || result[0] < 0.5) {
            simulatedPlacementBlock = null;
            currentTargetDamage = 0.0;
            currentSelfDamage = 0.0;
            return;
        }
        simulatedPlacementBlock = new BlockPos((int) result[5], (int) result[6], (int) result[7]);
        if (result.length >= 10) {
            currentTargetDamage = result[8];
            currentSelfDamage = result[9];
        } else {
            currentTargetDamage = 0.0;
            currentSelfDamage = 0.0;
        }
        long now = System.currentTimeMillis();
        if (now - lastPlaceTime < placeDelay.getValue().longValue()) {
            return;
        }
        int blockSlot = findBlockSlot(mc);
        if (blockSlot == -1) return;
        BlockPos neighborPos = new BlockPos((int) result[1], (int) result[2], (int) result[3]);
        Direction face = Direction.values()[(int) result[4]];
        BlockPos targetBlock = new BlockPos((int) result[5], (int) result[6], (int) result[7]);
        placedPositions.entrySet().removeIf(entry -> now - entry.getValue() > 1000);
        if (placedPositions.containsKey(targetBlock) || mc.level.getBlockState(targetBlock).getBlock() == Blocks.OBSIDIAN) {
            return;
        }
        Vec3 hitVec = Vec3.atCenterOf(neighborPos).add(new Vec3(face.getStepX(), face.getStepY(), face.getStepZ()).scale(0.5));
        rotateTo(mc, hitVec);
        if (strictRotation.getValue() && !isRotationAligned(mc, hitVec)) {
            return;
        }
        int originalSlot = InventoryUtility.getSelectedSlot(mc.player);
        String swap = swapMode.getValue();
        if (swap.equals("Normal")) {
            InventoryUtility.selectSlot(mc.player, blockSlot);
        } else if (swap.equals("Silent")) {
            InventoryUtility.silentSelectSlot(mc.player, blockSlot);
        } else if (swap.equals("None")) {
            if (InventoryUtility.getSelectedSlot(mc.player) != blockSlot) {
                return;
            }
        }
        BlockHitResult hitResult = new BlockHitResult(hitVec, face, neighborPos, false);
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hitResult);
        mc.player.swing(InteractionHand.MAIN_HAND);
        placedPositions.put(targetBlock, now);
        lastPlaceTime = now;
        lastPlacedBase = targetBlock;
        lastPlacedTime = now;
        if (swapMode.getValue().equals("Silent") && swapSwitchBack.getValue() && originalSlot != -1) {
            InventoryUtility.silentSelectSlot(mc.player, originalSlot);
        }
    }
    private boolean playerHasCrystals(Minecraft mc) {
        if (InventoryUtility.isHolding(mc.player, "end_crystal")) return true;
        if (InventoryUtility.isOffhand(mc.player, "end_crystal")) return true;
        return InventoryUtility.findSlot(mc.player, "end_crystal") != -1;
    }
    private void rotateTo(Minecraft mc, Vec3 target) {
        String mode = rotate.getValue();
        if (mode.equals("None")) return;
        float[] angles = RotationUtility.anglesTo(mc.player.getEyePosition(), target);
        if (mode.equals("Normal")) {
            mc.player.setYRot(angles[0]);
            mc.player.setXRot(angles[1]);
        } else if (mode.equals("Silent")) {
            silentRotation.set(angles[0], angles[1]);
        }
    }
    private boolean isRotationAligned(Minecraft mc, Vec3 target) {
        return silentRotation.isRotationAligned(mc, target, 12.0F);
    }
    private double[] collectSolidBlocks(Minecraft mc) {
        List<Double> data = new ArrayList<>();
        BlockPos playerPos = mc.player.blockPosition();
        int r = (int) Math.ceil(range.getValue()) + 2;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -4; dy <= 4; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    BlockPos pos = playerPos.offset(dx, dy, dz);
                    if (mc.level.isLoaded(pos)) {
                        BlockState state = mc.level.getBlockState(pos);
                        if (!state.isAir() && !state.liquid()) {
                            data.add((double) pos.getX());
                            data.add((double) pos.getY());
                            data.add((double) pos.getZ());
                        }
                    }
                }
            }
        }
        double[] arr = new double[data.size()];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = data.get(i);
        }
        return arr;
    }
    private int findBlockSlot(Minecraft mc) {
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(mc.player, i);
            if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)) continue;
            if (blockItem.getBlock() == Blocks.OBSIDIAN) {
                return i;
            }
        }
        if (swapInventory.getValue()) {
            for (int i = 9; i < 36; i++) {
                var stack = InventoryUtility.getItem(mc.player, i);
                if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)) continue;
                if (blockItem.getBlock() == Blocks.OBSIDIAN) {
                    int hotbarSlot = InventoryUtility.getSelectedSlot(mc.player);
                    InventoryUtility.handleInventoryClick(mc, mc.player, i, hotbarSlot, net.minecraft.world.inventory.ClickType.SWAP);
                    return hotbarSlot;
                }
            }
        }
        return -1;
    }
    private LivingEntity findTarget(Minecraft mc) {
        LivingEntity closest = null;
        double bestMetric = Double.MAX_VALUE;
        double maxDist = targetRange.getValue();
        String mode = targetMode.getValue();
        String typeFilter = targetType.getValue();
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof LivingEntity le)) continue;
            if (MobUtility.isSelf(le)) continue;
            if (MobUtility.isDead(le)) continue;
            if (typeFilter.equals("Players")) {
                if (!MobUtility.isPlayer(le)) continue;
            } else if (typeFilter.equals("Monsters")) {
                if (!MobUtility.isHostile(le)) continue;
            } else if (typeFilter.equals("Passives")) {
                if (MobUtility.isPlayer(le) || MobUtility.isHostile(le)) continue;
            }
            double dist = MobUtility.distanceToPlayer(le);
            if (dist > maxDist) continue;
            double metric = switch (mode) {
                case "Closest"   -> dist;
                case "LowestHP" -> MobUtility.getHealth(le);
                default          -> dist;
            };
            if (metric < bestMetric) {
                bestMetric = metric;
                closest = le;
            }
        }
        return closest;
    }
    private double[] getEntityStats(LivingEntity player) {
        int protectionEpf = 0;
        int blastProtectionEpf = 0;
        net.minecraft.world.entity.EquipmentSlot[] armorSlots = {
            net.minecraft.world.entity.EquipmentSlot.FEET,
            net.minecraft.world.entity.EquipmentSlot.LEGS,
            net.minecraft.world.entity.EquipmentSlot.CHEST,
            net.minecraft.world.entity.EquipmentSlot.HEAD
        };
        for (net.minecraft.world.entity.EquipmentSlot slot : armorSlots) {
            var armor = player.getItemBySlot(slot);
            if (armor.isEmpty()) continue;
            var enchants = InventoryUtility.getEnchantments(armor);
            if (enchants != null) {
                for (var enchantment : enchants.keySet()) {
                    String id = enchantment.getRegisteredName().toLowerCase();
                    int level = enchants.getLevel(enchantment);
                    if (id.contains("blast_protection")) {
                        blastProtectionEpf += level * 2;
                    } else if (id.equals("minecraft:protection") || id.endsWith(":protection")) {
                        protectionEpf += level;
                    }
                }
            }
        }
        int totems = 0;
        if (InventoryUtility.isTotem(player.getMainHandItem())) totems++;
        if (InventoryUtility.isTotem(player.getOffhandItem())) totems++;
        if (player instanceof Player p) {
            totems += InventoryUtility.countItem(p, "totem_of_undying");
        }
        double[] stats = new double[15];
        stats[0] = player.getArmorValue();
        var attrToughness = player.getAttribute(Attributes.ARMOR_TOUGHNESS);
        stats[1] = attrToughness != null ? attrToughness.getValue() : 0.0;
        stats[2] = blastProtectionEpf;
        stats[3] = protectionEpf;
        var resEffect = player.getEffect(MobEffects.RESISTANCE);
        stats[4] = resEffect != null ? resEffect.getAmplifier() + 1 : 0;
        var weakEffect = player.getEffect(MobEffects.WEAKNESS);
        stats[5] = weakEffect != null ? weakEffect.getAmplifier() + 1 : 0;
        var strEffect = player.getEffect(MobEffects.STRENGTH);
        stats[6] = strEffect != null ? strEffect.getAmplifier() + 1 : 0;
        int idx = 7;
        for (net.minecraft.world.entity.EquipmentSlot slot : armorSlots) {
            var armor = player.getItemBySlot(slot);
            if (armor.isEmpty()) {
                stats[idx++] = 0.0;
            } else if (!armor.isDamageableItem()) {
                stats[idx++] = 100.0;
            } else {
                double dur = (1.0 - (double) armor.getDamageValue() / armor.getMaxDamage()) * 100.0;
                stats[idx++] = dur;
            }
        }
        net.minecraft.world.phys.Vec3 motion = player.getDeltaMovement();
        if (motion != null) {
            stats[11] = motion.x;
            stats[12] = motion.y;
            stats[13] = motion.z;
        } else {
            stats[11] = 0.0;
            stats[12] = 0.0;
            stats[13] = 0.0;
        }
        stats[14] = totems;
        return stats;
    }
    private double[] javaFallbackCalculate(Minecraft mc, LivingEntity target, double[] solidBlocksData) {
        BlockPos tPos = target.blockPosition();
        Set<BlockPos> solids = new HashSet<>();
        for (int i = 0; i + 2 < solidBlocksData.length; i += 3) {
            solids.add(new BlockPos((int) solidBlocksData[i], (int) solidBlocksData[i+1], (int) solidBlocksData[i+2]));
        }
        BlockPos bestBlock = null;
        double bestDist = Double.MAX_VALUE;
        BlockPos bestNeighbor = null;
        int bestFace = 1;
        int r = 2;
        double maxPlaceRange = range.getValue();
        double maxTargetRange = targetRange.getValue();
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    BlockPos c = tPos.offset(dx, dy, dz);
                    if (solids.contains(c)) continue;
                    double pDist = Math.sqrt(c.distToCenterSqr(mc.player.getX(), mc.player.getEyeY(), mc.player.getZ()));
                    if (pDist > maxPlaceRange) continue;
                    double tDist = Math.sqrt(c.distToCenterSqr(target.getX(), target.getY(), target.getZ()));
                    if (tDist > maxTargetRange) continue;
                    if (solids.contains(c.above())) continue;
                    if (!airPlace.getValue() && solids.contains(c.above(2))) continue;
                    if (intersectsEntity(mc.player, c) || intersectsEntity(target, c)) continue;
                    boolean hasNeighbor = false;
                    BlockPos neighbor = null;
                    int faceIndex = 1;
                    for (Direction dir : Direction.values()) {
                        BlockPos n = c.relative(dir);
                        if (solids.contains(n)) {
                            hasNeighbor = true;
                            neighbor = n;
                            faceIndex = dir.getOpposite().ordinal();
                            break;
                        }
                    }
                    if (!hasNeighbor) {
                        if (airPlace.getValue()) {
                            neighbor = c;
                            faceIndex = 1;
                        } else {
                            continue;
                        }
                    }
                    double priorityDist = c.distSqr(tPos.below());
                    if (priorityDist < bestDist) {
                        bestDist = priorityDist;
                        bestBlock = c;
                        bestNeighbor = neighbor;
                        bestFace = faceIndex;
                    }
                }
            }
        }
        if (bestBlock != null) {
            return new double[]{1.0, bestNeighbor.getX(), bestNeighbor.getY(), bestNeighbor.getZ(), bestFace, bestBlock.getX(), bestBlock.getY(), bestBlock.getZ(), 0.0, 0.0};
        }
        return new double[]{0.0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    }
    private boolean intersectsEntity(Entity entity, BlockPos pos) {
        double minX = entity.getX() - 0.3;
        double maxX = entity.getX() + 0.3;
        double minY = entity.getY();
        double maxY = entity.getY() + 1.8;
        double minZ = entity.getZ() - 0.3;
        double maxZ = entity.getZ() + 0.3;
        double bMinX = pos.getX();
        double bMaxX = pos.getX() + 1.0;
        double bMinY = pos.getY();
        double bMaxY = pos.getY() + 1.0;
        double bMinZ = pos.getZ();
        double bMaxZ = pos.getZ() + 1.0;
        return (bMaxX > minX && bMinX < maxX &&
                bMaxY > minY && bMinY < maxY &&
                bMaxZ > minZ && bMinZ < maxZ);
    }
    private static native double[] nativeCalculateBasePlace(
        double playerX, double playerY, double playerZ,
        double playerHp, double playerAbs,
        double[] playerStats,
        double targetX, double targetY, double targetZ,
        double targetHp, double targetAbs,
        double[] targetStats,
        double[] solidBlocksData,
        double placeRange,
        double targetRange,
        double minTargetDmg,
        double maxSelfDmg,
        double selfDmgWeight,
        boolean antiSuicide,
        double antiSuicideMinHp,
        double predictTicks,
        boolean airPlace
    );
}
