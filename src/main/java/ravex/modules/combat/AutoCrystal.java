package ravex.modules.combat;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import ravex.utility.misc.MobUtility;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.rotation.AimUtility;
import ravex.utility.player.rotation.RotationUtility;
import ravex.utility.player.rotation.SilentRotation;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import ravex.utility.nativelib.NativeLibrary;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffects;
import java.util.ArrayList;
import java.util.List;
import ravex.manager.ModuleManager;
public class AutoCrystal extends Module {
    public final NumberParameter  placeRange     = new NumberParameter("PlaceRange",    4.5, 1.0, 6.0, 0.1);
    public final NumberParameter  breakRange     = new NumberParameter("BreakRange",    4.5, 1.0, 6.0, 0.1);
    public final NumberParameter  placeDelay     = new NumberParameter("PlaceDelay",   100, 0, 500, 10);
    public final NumberParameter  breakDelay     = new NumberParameter("BreakDelay",    50, 0, 500, 10);
    public final NumberParameter  minDamage      = new NumberParameter("MinDamage",     4.0, 1.0, 20.0, 0.5);
    public final NumberParameter  maxSelfDmg     = new NumberParameter("MaxSelfDmg",   8.0, 1.0, 20.0, 0.5);
    public final BooleanParameter antiSuicide    = new BooleanParameter("AntiSuicide",  true);
    public final NumberParameter  antiSuicideMinHp = new NumberParameter("AntiSuicideMinHP", 6.0, 1.0, 20.0, 0.5);
    public final ModeParameter    rotate         = new ModeParameter("RotateMode", "Silent",
            java.util.List.of("Silent", "Normal", "Packet", "None"));
    public final ModeParameter    swapMode       = new ModeParameter("SwapMode", "Silent",
            java.util.List.of("Silent", "Normal", "None"));
    public final NumberParameter  swapDelay      = new NumberParameter("SwapDelay", 0.0, 0.0, 500.0, 10.0);
    public final BooleanParameter onlyInRender   = new BooleanParameter("OnlyRender",   false);
    public final ModeParameter    targetMode     = new ModeParameter("Target", "Closest",
            java.util.List.of("Closest", "LowestHP", "HighestDamage"));
    public final ModeParameter    targetType     = new ModeParameter("TargetType", "Players",
            java.util.List.of("Players", "Monsters", "Passives", "All"));
    public final BooleanParameter renderPlacement = new BooleanParameter("RenderPlacement", true);
    public final BooleanParameter renderDamage    = new BooleanParameter("RenderDamage", true);
    public final BooleanParameter armorBreaker    = new BooleanParameter("ArmorBreaker", true);
    public final NumberParameter  armorPercent    = new NumberParameter("ArmorPercent", 15.0, 1.0, 50.0, 1.0);
    public final NumberParameter  predictTicks    = new NumberParameter("PredictTicks", 1.0, 0.0, 4.0, 0.1);
    public final BooleanParameter totemDetection  = new BooleanParameter("TotemDetection", true);
    public final NumberParameter  totemMinDamage  = new NumberParameter("TotemMinDamage", 1.5, 0.5, 10.0, 0.5);
    public final NumberParameter  totemSelfMinHp  = new NumberParameter("TotemSelfMinHP", 8.0, 2.0, 20.0, 0.5);
    public final ModeParameter    placeMode      = new ModeParameter("PlaceMode", "Normal",
            java.util.List.of("Normal", "Aggressive", "Smart"));
    public final NumberParameter  rotateSpeed     = new NumberParameter("RotateSpeed", 180.0, 10.0, 180.0, 5.0);
    public final NumberParameter  rotateRandomize = new NumberParameter("RotateRandomize", 0.0, 0.0, 3.0, 0.1);
    public final BooleanParameter antiSuicideCheckBreaking = new BooleanParameter("AntiSuicideBreak", true);
    public final BooleanParameter antiSuicideIgnoreWithTotem = new BooleanParameter("AntiSuicideIgnoreTotem", false);
    public final BooleanParameter totemCheckTarget = new BooleanParameter("TotemCheckTarget", true);
    public final BooleanParameter totemPopSwap     = new BooleanParameter("TotemPopSwap", false);
    public final NumberParameter  totemPopHp       = new NumberParameter("TotemPopHP", 6.0, 1.0, 20.0, 0.5);
    public final NumberParameter  placeWallRange  = new NumberParameter("PlaceWallRange", 3.5, 1.0, 6.0, 0.1);
    public final NumberParameter  breakWallRange  = new NumberParameter("BreakWallRange", 3.5, 1.0, 6.0, 0.1);
    public final BooleanParameter placeAirPlace   = new BooleanParameter("AirPlace", false);
    public final NumberParameter  placeUnderHp     = new NumberParameter("PlaceUnderHP", 10.0, 0.0, 36.0, 0.5);
    public final BooleanParameter placeMultiPlace  = new BooleanParameter("MultiPlace", false);
    public final BooleanParameter swapSwitchBack   = new BooleanParameter("SwapSwitchBack", true);
    public final BooleanParameter swapNoGap        = new BooleanParameter("SwapNoGap", true);
    public final BooleanParameter swapInventory   = new BooleanParameter("SwapInventory", false);
    public final ModeParameter    speedMode      = new ModeParameter("SpeedMode", "Normal",
            java.util.List.of("Legit", "Normal", "Aggressive"));
    public final NumberParameter  jitterDelay    = new NumberParameter("JitterDelay", 0.0, 0.0, 100.0, 5.0);
    public final BooleanParameter strictRotation = new BooleanParameter("StrictRotation", false);
    public final NumberParameter  maxRate        = new NumberParameter("MaxRate", 2.0, 1.0, 5.0, 1.0);
    public final BooleanParameter suicide        = new BooleanParameter("Suicide", false);
    public final BooleanParameter grimAC         = new BooleanParameter("GrimACBypass", false);
    public final BooleanParameter ncpBypass      = new BooleanParameter("NCPBypass", false);
    public final BooleanParameter bgBlockScanner = new BooleanParameter("BGBlockScanner", true);
    public final BooleanParameter kbPrediction   = new BooleanParameter("KBPrediction", true);
    public final BooleanParameter collateralPop  = new BooleanParameter("CollateralPopList", true);
    public static BlockPos currentPlacementBlock = null;
    public static double currentTargetDamage = 0.0;
    public static double currentSelfDamage = 0.0;
    public static int currentTargetTotems = 0;
    private long lastPlaceTime = 0;
    private long lastBreakTime = 0;
    private int  lastBreakId   = -1;
    private static final NativeLibrary NATIVE = NativeLibrary.of("ravex_autocrystal");
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
    private AutoCrystal() {
        super("AutoCrystal");
        armorPercent.setVisible(() -> armorBreaker.getValue());
        renderDamage.setVisible(() -> renderPlacement.getValue());
        antiSuicideMinHp.setVisible(() -> antiSuicide.getValue());
        totemMinDamage.setVisible(() -> totemDetection.getValue());
        totemSelfMinHp.setVisible(() -> totemDetection.getValue());
        swapDelay.setVisible(() -> !swapMode.getValue().equals("None"));
        rotateSpeed.setVisible(() -> !rotate.getValue().equals("None"));
        rotateRandomize.setVisible(() -> !rotate.getValue().equals("None"));
        antiSuicideCheckBreaking.setVisible(() -> antiSuicide.getValue());
        antiSuicideIgnoreWithTotem.setVisible(() -> antiSuicide.getValue());
        totemCheckTarget.setVisible(() -> totemDetection.getValue());
        totemPopHp.setVisible(() -> totemPopSwap.getValue());
        placeUnderHp.setVisible(() -> !placeMode.getValue().equals("Smart"));
        swapSwitchBack.setVisible(() -> !swapMode.getValue().equals("None"));
        swapNoGap.setVisible(() -> !swapMode.getValue().equals("None"));
        swapInventory.setVisible(() -> !swapMode.getValue().equals("None"));
        jitterDelay.setVisible(() -> !speedMode.getValue().equals("Aggressive"));
        strictRotation.setVisible(() -> !rotate.getValue().equals("None"));
        maxRate.setVisible(() -> !speedMode.getValue().equals("Legit"));
    }
    public static final SilentRotation silentRotation = new SilentRotation();
    private int originalSlot = -1;
    private double[] cachedBlockData = null;
    private long lastBlockScanTime = 0;
    public static boolean hasSilentRotations() {
        return silentRotation.hasRotation;
    }
    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;
        silentRotation.hasRotation = false;
        if (totemPopSwap.getValue()) {
            double selfHp = MobUtility.getHealthWithAbsorption(mc.player);
            if (selfHp <= totemPopHp.getValue()) {
                if (!InventoryUtility.isOffhand(mc.player, "totem_of_undying")) {
                    int totemSlot = InventoryUtility.findSlot(mc.player, "totem_of_undying");
                    if (totemSlot != -1) {
                        InventoryUtility.swapToOffhand(mc, mc.player, totemSlot);
                    }
                }
            }
        }
        LivingEntity target = findTarget(mc);
        if (target == null) {
            currentPlacementBlock = null;
            return;
        }
        Vec3 playerPos = mc.player.position();
        double pHp  = MobUtility.getHealth(mc.player);
        double pAbs = MobUtility.getAbsorption(mc.player);
        Vec3 targetPos = target.position();
        double tHp  = MobUtility.getHealth(target);
        double tAbs = MobUtility.getAbsorption(target);
        double[] blockData  = collectValidBlocks(mc, playerPos);
        double[] crystalData = collectCrystals(mc, playerPos);
        double[] pStats = getEntityStats(mc.player);
        double[] tStats = getEntityStats(target);
        double[] result;
        if (NATIVE.isLoaded()) {
            result = nativeTick(
                    playerPos.x, playerPos.y, playerPos.z,
                    pHp, pAbs, pStats,
                    targetPos.x, targetPos.y, targetPos.z,
                    tHp, tAbs, tStats,
                    blockData, crystalData,
                    placeRange.getValue(), placeWallRange.getValue(),
                    breakRange.getValue(), breakWallRange.getValue(),
                    minDamage.getValue(), maxSelfDmg.getValue(),
                    1.2, antiSuicide.getValue(),
                    antiSuicideCheckBreaking.getValue(), antiSuicideIgnoreWithTotem.getValue(),
                    armorBreaker.getValue(), armorPercent.getValue(),
                    predictTicks.getValue(), totemDetection.getValue(),
                    totemCheckTarget.getValue(), placeAirPlace.getValue(),
                    placeMultiPlace.getValue(), suicide.getValue(),
                    grimAC.getValue(), ncpBypass.getValue(),
                    bgBlockScanner.getValue(), kbPrediction.getValue(),
                    collateralPop.getValue()
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
        if (shouldPlace && antiSuicide.getValue()) {
            boolean ignoreSuicide = antiSuicideIgnoreWithTotem.getValue() && pStats[14] > 0.0;
            if (!ignoreSuicide) {
                double selfDmg = result[5];
                if (pHp + pAbs - selfDmg < antiSuicideMinHp.getValue()) {
                    shouldPlace = false;
                }
            }
        }
        if (shouldPlace) {
            currentPlacementBlock = new BlockPos((int) result[1], (int) result[2], (int) result[3]);
            currentTargetDamage = result[4];
            currentSelfDamage = result[5];
            currentTargetTotems = (int) tStats[14];
        } else {
            currentPlacementBlock = null;
        }
        long now = System.currentTimeMillis();
        Vec3 rotationTarget = null;
        if (shouldBreak) {
            int entityId = (int) result[7];
            Entity crystal = mc.level.getEntity(entityId);
            if (crystal instanceof EndCrystal) {
                rotationTarget = crystal.position();
            }
        }
        if (rotationTarget == null && shouldPlace) {
            rotationTarget = new Vec3(result[1] + 0.5, result[2] + 1.0, result[3] + 0.5);
        }
        if (rotationTarget != null) {
            rotateTo(mc, rotationTarget);
        }
        boolean isStrict = strictRotation.getValue() || speedMode.getValue().equals("Legit");
        boolean aligned = true;
        if (isStrict && rotationTarget != null) {
            aligned = isRotationAligned(mc, rotationTarget);
        }
        int limit = maxRate.getValue().intValue();
        if (speedMode.getValue().equals("Legit")) {
            limit = 1;
        }
        int actionsThisTick = 0;
        boolean checkBreakDelay = true;
        if (speedMode.getValue().equals("Aggressive")) {
            checkBreakDelay = false;
        }
        if (shouldBreak && aligned && actionsThisTick < limit) {
            if (!checkBreakDelay || now - lastBreakTime >= currentBreakDelay) {
                int entityId = (int) result[7];
                if (entityId != lastBreakId) {
                    Entity crystal = mc.level.getEntity(entityId);
                    if (crystal instanceof EndCrystal) {
                        MobUtility.attack(mc, crystal);
                        MobUtility.swingHand(mc);
                        lastBreakTime = now;
                        lastBreakId   = entityId;
                        actionsThisTick++;
                        double base = breakDelay.getValue();
                        double jitter = (Math.random() - 0.5) * jitterDelay.getValue();
                        currentBreakDelay = Math.max(0, (long)(base + jitter));
                    }
                }
            }
        }
        boolean checkPlaceDelay = true;
        if (speedMode.getValue().equals("Aggressive")) {
            checkPlaceDelay = false;
        } else if (placeMode.getValue().equals("Smart")) {
                if (currentTargetDamage < minDamage.getValue() && currentTargetDamage < MobUtility.getHealthWithAbsorption(target)) {
                shouldPlace = false;
            }
        }
        if (target != null) {
            double targetEffHp = MobUtility.getHealthWithAbsorption(target);
            if (targetEffHp <= placeUnderHp.getValue()) {
                checkPlaceDelay = false;
            }
        }
        if (shouldPlace && aligned && actionsThisTick < limit) {
            if (!checkPlaceDelay || now - lastPlaceTime >= currentPlaceDelay) {
                BlockPos placePos = new BlockPos(
                        (int) result[1], (int) result[2], (int) result[3]);
                boolean hasItem = switchToCrystal(mc);
                if (hasItem) {
                    net.minecraft.world.phys.Vec3 hitVec = new net.minecraft.world.phys.Vec3(
                            result[1] + 0.5, result[2] + 1.0, result[3] + 0.5);
                    net.minecraft.core.Direction face = net.minecraft.core.Direction.UP;
                    BlockHitResult hitResult = new BlockHitResult(hitVec, face, placePos, false);
                    mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND, hitResult);
                    mc.player.swing(mc.player.getUsedItemHand());
                    if (swapMode.getValue().equals("Silent") && swapSwitchBack.getValue() && originalSlot != -1) {
                        if (mc.player.connection != null) {
                            mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket(originalSlot));
                        }
                        originalSlot = -1;
                    }
                    lastPlaceTime = now;
                    actionsThisTick++;
                    double base = placeDelay.getValue();
                    double jitter = (Math.random() - 0.5) * jitterDelay.getValue();
                    currentPlaceDelay = Math.max(0, (long)(base + jitter));
                }
            }
        }
        boolean shouldPlace2 = placeMultiPlace.getValue() && result.length >= 16 && result[12] > 0.5;
        if (shouldPlace2 && antiSuicide.getValue()) {
            boolean ignoreSuicide2 = antiSuicideIgnoreWithTotem.getValue() && pStats[14] > 0.0;
            if (!ignoreSuicide2) {
                double selfDmg2 = result[17];
                if (pHp + pAbs - selfDmg2 < antiSuicideMinHp.getValue()) {
                    shouldPlace2 = false;
                }
            }
        }
        if (shouldPlace2 && aligned && actionsThisTick < limit) {
            if (!checkPlaceDelay || now - lastPlaceTime >= currentPlaceDelay) {
                BlockPos placePos2 = new BlockPos((int) result[13], (int) result[14], (int) result[15]);
                boolean hasItem = switchToCrystal(mc);
                if (hasItem) {
                    net.minecraft.world.phys.Vec3 hitVec2 = new net.minecraft.world.phys.Vec3(
                            result[13] + 0.5, result[14] + 1.0, result[15] + 0.5);
                    net.minecraft.core.Direction face = net.minecraft.core.Direction.UP;
                    BlockHitResult hitResult2 = new BlockHitResult(hitVec2, face, placePos2, false);
                    mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND, hitResult2);
                    mc.player.swing(mc.player.getUsedItemHand());
                    if (swapMode.getValue().equals("Silent") && swapSwitchBack.getValue() && originalSlot != -1) {
                        if (mc.player.connection != null) {
                            mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket(originalSlot));
                        }
                        originalSlot = -1;
                    }
                    lastPlaceTime = now;
                    actionsThisTick++;
                    double base = placeDelay.getValue();
                    double jitter = (Math.random() - 0.5) * jitterDelay.getValue();
                    currentPlaceDelay = Math.max(0, (long)(base + jitter));
                }
            }
        }
    }
    private LivingEntity findTarget(Minecraft mc) {
        LivingEntity closest = null;
        double bestMetric = Double.MAX_VALUE;
        double maxDist = Math.max(placeRange.getValue(), breakRange.getValue()) + 2.0;
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
                case "Closest"        -> dist;
                case "LowestHP"      -> MobUtility.getHealth(le);
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
    private double[] collectValidBlocks(Minecraft mc, Vec3 playerPos) {
        long now = System.currentTimeMillis();
        if (bgBlockScanner.getValue() && cachedBlockData != null && now - lastBlockScanTime < 150) {
            return cachedBlockData;
        }
        List<Double> data = new ArrayList<>();
        int r = (int) Math.ceil(placeRange.getValue()) + 1;
        BlockPos origin = BlockPos.containing(playerPos);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                for (int dy = -2; dy <= 2; dy++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    BlockState state = mc.level.getBlockState(pos);
                    if (state.is(Blocks.OBSIDIAN) || state.is(Blocks.BEDROCK)) {
                        BlockState above = mc.level.getBlockState(pos.above());
                        BlockState above2 = mc.level.getBlockState(pos.above(2));
                        if (above.isAir() && above2.isAir()) {
                            data.add((double) pos.getX());
                            data.add((double) pos.getY());
                            data.add((double) pos.getZ());
                        }
                    }
                }
            }
        }
        if (ModuleManager.get(ravex.modules.combat.BasePlace.class).getEnabled() && ModuleManager.get(ravex.modules.combat.BasePlace.class).autoCrystalSync.getValue() && BasePlace.lastPlacedBase != null) {
            long msLimit = (long) (ModuleManager.get(ravex.modules.combat.BasePlace.class).syncPredictTicks.getValue() * 50);
            if (System.currentTimeMillis() - BasePlace.lastPlacedTime <= msLimit) {
                BlockPos predictedPos = BasePlace.lastPlacedBase;
                double dist = Math.sqrt(predictedPos.distToCenterSqr(mc.player.getX(), mc.player.getEyeY(), mc.player.getZ()));
                if (dist <= placeRange.getValue()) {
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
        if (bgBlockScanner.getValue()) {
            cachedBlockData = arr;
            lastBlockScanTime = now;
        } else {
            cachedBlockData = null;
        }
        return arr;
    }
    private double[] collectCrystals(Minecraft mc, Vec3 playerPos) {
        List<Double> data = new ArrayList<>();
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof EndCrystal)) continue;
            if (mc.player.distanceTo(e) > breakRange.getValue() + 2.0) continue;
            data.add((double) e.getId());
            data.add(e.getX());
            data.add(e.getY());
            data.add(e.getZ());
        }
        double[] arr = new double[data.size()];
        for (int i = 0; i < arr.length; i++) arr[i] = data.get(i);
        return arr;
    }
    private boolean switchToCrystal(Minecraft mc) {
        if (InventoryUtility.isHolding(mc.player, "end_crystal")) return true;
        String mode = swapMode.getValue();
        if (mode.equals("None")) return false;
        if (swapNoGap.getValue() && mc.player.isUsingItem()) {
            var usingItem = mc.player.getUseItem();
            if (InventoryUtility.isGoldenApple(usingItem) || InventoryUtility.isEnchantedGoldenApple(usingItem)) {
                return false;
            }
        }
        int slot = InventoryUtility.findHotbarSlot(mc.player, "end_crystal");
        if (slot != -1) {
            if (mode.equals("Normal")) {
                InventoryUtility.selectSlot(mc.player, slot);
            } else if (mode.equals("Silent")) {
                originalSlot = InventoryUtility.getSelectedSlot(mc.player);
                InventoryUtility.silentSelectSlot(mc.player, slot);
            }
            return true;
        }
        if (swapInventory.getValue()) {
            slot = InventoryUtility.findSlot(mc.player, "end_crystal", 9, 36);
            if (slot != -1) {
                int targetHotbarSlot = 0;
                InventoryUtility.handleInventoryClick(mc, mc.player, slot, targetHotbarSlot, net.minecraft.world.inventory.ClickType.SWAP);
                if (mode.equals("Normal")) {
                    InventoryUtility.selectSlot(mc.player, targetHotbarSlot);
                } else if (mode.equals("Silent")) {
                    originalSlot = InventoryUtility.getSelectedSlot(mc.player);
                    InventoryUtility.silentSelectSlot(mc.player, targetHotbarSlot);
                }
                return true;
            }
        }
        return false;
    }
    private void rotateTo(Minecraft mc, Vec3 target) {
        String mode = rotate.getValue();
        if (mode.equals("None")) return;
        float[] targetAngles = RotationUtility.anglesTo(mc.player.getEyePosition(), target);
        float currentYaw = mc.player.getYRot();
        float currentPitch = mc.player.getXRot();
        if (mode.equals("Silent")) {
            if (!silentRotation.initialized) { silentRotation.init(currentYaw, currentPitch); }
            currentYaw = silentRotation.lastYaw;
            currentPitch = silentRotation.lastPitch;
        }
        float maxSpeed = rotateSpeed.getValue().floatValue();
        float[] limited = AimUtility.limitAngles(currentYaw, targetAngles[0], currentPitch, targetAngles[1], maxSpeed);
        float finalYaw = limited[0], finalPitch = limited[1];
        if (rotateRandomize.getValue() > 0.0) {
            float[] rnd = AimUtility.randomize(finalYaw, finalPitch, rotateRandomize.getValue().floatValue());
            finalYaw = rnd[0]; finalPitch = rnd[1];
        }
        if (mode.equals("Normal")) {
            mc.player.setYRot(finalYaw);
            mc.player.setXRot(finalPitch);
        } else if (mode.equals("Silent")) {
            silentRotation.set(finalYaw, finalPitch);
            silentRotation.lastYaw = finalYaw; silentRotation.lastPitch = finalPitch;
        } else if (mode.equals("Packet")) {
            if (mc.player.connection != null) {
                mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot(finalYaw, finalPitch, mc.player.onGround(), mc.player.horizontalCollision));
            }
        }
    }
    private long currentPlaceDelay = 0;
    private long currentBreakDelay = 0;
    private boolean isRotationAligned(Minecraft mc, Vec3 target) {
        if (rotate.getValue().equals("None")) return true;
        return silentRotation.isRotationAligned(mc, target, 10.0f);
    }
    private double calcQuickDamage(Minecraft mc, LivingEntity target) {
        Vec3 playerPos = mc.player.position();
        Vec3 targetPos = target.position();
        Vec3 crystalPos = targetPos.add(0, 1, 0);
        double dist = playerPos.distanceTo(crystalPos);
        if (dist > 12.0) return 0;
        double impact = Math.max(0, (1.0 - dist / 12.0));
        return (impact * impact + impact) / 2.0 * 84.0 + 1.0;
    }
    private double[] javaFallbackTick(
            Vec3 playerPos, double pHp, double pAbs,
            Vec3 targetPos, double tHp, double tAbs,
            double[] blockData, double[] crystalData) {
        double[] result = new double[12];
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return result;
        double bestBreakDmg = 0;
        int bestId = -1;
        Vec3 bestPos = Vec3.ZERO;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof EndCrystal)) continue;
            double dist = mc.player.distanceTo(e);
            if (dist > breakRange.getValue()) continue;
            Vec3 cp = e.position();
            double tdist = cp.distanceTo(targetPos);
            double sdist = cp.distanceTo(playerPos);
            if (tdist > 12 || sdist > 12) continue;
            double tImpact = Math.max(0, (1.0 - tdist / 12.0));
            double sImpact = Math.max(0, (1.0 - sdist / 12.0));
            double tDmg = (tImpact * tImpact + tImpact) / 2.0 * 84.0 + 1.0;
            double sDmg = (sImpact * sImpact + sImpact) / 2.0 * 84.0 + 1.0;
            if (tDmg < minDamage.getValue()) continue;
            if (!suicide.getValue()) {
                if (sDmg > maxSelfDmg.getValue()) continue;
                if (antiSuicide.getValue() && pHp + pAbs - sDmg <= 0) continue;
            }
            double score = suicide.getValue() ? (sDmg * 100.0 + tDmg) : tDmg;
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
    public static boolean maybeEnabled() {
        return maybeEnabled(AutoCrystal.class);
    }
    public static AutoCrystal itz() {
        return ModuleManager.get(AutoCrystal.class);
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
}
