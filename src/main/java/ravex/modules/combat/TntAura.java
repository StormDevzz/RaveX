package ravex.modules.combat;
<<<<<<< HEAD
import ravex.manager.ModuleManager;
=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
<<<<<<< HEAD
import net.minecraft.world.item.BlockItem;
import ravex.utility.misc.MobUtility;
=======
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
<<<<<<< HEAD
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.utility.player.rotation.RotationUtility;
import ravex.utility.player.rotation.SilentRotation;
=======
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.utility.player.rotation.RotationUtility;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import ravex.parameter.ColorParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import ravex.utility.nativelib.NativeLibrary;
<<<<<<< HEAD
import ravex.utility.player.InventoryUtility;
public class TntAura extends Module {
=======
public class TntAura extends Module {
    public static final TntAura INSTANCE = new TntAura();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
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
    public static final List<BlockPos> renderBlocks = new ArrayList<>();
<<<<<<< HEAD
    public static final SilentRotation silentRotation = new SilentRotation();
=======
    public static float silentYaw = 0;
    public static float silentPitch = 0;
    private static boolean hasSilentRotations = false;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    private static final NativeLibrary NATIVE = NativeLibrary.of("ravex_tntaura");
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
<<<<<<< HEAD
    public static boolean hasSilentRotations() { return silentRotation.hasRotation; }
=======
    public static boolean hasSilentRotations() { return hasSilentRotations; }
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3

    @Override
    protected void onEnable() {
        currentState = State.TRAPPING;
        lastActionTime = 0;
        gapPos = null;
        currentTarget = null;
        failedTntPlacements = 0;
        synchronized (renderBlocks) { renderBlocks.clear(); }
    }
    @Override
    protected void onDisable() {
<<<<<<< HEAD
        silentRotation.hasRotation = false;
=======
        hasSilentRotations = false;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        currentTarget = null;
        gapPos = null;
        failedTntPlacements = 0;
        synchronized (renderBlocks) { renderBlocks.clear(); }
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
        net.minecraft.world.entity.LivingEntity target = findTarget(mc);
        if (target == null) {
            if (autoDisable.getValue()) setEnabled(false);
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
        BlockPos neighborPos = new BlockPos((int) result[1], (int) result[2], (int) result[3]);
        Direction face = Direction.values()[(int) result[4]];
        BlockPos targetBlock = new BlockPos((int) result[5], (int) result[6], (int) result[7]);
        Vec3 hitVec = Vec3.atCenterOf(neighborPos).add(
            new Vec3(face.getStepX(), face.getStepY(), face.getStepZ()).scale(0.5));
        rotateTo(mc, hitVec);
        swapTo(mc, blockSlot);
        BlockHitResult hitResult = new BlockHitResult(hitVec, face, neighborPos, false);
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hitResult);
        mc.player.swing(InteractionHand.MAIN_HAND);
        restoreSlot(mc, blockSlot);
        synchronized (renderBlocks) { renderBlocks.add(targetBlock); }
        lastActionTime = now;
    }
    private void tickPlacingTnt(Minecraft mc, net.minecraft.world.entity.LivingEntity target, long now) {
        if (now - lastActionTime < tntDelay.getValue()) return;
        if (gapPos == null) {
            BlockPos feet = target.blockPosition();
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
            if (autoDisable.getValue()) setEnabled(false);
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
                    setEnabled(false);
                } else {
                    currentState = State.TRAPPING;
                    gapPos = null;
                    failedTntPlacements = 0;
                }
            }
            return;
        }
        failedTntPlacements = 0;
        BlockPos neighborPos = new BlockPos((int) result[1], (int) result[2], (int) result[3]);
        Direction face = Direction.values()[(int) result[4]];
        Vec3 hitVec = Vec3.atCenterOf(neighborPos).add(
            new Vec3(face.getStepX(), face.getStepY(), face.getStepZ()).scale(0.5));
        rotateTo(mc, hitVec);
        swapTo(mc, tntSlot);
        BlockHitResult hitResult = new BlockHitResult(hitVec, face, neighborPos, false);
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hitResult);
        mc.player.swing(InteractionHand.MAIN_HAND);
        restoreSlot(mc, tntSlot);
        currentState = State.IGNITING;
        lastActionTime = now;
    }
    private void tickIgniting(Minecraft mc, net.minecraft.world.entity.LivingEntity target, long now) {
        if (now - lastActionTime < igniteDelay.getValue()) return;
        int flintSlot = findFlintAndSteelSlot(mc);
        if (flintSlot == -1) {
            if (autoDisable.getValue()) setEnabled(false);
            return;
        }
        BlockPos tntPos = new BlockPos(gapPos[0], gapPos[1], gapPos[2]);
        Vec3 hitVec = Vec3.atCenterOf(tntPos);
        rotateTo(mc, hitVec);
        swapTo(mc, flintSlot);
        BlockHitResult hitResult = new BlockHitResult(hitVec, Direction.UP, tntPos, false);
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hitResult);
        mc.player.swing(InteractionHand.MAIN_HAND);
        restoreSlot(mc, flintSlot);
        currentState = State.WAITING;
        lastActionTime = now;
    }
    private void tickWaiting(Minecraft mc, long now) {
        if (now - lastActionTime > 5000) {
            if (autoDisable.getValue()) {
                setEnabled(false);
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
<<<<<<< HEAD
            if (MobUtility.isSelf(le)) continue;
            if (MobUtility.isDead(le)) continue;
            if (typeFilter.equals("Players") && !MobUtility.isPlayer(le)) continue;
            if (typeFilter.equals("Monsters") && !(le instanceof net.minecraft.world.entity.monster.Monster)) continue;
            double dist = MobUtility.distanceToPlayer(le);
            if (dist > maxDist) continue;
            double metric = mode.equals("LowestHP") ? MobUtility.getHealth(le) : dist;
=======
            if (le == mc.player) continue;
            if (le.isDeadOrDying()) continue;
            if (typeFilter.equals("Players") && !(le instanceof Player)) continue;
            if (typeFilter.equals("Monsters") && !(le instanceof net.minecraft.world.entity.monster.Monster)) continue;
            double dist = mc.player.distanceTo(le);
            if (dist > maxDist) continue;
            double metric = mode.equals("LowestHP") ? le.getHealth() : dist;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            if (metric < bestMetric) {
                bestMetric = metric;
                closest = le;
            }
        }
        return closest;
    }
    private int findObsidianSlot(Minecraft mc) {
        for (int i = 0; i < 9; i++) {
<<<<<<< HEAD
            var stack = InventoryUtility.getItem(mc.player, i);
=======
            ItemStack stack = mc.player.getInventory().getItem(i);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem bi) {
                if (bi.getBlock() == Blocks.OBSIDIAN) return i;
            }
        }
        for (int i = 0; i < 9; i++) {
<<<<<<< HEAD
            var stack = InventoryUtility.getItem(mc.player, i);
=======
            ItemStack stack = mc.player.getInventory().getItem(i);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem bi) {
                if (bi.getBlock().defaultBlockState().isCollisionShapeFullBlock(mc.level, BlockPos.ZERO)) {
                    return i;
                }
            }
        }
        return -1;
    }
    private int findTntSlot(Minecraft mc) {
        for (int i = 0; i < 9; i++) {
<<<<<<< HEAD
            var stack = InventoryUtility.getItem(mc.player, i);
=======
            ItemStack stack = mc.player.getInventory().getItem(i);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem bi) {
                if (bi.getBlock() == Blocks.TNT) return i;
            }
        }
        return -1;
    }
    private int findFlintAndSteelSlot(Minecraft mc) {
        for (int i = 0; i < 9; i++) {
<<<<<<< HEAD
            var stack = InventoryUtility.getItem(mc.player, i);
            if (!stack.isEmpty() && InventoryUtility.isItem(stack, "flint_and_steel")) return i;
=======
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(Items.FLINT_AND_STEEL)) return i;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
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
        for (int i = 0; i < arr.length; i++) arr[i] = data.get(i);
        return arr;
    }
    private void rotateTo(Minecraft mc, Vec3 target) {
        String mode = rotateMode.getValue();
        if (mode.equals("None")) return;
        float[] angles = RotationUtility.anglesTo(mc.player.getEyePosition(), target);
        float yaw = angles[0], pitch = angles[1];
        if (mode.equals("Normal")) {
            mc.player.setYRot(yaw);
            mc.player.setXRot(pitch);
        } else if (mode.equals("Silent")) {
<<<<<<< HEAD
            silentRotation.set(yaw, pitch);
=======
            silentYaw = yaw; silentPitch = pitch; hasSilentRotations = true;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        } else if (mode.equals("Packet") && mc.player.connection != null) {
            mc.player.connection.send(
                new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot(
                    yaw, pitch, mc.player.onGround(), mc.player.horizontalCollision));
        }
    }
    private int savedSlot = -1;
    private void swapTo(Minecraft mc, int slot) {
        String swap = swapMode.getValue();
<<<<<<< HEAD
        savedSlot = InventoryUtility.getSelectedSlot(mc.player);
        if (swap.equals("Normal")) {
            InventoryUtility.selectSlot(mc.player, slot);
=======
        savedSlot = mc.player.getInventory().getSelectedSlot();
        if (swap.equals("Normal")) {
            mc.player.getInventory().setSelectedSlot(slot);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
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
        BlockPos feet = target.blockPosition();
        double dx = mc.player.getX() - (feet.getX() + 0.5);
        double dz = mc.player.getZ() - (feet.getZ() + 0.5);
        int headY = feet.getY() + 1;
        BlockPos gapBlock;
        if (Math.abs(dx) >= Math.abs(dz)) {
            gapBlock = dx > 0 ? new BlockPos(feet.getX() + 1, headY, feet.getZ())
                              : new BlockPos(feet.getX() - 1, headY, feet.getZ());
        } else {
            gapBlock = dz > 0 ? new BlockPos(feet.getX(), headY, feet.getZ() + 1)
                              : new BlockPos(feet.getX(), headY, feet.getZ() - 1);
        }
        if (gapPos == null) {
            gapPos = new int[]{gapBlock.getX(), gapBlock.getY(), gapBlock.getZ()};
        }
        List<BlockPos> candidates = new ArrayList<>();
        candidates.add(feet.north()); candidates.add(feet.south());
        candidates.add(feet.east());  candidates.add(feet.west());
        BlockPos[] headSides = {feet.above().north(), feet.above().south(),
                                feet.above().east(), feet.above().west()};
        for (BlockPos h : headSides) {
            if (!h.equals(gapBlock)) candidates.add(h);
        }
        if (roof.getValue()) candidates.add(feet.above(2));
        Set<BlockPos> solids = new HashSet<>();
        for (int i = 0; i + 2 < solidData.length; i += 3) {
            solids.add(new BlockPos((int) solidData[i], (int) solidData[i + 1], (int) solidData[i + 2]));
        }
        Vec3 eyePos = mc.player.getEyePosition();
        double r = range.getValue();
        for (BlockPos cand : candidates) {
            if (solids.contains(cand)) continue;
            if (eyePos.distanceToSqr(Vec3.atCenterOf(cand)) > r * r) continue;
            for (Direction d : Direction.values()) {
                BlockPos side = cand.relative(d);
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
        BlockPos gap = new BlockPos(gapPos[0], gapPos[1], gapPos[2]);
        Vec3 eyePos = mc.player.getEyePosition();
        double r = range.getValue();
        if (eyePos.distanceToSqr(Vec3.atCenterOf(gap)) > r * r) return new double[]{0.0};
        for (Direction d : Direction.values()) {
            BlockPos side = gap.relative(d);
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
<<<<<<< HEAD
    public static boolean maybeEnabled() {
        return maybeEnabled(TntAura.class);
    }
    public static TntAura itz() {
        return ModuleManager.get(TntAura.class);
    }

}
=======
}
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
