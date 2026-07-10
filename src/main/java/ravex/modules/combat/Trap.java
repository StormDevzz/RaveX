package ravex.modules.combat;
<<<<<<< HEAD
import ravex.manager.ModuleManager;
=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import ravex.utility.misc.MobUtility;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
<<<<<<< HEAD
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.rotation.RotationUtility;
import ravex.utility.player.rotation.SilentRotation;
=======
import ravex.utility.player.rotation.RotationUtility;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import ravex.parameter.ColorParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import java.util.ArrayList;
import java.util.List;
import ravex.utility.nativelib.NativeLibrary;
public class Trap extends Module {
<<<<<<< HEAD
=======
    public static final Trap INSTANCE = new Trap();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    public final NumberParameter  range          = new NumberParameter("Range",          4.5, 1.0, 6.0, 0.1);
    public final NumberParameter  placeDelay     = new NumberParameter("PlaceDelay",     50.0, 0.0, 500.0, 10.0);
    public final ModeParameter    swapMode       = new ModeParameter("SwapMode", "Silent",
            java.util.List.of("Silent", "Normal", "None"));
    public final ModeParameter    rotate         = new ModeParameter("RotateMode", "Silent",
            java.util.List.of("Silent", "Normal", "Packet", "None"));
    public final BooleanParameter roof           = new BooleanParameter("Roof",           true);
    public final BooleanParameter autoDisable    = new BooleanParameter("AutoDisable",    true);
    public final ModeParameter    targetMode     = new ModeParameter("Target", "Closest",
            java.util.List.of("Closest", "LowestHP"));
    public final ModeParameter    targetType     = new ModeParameter("TargetType", "Players",
            java.util.List.of("Players", "Monsters", "Passives", "All"));
    public final ModeParameter    speedMode      = new ModeParameter("SpeedMode", "Normal",
            java.util.List.of("Legit", "Normal", "Aggressive"));
    public final NumberParameter  jitterDelay    = new NumberParameter("JitterDelay", 0.0, 0.0, 100.0, 5.0);
    public final BooleanParameter strictRotation = new BooleanParameter("StrictRotation", false);
    public final NumberParameter  maxRate        = new NumberParameter("MaxRate", 2.0, 1.0, 5.0, 1.0);
    public final BooleanParameter swapSwitchBack = new BooleanParameter("SwapSwitchBack", true);
    public final BooleanParameter swapInventory  = new BooleanParameter("SwapInventory", false);
    public final BooleanParameter render         = new BooleanParameter("Render",         true);
    public final ColorParameter   color          = new ColorParameter("Color",           0xFFFFAA00);
    private long lastPlaceTime = 0;
    private long currentPlaceDelay = 0;
<<<<<<< HEAD
    public static final SilentRotation silentRotation = new SilentRotation();
=======
    public static float silentYaw = 0;
    public static float silentPitch = 0;
    private static boolean hasSilentRotations = false;
    private float lastSilentYaw = 0f;
    private float lastSilentPitch = 0f;
    private boolean lastSilentInit = false;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    public static final List<BlockPos> trapBlocks = new ArrayList<>();
    private static final NativeLibrary NATIVE = NativeLibrary.of("ravex_trap");
    static {
        NATIVE.load();
    }
    public static boolean hasSilentRotations() {
        return silentRotation.hasRotation;
    }
    private static native double[] nativeCalculateTrap(
            double playerX, double playerY, double playerZ,
            double targetX, double targetY, double targetZ,
            double[] solidBlockData,
            double range,
            boolean roof
    );
    private Trap() {
        super("Trap");
        jitterDelay.setVisible(() -> !speedMode.getValue().equals("Aggressive"));
        strictRotation.setVisible(() -> !rotate.getValue().equals("None"));
        maxRate.setVisible(() -> !speedMode.getValue().equals("Legit"));
        swapSwitchBack.setVisible(() -> !swapMode.getValue().equals("None"));
        swapInventory.setVisible(() -> !swapMode.getValue().equals("None"));
    }
    @Override
    protected void onEnable() {
        lastPlaceTime = 0;
        currentPlaceDelay = 0;
        silentRotation.initialized = false;
        synchronized (trapBlocks) {
            trapBlocks.clear();
        }
    }
    @Override
    protected void onDisable() {
        synchronized (trapBlocks) {
            trapBlocks.clear();
        }
    }
    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;
<<<<<<< HEAD
        silentRotation.hasRotation = false;
=======
        hasSilentRotations = false;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        synchronized (trapBlocks) {
            trapBlocks.clear();
        }
        net.minecraft.world.entity.LivingEntity target = findTarget(mc);
        if (target == null) {
            if (autoDisable.getValue()) {
                setEnabled(false);
            }
            return;
        }
        double[] solidBlockData = collectSolidBlocks(mc);
        List<Double> activeSolidBlocks = new ArrayList<>();
        for (double d : solidBlockData) {
            activeSolidBlocks.add(d);
        }
        int simLimit = 9;
        int simCount = 0;
        List<BlockPos> simulatedBlocks = new ArrayList<>();
        while (simCount < simLimit) {
            double[] currentSolidData = new double[activeSolidBlocks.size()];
            for (int i = 0; i < currentSolidData.length; i++) {
                currentSolidData[i] = activeSolidBlocks.get(i);
            }
            double[] result = null;
            if (NATIVE.isLoaded()) {
                result = nativeCalculateTrap(
                        mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                        target.getX(), target.getY(), target.getZ(),
                        currentSolidData,
                        range.getValue(),
                        roof.getValue()
                );
            } else {
                result = javaFallbackCalculate(mc, target, currentSolidData);
            }
            if (result == null || result[0] < 0.5) {
                break;
            }
            BlockPos targetBlock = new BlockPos((int) result[5], (int) result[6], (int) result[7]);
            simulatedBlocks.add(targetBlock);
            simCount++;
            activeSolidBlocks.add((double) targetBlock.getX());
            activeSolidBlocks.add((double) targetBlock.getY());
            activeSolidBlocks.add((double) targetBlock.getZ());
        }
        synchronized (trapBlocks) {
            trapBlocks.addAll(simulatedBlocks);
        }
        long now = System.currentTimeMillis();
        boolean checkPlaceDelay = !speedMode.getValue().equals("Aggressive");
        if (checkPlaceDelay && now - lastPlaceTime < currentPlaceDelay) {
            return;
        }
        int blockSlot = findBlockSlot(mc);
        if (blockSlot == -1) return;
        activeSolidBlocks.clear();
        for (double d : solidBlockData) {
            activeSolidBlocks.add(d);
        }
        int limit = maxRate.getValue().intValue();
        if (speedMode.getValue().equals("Legit")) {
            limit = 1;
        }
        int actionsThisTick = 0;
<<<<<<< HEAD
        int originalSlot = InventoryUtility.getSelectedSlot(mc.player);
=======
        int originalSlot = mc.player.getInventory().getSelectedSlot();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        boolean placedAny = false;
        while (actionsThisTick < limit) {
            double[] currentSolidData = new double[activeSolidBlocks.size()];
            for (int i = 0; i < currentSolidData.length; i++) {
                currentSolidData[i] = activeSolidBlocks.get(i);
            }
            double[] result = null;
            if (NATIVE.isLoaded()) {
                result = nativeCalculateTrap(
                        mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                        target.getX(), target.getY(), target.getZ(),
                        currentSolidData,
                        range.getValue(),
                        roof.getValue()
                );
            } else {
                result = javaFallbackCalculate(mc, target, currentSolidData);
            }
            if (result == null || result[0] < 0.5) {
                break;
            }
            BlockPos neighborPos = new BlockPos((int) result[1], (int) result[2], (int) result[3]);
            Direction face = Direction.values()[(int) result[4]];
            BlockPos targetBlock = new BlockPos((int) result[5], (int) result[6], (int) result[7]);
            Vec3 hitVec = Vec3.atCenterOf(neighborPos).add(new Vec3(face.getStepX(), face.getStepY(), face.getStepZ()).scale(0.5));
            rotateTo(mc, hitVec);
            boolean isStrict = strictRotation.getValue() || speedMode.getValue().equals("Legit");
            if (isStrict && !isRotationAligned(mc, hitVec)) {
                break;
            }
            String swap = swapMode.getValue();
            if (swap.equals("Normal")) {
                InventoryUtility.selectSlot(mc.player, blockSlot);
            } else if (swap.equals("Silent")) {
                InventoryUtility.silentSelectSlot(mc.player, blockSlot);
            } else if (swap.equals("None")) {
                if (InventoryUtility.getSelectedSlot(mc.player) != blockSlot) {
                    break;
                }
            }
            BlockHitResult hitResult = new BlockHitResult(hitVec, face, neighborPos, false);
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hitResult);
            mc.player.swing(InteractionHand.MAIN_HAND);
            placedAny = true;
            actionsThisTick++;
            activeSolidBlocks.add((double) targetBlock.getX());
            activeSolidBlocks.add((double) targetBlock.getY());
            activeSolidBlocks.add((double) targetBlock.getZ());
        }
        if (placedAny && swapMode.getValue().equals("Silent") && swapSwitchBack.getValue() && originalSlot != -1) {
            InventoryUtility.silentSelectSlot(mc.player, originalSlot);
        }
        if (placedAny) {
            lastPlaceTime = now;
            double base = placeDelay.getValue();
            double jitter = (Math.random() - 0.5) * jitterDelay.getValue();
            currentPlaceDelay = Math.max(0, (long)(base + jitter));
        } else {
            if (autoDisable.getValue() && simulatedBlocks.isEmpty()) {
                setEnabled(false);
            }
        }
<<<<<<< HEAD
        if (!silentRotation.hasRotation) {
            silentRotation.initialized = false;
=======
        if (!hasSilentRotations) {
            lastSilentInit = false;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        }
    }
    private net.minecraft.world.entity.LivingEntity findTarget(Minecraft mc) {
        net.minecraft.world.entity.LivingEntity closest = null;
        double bestMetric = Double.MAX_VALUE;
        double maxDist = range.getValue() + 2.0;
        String mode = targetMode.getValue();
        String typeFilter = targetType.getValue();
        for (net.minecraft.world.entity.Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof net.minecraft.world.entity.LivingEntity le)) continue;
<<<<<<< HEAD
            if (MobUtility.isSelf(le)) continue;
            if (MobUtility.isDead(le)) continue;
=======
            if (le == mc.player) continue;
            if (le.isDeadOrDying()) continue;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            if (typeFilter.equals("Players")) {
                if (!MobUtility.isPlayer(le)) continue;
            } else if (typeFilter.equals("Monsters")) {
                if (!MobUtility.isHostile(le)) continue;
            } else if (typeFilter.equals("Passives")) {
                if (MobUtility.isPlayer(le) || MobUtility.isHostile(le)) continue;
            }
<<<<<<< HEAD
            double dist = MobUtility.distanceToPlayer(le);
            if (dist > maxDist) continue;
            double metric = switch (mode) {
                case "Closest"   -> dist;
                case "LowestHP" -> MobUtility.getHealth(le);
=======
            double dist = mc.player.distanceTo(le);
            if (dist > maxDist) continue;
            double metric = switch (mode) {
                case "Closest"   -> dist;
                case "LowestHP" -> le.getHealth();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
                default          -> dist;
            };
            if (metric < bestMetric) {
                bestMetric = metric;
                closest = le;
            }
        }
        return closest;
    }
    private int findBlockSlot(Minecraft mc) {
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(mc.player, i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem blockItem) {
                if (blockItem.getBlock() == Blocks.OBSIDIAN) {
                    return i;
                }
            }
        }
        if (swapInventory.getValue()) {
            for (int i = 9; i < 36; i++) {
                var stack = InventoryUtility.getItem(mc.player, i);
                if (!stack.isEmpty() && stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() == Blocks.OBSIDIAN) {
                    InventoryUtility.handleInventoryClick(mc, mc.player, i, 0, net.minecraft.world.inventory.ClickType.SWAP);
                    return 0;
                }
            }
        }
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(mc.player, i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem blockItem) {
                var block = blockItem.getBlock();
                if (block.defaultBlockState().isCollisionShapeFullBlock(mc.level, BlockPos.ZERO)) {
                    return i;
                }
            }
        }
        return -1;
    }
    private double[] collectSolidBlocks(Minecraft mc) {
        List<Double> data = new ArrayList<>();
        double r = range.getValue() + 3.0;
        BlockPos playerPos = mc.player.blockPosition();
        int rx = (int) Math.ceil(r);
        int ry = 3;
        int rz = (int) Math.ceil(r);
        for (int dx = -rx; dx <= rx; dx++) {
            for (int dy = -ry; dy <= ry; dy++) {
                for (int dz = -rz; dz <= rz; dz++) {
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
    private double[] javaFallbackCalculate(Minecraft mc, net.minecraft.world.entity.LivingEntity target, double[] solidBlockData) {
        BlockPos targetPos = target.blockPosition();
        List<BlockPos> candidates = new ArrayList<>();
        candidates.add(targetPos.north());
        candidates.add(targetPos.south());
        candidates.add(targetPos.east());
        candidates.add(targetPos.west());
        candidates.add(targetPos.north().above());
        candidates.add(targetPos.south().above());
        candidates.add(targetPos.east().above());
        candidates.add(targetPos.west().above());
        if (roof.getValue()) {
            candidates.add(targetPos.above(2));
        }
        java.util.Set<BlockPos> simulatedSolids = new java.util.HashSet<>();
        for (int i = 0; i + 2 < solidBlockData.length; i += 3) {
            simulatedSolids.add(new BlockPos((int) solidBlockData[i], (int) solidBlockData[i+1], (int) solidBlockData[i+2]));
        }
        Vec3 eyePos = mc.player.getEyePosition();
        double r = range.getValue();
        for (BlockPos cand : candidates) {
            if (simulatedSolids.contains(cand)) continue;
            if (eyePos.distanceToSqr(Vec3.atCenterOf(cand)) > r * r) continue;
            BlockPos neighbor = null;
            Direction face = null;
            for (Direction d : Direction.values()) {
                BlockPos side = cand.relative(d);
                if (simulatedSolids.contains(side)) {
                    neighbor = side;
                    face = d.getOpposite();
                    break;
                }
            }
            if (neighbor == null) {
                BlockPos below = cand.below();
                if (!simulatedSolids.contains(below)) {
                    for (Direction d : Direction.values()) {
                        BlockPos side = below.relative(d);
                        if (simulatedSolids.contains(side)) {
                            neighbor = side;
                            face = d.getOpposite();
                            cand = below;
                            break;
                        }
                    }
                }
            }
            if (neighbor != null) {
                double[] result = new double[8];
                result[0] = 1.0;
                result[1] = neighbor.getX();
                result[2] = neighbor.getY();
                result[3] = neighbor.getZ();
                result[4] = face.ordinal();
                result[5] = cand.getX();
                result[6] = cand.getY();
                result[7] = cand.getZ();
                return result;
            }
        }
        return new double[]{0.0};
    }
    private void rotateTo(Minecraft mc, Vec3 target) {
        String mode = rotate.getValue();
        if (mode.equals("None")) return;
        float[] angles = RotationUtility.anglesTo(mc.player.getEyePosition(), target);
<<<<<<< HEAD
        float currentYaw = mc.player.getYRot(), currentPitch = mc.player.getXRot();
        if (mode.equals("Silent")) {
            if (!silentRotation.initialized) {
                silentRotation.init(currentYaw, currentPitch);
            }
            currentYaw = silentRotation.lastYaw; currentPitch = silentRotation.lastPitch;
        }
        if (mode.equals("Normal")) {
            mc.player.setYRot(angles[0]); mc.player.setXRot(angles[1]);
        } else if (mode.equals("Silent")) {
            silentRotation.set(angles[0], angles[1]);
            silentRotation.lastYaw = angles[0]; silentRotation.lastPitch = angles[1];
        } else if (mode.equals("Packet") && mc.player.connection != null) {
            mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot(angles[0], angles[1], mc.player.onGround(), mc.player.horizontalCollision));
=======
        float targetYaw = angles[0], targetPitch = angles[1];
        float currentYaw = mc.player.getYRot(), currentPitch = mc.player.getXRot();
        if (mode.equals("Silent")) {
            if (!lastSilentInit) {
                lastSilentYaw = currentYaw; lastSilentPitch = currentPitch; lastSilentInit = true;
            }
            currentYaw = lastSilentYaw; currentPitch = lastSilentPitch;
        }
        if (mode.equals("Normal")) {
            mc.player.setYRot(targetYaw); mc.player.setXRot(targetPitch);
        } else if (mode.equals("Silent")) {
            silentYaw = targetYaw; silentPitch = targetPitch; hasSilentRotations = true;
            lastSilentYaw = targetYaw; lastSilentPitch = targetPitch;
        } else if (mode.equals("Packet") && mc.player.connection != null) {
            mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot(targetYaw, targetPitch, mc.player.onGround(), mc.player.horizontalCollision));
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        }
    }
    private boolean isRotationAligned(Minecraft mc, Vec3 target) {
        if (rotate.getValue().equals("None")) return true;
<<<<<<< HEAD
        return silentRotation.isRotationAligned(mc, target, 10.0f);
    }
    public static boolean isNativeAvailable() {
        return NATIVE.isLoaded();
    }
    public static boolean maybeEnabled() {
        return maybeEnabled(Trap.class);
    }
    public static Trap itz() {
        return ModuleManager.get(Trap.class);
=======
        float[] targetAngles = RotationUtility.anglesTo(mc.player.getEyePosition(), target);
        float currentYaw = mc.player.getYRot(), currentPitch = mc.player.getXRot();
        if (rotate.getValue().equals("Silent") && lastSilentInit) {
            currentYaw = lastSilentYaw; currentPitch = lastSilentPitch;
        }
        float diffYaw = Math.abs(RotationUtility.diffYaw(currentYaw, targetAngles[0]));
        float diffPitch = Math.abs(RotationUtility.diffPitch(currentPitch, targetAngles[1]));
        return diffYaw <= 10.0f && diffPitch <= 10.0f;
    }
    public static boolean isNativeAvailable() {
        return NATIVE.isLoaded();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    }
}
