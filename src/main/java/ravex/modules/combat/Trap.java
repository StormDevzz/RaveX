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
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import java.util.ArrayList;
import java.util.List;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;




@Module(name = "Trap", category = "Combat")
public class Trap {
    @Parameter(name = "Range", min = 1.0, max = 6.0, step = 0.1)
    public double range = 4.5;
    @Parameter(name = "PlaceDelay", min = 0.0, max = 500.0, step = 10.0)
    public double placeDelay = 50.0;
    @Parameter(name = "Swap", modes = {"NCP", "Vanilla", "Legit", "None"})
    public String swapMode = "NCP";
    @Parameter(name = "Rotate", modes = {"NCP", "Vanilla", "Legit", "None"})
    public String rotate = "NCP";
    @Parameter(name = "Roof")
    public boolean roof = true;
    @Parameter(name = "AutoDisable")
    public boolean autoDisable = true;
    @Parameter(name = "Target", modes = {"Closest", "LowestHP"})
    public String targetMode = "Closest";
    @Parameter(name = "TargetType", modes = {"Players", "Monsters", "Passives", "All"})
    public String targetType = "Players";
    @Parameter(name = "SpeedMode", modes = {"Legit", "Normal", "Aggressive"})
    public String speedMode = "Normal";
    @Parameter(name = "JitterDelay", min = 0.0, max = 100.0, step = 5.0)
    public double jitterDelay = 0.0;
    @Parameter(name = "MaxRate", min = 1.0, max = 5.0, step = 1.0)
    public double maxRate = 2.0;
    @Parameter(name = "SwapSwitchBack")
    public boolean swapSwitchBack = true;
    @Parameter(name = "SwapInventory")
    public boolean swapInventory = false;
    @Parameter(name = "Render")
    public boolean render = true;
    @Parameter(name = "Color", color = true, visible = "render")
    public int color = 0xFFFFAA00;
    private long lastPlaceTime = 0;
    private long currentPlaceDelay = 0;
    public static final SilentRotationUtility silentRotation = new SilentRotationUtility();
    public static final List<net.minecraft.core.BlockPos> trapBlocks = new ArrayList<>();
    private static final NativeLibraryUtility NATIVE = NativeLibraryUtility.of("ravex_trap");
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
    public void onEnable() {
        lastPlaceTime = 0;
        currentPlaceDelay = 0;
        silentRotation.initialized = false;
        synchronized (trapBlocks) {
            trapBlocks.clear();
        }
    }
    public void onDisable() {
        synchronized (trapBlocks) {
            trapBlocks.clear();
        }
    }
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getLevel() == null || mc.getGameMode() == null) return;
        silentRotation.hasRotation = false;
        synchronized (trapBlocks) {
            trapBlocks.clear();
        }
        net.minecraft.world.entity.LivingEntity target = findTarget(mc);
        if (target == null) {
            if (autoDisable) {
                Modules.setEnabled(Trap.class, false);
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
        List<net.minecraft.core.BlockPos> simulatedBlocks = new ArrayList<>();
        while (simCount < simLimit) {
            double[] currentSolidData = new double[activeSolidBlocks.size()];
            for (int i = 0; i < currentSolidData.length; i++) {
                currentSolidData[i] = activeSolidBlocks.get(i);
            }
            double[] result = null;
            if (NATIVE.isLoaded()) {
                result = nativeCalculateTrap(
                        mc.getPlayer().getX(), mc.getPlayer().getY(), mc.getPlayer().getZ(),
                        target.getX(), target.getY(), target.getZ(),
                        currentSolidData,
                        range,
                        roof
                );
            } else {
                result = javaFallbackCalculate(mc, target, currentSolidData);
            }
            if (result == null || result[0] < 0.5) {
                break;
            }
            net.minecraft.core.BlockPos targetBlock = new net.minecraft.core.BlockPos((int) result[5], (int) result[6], (int) result[7]);
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
        boolean checkPlaceDelay = !speedMode.equals("Aggressive");
        if (checkPlaceDelay && now - lastPlaceTime < currentPlaceDelay) {
            return;
        }
        int blockSlot = findBlockSlot(mc);
        if (blockSlot == -1) return;
        activeSolidBlocks.clear();
        for (double d : solidBlockData) {
            activeSolidBlocks.add(d);
        }
        int limit = (int) maxRate;
        if (speedMode.equals("Legit")) {
            limit = 1;
        }
        int actionsThisTick = 0;
        int originalSlot = InventoryUtility.getSelectedSlot(mc.getPlayer());
        boolean placedAny = false;
        while (actionsThisTick < limit) {
            double[] currentSolidData = new double[activeSolidBlocks.size()];
            for (int i = 0; i < currentSolidData.length; i++) {
                currentSolidData[i] = activeSolidBlocks.get(i);
            }
            double[] result = null;
            if (NATIVE.isLoaded()) {
                result = nativeCalculateTrap(
                        mc.getPlayer().getX(), mc.getPlayer().getY(), mc.getPlayer().getZ(),
                        target.getX(), target.getY(), target.getZ(),
                        currentSolidData,
                        range,
                        roof
                );
            } else {
                result = javaFallbackCalculate(mc, target, currentSolidData);
            }
            if (result == null || result[0] < 0.5) {
                break;
            }
            net.minecraft.core.BlockPos neighborPos = new net.minecraft.core.BlockPos((int) result[1], (int) result[2], (int) result[3]);
            net.minecraft.core.Direction face = net.minecraft.core.Direction.values()[(int) result[4]];
            net.minecraft.core.BlockPos targetBlock = new net.minecraft.core.BlockPos((int) result[5], (int) result[6], (int) result[7]);
            net.minecraft.world.phys.Vec3 hitVec = PhysicUtility.centerOf(neighborPos).add(PhysicUtility.vec3(face.getStepX(), face.getStepY(), face.getStepZ()).scale(0.5));
            rotateTo(mc, hitVec);
            if (speedMode.equals("Legit") && !isRotationAligned(mc, hitVec)) {
                break;
            }
            String swap = swapMode;
            if (swap.equals("NCP")) {
                if (InventoryUtility.getSelectedSlot(mc.getPlayer()) != blockSlot) {
                    InventoryUtility.silentSelectSlot(mc.getPlayer(), blockSlot);
                }
            } else if (swap.equals("Vanilla")) {
                InventoryUtility.selectSlot(mc.getPlayer(), blockSlot);
            } else if (swap.equals("Legit")) {
                if (InventoryUtility.getSelectedSlot(mc.getPlayer()) != blockSlot) {
                    InventoryUtility.selectSlot(mc.getPlayer(), blockSlot);
                }
            } else if (swap.equals("None")) {
                if (InventoryUtility.getSelectedSlot(mc.getPlayer()) != blockSlot) {
                    break;
                }
            }
            net.minecraft.world.phys.BlockHitResult hitResult = new net.minecraft.world.phys.BlockHitResult(hitVec, face, neighborPos, false);
            mc.getGameMode().useItemOn(mc.getPlayer(), net.minecraft.world.InteractionHand.MAIN_HAND, hitResult);
            SwingUtility.swing(mc.getPlayer(), net.minecraft.world.InteractionHand.MAIN_HAND);
            placedAny = true;
            actionsThisTick++;
            activeSolidBlocks.add((double) targetBlock.getX());
            activeSolidBlocks.add((double) targetBlock.getY());
            activeSolidBlocks.add((double) targetBlock.getZ());
        }
        if (placedAny && swapMode.equals("NCP") && swapSwitchBack && originalSlot != -1) {
            InventoryUtility.silentSelectSlot(mc.getPlayer(), originalSlot);
        } else if (placedAny && (swapMode.equals("Vanilla") || swapMode.equals("Legit")) && swapSwitchBack && originalSlot != -1 && originalSlot != blockSlot) {
            InventoryUtility.selectSlot(mc.getPlayer(), originalSlot);
        }
        if (placedAny) {
            lastPlaceTime = now;
            double base = placeDelay;
            double jitter = (Math.random() - 0.5) * jitterDelay;
            currentPlaceDelay = Math.max(0, (long)(base + jitter));
        } else {
            if (autoDisable && simulatedBlocks.isEmpty()) {
                Modules.setEnabled(Trap.class, false);
            }
        }
        if (!silentRotation.hasRotation) {
            silentRotation.initialized = false;
        }
    }
    private net.minecraft.world.entity.LivingEntity findTarget(MinecraftWrapper mc) {
        net.minecraft.world.entity.LivingEntity closest = null;
        double bestMetric = Double.MAX_VALUE;
        double maxDist = range + 2.0;
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
                case "Closest"   -> dist;
                case "LowestHP" -> EntityUtility.getHealth(le);
                default          -> dist;
            };
            if (metric < bestMetric) {
                bestMetric = metric;
                closest = le;
            }
        }
        return closest;
    }
    private int findBlockSlot(MinecraftWrapper mc) {
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(mc.getPlayer(), i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem blockItem) {
                if (blockItem.getBlock() == net.minecraft.world.level.block.Blocks.OBSIDIAN) {
                    return i;
                }
            }
        }
        if (swapInventory) {
            for (int i = 9; i < 36; i++) {
                var stack = InventoryUtility.getItem(mc.getPlayer(), i);
                if (!stack.isEmpty() && stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() == net.minecraft.world.level.block.Blocks.OBSIDIAN) {
                    InventoryUtility.handleInventoryClick(mc, mc.getPlayer(), i, 0, InventoryUtility.SWAP);
                    return 0;
                }
            }
        }
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(mc.getPlayer(), i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem blockItem) {
                var block = blockItem.getBlock();
                if (block.defaultBlockState().isCollisionShapeFullBlock(mc.getLevel(), net.minecraft.core.BlockPos.ZERO)) {
                    return i;
                }
            }
        }
        return -1;
    }
    private double[] collectSolidBlocks(MinecraftWrapper mc) {
        List<Double> data = new ArrayList<>();
        double r = range + 3.0;
        net.minecraft.core.BlockPos playerPos = mc.getPlayer().blockPosition();
        int rx = (int) Math.ceil(r);
        int ry = 3;
        int rz = (int) Math.ceil(r);
        for (int dx = -rx; dx <= rx; dx++) {
            for (int dy = -ry; dy <= ry; dy++) {
                for (int dz = -rz; dz <= rz; dz++) {
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
    private double[] javaFallbackCalculate(MinecraftWrapper mc, net.minecraft.world.entity.LivingEntity target, double[] solidBlockData) {
        net.minecraft.core.BlockPos targetPos = target.blockPosition();
        List<net.minecraft.core.BlockPos> candidates = new ArrayList<>();
        candidates.add(targetPos.north());
        candidates.add(targetPos.south());
        candidates.add(targetPos.east());
        candidates.add(targetPos.west());
        candidates.add(targetPos.north().above());
        candidates.add(targetPos.south().above());
        candidates.add(targetPos.east().above());
        candidates.add(targetPos.west().above());
        if (roof) {
            candidates.add(targetPos.above(2));
        }
        java.util.Set<net.minecraft.core.BlockPos> simulatedSolids = new java.util.HashSet<>();
        for (int i = 0; i + 2 < solidBlockData.length; i += 3) {
            simulatedSolids.add(new net.minecraft.core.BlockPos((int) solidBlockData[i], (int) solidBlockData[i+1], (int) solidBlockData[i+2]));
        }
        net.minecraft.world.phys.Vec3 eyePos = mc.getPlayer().getEyePosition();
        double r = range;
        for (net.minecraft.core.BlockPos cand : candidates) {
            if (simulatedSolids.contains(cand)) continue;
            if (eyePos.distanceToSqr(PhysicUtility.centerOf(cand)) > r * r) continue;
            net.minecraft.core.BlockPos neighbor = null;
            net.minecraft.core.Direction face = null;
            for (net.minecraft.core.Direction d : net.minecraft.core.Direction.values()) {
                net.minecraft.core.BlockPos side = cand.relative(d);
                if (simulatedSolids.contains(side)) {
                    neighbor = side;
                    face = d.getOpposite();
                    break;
                }
            }
            if (neighbor == null) {
                net.minecraft.core.BlockPos below = cand.below();
                if (!simulatedSolids.contains(below)) {
                    for (net.minecraft.core.Direction d : net.minecraft.core.Direction.values()) {
                        net.minecraft.core.BlockPos side = below.relative(d);
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
    private void rotateTo(MinecraftWrapper mc, net.minecraft.world.phys.Vec3 target) {
        String mode = rotate;
        if (mode.equals("None")) return;
        float[] angles = RotationUtility.anglesTo(mc.getPlayer().getEyePosition(), target);
        float currentYaw = mc.getPlayer().getYRot();
        float currentPitch = mc.getPlayer().getXRot();
        if (mode.equals("NCP")) {
            if (!silentRotation.initialized) {
                silentRotation.init(currentYaw, currentPitch);
            }
            currentYaw = silentRotation.lastYaw;
            currentPitch = silentRotation.lastPitch;
            float[] limited = AimUtility.limitAngles(currentYaw, RotationUtility.fixAngle(angles[0]), currentPitch, RotationUtility.fixAngle(angles[1]), 180.0f);
            silentRotation.set(limited[0], limited[1]);
            silentRotation.lastYaw = limited[0];
            silentRotation.lastPitch = limited[1];
        } else if (mode.equals("Vanilla")) {
            mc.getPlayer().setYRot(angles[0]);
            mc.getPlayer().setXRot(angles[1]);
        } else if (mode.equals("Legit")) {
            float maxSpeed = 90.0f;
            float[] limited = AimUtility.limitAngles(currentYaw, angles[0], currentPitch, angles[1], maxSpeed);
            limited = AimUtility.randomize(limited[0], limited[1], 1.5f);
            mc.getPlayer().setYRot(limited[0]);
            mc.getPlayer().setXRot(limited[1]);
        }
    }
    private boolean isRotationAligned(MinecraftWrapper mc, net.minecraft.world.phys.Vec3 target) {
        if (rotate.equals("None")) return true;
        return silentRotation.isRotationAligned(mc, target, 10.0f);
    }
    public static boolean isNativeAvailable() {
        return NATIVE.isLoaded();
    }




}