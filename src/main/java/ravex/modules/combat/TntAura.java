package ravex.modules.combat;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.block.BlockUtility;
import ravex.utility.misc.MobUtility;
import ravex.utility.misc.PhysicUtility;
import ravex.utility.nativelib.NativeLibraryUtility;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.rotation.RotationUtility;
import ravex.utility.player.rotation.SilentRotationUtility;
import ravex.utility.player.SwingUtility;
import net.minecraft.world.item.BlockItem;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import ravex.utility.network.NetworkUtility;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;




@Module(name = "TntAura", category = "Combat")
public class TntAura {
    @Parameter(name = "Range", min = 1.0, max = 6.0, step = 0.1)
    public double range = 4.5;
    @Parameter(name = "PlaceDelay", min = 0.0, max = 500.0, step = 10.0)
    public double placeDelay = 50.0;
    @Parameter(name = "TNTDelay", min = 0.0, max = 1000.0, step = 10.0)
    public double tntDelay = 200.0;
    @Parameter(name = "IgniteDelay", min = 0.0, max = 500.0, step = 10.0)
    public double igniteDelay = 100.0;
    @Parameter(name = "SwapMode", modes = {"Silent", "Normal", "None"})
    public String swapMode = "Silent";
    @Parameter(name = "RotateMode", modes = {"Silent", "Normal", "Packet", "None"})
    public String rotateMode = "Silent";
    @Parameter(name = "Roof")
    public boolean roof = true;
    @Parameter(name = "AutoDisable")
    public boolean autoDisable = true;
    @Parameter(name = "Target", modes = {"Closest", "LowestHP"})
    public String targetMode = "Closest";
    @Parameter(name = "TargetType", modes = {"Players", "Monsters", "All"})
    public String targetType = "Players";
    @Parameter(name = "MaxRate", min = 1.0, max = 5.0, step = 1.0)
    public double maxRate = 2.0;
    @Parameter(name = "Render")
    public boolean render = true;
    @Parameter(name = "Color", color = true)
    public int color = 0xFFFF4400;
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
    public void onEnable() {
        currentState = State.TRAPPING;
        lastActionTime = 0;
        gapPos = null;
        currentTarget = null;
        failedTntPlacements = 0;
        synchronized (renderBlocks) { renderBlocks.clear(); }
    }
    public void onDisable() {
        silentRotation.hasRotation = false;
        currentTarget = null;
        gapPos = null;
        failedTntPlacements = 0;
        synchronized (renderBlocks) { renderBlocks.clear(); }
    }
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getLevel() == null || mc.getGameMode() == null) return;
        silentRotation.hasRotation = false;
        net.minecraft.world.entity.LivingEntity target = findTarget(mc);
        if (target == null) {
            if (autoDisable) Modules.setEnabled(TntAura.class, false);
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
    private void tickTrapping(MinecraftWrapper mc, net.minecraft.world.entity.LivingEntity target, long now) {
        if (now - lastActionTime < placeDelay) return;
        double[] solidData = collectSolidBlocks(mc);
        double[] gapData = gapPos != null ? new double[]{gapPos[0], gapPos[1], gapPos[2]} : null;
        double[] result = null;
        double placeRange = range + 1.5;
        if (NATIVE.isLoaded()) {
            result = nativeCalculateCage(
                mc.getPlayer().getX(), mc.getPlayer().getY(), mc.getPlayer().getZ(),
                target.getX(), target.getY(), target.getZ(),
                solidData, placeRange, roof,
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
        net.minecraft.world.phys.Vec3 hitVec = PhysicUtility.centerOf(neighborPos).add(
            PhysicUtility.vec3(face.getStepX(), face.getStepY(), face.getStepZ()).scale(0.5));
        rotateTo(mc, hitVec);
        swapTo(mc, blockSlot);
        net.minecraft.world.phys.BlockHitResult hitResult = new net.minecraft.world.phys.BlockHitResult(hitVec, face, neighborPos, false);
        mc.getGameMode().useItemOn(mc.getPlayer(), net.minecraft.world.InteractionHand.MAIN_HAND, hitResult);
        SwingUtility.swing(mc.getPlayer(), net.minecraft.world.InteractionHand.MAIN_HAND);
        restoreSlot(mc, blockSlot);
        synchronized (renderBlocks) { renderBlocks.add(targetBlock); }
        lastActionTime = now;
    }
    private void tickPlacingTnt(MinecraftWrapper mc, net.minecraft.world.entity.LivingEntity target, long now) {
        if (now - lastActionTime < tntDelay) return;
        if (gapPos == null) {
            net.minecraft.core.BlockPos feet = target.blockPosition();
            double dx = mc.getPlayer().getX() - (feet.getX() + 0.5);
            double dz = mc.getPlayer().getZ() - (feet.getZ() + 0.5);
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
            if (autoDisable) Modules.setEnabled(TntAura.class, false);
            return;
        }
        double[] solidData = collectSolidBlocks(mc);
        double[] result = null;
        double placeRange = range + 1.5;
        if (NATIVE.isLoaded()) {
            result = nativeCalculateTntSlot(
                mc.getPlayer().getX(), mc.getPlayer().getY(), mc.getPlayer().getZ(),
                gapPos[0], gapPos[1], gapPos[2],
                solidData, placeRange
            );
        } else {
            result = javaFallbackTntPlacement(mc);
        }
        if (result == null || result[0] < 0.5) {
            failedTntPlacements++;
            if (failedTntPlacements >= 5) {
                if (autoDisable) {
                    Modules.setEnabled(TntAura.class, false);
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
        net.minecraft.world.phys.Vec3 hitVec = PhysicUtility.centerOf(neighborPos).add(
            PhysicUtility.vec3(face.getStepX(), face.getStepY(), face.getStepZ()).scale(0.5));
        rotateTo(mc, hitVec);
        swapTo(mc, tntSlot);
        net.minecraft.world.phys.BlockHitResult hitResult = new net.minecraft.world.phys.BlockHitResult(hitVec, face, neighborPos, false);
        mc.getGameMode().useItemOn(mc.getPlayer(), net.minecraft.world.InteractionHand.MAIN_HAND, hitResult);
        SwingUtility.swing(mc.getPlayer(), net.minecraft.world.InteractionHand.MAIN_HAND);
        restoreSlot(mc, tntSlot);
        currentState = State.IGNITING;
        lastActionTime = now;
    }
    private void tickIgniting(MinecraftWrapper mc, net.minecraft.world.entity.LivingEntity target, long now) {
        if (now - lastActionTime < igniteDelay) return;
        int flintSlot = findFlintAndSteelSlot(mc);
        if (flintSlot == -1) {
            if (autoDisable) Modules.setEnabled(TntAura.class, false);
            return;
        }
        net.minecraft.core.BlockPos tntPos = new net.minecraft.core.BlockPos(gapPos[0], gapPos[1], gapPos[2]);
        net.minecraft.world.phys.Vec3 hitVec = PhysicUtility.centerOf(tntPos);
        rotateTo(mc, hitVec);
        swapTo(mc, flintSlot);
        net.minecraft.world.phys.BlockHitResult hitResult = new net.minecraft.world.phys.BlockHitResult(hitVec, net.minecraft.core.Direction.UP, tntPos, false);
        mc.getGameMode().useItemOn(mc.getPlayer(), net.minecraft.world.InteractionHand.MAIN_HAND, hitResult);
        SwingUtility.swing(mc.getPlayer(), net.minecraft.world.InteractionHand.MAIN_HAND);
        restoreSlot(mc, flintSlot);
        currentState = State.WAITING;
        lastActionTime = now;
    }
    private void tickWaiting(MinecraftWrapper mc, long now) {
        if (now - lastActionTime > 5000) {
            if (autoDisable) {
                Modules.setEnabled(TntAura.class, false);
            } else {
                currentState = State.TRAPPING;
                gapPos = null;
                synchronized (renderBlocks) { renderBlocks.clear(); }
            }
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
    private int findObsidianSlot(MinecraftWrapper mc) {
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(mc.getPlayer(), i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem bi) {
                if (bi.getBlock() == net.minecraft.world.level.block.Blocks.OBSIDIAN) return i;
            }
        }
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(mc.getPlayer(), i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem bi) {
                if (bi.getBlock().defaultBlockState().isCollisionShapeFullBlock(mc.getLevel(), net.minecraft.core.BlockPos.ZERO)) {
                    return i;
                }
            }
        }
        return -1;
    }
    private int findTntSlot(MinecraftWrapper mc) {
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(mc.getPlayer(), i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem bi) {
                if (bi.getBlock() == net.minecraft.world.level.block.Blocks.TNT) return i;
            }
        }
        return -1;
    }
    private int findFlintAndSteelSlot(MinecraftWrapper mc) {
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(mc.getPlayer(), i);
            if (!stack.isEmpty() && InventoryUtility.isItem(stack, "flint_and_steel")) return i;
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
        for (int i = 0; i < arr.length; i++) arr[i] = data.get(i);
        return arr;
    }
    private void rotateTo(MinecraftWrapper mc, net.minecraft.world.phys.Vec3 target) {
        String mode = rotateMode;
        if (mode.equals("None")) return;
        float[] angles = RotationUtility.anglesTo(mc.getPlayer().getEyePosition(), target);
        float yaw = angles[0], pitch = angles[1];
        if (mode.equals("Normal")) {
            mc.getPlayer().setYRot(yaw);
            mc.getPlayer().setXRot(pitch);
        } else if (mode.equals("Silent")) {
            silentRotation.set(yaw, pitch);
        } else if (mode.equals("Packet")) {
            NetworkUtility.sendRot(yaw, pitch, mc.getPlayer().onGround(), mc.getPlayer().horizontalCollision);
        }
    }
    private int savedSlot = -1;
    private void swapTo(MinecraftWrapper mc, int slot) {
        String swap = swapMode;
        savedSlot = InventoryUtility.getSelectedSlot(mc.getPlayer());
        if (swap.equals("Normal")) {
            InventoryUtility.selectSlot(mc.getPlayer(), slot);
        } else if (swap.equals("Silent")) {
            if (mc.getPlayer().connection != null) {
                NetworkUtility.sendSetCarriedItem(slot);
            }
        }
    }
    private void restoreSlot(MinecraftWrapper mc, int slot) {
        if (swapMode.equals("Silent") && savedSlot != -1) {
            if (mc.getPlayer().connection != null) {
                NetworkUtility.sendSetCarriedItem(savedSlot);
            }
        }
    }
    private double[] javaFallbackCage(MinecraftWrapper mc, net.minecraft.world.entity.LivingEntity target, double[] solidData) {
        net.minecraft.core.BlockPos feet = target.blockPosition();
        double dx = mc.getPlayer().getX() - (feet.getX() + 0.5);
        double dz = mc.getPlayer().getZ() - (feet.getZ() + 0.5);
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
        if (roof) candidates.add(feet.above(2));
        Set<net.minecraft.core.BlockPos> solids = new HashSet<>();
        for (int i = 0; i + 2 < solidData.length; i += 3) {
            solids.add(new net.minecraft.core.BlockPos((int) solidData[i], (int) solidData[i + 1], (int) solidData[i + 2]));
        }
        net.minecraft.world.phys.Vec3 eyePos = mc.getPlayer().getEyePosition();
        double r = range;
        for (net.minecraft.core.BlockPos cand : candidates) {
            if (solids.contains(cand)) continue;
            if (eyePos.distanceToSqr(PhysicUtility.centerOf(cand)) > r * r) continue;
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
    private double[] javaFallbackTntPlacement(MinecraftWrapper mc) {
        if (gapPos == null) return new double[]{0.0};
        net.minecraft.core.BlockPos gap = new net.minecraft.core.BlockPos(gapPos[0], gapPos[1], gapPos[2]);
        net.minecraft.world.phys.Vec3 eyePos = mc.getPlayer().getEyePosition();
        double r = range;
        if (eyePos.distanceToSqr(PhysicUtility.centerOf(gap)) > r * r) return new double[]{0.0};
        for (net.minecraft.core.Direction d : net.minecraft.core.Direction.values()) {
            net.minecraft.core.BlockPos side = gap.relative(d);
            net.minecraft.world.level.block.state.BlockState state = mc.getLevel().getBlockState(side);
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




}