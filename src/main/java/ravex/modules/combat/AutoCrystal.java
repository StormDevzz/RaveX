package ravex.modules.combat;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.block.BlockUtility;
import ravex.utility.misc.EntityUtility;
import ravex.utility.misc.PhysicUtility;
import ravex.utility.nativelib.NativeLibraryUtility;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.rotation.AimUtility;
import ravex.utility.player.rotation.RotationUtility;
import ravex.utility.player.rotation.SilentRotationUtility;
import ravex.utility.player.SwingUtility;
import ravex.utility.misc.PotionUtility;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.phys.AABB;
import java.util.ArrayList;
import java.util.List;
import ravex.utility.network.NetworkUtility;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;






@Module(name = "AutoCrystal", category = "Combat")
public class AutoCrystal {
    @Parameter(name = "PlaceRange", min = 1.0, max = 6.0, step = 0.1)
    public double placeRange = 4.5;
    @Parameter(name = "BreakRange", min = 1.0, max = 6.0, step = 0.1)
    public double breakRange = 4.5;
    @Parameter(name = "PlaceDelay", min = 0, max = 500, step = 10)
    public double placeDelay = 100;
    @Parameter(name = "BreakDelay", min = 0, max = 500, step = 10)
    public double breakDelay = 50;
    @Parameter(name = "MinDamage", min = 1.0, max = 20.0, step = 0.5)
    public double minDamage = 4.0;
    @Parameter(name = "MaxSelfDmg", min = 1.0, max = 20.0, step = 0.5)
    public double maxSelfDmg = 8.0;
    @Parameter(name = "AntiSuicide")
    public boolean antiSuicide = true;
    @Parameter(name = "AntiSuicideMinHP", min = 1.0, max = 20.0, step = 0.5)
    public double antiSuicideMinHp = 6.0;
    @Parameter(name = "RotateMode", modes = {"Grim", "NCP", "NCPStrict", "None"})
    public String rotate = "Grim";
    @Parameter(name = "SwapMode", modes = {"Grim", "NCP", "NCPStrict", "None"})
    public String swapMode = "Grim";
    @Parameter(name = "SwapDelay", min = 0.0, max = 500.0, step = 10.0)
    public double swapDelay = 0.0;
    @Parameter(name = "OnlyRender")
    public boolean onlyInRender = false;
    @Parameter(name = "Target", modes = {"Closest", "LowestHP", "HighestDamage"})
    public String targetMode = "Closest";
    @Parameter(name = "TargetType", modes = {"Players", "Monsters", "Passives", "All"})
    public String targetType = "Players";
    @Parameter(name = "RenderPlacement")
    public boolean renderPlacement = true;
    @Parameter(name = "RenderDamage")
    public boolean renderDamage = true;
    @Parameter(name = "ArmorBreaker")
    public boolean armorBreaker = true;
    @Parameter(name = "ArmorPercent", min = 1.0, max = 50.0, step = 1.0)
    public double armorPercent = 15.0;
    @Parameter(name = "PredictTicks", min = 0.0, max = 4.0, step = 0.1)
    public double predictTicks = 1.0;
    @Parameter(name = "TotemDetection")
    public boolean totemDetection = true;
    @Parameter(name = "TotemMinDamage", min = 0.5, max = 10.0, step = 0.5)
    public double totemMinDamage = 1.5;
    @Parameter(name = "TotemSelfMinHP", min = 2.0, max = 20.0, step = 0.5)
    public double totemSelfMinHp = 8.0;
    @Parameter(name = "PlaceMode", modes = {"Strict", "NCPStrict", "Grim"})
    public String placeMode = "Grim";
    @Parameter(name = "RotateSpeed", min = 10.0, max = 180.0, step = 5.0)
    public double rotateSpeed = 180.0;
    @Parameter(name = "RotateRandomize", min = 0.0, max = 3.0, step = 0.1)
    public double rotateRandomize = 0.0;
    @Parameter(name = "AntiSuicideBreak")
    public boolean antiSuicideCheckBreaking = true;
    @Parameter(name = "AntiSuicideIgnoreTotem")
    public boolean antiSuicideIgnoreWithTotem = false;
    @Parameter(name = "TotemCheckTarget")
    public boolean totemCheckTarget = true;
    @Parameter(name = "TotemPopSwap")
    public boolean totemPopSwap = false;
    @Parameter(name = "TotemPopHP", min = 1.0, max = 20.0, step = 0.5)
    public double totemPopHp = 6.0;
    @Parameter(name = "PlaceWallRange", min = 1.0, max = 6.0, step = 0.1)
    public double placeWallRange = 3.5;
    @Parameter(name = "BreakWallRange", min = 1.0, max = 6.0, step = 0.1)
    public double breakWallRange = 3.5;
    @Parameter(name = "AirPlace")
    public boolean placeAirPlace = false;
    @Parameter(name = "PlaceUnderHP", min = 0.0, max = 36.0, step = 0.5)
    public double placeUnderHp = 10.0;
    @Parameter(name = "MultiPlace")
    public boolean placeMultiPlace = false;
    @Parameter(name = "SwapSwitchBack")
    public boolean swapSwitchBack = true;
    @Parameter(name = "SwapNoGap")
    public boolean swapNoGap = true;
    @Parameter(name = "SwapInventory")
    public boolean swapInventory = false;
    @Parameter(name = "BGBlockScanner")
    public boolean bgBlockScanner = true;
    @Parameter(name = "Suicide")
    public boolean suicide = false;
    @Parameter(name = "KBPrediction")
    public boolean kbPrediction = true;
    @Parameter(name = "CollateralPopList")
    public boolean collateralPop = true;
    public static net.minecraft.core.BlockPos currentPlacementBlock = null;
    public static double currentTargetDamage = 0.0;
    public static double currentSelfDamage = 0.0;
    public static int currentTargetTotems = 0;
    private long lastPlaceTime = 0;
    private long lastBreakTime = 0;
    private int  lastBreakId   = -1;
    private static final NativeLibraryUtility NATIVE = NativeLibraryUtility.of("ravex_autocrystal");
    static {
        NATIVE.load();
    }
    private static native double[] nativeTick(
            double pX, double pY, double pZ,
            double pHp, double pAbs,
            double[] pStats,
            double tX, double tY, double tZ,
            double tHp, double tAbs,
            double[] tStats,
            double[] blockData,
            double[] crystalData,
            double placeRange, double placeWallRange,
            double breakRange, double breakWallRange,
            double minTargetDmg, double maxSelfDmg,
            double selfDmgWeight, boolean antiSuicide,
            boolean antiSuicideCheckBreaking, boolean antiSuicideIgnoreWithTotem,
            boolean armorBreaker, double armorPercent,
            double predictTicks, boolean totemDetection,
            boolean totemCheckTarget, boolean placeAirPlace,
            boolean placeMultiPlace, boolean suicide,
            boolean grimAC, boolean ncpBypass,
            boolean bgBlockScanner, boolean kbPrediction,
            boolean collateralPop
    );
    public static native double[] nativeCalcDamage(
            double expX, double expY, double expZ,
            double entityX, double entityY, double entityZ,
            double entityHp, double entityAbs,
            double[] stats,
            double[] blockData
    );
    public static final SilentRotationUtility silentRotation = new SilentRotationUtility();
    private int originalSlot = -1;
    private double[] cachedBlockData = null;
    private long lastBlockScanTime = 0;
    public static boolean hasSilentRotations() {
        return silentRotation.hasRotation;
    }
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getLevel() == null || mc.getGameMode() == null) return;
        silentRotation.hasRotation = false;
        if (totemPopSwap) {
            double selfHp = EntityUtility.getHealthWithAbsorption(mc.getPlayer());
            if (selfHp <= totemPopHp) {
                if (!InventoryUtility.isOffhand(mc.getPlayer(), "totem_of_undying")) {
                    int totemSlot = InventoryUtility.findSlot(mc.getPlayer(), "totem_of_undying");
                    if (totemSlot != -1) {
                        InventoryUtility.swapToOffhand(mc, mc.getPlayer(), totemSlot);
                    }
                }
            }
        }
        net.minecraft.world.entity.LivingEntity target = findTarget(mc);
        if (target == null) {
            currentPlacementBlock = null;
            return;
        }
        net.minecraft.world.phys.Vec3 playerPos = mc.getPlayer().position();
        double pHp  = EntityUtility.getHealth(mc.getPlayer());
        double pAbs = EntityUtility.getAbsorption(mc.getPlayer());
        net.minecraft.world.phys.Vec3 targetPos = target.position();
        double tHp  = EntityUtility.getHealth(target);
        double tAbs = EntityUtility.getAbsorption(target);
        double[] blockData  = collectValidBlocks(mc, playerPos);
        double[] crystalData = collectCrystals(mc, playerPos);
        double[] pStats = getEntityStats(mc.getPlayer());
        double[] tStats = getEntityStats(target);
        boolean grimAC = rotate.equals("Grim") || placeMode.equals("Grim");
        boolean ncpBypass = rotate.equals("NCP") || rotate.equals("NCPStrict") || placeMode.equals("NCPStrict");
        double[] result;
        if (NATIVE.isLoaded()) {
            result = nativeTick(
                    playerPos.x, playerPos.y, playerPos.z,
                    pHp, pAbs, pStats,
                    targetPos.x, targetPos.y, targetPos.z,
                    tHp, tAbs, tStats,
                    blockData, crystalData,
                    placeRange, placeWallRange,
                    breakRange, breakWallRange,
                    minDamage, maxSelfDmg,
                    1.2, antiSuicide,
                    antiSuicideCheckBreaking, antiSuicideIgnoreWithTotem,
                    armorBreaker, armorPercent,
                    predictTicks, totemDetection,
                    totemCheckTarget, placeAirPlace,
                    placeMultiPlace, suicide,
                    grimAC, ncpBypass,
                    bgBlockScanner, kbPrediction,
                    collateralPop
            );
        } else {
            result = javaFallbackTick(
                    playerPos, pHp, pAbs,
                    targetPos, tHp, tAbs,
                    blockData, crystalData
            );
        }
        if (result == null || result.length < 12) {
            currentPlacementBlock = null;
            return;
        }
        boolean shouldPlace = result[0] > 0.5;
        boolean shouldBreak = result[6] > 0.5;
        if (shouldPlace && antiSuicide) {
            boolean ignoreSuicide = antiSuicideIgnoreWithTotem && pStats[14] > 0.0;
            if (!ignoreSuicide) {
                double selfDmg = result[5];
                if (pHp + pAbs - selfDmg < antiSuicideMinHp) {
                    shouldPlace = false;
                }
            }
        }
        if (shouldPlace) {
            currentPlacementBlock = new net.minecraft.core.BlockPos((int) result[1], (int) result[2], (int) result[3]);
            currentTargetDamage = result[4];
            currentSelfDamage = result[5];
            currentTargetTotems = (int) tStats[14];
        } else {
            currentPlacementBlock = null;
        }
        long now = System.currentTimeMillis();
        net.minecraft.world.phys.Vec3 rotationTarget = null;
        if (shouldBreak) {
            int entityId = (int) result[7];
            net.minecraft.world.entity.Entity crystal = mc.getLevel().getEntity(entityId);
            if (crystal instanceof EndCrystal) {
                rotationTarget = crystal.position();
            }
        }
        if (rotationTarget == null && shouldPlace) {
            rotationTarget = new net.minecraft.world.phys.Vec3(result[1] + 0.5, result[2] + 1.0, result[3] + 0.5);
        }
        if (rotationTarget != null) {
            rotateTo(mc, rotationTarget);
        }
        boolean isStrict = rotate.equals("Grim") || rotate.equals("NCPStrict");
        boolean aligned = true;
        if (isStrict && rotationTarget != null) {
            aligned = isRotationAligned(mc, rotationTarget);
        }
        int actionsThisTick = 0;
        if (shouldBreak && aligned) {
            if (now - lastBreakTime >= currentBreakDelay) {
                int entityId = (int) result[7];
                if (entityId != lastBreakId) {
                    net.minecraft.world.entity.Entity crystal = mc.getLevel().getEntity(entityId);
                    if (crystal instanceof EndCrystal) {
                        EntityUtility.attack(mc, crystal);
                        EntityUtility.swingHand(mc);
                        lastBreakTime = now;
                        lastBreakId   = entityId;
                        actionsThisTick++;
                        currentBreakDelay = (long) breakDelay;
                    }
                }
            }
        }
        boolean checkPlaceDelay = true;
        if (target != null) {
            double targetEffHp = EntityUtility.getHealthWithAbsorption(target);
            if (targetEffHp <= placeUnderHp) {
                checkPlaceDelay = false;
            }
        }
        if (shouldPlace && aligned && actionsThisTick < 2) {
            if (!checkPlaceDelay || now - lastPlaceTime >= currentPlaceDelay) {
                net.minecraft.core.BlockPos placePos = new net.minecraft.core.BlockPos(
                        (int) result[1], (int) result[2], (int) result[3]);
                boolean hasItem = switchToCrystal(mc);
                if (hasItem) {
                    net.minecraft.world.phys.Vec3 hitVec = new net.minecraft.world.phys.Vec3(
                            result[1] + 0.5, result[2] + 1.0, result[3] + 0.5);
                    net.minecraft.core.Direction face = net.minecraft.core.Direction.UP;
                    net.minecraft.world.phys.BlockHitResult hitResult = new net.minecraft.world.phys.BlockHitResult(hitVec, face, placePos, false);
                    mc.getGameMode().useItemOn(mc.getPlayer(), net.minecraft.world.InteractionHand.MAIN_HAND, hitResult);
                    SwingUtility.swing(mc.getPlayer(), mc.getPlayer().getUsedItemHand());
                    if (swapSwitchBack && originalSlot != -1) {
                        if (mc.getPlayer().connection != null) {
                            NetworkUtility.sendSetCarriedItem(originalSlot);
                        }
                        originalSlot = -1;
                    }
                    lastPlaceTime = now;
                    actionsThisTick++;
                    currentPlaceDelay = (long) placeDelay;
                }
            }
        }
        boolean shouldPlace2 = placeMultiPlace && result.length >= 16 && result[12] > 0.5;
        if (shouldPlace2 && antiSuicide) {
            boolean ignoreSuicide2 = antiSuicideIgnoreWithTotem && pStats[14] > 0.0;
            if (!ignoreSuicide2) {
                double selfDmg2 = result[17];
                if (pHp + pAbs - selfDmg2 < antiSuicideMinHp) {
                    shouldPlace2 = false;
                }
            }
        }
        if (shouldPlace2 && aligned && actionsThisTick < 2) {
            if (!checkPlaceDelay || now - lastPlaceTime >= currentPlaceDelay) {
                net.minecraft.core.BlockPos placePos2 = new net.minecraft.core.BlockPos((int) result[13], (int) result[14], (int) result[15]);
                boolean hasItem = switchToCrystal(mc);
                if (hasItem) {
                    net.minecraft.world.phys.Vec3 hitVec2 = new net.minecraft.world.phys.Vec3(
                            result[13] + 0.5, result[14] + 1.0, result[15] + 0.5);
                    net.minecraft.core.Direction face = net.minecraft.core.Direction.UP;
                    net.minecraft.world.phys.BlockHitResult hitResult2 = new net.minecraft.world.phys.BlockHitResult(hitVec2, face, placePos2, false);
                    mc.getGameMode().useItemOn(mc.getPlayer(), net.minecraft.world.InteractionHand.MAIN_HAND, hitResult2);
                    SwingUtility.swing(mc.getPlayer(), mc.getPlayer().getUsedItemHand());
                    if (swapSwitchBack && originalSlot != -1) {
                        if (mc.getPlayer().connection != null) {
                            NetworkUtility.sendSetCarriedItem(originalSlot);
                        }
                        originalSlot = -1;
                    }
                    lastPlaceTime = now;
                    actionsThisTick++;
                    currentPlaceDelay = (long) placeDelay;
                }
            }
        }
    }
    private net.minecraft.world.entity.LivingEntity findTarget(MinecraftWrapper mc) {
        net.minecraft.world.entity.LivingEntity closest = null;
        double bestMetric = Double.MAX_VALUE;
        double maxDist = Math.max(placeRange, breakRange) + 2.0;
        String mode = targetMode;
        String typeFilter = targetType;
        for (net.minecraft.world.entity.Entity e : mc.getLevel().entitiesForRendering()) {
            if (!(e instanceof net.minecraft.world.entity.LivingEntity le)) continue;
            if (EntityUtility.isSelf(le)) continue;
            if (EntityUtility.isDead(le)) continue;
            if (typeFilter.equals("Players")) {
                if (!EntityUtility.isPlayer(le)) continue;
            } else if (typeFilter.equals("Monsters")) {
                if (!EntityUtility.isHostile(le)) continue;
            } else if (typeFilter.equals("Passives")) {
                if (EntityUtility.isPlayer(le) || EntityUtility.isHostile(le)) continue;
            }
            double dist = EntityUtility.distanceToPlayer(le);
            if (dist > maxDist) continue;
            double metric = switch (mode) {
                case "Closest"        -> dist;
                case "LowestHP"      -> EntityUtility.getHealth(le);
                case "HighestDamage" -> -calcQuickDamage(mc, le);
                default               -> dist;
            };
            if (metric < bestMetric) {
                bestMetric = metric;
                closest = le;
            }
        }
        return closest;
    }
    private double[] collectValidBlocks(MinecraftWrapper mc, net.minecraft.world.phys.Vec3 playerPos) {
        long now = System.currentTimeMillis();
        if (bgBlockScanner && cachedBlockData != null && now - lastBlockScanTime < 150) {
            return cachedBlockData;
        }
        List<Double> data = new ArrayList<>();
        int r = (int) Math.ceil(placeRange) + 1;
        net.minecraft.core.BlockPos origin = net.minecraft.core.BlockPos.containing(playerPos);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                for (int dy = -2; dy <= 2; dy++) {
                    net.minecraft.core.BlockPos pos = origin.offset(dx, dy, dz);
                    net.minecraft.world.level.block.state.BlockState state = mc.getLevel().getBlockState(pos);
                    if (state.is(net.minecraft.world.level.block.Blocks.OBSIDIAN) || state.is(net.minecraft.world.level.block.Blocks.BEDROCK)) {
                        net.minecraft.world.level.block.state.BlockState above = mc.getLevel().getBlockState(pos.above());
                        net.minecraft.world.level.block.state.BlockState above2 = mc.getLevel().getBlockState(pos.above(2));
                        if (above.isAir() && above2.isAir()) {
                            data.add((double) pos.getX());
                            data.add((double) pos.getY());
                            data.add((double) pos.getZ());
                        }
                    }
                }
            }
        }
        if (Modules.enabled(BasePlace.class) && Modules.get(BasePlace.class).autoCrystalSync && BasePlace.lastPlacedBase != null) {
            long msLimit = (long) (Modules.get(BasePlace.class).syncPredictTicks * 50);
            if (System.currentTimeMillis() - BasePlace.lastPlacedTime <= msLimit) {
                net.minecraft.core.BlockPos predictedPos = BasePlace.lastPlacedBase;
                double dist = Math.sqrt(predictedPos.distToCenterSqr(mc.getPlayer().getX(), mc.getPlayer().getEyeY(), mc.getPlayer().getZ()));
                if (dist <= placeRange) {
                    boolean alreadyAdded = false;
                    for (int i = 0; i < data.size(); i += 3) {
                        if (data.get(i) == predictedPos.getX() &&
                            data.get(i+1) == predictedPos.getY() &&
                            data.get(i+2) == predictedPos.getZ()) {
                            alreadyAdded = true;
                            break;
                        }
                    }
                    if (!alreadyAdded) {
                        data.add((double) predictedPos.getX());
                        data.add((double) predictedPos.getY());
                        data.add((double) predictedPos.getZ());
                    }
                }
            }
        }
        double[] arr = new double[data.size()];
        for (int i = 0; i < arr.length; i++) arr[i] = data.get(i);
        if (bgBlockScanner) {
            cachedBlockData = arr;
            lastBlockScanTime = now;
        } else {
            cachedBlockData = null;
        }
        return arr;
    }
    private double[] collectCrystals(MinecraftWrapper mc, net.minecraft.world.phys.Vec3 playerPos) {
        List<Double> data = new ArrayList<>();
        for (net.minecraft.world.entity.Entity e : mc.getLevel().entitiesForRendering()) {
            if (!(e instanceof EndCrystal)) continue;
            if (mc.getPlayer().distanceTo(e) > breakRange + 2.0) continue;
            data.add((double) e.getId());
            data.add(e.getX());
            data.add(e.getY());
            data.add(e.getZ());
        }
        double[] arr = new double[data.size()];
        for (int i = 0; i < arr.length; i++) arr[i] = data.get(i);
        return arr;
    }
    private boolean switchToCrystal(MinecraftWrapper mc) {
        if (InventoryUtility.isHolding(mc.getPlayer(), "end_crystal")) return true;
        String mode = swapMode;
        if (mode.equals("None")) return false;
        if (swapNoGap && mc.getPlayer().isUsingItem()) {
            var usingItem = mc.getPlayer().getUseItem();
            if (InventoryUtility.isGoldenApple(usingItem) || InventoryUtility.isEnchantedGoldenApple(usingItem)) {
                return false;
            }
        }
        int slot = InventoryUtility.findHotbarSlot(mc.getPlayer(), "end_crystal");
        if (slot != -1) {
            originalSlot = InventoryUtility.getSelectedSlot(mc.getPlayer());
            InventoryUtility.silentSelectSlot(mc.getPlayer(), slot);
            return true;
        }
        if (swapInventory) {
            slot = InventoryUtility.findSlot(mc.getPlayer(), "end_crystal", 9, 36);
            if (slot != -1) {
                int targetHotbarSlot = 0;
                InventoryUtility.handleInventoryClick(mc, mc.getPlayer(), slot, targetHotbarSlot, InventoryUtility.SWAP);
                originalSlot = InventoryUtility.getSelectedSlot(mc.getPlayer());
                InventoryUtility.silentSelectSlot(mc.getPlayer(), targetHotbarSlot);
                return true;
            }
        }
        return false;
    }
    private void rotateTo(MinecraftWrapper mc, net.minecraft.world.phys.Vec3 target) {
        String mode = rotate;
        if (mode.equals("None")) return;
        float[] targetAngles = RotationUtility.anglesTo(mc.getPlayer().getEyePosition(), target);
        float currentYaw = mc.getPlayer().getYRot();
        float currentPitch = mc.getPlayer().getXRot();
        if (!silentRotation.initialized) { silentRotation.init(currentYaw, currentPitch); }
        currentYaw = silentRotation.lastYaw;
        currentPitch = silentRotation.lastPitch;
        float maxSpeed = (float) rotateSpeed;
        float[] limited = AimUtility.limitAngles(currentYaw, targetAngles[0], currentPitch, targetAngles[1], maxSpeed);
        float finalYaw = limited[0], finalPitch = limited[1];
        if (rotateRandomize > 0.0) {
            float[] rnd = AimUtility.randomize(finalYaw, finalPitch, (float) rotateRandomize);
            finalYaw = rnd[0]; finalPitch = rnd[1];
        }
        silentRotation.set(finalYaw, finalPitch);
        silentRotation.lastYaw = finalYaw;
        silentRotation.lastPitch = finalPitch;
    }
    private long currentPlaceDelay = 0;
    private long currentBreakDelay = 0;
    private boolean isRotationAligned(MinecraftWrapper mc, net.minecraft.world.phys.Vec3 target) {
        if (rotate.equals("None")) return true;
        return silentRotation.isRotationAligned(mc, target, 10.0f);
    }
    private double calcQuickDamage(MinecraftWrapper mc, net.minecraft.world.entity.LivingEntity target) {
        net.minecraft.world.phys.Vec3 playerPos = mc.getPlayer().position();
        net.minecraft.world.phys.Vec3 targetPos = target.position();
        net.minecraft.world.phys.Vec3 crystalPos = targetPos.add(0, 1, 0);
        double dist = playerPos.distanceTo(crystalPos);
        if (dist > 12.0) return 0;
        double impact = Math.max(0, (1.0 - dist / 12.0));
        return (impact * impact + impact) / 2.0 * 84.0 + 1.0;
    }
    private double[] javaFallbackTick(
            net.minecraft.world.phys.Vec3 playerPos, double pHp, double pAbs,
            net.minecraft.world.phys.Vec3 targetPos, double tHp, double tAbs,
            double[] blockData, double[] crystalData) {
        double[] result = new double[12];
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getLevel() == null) return result;
        double bestBreakDmg = 0;
        int bestId = -1;
        net.minecraft.world.phys.Vec3 bestPos = net.minecraft.world.phys.Vec3.ZERO;
        for (net.minecraft.world.entity.Entity e : mc.getLevel().entitiesForRendering()) {
            if (!(e instanceof EndCrystal)) continue;
            double dist = mc.getPlayer().distanceTo(e);
            if (dist > breakRange) continue;
            net.minecraft.world.phys.Vec3 cp = e.position();
            double tdist = cp.distanceTo(targetPos);
            double sdist = cp.distanceTo(playerPos);
            if (tdist > 12 || sdist > 12) continue;
            double tImpact = Math.max(0, (1.0 - tdist / 12.0));
            double sImpact = Math.max(0, (1.0 - sdist / 12.0));
            double tDmg = (tImpact * tImpact + tImpact) / 2.0 * 84.0 + 1.0;
            double sDmg = (sImpact * sImpact + sImpact) / 2.0 * 84.0 + 1.0;
            if (tDmg < minDamage) continue;
            if (!suicide) {
                if (sDmg > maxSelfDmg) continue;
                if (antiSuicide && pHp + pAbs - sDmg <= 0) continue;
            }
            double score = suicide ? (sDmg * 100.0 + tDmg) : tDmg;
            if (score > bestBreakDmg) {
                bestBreakDmg = score;
                bestId = e.getId();
                bestPos = e.position();
            }
        }
        if (bestId != -1) {
            result[6] = 1.0;
            result[7] = bestId;
            result[8] = bestPos.x;
            result[9] = bestPos.y;
            result[10] = bestPos.z;
            result[11] = bestBreakDmg;
        }
        return result;
    }
    public static boolean isNativeAvailable() {
        return NATIVE.isLoaded();
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


}