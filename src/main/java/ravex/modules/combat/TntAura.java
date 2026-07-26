package ravex.modules.combat;

import ravex.modules.annotations.ModuleInfo;
import net.minecraft.client.Minecraft;
import ravex.utility.misc.block.BlockUtility;
import ravex.utility.player.SwingUtility;
import net.minecraft.world.item.BlockItem;
import ravex.utility.misc.MobUtility;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import ravex.utility.misc.PhysicUtility;

import ravex.parameter.BooleanParameter;
import ravex.utility.player.rotation.RotationUtility;
import ravex.utility.player.rotation.SilentRotationUtility;
import ravex.parameter.ColorParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import ravex.utility.nativelib.NativeLibraryUtility;
import ravex.utility.player.InventoryUtility;
@ModuleInfo(name = "TntAura", category = "Combat")
public class TntAura extends ravex.modules.Module {
public final NumberParameter  range        = new NumberParameter("Range", 4.5, 1.0, 6.0, 0.1);
    public final NumberParameter  placeDelay   = new NumberParameter("PlaceDelay", 50.0, 0.0, 500.0, 10.0);
    public final NumberParameter  tntDelay     = new NumberParameter("TNTDelay", 200.0, 0.0, 1000.0, 10.0);
    public final NumberParameter  igniteDelay  = new NumberParameter("IgniteDelay", 100.0, 0.0, 500.0, 10.0);
    public final ModeParameter    swapMode     = new ModeParameter("SwapMode", "Silent",
            java.util.List.of("Silent", "Normal", "None"));
    public final ModeParameter    rotateMode   = new ModeParameter("RotateMode", "Silent",
            java.util.List.of("Silent", "Normal", "Packet", "None"));
    public final BooleanParameter roof         = new BooleanParameter("Roof", true);
    public final BooleanParameter autoDisable  = new BooleanParameter("AutoDisable", true);
    public final ModeParameter    targetMode   = new ModeParameter("Target", "Closest",
            java.util.List.of("Closest", "LowestHP"));
    public final ModeParameter    targetType   = new ModeParameter("TargetType", "Players",
            java.util.List.of("Players", "Monsters", "All"));
    public final NumberParameter  maxRate      = new NumberParameter("MaxRate", 2.0, 1.0, 5.0, 1.0);
    public final BooleanParameter render       = new BooleanParameter("Render", true);
    public final ColorParameter   color        = new ColorParameter("Color", 0xFFFF4400);
    private enum State { TRAPPING, PLACING_TNT, IGNITING, WAITING }
    private State currentState = State.TRAPPING;
    private long lastActionTime = 0;
    private int[] gapPos = null;
    private net.minecraft.world.entity.LivingEntity currentTarget = null;
    private int failedTntPlacements = 0;
    public static final List<net.minecraft.core.BlockPos> renderBlocks = new ArrayList<>();
    public static final SilentRotationUtility silentRotation = new SilentRotationUtility();
    private static final NativeLibraryUtility NATIVE = NativeLibraryUtility.of("ravex_tntaura");
    static {
        NATIVE.load();
    }
    private static native double[] nativeCalculateCage(
        double playerX, double playerY, double playerZ,
        double targetX, double targetY, double targetZ,
        double[] solidBlockData,
        double range, boolean roof,
        int gapDirection, double[] gapPosData
    );
    private static native double[] nativeCalculateTntSlot(
        double playerX, double playerY, double playerZ,
        double gapX, double gapY, double gapZ,
        double[] solidBlockData, double range
    );
    private static native double[] nativeEstimateDamage(
        double tntX, double tntY, double tntZ,
        double targetX, double targetY, double targetZ,
        double targetHealth,
        int armorPoints, int armorToughness,
        int blastProtLevel,
        boolean hasResistance, int resistanceAmplifier
    );
    public static boolean hasSilentRotations() { return silentRotation.hasRotation; }
    protected void onEnable() {
        currentState = State.TRAPPING;
        lastActionTime = 0;
        gapPos = null;
        currentTarget = null;
        failedTntPlacements = 0;
        synchronized (renderBlocks) { renderBlocks.clear(); }
    }
    protected void onDisable() {
        silentRotation.hasRotation = false;
        currentTarget = null;
        gapPos = null;
        failedTntPlacements = 0;
        synchronized (renderBlocks) { renderBlocks.clear(); }
    }
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;
        silentRotation.hasRotation = false;
        net.minecraft.world.entity.LivingEntity target = findTarget(mc);
        if (target == null) {
            if (autoDisable.getValue()) enabled = false;
            return;
        }
        if (currentTarget != target) {
            currentTarget = target;
            currentState = State.TRAPPING;
            gapPos = null;
        }
        long now = System.currentTimeMillis();
        switch (currentState) {
            case TRAPPING:
                tickTrapping(mc, target, now);
                break;
            case PLACING_TNT:
                tickPlacingTnt(mc, target, now);
                break;
            case IGNITING:
                tickIgniting(mc, target, now);
                break;
            case WAITING:
                tickWaiting(mc, now);
                break;
        }
    }
    private void tickTrapping(Minecraft mc, net.minecraft.world.entity.LivingEntity target, long now) {
        if (now - lastActionTime < placeDelay.getValue()) return;
        double[] solidData = collectSolidBlocks(mc);
        double[] gapData = gapPos != null ? new double[]{gapPos[0], gapPos[1], gapPos[2]} : null;
        double[] result = null;
        double placeRange = range.getValue() + 1.5;
        if (NATIVE.isLoaded()) {
            result = nativeCalculateCage(
                mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                target.getX(), target.getY(), target.getZ(),
                solidData, placeRange, roof.getValue(),
                0, gapData
            );
        } else {
            result = javaFallbackCage(mc, target, solidData);
        }
        if (result == null || result[0] < 0.5) {
            currentState = State.PLACING_TNT;
            lastActionTime = now;
            return;
        }
        if (gapPos == null && result.length >= 11) {
            gapPos = new int[]{(int) result[8], (int) result[9], (int) result[10]};
        }
        int blockSlot = findObsidianSlot(mc);
        if (blockSlot == -1) return;
        net.minecraft.core.BlockPos neighborPos = new net.minecraft.core.BlockPos((int) result[1], (int) result[2], (int) result[3]);
        net.minecraft.core.Direction face = net.minecraft.core.Direction.values()[(int) result[4]];
        net.minecraft.core.BlockPos targetBlock = new net.minecraft.core.BlockPos((int) result[5], (int) result[6], (int) result[7]);
        net.minecraft.world.phys.Vec3 hitVec = net.minecraft.world.phys.Vec3.atCenterOf(neighborPos).add(
            new net.minecraft.world.phys.Vec3(face.getStepX(), face.getStepY(), face.getStepZ()).scale(0.5));
        rotateTo(mc, hitVec);
        swapTo(mc, blockSlot);
        BlockHitResult hitResult = new BlockHitResult(hitVec, face, neighborPos, false);
        mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND, hitResult);
        SwingUtility.swing(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND);
        restoreSlot(mc, blockSlot);
        synchronized (renderBlocks) { renderBlocks.add(targetBlock); }
        lastActionTime = now;
    }
    private void tickPlacingTnt(Minecraft mc, net.minecraft.world.entity.LivingEntity target, long now) {
        if (now - lastActionTime < tntDelay.getValue()) return;
        if (gapPos == null) {
            net.minecraft.core.BlockPos feet = target.blockPosition();
            double dx = mc.player.getX() - (feet.getX() + 0.5);
            double dz = mc.player.getZ() - (feet.getZ() + 0.5);
            int headY = feet.getY() + 1;
            if (Math.abs(dx) >= Math.abs(dz)) {
                gapPos = dx > 0 ? new int[]{feet.getX() + 1, headY, feet.getZ()}
                                : new int[]{feet.getX() - 1, headY, feet.getZ()};
            } else {
                gapPos = dz > 0 ? new int[]{feet.getX(), headY, feet.getZ() + 1}
                                : new int[]{feet.getX(), headY, feet.getZ() - 1};
            }
        }
        int tntSlot = findTntSlot(mc);
        if (tntSlot == -1) {
            if (autoDisable.getValue()) enabled = false;
            return;
        }
        double[] solidData = collectSolidBlocks(mc);
        double[] result = null;
        double placeRange = range.getValue() + 1.5;
        if (NATIVE.isLoaded()) {
            result = nativeCalculateTntSlot(
                mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                gapPos[0], gapPos[1], gapPos[2],
                solidData, placeRange
            );
        } else {
            result = javaFallbackTntPlacement(mc);
        }
        if (result == null || result[0] < 0.5) {
            failedTntPlacements++;
            if (failedTntPlacements >= 5) {
                if (autoDisable.getValue()) {
                    enabled = false;
                } else {
                    currentState = State.TRAPPING;
                    gapPos = null;
                    failedTntPlacements = 0;
                }
            }
            return;
        }
        failedTntPlacements = 0;
        net.minecraft.core.BlockPos neighborPos = new net.minecraft.core.BlockPos((int) result[1], (int) result[2], (int) result[3]);
        net.minecraft.core.Direction face = net.minecraft.core.Direction.values()[(int) result[4]];
        net.minecraft.world.phys.Vec3 hitVec = net.minecraft.world.phys.Vec3.atCenterOf(neighborPos).add(
            new net.minecraft.world.phys.Vec3(face.getStepX(), face.getStepY(), face.getStepZ()).scale(0.5));
        rotateTo(mc, hitVec);
        swapTo(mc, tntSlot);
        BlockHitResult hitResult = new BlockHitResult(hitVec, face, neighborPos, false);
        mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND, hitResult);
        SwingUtility.swing(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND);
        restoreSlot(mc, tntSlot);
        currentState = State.IGNITING;
        lastActionTime = now;
    }
    private void tickIgniting(Minecraft mc, net.minecraft.world.entity.LivingEntity target, long now) {
        if (now - lastActionTime < igniteDelay.getValue()) return;
        int flintSlot = findFlintAndSteelSlot(mc);
        if (flintSlot == -1) {
            if (autoDisable.getValue()) enabled = false;
            return;
        }
        net.minecraft.core.BlockPos tntPos = new net.minecraft.core.BlockPos(gapPos[0], gapPos[1], gapPos[2]);
        net.minecraft.world.phys.Vec3 hitVec = net.minecraft.world.phys.Vec3.atCenterOf(tntPos);
        rotateTo(mc, hitVec);
        swapTo(mc, flintSlot);
        BlockHitResult hitResult = new BlockHitResult(hitVec, net.minecraft.core.Direction.UP, tntPos, false);
        mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND, hitResult);
        SwingUtility.swing(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND);
        restoreSlot(mc, flintSlot);
        currentState = State.WAITING;
        lastActionTime = now;
    }
    private void tickWaiting(Minecraft mc, long now) {
        if (now - lastActionTime > 5000) {
            if (autoDisable.getValue()) {
                enabled = false;
            } else {
                currentState = State.TRAPPING;
                gapPos = null;
                synchronized (renderBlocks) { renderBlocks.clear(); }
            }
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
            if (MobUtility.isSelf(le)) continue;
            if (MobUtility.isDead(le)) continue;
            if (typeFilter.equals("Players") && !MobUtility.isPlayer(le)) continue;
            if (typeFilter.equals("Monsters") && !(le instanceof net.minecraft.world.entity.monster.Monster)) continue;
            double dist = MobUtility.distanceToPlayer(le);
            if (dist > maxDist) continue;
            double metric = mode.equals("LowestHP") ? MobUtility.getHealth(le) : dist;
            if (metric < bestMetric) {
                bestMetric = metric;
                closest = le;
            }
        }
        return closest;
    }
    private int findObsidianSlot(Minecraft mc) {
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(mc.player, i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem bi) {
                if (bi.getBlock() == net.minecraft.world.level.block.Blocks.OBSIDIAN) return i;
            }
        }
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(mc.player, i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem bi) {
                if (bi.getBlock().defaultBlockState().isCollisionShapeFullBlock(mc.level, net.minecraft.core.BlockPos.ZERO)) {
                    return i;
                }
            }
        }
        return -1;
    }
    private int findTntSlot(Minecraft mc) {
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(mc.player, i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem bi) {
                if (bi.getBlock() == net.minecraft.world.level.block.Blocks.TNT) return i;
            }
        }
        return -1;
    }
    private int findFlintAndSteelSlot(Minecraft mc) {
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(mc.player, i);
            if (!stack.isEmpty() && InventoryUtility.isItem(stack, "flint_and_steel")) return i;
        }
        return -1;
    }
    private double[] collectSolidBlocks(Minecraft mc) {
        List<Double> data = new ArrayList<>();
        double r = range.getValue() + 3.0;
        net.minecraft.core.BlockPos playerPos = mc.player.blockPosition();
        int rx = (int) Math.ceil(r);
        int ry = 3;
        int rz = (int) Math.ceil(r);
        for (int dx = -rx; dx <= rx; dx++) {
            for (int dy = -ry; dy <= ry; dy++) {
                for (int dz = -rz; dz <= rz; dz++) {
                    net.minecraft.core.BlockPos pos = playerPos.offset(dx, dy, dz);
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
        for (int i = 0; i < arr.length; i++) arr[i] = data.get(i);
        return arr;
    }
    private void rotateTo(Minecraft mc, net.minecraft.world.phys.Vec3 target) {
        String mode = rotateMode.getValue();
        if (mode.equals("None")) return;
        float[] angles = RotationUtility.anglesTo(mc.player.getEyePosition(), target);
        float yaw = angles[0], pitch = angles[1];
        if (mode.equals("Normal")) {
            mc.player.setYRot(yaw);
            mc.player.setXRot(pitch);
        } else if (mode.equals("Silent")) {
            silentRotation.set(yaw, pitch);
        } else if (mode.equals("Packet") && mc.player.connection != null) {
            mc.player.connection.send(
                new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot(
                    yaw, pitch, mc.player.onGround(), mc.player.horizontalCollision));
        }
    }
    private int savedSlot = -1;
    private void swapTo(Minecraft mc, int slot) {
        String swap = swapMode.getValue();
        savedSlot = InventoryUtility.getSelectedSlot(mc.player);
        if (swap.equals("Normal")) {
            InventoryUtility.selectSlot(mc.player, slot);
        } else if (swap.equals("Silent")) {
            if (mc.player.connection != null) {
                mc.player.connection.send(
                    new net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket(slot));
            }
        }
    }
    private void restoreSlot(Minecraft mc, int slot) {
        if (swapMode.getValue().equals("Silent") && savedSlot != -1) {
            if (mc.player.connection != null) {
                mc.player.connection.send(
                    new net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket(savedSlot));
            }
        }
    }
    private double[] javaFallbackCage(Minecraft mc, net.minecraft.world.entity.LivingEntity target, double[] solidData) {
        net.minecraft.core.BlockPos feet = target.blockPosition();
        double dx = mc.player.getX() - (feet.getX() + 0.5);
        double dz = mc.player.getZ() - (feet.getZ() + 0.5);
        int headY = feet.getY() + 1;
        net.minecraft.core.BlockPos gapBlock;
        if (Math.abs(dx) >= Math.abs(dz)) {
            gapBlock = dx > 0 ? new net.minecraft.core.BlockPos(feet.getX() + 1, headY, feet.getZ())
                              : new net.minecraft.core.BlockPos(feet.getX() - 1, headY, feet.getZ());
        } else {
            gapBlock = dz > 0 ? new net.minecraft.core.BlockPos(feet.getX(), headY, feet.getZ() + 1)
                              : new net.minecraft.core.BlockPos(feet.getX(), headY, feet.getZ() - 1);
        }
        if (gapPos == null) {
            gapPos = new int[]{gapBlock.getX(), gapBlock.getY(), gapBlock.getZ()};
        }
        List<net.minecraft.core.BlockPos> candidates = new ArrayList<>();
        candidates.add(feet.north()); candidates.add(feet.south());
        candidates.add(feet.east());  candidates.add(feet.west());
        net.minecraft.core.BlockPos[] headSides = {feet.above().north(), feet.above().south(),
                                feet.above().east(), feet.above().west()};
        for (net.minecraft.core.BlockPos h : headSides) {
            if (!h.equals(gapBlock)) candidates.add(h);
        }
        if (roof.getValue()) candidates.add(feet.above(2));
        Set<net.minecraft.core.BlockPos> solids = new HashSet<>();
        for (int i = 0; i + 2 < solidData.length; i += 3) {
            solids.add(new net.minecraft.core.BlockPos((int) solidData[i], (int) solidData[i + 1], (int) solidData[i + 2]));
        }
        net.minecraft.world.phys.Vec3 eyePos = mc.player.getEyePosition();
        double r = range.getValue();
        for (net.minecraft.core.BlockPos cand : candidates) {
            if (solids.contains(cand)) continue;
            if (eyePos.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(cand)) > r * r) continue;
            for (net.minecraft.core.Direction d : net.minecraft.core.Direction.values()) {
                net.minecraft.core.BlockPos side = cand.relative(d);
                if (solids.contains(side)) {
                    return new double[]{
                        1.0,
                        side.getX(), side.getY(), side.getZ(),
                        d.getOpposite().ordinal(),
                        cand.getX(), cand.getY(), cand.getZ(),
                        gapBlock.getX(), gapBlock.getY(), gapBlock.getZ()
                    };
                }
            }
        }
        return new double[]{0.0};
    }
    private double[] javaFallbackTntPlacement(Minecraft mc) {
        if (gapPos == null) return new double[]{0.0};
        net.minecraft.core.BlockPos gap = new net.minecraft.core.BlockPos(gapPos[0], gapPos[1], gapPos[2]);
        net.minecraft.world.phys.Vec3 eyePos = mc.player.getEyePosition();
        double r = range.getValue();
        if (eyePos.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(gap)) > r * r) return new double[]{0.0};
        for (net.minecraft.core.Direction d : net.minecraft.core.Direction.values()) {
            net.minecraft.core.BlockPos side = gap.relative(d);
            BlockState state = mc.level.getBlockState(side);
            if (!state.isAir() && !state.liquid()) {
                return new double[]{
                    1.0,
                    side.getX(), side.getY(), side.getZ(),
                    d.getOpposite().ordinal(),
                    gap.getX(), gap.getY(), gap.getZ(),
                };
            }
        }
        return new double[]{0.0};
    }
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("TntAura").getEnabled();
    }
    public static TntAura itz() {
        return ravex.manager.ModuleManager.delegate(TntAura.class);
    }

    public java.util.List<ravex.parameter.Parameter<?>> getParameters() {
        java.util.List<ravex.parameter.Parameter<?>> list = new java.util.ArrayList<>();
        for (java.lang.reflect.Field field : getClass().getDeclaredFields()) {
            if (ravex.parameter.Parameter.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    list.add((ravex.parameter.Parameter<?>) field.get(this));
                } catch (Exception ignored) {}
            }
        }
        return list;
    }
}