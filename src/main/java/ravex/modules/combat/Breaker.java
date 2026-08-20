package ravex.modules.combat;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.EntityUtility;
import ravex.utility.misc.PhysicUtility;
import ravex.utility.nativelib.NativeLibraryUtility;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.rotation.RotationUtility;
import ravex.utility.player.rotation.SilentRotationUtility;
import ravex.utility.player.SwingUtility;
import java.util.ArrayList;
import java.util.List;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;
import ravex.modules.player.PacketMine;
import ravex.utility.misc.CombatUtility;





@Module(name = "Breaker", category = "Combat")
public class Breaker {
    @Parameter(name = "BreakRange", min = 1.0, max = 6.0, step = 0.1)
    public double range = 4.5;
    @Parameter(name = "CrystalRange", min = 1.0, max = 6.0, step = 0.1)
    public double crystalRange = 5.0;
    @Parameter(name = "MinDamage", min = 1.0, max = 20.0, step = 0.5)
    public double minDamage = 4.0;
    @Parameter(name = "MaxSelfDmg", min = 1.0, max = 20.0, step = 0.5)
    public double maxSelfDmg = 8.0;
    @Parameter(name = "SelfDmgWeight", min = 0, max = 5.0, step = 0.1)
    public double selfDamageWeight = 1.2;
    @Parameter(name = "AntiSuicide")
    public boolean antiSuicide = true;
    @Parameter(name = "AntiSuicideMinHP", min = 1.0, max = 20.0, step = 0.5)
    public double antiSuicideMinHp = 6.0;
    @Parameter(name = "Rotate", modes = {"Silent", "Normal", "None"})
    public String rotate = "Silent";
    @Parameter(name = "SyncPacketMine")
    public boolean syncPacketMine = false;
    @Parameter(name = "Color", color = true)
    public int color = 0x3F00FFFF;
    public static final SilentRotationUtility silentRotation = new SilentRotationUtility();
    public static net.minecraft.core.BlockPos currentMiningBlock = null;
    private static final NativeLibraryUtility NATIVE = NativeLibraryUtility.of("ravex_breaker");
    public void onDisable() {
        var mc = MinecraftWrapper.getWrapper();
        if (currentMiningBlock != null && mc.getGameMode() != null && !syncPacketMine) {
            mc.getGameMode().stopDestroyBlock();
        }
        currentMiningBlock = null;
        silentRotation.hasRotation = false;
    }
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getLevel() == null || mc.getGameMode() == null) {
            currentMiningBlock = null;
            silentRotation.hasRotation = false;
            return;
        }
        silentRotation.hasRotation = false;
        if (syncPacketMine) {
            boolean packetMineEnabled = Modules.enabled(PacketMine.class);
            if (!packetMineEnabled) {
                syncPacketMine = false;
                mc.getPlayer().displayClientMessage(
                        net.minecraft.network.chat.Component
                                .literal("§7[§cBreaker§7] §cPacketMine was disabled, Sync PacketMine turned off!"),
                        false);
            }
        }
        if (!NATIVE.isLoaded())
            return;
        net.minecraft.world.entity.player.Player target = findTarget(mc);
        if (target == null) {
            if (currentMiningBlock != null) {
                if (!syncPacketMine) {
                    mc.getGameMode().stopDestroyBlock();
                }
                currentMiningBlock = null;
            }
            return;
        }
        List<net.minecraft.core.BlockPos> solid = collectSolidBlocks(mc, target);
        List<net.minecraft.core.BlockPos> candidates = new ArrayList<>();
        net.minecraft.core.BlockPos tPos = target.blockPosition();
        int tx = tPos.getX();
        int ty = tPos.getY();
        int tz = tPos.getZ();
        for (net.minecraft.core.BlockPos pos : solid) {
            net.minecraft.world.level.block.state.BlockState state = mc.getLevel().getBlockState(pos);
            if (state.getDestroySpeed(mc.getLevel(), pos) > 0.0f) {
                int px = pos.getX();
                int py = pos.getY();
                int pz = pos.getZ();
                boolean isTargetCover = false;
                if (px == tx && py == ty - 1 && pz == tz) {
                    isTargetCover = true;
                } else if (py == ty) {
                    int dx = Math.abs(px - tx);
                    int dz = Math.abs(pz - tz);
                    if (dx <= 1 && dz <= 1 && (dx + dz > 0)) {
                        isTargetCover = true;
                    }
                } else if (py == ty + 1) {
                    int dx = Math.abs(px - tx);
                    int dz = Math.abs(pz - tz);
                    if (dx <= 1 && dz <= 1 && (dx + dz > 0)) {
                        isTargetCover = true;
                    }
                } else if (px == tx && py == ty + 2 && pz == tz) {
                    isTargetCover = true;
                }
                if (isTargetCover) {
                    candidates.add(pos);
                }
            }
        }
        if (candidates.isEmpty()) {
            if (currentMiningBlock != null) {
                if (!syncPacketMine) {
                    mc.getGameMode().stopDestroyBlock();
                }
                currentMiningBlock = null;
            }
            return;
        }
        net.minecraft.core.BlockPos targetPos = null;
        if (currentMiningBlock != null) {
            double dist = PhysicUtility.centerOf(currentMiningBlock).distanceTo(mc.getPlayer().getEyePosition());
            if (dist <= range && candidates.contains(currentMiningBlock)) {
                targetPos = currentMiningBlock;
            }
        }
        if (targetPos == null) {
            double[] solidData = flatten(solid);
            double[] candData = flatten(candidates);
            double[] result = nativeCalculateBreaker(
                    mc.getPlayer().getX(), mc.getPlayer().getY(), mc.getPlayer().getZ(),
                    mc.getPlayer().getHealth(), mc.getPlayer().getAbsorptionAmount(), CombatUtility.getEntityStats(mc.getPlayer()),
                    target.getX(), target.getY(), target.getZ(),
                    target.getHealth(), target.getAbsorptionAmount(), CombatUtility.getEntityStats(target),
                    solidData,
                    candData,
                    range,
                    crystalRange,
                    minDamage,
                    maxSelfDmg,
                    selfDamageWeight,
                    antiSuicide,
                    antiSuicideMinHp);
            if (result == null || result[0] < 0.5) {
                if (currentMiningBlock != null) {
                    if (!syncPacketMine) {
                        mc.getGameMode().stopDestroyBlock();
                    }
                    currentMiningBlock = null;
                }
                return;
            }
            targetPos = new net.minecraft.core.BlockPos((int) result[1], (int) result[2], (int) result[3]);
        }
        if (!syncPacketMine) {
            String rotMode = rotate;
            if (rotMode.equals("Normal")) {
                rotateTo(mc, PhysicUtility.centerOf(targetPos));
            } else if (rotMode.equals("Silent")) {
                silentRotation.setAnglesTo(mc, PhysicUtility.centerOf(targetPos));
            }
        }
        if (syncPacketMine) {
            if (!Modules.get(PacketMine.class).isTargetBlock(targetPos)) {
                ravex.modules.player.PacketMine.miningBlocks.removeIf(m -> !m.done);
                String name = mc.getLevel().getBlockState(targetPos).getBlock().getName().getString();
                long breakMs = Modules.get(PacketMine.class).calcBreakTime(mc, targetPos);
                ravex.modules.player.PacketMine.miningBlocks.add(
                        new ravex.modules.player.PacketMine.MiningBlock(targetPos, breakMs, name));
            }
            currentMiningBlock = targetPos;
        } else {
            if (currentMiningBlock == null || !currentMiningBlock.equals(targetPos)) {
                if (currentMiningBlock != null) {
                    mc.getGameMode().stopDestroyBlock();
                }
                currentMiningBlock = targetPos;
                mc.getGameMode().startDestroyBlock(targetPos, net.minecraft.core.Direction.UP);
            } else {
                mc.getGameMode().continueDestroyBlock(targetPos, net.minecraft.core.Direction.UP);
            }
            SwingUtility.swing(mc.getPlayer(), net.minecraft.world.InteractionHand.MAIN_HAND);
        }
    }

    private net.minecraft.world.entity.player.Player findTarget(MinecraftWrapper mc) {
        net.minecraft.world.entity.player.Player closest = null;
        double bestDist = Double.MAX_VALUE;
        double maxDist = range + 3.0;
        for (net.minecraft.world.entity.player.Player p : mc.getLevel().players()) {
            if (EntityUtility.isSelf(p) || EntityUtility.isDead(p))
                continue;
            double dist = EntityUtility.distanceToPlayer(p);
            if (dist <= maxDist && dist < bestDist) {
                bestDist = dist;
                closest = p;
            }
        }
        return closest;
    }

    private List<net.minecraft.core.BlockPos> collectSolidBlocks(MinecraftWrapper mc, net.minecraft.world.entity.player.Player target) {
        List<net.minecraft.core.BlockPos> found = new ArrayList<>();
        net.minecraft.core.BlockPos tPos = target.blockPosition();
        int r = 2;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -1; dy <= 2; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    net.minecraft.core.BlockPos pos = tPos.offset(dx, dy, dz);
                    if (mc.getLevel().isLoaded(pos)) {
                        net.minecraft.world.level.block.state.BlockState state = mc.getLevel().getBlockState(pos);
                        if (!state.isAir() && !state.liquid()) {
                            found.add(pos);
                        }
                    }
                }
            }
        }
        return found;
    }

    private double[] flatten(List<net.minecraft.core.BlockPos> list) {
        double[] arr = new double[list.size() * 3];
        for (int i = 0; i < list.size(); i++) {
            net.minecraft.core.BlockPos p = list.get(i);
            arr[i * 3] = p.getX();
            arr[i * 3 + 1] = p.getY();
            arr[i * 3 + 2] = p.getZ();
        }
        return arr;
    }

    private void rotateTo(MinecraftWrapper mc, net.minecraft.world.phys.Vec3 target) {
        float[] angles = RotationUtility.anglesTo(mc.getPlayer().getEyePosition(), target);
        mc.getPlayer().setYRot(angles[0]);
        mc.getPlayer().setXRot(angles[1]);
    }

    private static native double[] nativeCalculateBreaker(
            double playerX, double playerY, double playerZ,
            double playerHp, double playerAbs, double[] playerStats,
            double targetX, double targetY, double targetZ,
            double targetHp, double targetAbs, double[] targetStats,
            double[] solidBlocksData,
            double[] breakableCandidatesData,
            double breakRange,
            double crystalPlaceRange,
            double minTargetDmg,
            double maxSelfDmg,
            double selfDmgWeight,
            boolean antiSuicide,
            double antiSuicideMinHp);





}