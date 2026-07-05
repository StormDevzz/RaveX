package ravex.modules.combat;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.utility.player.rotation.RotationUtility;
import ravex.parameter.ColorParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import java.util.ArrayList;
import java.util.List;
import ravex.utility.nativelib.NativeLibrary;
public class Trap extends Module {
    public static final Trap INSTANCE = new Trap();
    public final NumberParameter  range          = new NumberParameter("Range",          4.5, 1.0, 6.0, 0.1);
    public final NumberParameter  placeDelay     = new NumberParameter("Place Delay",     50.0, 0.0, 500.0, 10.0);
    public final ModeParameter    swapMode       = new ModeParameter("Swap Mode", "Silent",
            java.util.List.of("Silent", "Normal", "None"));
    public final ModeParameter    rotate         = new ModeParameter("Rotate Mode", "Silent",
            java.util.List.of("Silent", "Normal", "Packet", "None"));
    public final BooleanParameter roof           = new BooleanParameter("Roof",           true);
    public final BooleanParameter autoDisable    = new BooleanParameter("Auto Disable",    true);
    public final ModeParameter    targetMode     = new ModeParameter("Target", "Closest",
            java.util.List.of("Closest", "Lowest HP"));
    public final ModeParameter    targetType     = new ModeParameter("Target Type", "Players",
            java.util.List.of("Players", "Monsters", "Passives", "All"));
    public final ModeParameter    speedMode      = new ModeParameter("Speed Mode", "Normal",
            java.util.List.of("Legit", "Normal", "Aggressive"));
    public final NumberParameter  jitterDelay    = new NumberParameter("Jitter Delay", 0.0, 0.0, 100.0, 5.0);
    public final BooleanParameter strictRotation = new BooleanParameter("Strict Rotation", false);
    public final NumberParameter  maxRate        = new NumberParameter("Max Rate", 2.0, 1.0, 5.0, 1.0);
    public final BooleanParameter swapSwitchBack = new BooleanParameter("Swap Switch Back", true);
    public final BooleanParameter swapInventory  = new BooleanParameter("Swap Inventory", false);
    public final BooleanParameter render         = new BooleanParameter("Render",         true);
    public final ColorParameter   color          = new ColorParameter("Color",           0xFFFFAA00);
    private long lastPlaceTime = 0;
    private long currentPlaceDelay = 0;
    public static float silentYaw = 0;
    public static float silentPitch = 0;
    private static boolean hasSilentRotations = false;
    private float lastSilentYaw = 0f;
    private float lastSilentPitch = 0f;
    private boolean lastSilentInit = false;
    public static final List<BlockPos> trapBlocks = new ArrayList<>();
    private static final NativeLibrary NATIVE = NativeLibrary.of("ravex_trap");
    static {
        NATIVE.load();
    }
    public static boolean hasSilentRotations() {
        return hasSilentRotations;
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
        lastSilentInit = false;
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
        hasSilentRotations = false;
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
        int originalSlot = mc.player.getInventory().getSelectedSlot();
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
                mc.player.getInventory().setSelectedSlot(blockSlot);
            } else if (swap.equals("Silent")) {
                if (mc.player.connection != null) {
                    mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket(blockSlot));
                }
            } else if (swap.equals("None")) {
                if (mc.player.getInventory().getSelectedSlot() != blockSlot) {
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
            if (mc.player.connection != null) {
                mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket(originalSlot));
            }
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
        if (!hasSilentRotations) {
            lastSilentInit = false;
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
            if (le == mc.player) continue;
            if (le.isDeadOrDying()) continue;
            if (typeFilter.equals("Players")) {
                if (!(le instanceof Player)) continue;
            } else if (typeFilter.equals("Monsters")) {
                if (!(le instanceof net.minecraft.world.entity.monster.Monster || le instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon || le instanceof net.minecraft.world.entity.boss.wither.WitherBoss)) continue;
            } else if (typeFilter.equals("Passives")) {
                if (le instanceof Player || le instanceof net.minecraft.world.entity.monster.Monster || le instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon || le instanceof net.minecraft.world.entity.boss.wither.WitherBoss) continue;
            }
            double dist = mc.player.distanceTo(le);
            if (dist > maxDist) continue;
            double metric = switch (mode) {
                case "Closest"   -> dist;
                case "Lowest HP" -> le.getHealth();
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
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem blockItem) {
                if (blockItem.getBlock() == Blocks.OBSIDIAN) {
                    return i;
                }
            }
        }
        if (swapInventory.getValue()) {
            for (int i = 9; i < 36; i++) {
                ItemStack stack = mc.player.getInventory().getItem(i);
                if (!stack.isEmpty() && stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() == Blocks.OBSIDIAN) {
                    mc.gameMode.handleInventoryMouseClick(
                        mc.player.containerMenu.containerId,
                        i,
                        0,
                        net.minecraft.world.inventory.ClickType.SWAP,
                        mc.player
                    );
                    return 0;
                }
            }
        }
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem blockItem) {
                Block block = blockItem.getBlock();
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
        }
    }
    private boolean isRotationAligned(Minecraft mc, Vec3 target) {
        if (rotate.getValue().equals("None")) return true;
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
    }
}
