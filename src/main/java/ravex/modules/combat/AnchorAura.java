package ravex.modules.combat;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.RaveX;
import ravex.utility.misc.block.BlockUtility;
import ravex.utility.misc.EntityUtility;
import ravex.utility.misc.MobUtility;
import ravex.utility.misc.PhysicUtility;
import ravex.utility.nativelib.NativeLibraryUtility;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.rotation.AimUtility;
import ravex.utility.player.rotation.RotationUtility;
import ravex.utility.player.rotation.SilentRotationUtility;
import ravex.utility.player.SwingUtility;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import ravex.utility.misc.PotionUtility;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import ravex.mcwrapper.MinecraftWrapper;






@Module(name = "AnchorAura", category = "Combat")
public class AnchorAura {
    @Parameter(name = "Target", modes = {"Closest", "LowestHP"})
    public String targetMode = "Closest";
    @Parameter(name = "TargetType", modes = {"Players", "Monsters", "Passives", "All"})
    public String targetType = "Players";
    @Parameter(name = "Range", min = 1.0, max = 6.0, step = 0.1)
    public double range = 4.5;
    @Parameter(name = "TargetRange", min = 1.0, max = 10.0, step = 0.1)
    public double targetRange = 6.0;
    @Parameter(name = "MinDamage", min = 1.0, max = 20.0, step = 0.5)
    public double minDamage = 4.0;
    @Parameter(name = "MaxSelfDmg", min = 1.0, max = 20.0, step = 0.5)
    public double maxSelfDmg = 8.0;
    @Parameter(name = "SelfDmgWeight", min = 0.0, max = 5.0, step = 0.1)
    public double selfDamageWeight = 1.2;
    @Parameter(name = "AntiSuicide")
    public boolean antiSuicide = true;
    @Parameter(name = "AntiSuicideMinHP", min = 1.0, max = 20.0, step = 0.5)
    public double antiSuicideMinHp = 6.0;
    @Parameter(name = "PredictTicks", min = 0.0, max = 4.0, step = 0.1)
    public double predictTicks = 1.0;
    @Parameter(name = "ConsiderDurability")
    public boolean alwaysConsiderDurability = true;
    @Parameter(name = "DurabilityThreshold", min = 1.0, max = 100.0, step = 5.0)
    public double armorDurabilityThreshold = 20.0;
    @Parameter(name = "Delay", min = 0.0, max = 1000.0, step = 10.0)
    public double placeDelay = 100.0;
    @Parameter(name = "AirPlace")
    public boolean airPlace = false;
    @Parameter(name = "AirPlaceBypass", modes = {"NCP", "Grim", "None"})
    public String airPlaceBypass = "None";
    @Parameter(name = "Rotate", modes = {"Grim", "NCP", "NCPStrict", "None"})
    public String rotate = "Grim";
    @Parameter(name = "Swap", modes = {"Grim", "NCP", "NCPStrict", "None"})
    public String swapMode = "Grim";
    @Parameter(name = "SwitchBack")
    public boolean swapSwitchBack = true;
    @Parameter(name = "SwapInv")
    public boolean swapInventory = true;
    @Parameter(name = "Render")
    public boolean render = true;
    @Parameter(name = "Color", color = true)
    public int color = 0x3F00FFFF;
    public static net.minecraft.core.BlockPos simulatedPlacementBlock = null;
    public static double currentTargetDamage = 0.0;
    public static double currentSelfDamage = 0.0;
    private static final SilentRotationUtility silentRotation = new SilentRotationUtility();
    private long lastActionTime = 0;
    private static final NativeLibraryUtility NATIVE = NativeLibraryUtility.of("ravex_anchoraura");
    static {
        NATIVE.load();
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
    public void onEnable() {
        lastActionTime = 0;
        silentRotation.reset();
        simulatedPlacementBlock = null;
        currentTargetDamage = 0.0;
        currentSelfDamage = 0.0;
    }
    public void onDisable() {
        silentRotation.reset();
        simulatedPlacementBlock = null;
    }
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getLevel() == null || mc.getGameMode() == null)
            return;
        silentRotation.hasRotation = false;
        net.minecraft.world.entity.LivingEntity target = findTarget(mc);
        if (target == null) {
            simulatedPlacementBlock = null;
            return;
        }
        long now = System.currentTimeMillis();
        boolean canAct = (now - lastActionTime >= (long) placeDelay);
        net.minecraft.core.BlockPos existingAnchor = findExistingAnchor(mc, target);
        if (existingAnchor != null) {
            simulatedPlacementBlock = existingAnchor;
            net.minecraft.world.level.block.state.BlockState state = mc.getLevel().getBlockState(existingAnchor);
            int charges = getAnchorCharges(state);
            calculateExpectedDamages(mc, target, existingAnchor);
            if (!canAct)
                return;
            if (charges == 0) {
                int glowstoneSlot = findItemSlot(mc, "glowstone");
                if (glowstoneSlot == -1)
                    return;
                net.minecraft.world.phys.Vec3 hitVec = net.minecraft.world.phys.Vec3.atCenterOf(existingAnchor);
                rotateTo(mc, hitVec);
                performUse(mc, glowstoneSlot, existingAnchor, net.minecraft.core.Direction.UP, hitVec);
            } else {
                int triggerSlot = findNonGlowstoneSlot(mc);
                if (triggerSlot == -1)
                    return;
                net.minecraft.world.phys.Vec3 hitVec = net.minecraft.world.phys.Vec3.atCenterOf(existingAnchor);
                rotateTo(mc, hitVec);
                performUse(mc, triggerSlot, existingAnchor, net.minecraft.core.Direction.UP, hitVec);
            }
            return;
        }
        double[] solidBlockData = collectSolidBlocks(mc);
        double[] result;
        if (NATIVE.isLoaded()) {
            result = nativeCalculateAnchorAura(
                    mc.getPlayer().getX(), mc.getPlayer().getY(), mc.getPlayer().getZ(),
                    mc.getPlayer().getHealth(), mc.getPlayer().getAbsorptionAmount(), getEntityStats(mc.getPlayer()),
                    target.getX(), target.getY(), target.getZ(),
                    target.getHealth(), target.getAbsorptionAmount(), getEntityStats(target),
                    solidBlockData,
                    range,
                    targetRange,
                    minDamage,
                    maxSelfDmg,
                    selfDamageWeight,
                    antiSuicide,
                    antiSuicideMinHp,
                    predictTicks,
                    alwaysConsiderDurability,
                    armorDurabilityThreshold);
        } else {
            result = javaFallbackCalculate(mc, target, solidBlockData);
        }
        if (result == null || result[0] < 0.5) {
            simulatedPlacementBlock = null;
            currentTargetDamage = 0.0;
            currentSelfDamage = 0.0;
            return;
        }
        simulatedPlacementBlock = new net.minecraft.core.BlockPos((int) result[5], (int) result[6], (int) result[7]);
        if (result.length >= 10) {
            currentTargetDamage = result[8];
            currentSelfDamage = result[9];
        } else {
            currentTargetDamage = 0.0;
            currentSelfDamage = 0.0;
        }
        if (!canAct)
            return;
        int anchorSlot = findItemSlot(mc, "respawn_anchor");
        if (anchorSlot == -1)
            return;
        net.minecraft.core.BlockPos neighborPos = new net.minecraft.core.BlockPos((int) result[1], (int) result[2], (int) result[3]);
        net.minecraft.core.Direction face = net.minecraft.core.Direction.values()[(int) result[4]];
        net.minecraft.core.BlockPos targetBlock = new net.minecraft.core.BlockPos((int) result[5], (int) result[6], (int) result[7]);
        net.minecraft.world.phys.Vec3 hitVec = net.minecraft.world.phys.Vec3.atCenterOf(neighborPos)
                .add(new net.minecraft.world.phys.Vec3(face.getStepX(), face.getStepY(), face.getStepZ()).scale(0.5));
        rotateTo(mc, hitVec);
        performUse(mc, anchorSlot, neighborPos, face, hitVec);
    }

    private void performUse(MinecraftWrapper mc, int slot, net.minecraft.core.BlockPos targetBlock, net.minecraft.core.Direction face, net.minecraft.world.phys.Vec3 hitVec) {
        int originalSlot = InventoryUtility.getSelectedSlot(mc.getPlayer());
        String swap = swapMode;
        if (swap.equals("None")) {
            if (InventoryUtility.getSelectedSlot(mc.getPlayer()) != slot)
                return;
        } else {
            originalSlot = InventoryUtility.getSelectedSlot(mc.getPlayer());
            InventoryUtility.silentSelectSlot(mc.getPlayer(), slot);
        }
        net.minecraft.world.phys.BlockHitResult hitResult = new net.minecraft.world.phys.BlockHitResult(hitVec, face, targetBlock, false);
        mc.getGameMode().useItemOn(mc.getPlayer(), net.minecraft.world.InteractionHand.MAIN_HAND, hitResult);
        SwingUtility.swing(mc.getPlayer(), net.minecraft.world.InteractionHand.MAIN_HAND);
        lastActionTime = System.currentTimeMillis();
        if (swapSwitchBack && originalSlot != -1 && !swap.equals("None")) {
            InventoryUtility.silentSelectSlot(mc.getPlayer(), originalSlot);
        }
    }

    private void calculateExpectedDamages(MinecraftWrapper mc, net.minecraft.world.entity.LivingEntity target, net.minecraft.core.BlockPos anchorPos) {
        if (NATIVE.isLoaded()) {
            double[] solidBlockData = collectSolidBlocks(mc);
            double[] result = nativeCalculateAnchorAura(
                    mc.getPlayer().getX(), mc.getPlayer().getY(), mc.getPlayer().getZ(),
                    mc.getPlayer().getHealth(), mc.getPlayer().getAbsorptionAmount(), getEntityStats(mc.getPlayer()),
                    target.getX(), target.getY(), target.getZ(),
                    target.getHealth(), target.getAbsorptionAmount(), getEntityStats(target),
                    solidBlockData,
                    range,
                    targetRange,
                    minDamage,
                    maxSelfDmg,
                    selfDamageWeight,
                    antiSuicide,
                    antiSuicideMinHp,
                    predictTicks,
                    alwaysConsiderDurability,
                    armorDurabilityThreshold);
            if (result != null && result[0] > 0.5) {
                currentTargetDamage = result[8];
                currentSelfDamage = result[9];
            }
        } else {
            currentTargetDamage = 8.5;
            currentSelfDamage = 2.1;
        }
    }

    private net.minecraft.core.BlockPos findExistingAnchor(MinecraftWrapper mc, net.minecraft.world.entity.LivingEntity target) {
        net.minecraft.core.BlockPos tPos = target.blockPosition();
        double maxDist = targetRange;
        double maxPlaceDist = range;
        net.minecraft.core.BlockPos bestAnchor = null;
        double bestDist = Double.MAX_VALUE;
        int r = 3;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    net.minecraft.core.BlockPos p = tPos.offset(dx, dy, dz);
                    if (mc.getLevel().getBlockState(p).is(net.minecraft.world.level.block.Blocks.RESPAWN_ANCHOR)) {
                        double pDist = Math
                                .sqrt(p.distToCenterSqr(mc.getPlayer().getX(), mc.getPlayer().getEyeY(), mc.getPlayer().getZ()));
                        if (pDist <= maxPlaceDist) {
                            double tDist = Math.sqrt(p.distToCenterSqr(target.getX(), target.getY(), target.getZ()));
                            if (tDist <= maxDist) {
                                if (tDist < bestDist) {
                                    bestDist = tDist;
                                    bestAnchor = p;
                                }
                            }
                        }
                    }
                }
            }
        }
        return bestAnchor;
    }

    private int findItemSlot(MinecraftWrapper mc, String itemName) {
        int slot = InventoryUtility.findHotbarSlot(mc.getPlayer(), itemName);
        if (slot != -1) return slot;
        if (swapInventory) {
            slot = InventoryUtility.findSlot(mc.getPlayer(), itemName, 9, 36);
            if (slot != -1) {
                int hotbarSlot = InventoryUtility.getSelectedSlot(mc.getPlayer());
                InventoryUtility.handleInventoryClick(mc, mc.getPlayer(), slot, hotbarSlot, InventoryUtility.SWAP);
                return hotbarSlot;
            }
        }
        return -1;
    }

    private int findNonGlowstoneSlot(MinecraftWrapper mc) {
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(mc.getPlayer(), i);
            if (!stack.isEmpty() && !InventoryUtility.isGlowstone(stack)) {
                return i;
            }
        }
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(mc.getPlayer(), i);
            if (stack.isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    private void rotateTo(MinecraftWrapper mc, net.minecraft.world.phys.Vec3 target) {
        String mode = rotate;
        if (mode.equals("None"))
            return;
        float[] angles = RotationUtility.anglesTo(mc.getPlayer().getEyePosition(), target);
        float currentYaw = mc.getPlayer().getYRot();
        float currentPitch = mc.getPlayer().getXRot();
        if (!silentRotation.initialized) { silentRotation.init(currentYaw, currentPitch); }
        currentYaw = silentRotation.lastYaw;
        currentPitch = silentRotation.lastPitch;
        float maxSpeed = 180.0f;
        float[] limited = AimUtility.limitAngles(currentYaw, angles[0], currentPitch, angles[1], maxSpeed);
        float finalYaw = limited[0], finalPitch = limited[1];
        silentRotation.set(finalYaw, finalPitch);
        silentRotation.lastYaw = finalYaw;
        silentRotation.lastPitch = finalPitch;
    }

    private boolean isRotationAligned(MinecraftWrapper mc, net.minecraft.world.phys.Vec3 target) {
        return silentRotation.isRotationAligned(mc, target, 12.0F);
    }

    private double[] collectSolidBlocks(MinecraftWrapper mc) {
        List<Double> data = new ArrayList<>();
        net.minecraft.core.BlockPos playerPos = mc.getPlayer().blockPosition();
        int r = (int) Math.ceil(range) + 2;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -4; dy <= 4; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    net.minecraft.core.BlockPos pos = playerPos.offset(dx, dy, dz);
                    if (mc.getLevel().isLoaded(pos)) {
                        net.minecraft.world.level.block.state.BlockState state = mc.getLevel().getBlockState(pos);
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

    private net.minecraft.world.entity.LivingEntity findTarget(MinecraftWrapper mc) {
        net.minecraft.world.entity.LivingEntity closest = null;
        double bestMetric = Double.MAX_VALUE;
        double maxDist = targetRange;
        String mode = targetMode;
        String typeFilter = targetType;
        for (net.minecraft.world.entity.Entity e : mc.getLevel().entitiesForRendering()) {
            if (!(e instanceof net.minecraft.world.entity.LivingEntity le))
                continue;
            if (MobUtility.isSelf(le))
                continue;
            if (MobUtility.isDead(le))
                continue;
            if (typeFilter.equals("Players")) {
                if (!MobUtility.isPlayer(le))
                    continue;
            } else if (typeFilter.equals("Monsters")) {
                if (!MobUtility.isHostile(le))
                    continue;
            } else if (typeFilter.equals("Passives")) {
                if (MobUtility.isPlayer(le) || MobUtility.isHostile(le))
                    continue;
            }
            double dist = MobUtility.distanceToPlayer(le);
            if (dist > maxDist)
                continue;
            double metric = switch (mode) {
                case "Closest" -> dist;
                case "LowestHP" -> MobUtility.getHealth(le);
                default -> dist;
            };
            if (metric < bestMetric) {
                bestMetric = metric;
                closest = le;
            }
        }
        return closest;
    }

    private double[] getEntityStats(net.minecraft.world.entity.LivingEntity player) {
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
            if (armor.isEmpty())
                continue;
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
        if (InventoryUtility.isTotem(player.getMainHandItem()))
            totems++;
        if (InventoryUtility.isTotem(player.getOffhandItem()))
            totems++;
        if (player instanceof net.minecraft.world.entity.player.Player p) {
            totems += InventoryUtility.countItem(p, "totem_of_undying");
        }
        double[] stats = new double[15];
        stats[0] = player.getArmorValue();
        stats[1] = PotionUtility.getArmorToughness(player);
        stats[2] = blastProtectionEpf;
        stats[3] = protectionEpf;
        stats[4] = PotionUtility.getResistanceAmplifier(player);
        stats[5] = PotionUtility.getWeaknessAmplifier(player);
        stats[6] = PotionUtility.getStrengthAmplifier(player);
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

    private double[] javaFallbackCalculate(MinecraftWrapper mc, net.minecraft.world.entity.LivingEntity target, double[] solidBlocksData) {
        net.minecraft.core.BlockPos tPos = target.blockPosition();
        Set<net.minecraft.core.BlockPos> solids = new HashSet<>();
        for (int i = 0; i + 2 < solidBlocksData.length; i += 3) {
            solids.add(
                    new net.minecraft.core.BlockPos((int) solidBlocksData[i], (int) solidBlocksData[i + 1], (int) solidBlocksData[i + 2]));
        }
        net.minecraft.core.BlockPos bestBlock = null;
        double bestDist = Double.MAX_VALUE;
        net.minecraft.core.BlockPos bestNeighbor = null;
        int bestFace = 1;
        int r = 2;
        double maxPlaceRange = range;
        double maxTargetRange = targetRange;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    net.minecraft.core.BlockPos c = tPos.offset(dx, dy, dz);
                    if (solids.contains(c))
                        continue;
                    double pDist = Math
                            .sqrt(c.distToCenterSqr(mc.getPlayer().getX(), mc.getPlayer().getEyeY(), mc.getPlayer().getZ()));
                    if (pDist > maxPlaceRange)
                        continue;
                    double tDist = Math.sqrt(c.distToCenterSqr(target.getX(), target.getY(), target.getZ()));
                    if (tDist > maxTargetRange)
                        continue;
                    if (solids.contains(c.above()))
                        continue;
                    if (intersectsEntity(mc.getPlayer(), c) || intersectsEntity(target, c))
                        continue;
                    boolean hasNeighbor = false;
                    net.minecraft.core.BlockPos neighbor = null;
                    int faceIndex = 1;
                    for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
                        net.minecraft.core.BlockPos n = c.relative(dir);
                        if (solids.contains(n)) {
                            hasNeighbor = true;
                            neighbor = n;
                            faceIndex = dir.getOpposite().ordinal();
                            break;
                        }
                    }
                    if (!hasNeighbor) {
                        if (airPlace) {
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
            return new double[] { 1.0, bestNeighbor.getX(), bestNeighbor.getY(), bestNeighbor.getZ(), bestFace,
                    bestBlock.getX(), bestBlock.getY(), bestBlock.getZ(), 8.0, 2.0 };
        }
        return new double[] { 0.0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
    }

    private boolean intersectsEntity(net.minecraft.world.entity.Entity entity, net.minecraft.core.BlockPos pos) {
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

    private int getAnchorCharges(net.minecraft.world.level.block.state.BlockState state) {
        for (net.minecraft.world.level.block.state.properties.Property<?> prop : state.getProperties()) {
            if (prop.getName().equals("charges")
                    && prop instanceof net.minecraft.world.level.block.state.properties.IntegerProperty intProp) {
                return state.getValue(intProp);
            }
        }
        return 0;
    }

    private static native double[] nativeCalculateAnchorAura(
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
            boolean alwaysConsiderDurability,
            double armorDurabilityThreshold);


}