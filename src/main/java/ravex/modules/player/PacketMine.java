package ravex.modules.player;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.block.BlockUtility;
import ravex.utility.misc.PhysicUtility;

import ravex.utility.network.NetworkUtility;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.rotation.RotationUtility;
import ravex.utility.player.rotation.SilentRotationUtility;
import java.util.ArrayList;
import java.util.List;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;
@Module(name = "PacketMine", category = "Player")
public class PacketMine {
    @Parameter(name = "Mode", modes = {"Vanilla", "Grim", "NCP"})
    public String mode = "Vanilla";
    @Parameter(name = "Range", min = 2.0, max = 10.0, step = 0.5)
    public double range = 6.0;
    @Parameter(name = "Rotate", modes = {"Strict", "Vanilla"})
    public String rotate = "Vanilla";
    @Parameter(name = "SwapMode", modes = {"Silent", "Normal", "Alternative", "None"})
    public String swapMode = "Silent";
    // server accepts the stop from stage 7
    @Parameter(name = "MinStage", min = 7, max = 10, step = 1)
    public double minStage = 9;
    @Parameter(name = "Render")
    public boolean render = true;
    @Parameter(name = "Color", color = true, visible = "render")
    public int color = 0xFF40A9F8;
    @Parameter(name = "DoubleMine")
    public boolean doubleMine = false;
    @Parameter(name = "MaxBlocks", min = 2, max = 10, step = 1)
    public double maxBlocks = 2;
    @Parameter(name = "Raycast")
    public boolean raycast = false;
    @Parameter(name = "GrimRange", min = 0.0, max = 6.0, step = 0.1, visible = "mode=Grim")
    public double grimRange = 4.5;
    @Parameter(name = "GrimMode", modes = {"Strict", "Normal", "Dev"}, visible = "mode=Grim")
    public String grimMode = "Strict";
    public static final SilentRotationUtility silentRotation = new SilentRotationUtility();
    public static class MiningBlock {
        public net.minecraft.core.BlockPos pos;
        public long startTime;
        public long breakAt;
        public boolean done;
        public boolean sentStop;
        public long stopSentAt;
        public boolean started;
        public boolean endSwapped;
        public long visibleUntil;
        public String blockName;
        public int attempts;
        public int serverStage = -1;
        public MiningBlock(net.minecraft.core.BlockPos pos, long breakAt, String blockName) {
            this.pos = pos;
            this.startTime = System.currentTimeMillis();
            this.breakAt = breakAt;
            this.done = false;
            this.sentStop = false;
            this.started = false;
            this.visibleUntil = Long.MAX_VALUE;
            this.blockName = blockName;
        }
    }
    public static final List<MiningBlock> miningBlocks = new ArrayList<>();
    private int restoreSlot = -1;
    private int toolSlot = -1;
    private int lastSentSlot = -2;
    private boolean altSwapped = false;
    private boolean needRestore = false;
    private boolean attackWasDown = false;
    public void onEnable() {
        miningBlocks.clear();
        restoreSlot = -1;
        toolSlot = -1;
        lastSentSlot = -2;
        altSwapped = false;
        needRestore = false;
        attackWasDown = false;
    }

    public void onDisable() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() != null && mc.getGameMode() != null) {
            mc.getGameMode().stopDestroyBlock();
        }
        for (var block : miningBlocks) {
            if (!block.sentStop) {
                if (!mode.equals("Grim")) {
                    sendStop(mc, block.pos);
                }
            }
        }
        miningBlocks.clear();
        restoreSlotNow();
        attackWasDown = false;
    }
    public long calcBreakTime(MinecraftWrapper mc, net.minecraft.core.BlockPos pos) {
        return calcBreakTimeForSlot(mc, pos, -1);
    }

    // client speed attribute lags slot changes
    public long calcBreakTimeForSlot(MinecraftWrapper mc, net.minecraft.core.BlockPos pos, int slot) {
        var player = mc.getPlayer();
        var state = BlockUtility.getState(mc.getLevel(), pos.getX(), pos.getY(), pos.getZ());
        var tool = slot >= 0 ? InventoryUtility.getItem(player, slot) : InventoryUtility.getMainHand(player);
        float speed = destroySpeed(player, tool, state);
        float hardness = state.getDestroySpeed(mc.getLevel(), pos);
        if (speed <= 0 || hardness < 0) return 2000;
        boolean canHarvest = !state.requiresCorrectToolForDrops() || tool.isCorrectToolForDrops(state);
        float damage = speed / hardness / (canHarvest ? 30f : 100f);
        if (damage <= 0) return 2000;
        long ms = (long) (Math.ceil(1.0 / damage) * 50);
        ms = Math.max(100, Math.min(mode.equals("Grim") ? 20000 : mode.equals("NCP") ? 10000 : 5000, ms));
        return Math.max(50, ms);
    }

    // mirror of vanilla getDestroySpeed
    private static float destroySpeed(net.minecraft.world.entity.player.Player player,
                                      net.minecraft.world.item.ItemStack tool,
                                      net.minecraft.world.level.block.state.BlockState state) {
        float f = tool.getDestroySpeed(state);
        if (f > 1.0f) {
            int eff = InventoryUtility.getEnchantmentLevel(tool, "minecraft:efficiency");
            if (eff > 0) f += eff * eff + 1; // efficiency: level squared plus one
        }
        if (net.minecraft.world.effect.MobEffectUtil.hasDigSpeed(player)) {
            f *= 1.0f + 0.2f * (net.minecraft.world.effect.MobEffectUtil.getDigSpeedAmplification(player) + 1);
        }
        var fatigue = player.getEffect(net.minecraft.world.effect.MobEffects.MINING_FATIGUE);
        if (fatigue != null) {
            int amp = fatigue.getAmplifier();
            f *= amp == 0 ? 0.3f : amp == 1 ? 0.09f : amp == 2 ? 0.0027f : 0.00081f;
        }
        f *= (float) player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.BLOCK_BREAK_SPEED);
        if (player.isEyeInFluid(net.minecraft.tags.FluidTags.WATER)) {
            f *= (float) player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.SUBMERGED_MINING_SPEED);
        }
        if (!player.onGround()) f /= 5.0f;
        return f;
    }
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getLevel() == null || mc.getGameMode() == null) return;
        boolean leftClick = mc.getOptions().keyAttack.isDown();
        boolean clicked = leftClick && !attackWasDown;
        attackWasDown = leftClick;
        if (clicked && mc.getHitResult() != null && mc.getHitResult().getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            net.minecraft.core.BlockPos target = ((net.minecraft.world.phys.BlockHitResult) mc.getHitResult()).getBlockPos();
            if (isBreakable(mc, target) && !isTargetBlock(target)) {
                int max = doubleMine ? (int) maxBlocks : 1;
                // stopped blocks only wait for the ack
                long activeCount = miningBlocks.stream().filter(m -> !m.done && !m.sentStop).count();
                if (activeCount >= max) return;
                String name = BlockUtility.getState(mc.getLevel(), target.getX(), target.getY(), target.getZ()).getBlock().getName().getString();
                long breakMs = calcBreakTime(mc, target);
                MiningBlock mb = new MiningBlock(target, breakMs, name);
                miningBlocks.add(mb);
                // force a held-slot sync on the next start
                lastSentSlot = -2;
                if (mode.equals("Grim")) {
                    mc.getOptions().keyAttack.setDown(false);
                }
            }
        }
        silentRotation.hasRotation = false;
        long now = System.currentTimeMillis();
        miningBlocks.removeIf(m -> m.done && now > m.visibleUntil);
        var server = mc.getSingleplayerServer();
        var serverLevel = (server != null) ? server.getLevel(mc.getLevel().dimension()) : null;
        net.minecraft.core.BlockPos firstPos = null;
        for (MiningBlock mb : miningBlocks) {
            if (!mb.done) { firstPos = mb.pos; break; }
        }
        if (firstPos != null) {
            // tool is always picked and restored
            toolSlot = findBestToolSlot(mc, firstPos);
            // normal swaps now, silent at the end
            if (!swapMode.equals("Silent")) applySwap(mc);
            rotateTo(mc, firstPos);
            // remeasure with the real tool after the swap
            for (MiningBlock mb : miningBlocks) {
                if (!mb.done && !mb.started) mb.breakAt = calcBreakTimeForSlot(mc, mb.pos, toolSlot);
            }
        }
        // the server rates the dig by the item held at stop,
        // the tool only has to arrive just before the finish
        if ((swapMode.equals("Silent") || swapMode.equals("Alternative")) && toolSlot >= 0) {
            int pingLead = 150;
            try {
                var conn = mc.getConnection();
                var info = conn != null && mc.getPlayer() != null
                    ? conn.getPlayerInfo(mc.getPlayer().getUUID()) : null;
                if (info != null) pingLead = Math.min(500, Math.max(150, info.getLatency() * 2 + 50));
            } catch (Throwable ignored) {}
            for (MiningBlock mb : miningBlocks) {
                if (mb.done || mb.endSwapped) continue;
                if (now - mb.startTime >= mb.breakAt - pingLead) {
                    restoreSlot = InventoryUtility.getSelectedSlot(mc.getPlayer());
                    needRestore = true;
                    NetworkUtility.sendSetCarriedItem(toolSlot);
                    lastSentSlot = toolSlot;
                    mb.endSwapped = true;
                }
            }
        }
        // grim with a visible tool uses the vanilla controller
        boolean controllerPath = mode.equals("Grim") && !doubleMine
            && !swapMode.equals("Silent") && !swapMode.equals("Alternative");
        if (controllerPath) {
            MiningBlock mb = miningBlocks.stream().filter(m -> !m.done).findFirst().orElse(null);
            if (mb != null) {
                if (!mb.started) {
                    mc.getGameMode().stopDestroyBlock();
                    net.minecraft.core.Direction dir = getDirection(mc, mb.pos);
                    mc.getGameMode().startDestroyBlock(mb.pos, dir);
                    mb.breakAt = calcBreakTimeForSlot(mc, mb.pos, toolSlot);
                    mb.started = true;
                    mb.startTime = now;
                }
                net.minecraft.core.Direction dir = getDirection(mc, mb.pos);
                mc.getGameMode().continueDestroyBlock(mb.pos, dir);
                long predTime = now - mb.startTime;
                if (serverLevel != null && mc.getPlayer() != null) {
                    if (predTime >= mb.breakAt) {
                        serverLevel.destroyBlock(mb.pos, true, mc.getPlayer());
                        mb.done = true;
                        mb.visibleUntil = now + 2500;
                    }
                } else if (BlockUtility.isAir(mc.getLevel(), mb.pos) || predTime > 20000) {
                    mb.done = true;
                    mb.visibleUntil = now + 2500;
                }
            }
        } else {
            int ping = pingMs(mc);
            for (MiningBlock mb : miningBlocks) {
                if (mb.done) continue;
                if (mb.sentStop) {
                    // the break lands a round trip after the stop,
                    // so wait for air before retrying
                    if (BlockUtility.isAir(mc.getLevel(), mb.pos)) {
                        mb.done = true;
                        mb.visibleUntil = now + 2500;
                        ravex.RaveX.LOGGER.info("[PacketMine] done {} after {} attempts", mb.pos, mb.attempts + 1);
                    } else if (now - mb.stopSentAt >= Math.min(3000, Math.max(750, ping * 2 + 300))) {
                        if (++mb.attempts > 3) {
                            mb.done = true;
                            mb.visibleUntil = now + 1500;
                            ravex.RaveX.LOGGER.info("[PacketMine] gave up {} after {} attempts", mb.pos, mb.attempts);
                        } else {
                            mb.started = false;
                            mb.sentStop = false;
                            mb.breakAt = calcBreakTimeForSlot(mc, mb.pos, toolSlot);
                            mb.startTime = now;
                        }
                    }
                    continue;
                }
                // stop at minStage/10, the ping cancels out
                long stopAt = Math.max(50, (mb.breakAt * (int) minStage) / 10);
                if (mb.serverStage >= (int) minStage || now - mb.startTime >= stopAt) {
                    if (serverLevel != null && mc.getPlayer() != null) {
                        serverLevel.destroyBlock(mb.pos, true, mc.getPlayer());
                    }
                    // a duplicate start resets the server dig timer
                    sendStop(mc, mb.pos);
                    mb.sentStop = true;
                    mb.stopSentAt = now;
                    ravex.RaveX.LOGGER.info("[PacketMine] STOP {} after {}ms (breakAt {}ms, stage {}) attempt {} slot {}",
                        mb.pos, now - mb.startTime, mb.breakAt, mb.serverStage, mb.attempts + 1,
                        InventoryUtility.getSelectedSlot(mc.getPlayer()));
                }
            }
            for (MiningBlock mb : miningBlocks) {
                // swing every tick like a vanilla dig
                if (!mb.done && mb.started && !mb.sentStop) {
                    NetworkUtility.sendSwing(net.minecraft.world.InteractionHand.MAIN_HAND);
                }
            }
            for (MiningBlock mb : miningBlocks) {
                if (mb.done || mb.started) continue;
                // we suppress interactions, so push the slot ourselves
                int held = (toolSlot >= 0 && !swapMode.equals("None"))
                    ? toolSlot
                    : InventoryUtility.getSelectedSlot(mc.getPlayer());
                if (held != lastSentSlot) {
                    NetworkUtility.sendSetCarriedItem(held);
                    lastSentSlot = held;
                    // align the vanilla tracker or it resends the slot
                    if (held == InventoryUtility.getSelectedSlot(mc.getPlayer()) && mc.getGameMode() != null) {
                        ((ravex.mixin.network.AccessorMultiPlayerGameMode) (Object) mc.getGameMode().getRaw())
                            .setCarriedIndex(held);
                    }
                }
                mb.breakAt = calcBreakTimeForSlot(mc, mb.pos, toolSlot);
                mb.serverStage = -1;
                sendStart(mc, mb.pos);
                mb.started = true;
                ravex.RaveX.LOGGER.info("[PacketMine] START {} breakAt {}ms attempt {} tool {} slot {}",
                    mb.pos, mb.breakAt, mb.attempts + 1, toolSlot, held);
            }
        }
        if (needRestore && miningBlocks.stream().noneMatch(m -> !m.done)) {
            restoreSlotNow();
        }
    }
    public boolean isTargetBlock(net.minecraft.core.BlockPos pos) {
        for (MiningBlock mb : miningBlocks) {
            if (mb.pos.equals(pos) && !mb.done) return true;
        }
        return false;
    }
    // crack stage 0..10 from the server, own id only
    public static void onServerDigProgress(int entityId, net.minecraft.core.BlockPos pos, int stage) {
        var mc = MinecraftWrapper.getWrapper();
        var self = mc.getPlayer();
        if (self == null || self.getId() != entityId) return;
        PacketMine pm = Modules.get(PacketMine.class);
        int min = pm != null ? (int) pm.minStage : 10;
        long now = System.currentTimeMillis();
        for (MiningBlock mb : miningBlocks) {
            if (mb.done || !mb.pos.equals(pos)) continue;
            mb.serverStage = stage < 0 ? -1 : Math.max(mb.serverStage, stage);
            if (!mb.sentStop && mb.started && stage >= min) {
                // finish inside packet handling, no tick wait
                sendStop(mc, mb.pos);
                mb.sentStop = true;
                mb.stopSentAt = now;
                ravex.RaveX.LOGGER.info("[PacketMine] STOP {} on stage {} after {}ms attempt {}",
                    mb.pos, stage, now - mb.startTime, mb.attempts + 1);
            }
        }
    }
    private boolean isBreakable(MinecraftWrapper mc, net.minecraft.core.BlockPos pos) {
        var state = BlockUtility.getState(mc.getLevel(), pos.getX(), pos.getY(), pos.getZ());
        if (state.isAir()) return false;
        if (BlockUtility.isBlock(state, "bedrock")) return false;
        if (BlockUtility.destroySpeed(mc.getLevel(), pos) < 0) return false;
        if (!mc.getLevel().getWorldBorder().isWithinBounds(pos)) return false;
        double dist = PhysicUtility.centerOf(pos).distanceTo(mc.getPlayer().getEyePosition());
        double maxDist = mode.equals("Grim") ? grimRange
            : mode.equals("NCP") ? 4.5 : range;
        if (dist > maxDist) return false;
        if (!checkVisibility(mc, pos)) return false;
        return true;
    }
    private void sendStart(MinecraftWrapper mc, net.minecraft.core.BlockPos pos) {
        digPacket(mc, net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos);
    }
    private static void sendStop(MinecraftWrapper mc, net.minecraft.core.BlockPos pos) {
        digPacket(mc, net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pos);
    }
    // prediction path gives the shared sequence id
    private static void digPacket(MinecraftWrapper mc,
                                  net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action action,
                                  net.minecraft.core.BlockPos pos) {
        var gm = mc.getGameMode();
        var level = mc.getLevel();
        if (gm == null || level == null) return;
        net.minecraft.core.Direction dir = getDirection(mc, pos);
        ((ravex.mixin.network.AccessorMultiPlayerGameMode) (Object) gm.getRaw())
            .ravexStartPrediction(level, seq ->
                new net.minecraft.network.protocol.game.ServerboundPlayerActionPacket(action, pos, dir, seq));
    }
    private int pingMs(MinecraftWrapper mc) {
        try {
            var conn = mc.getConnection();
            var info = conn != null && mc.getPlayer() != null
                ? conn.getPlayerInfo(mc.getPlayer().getUUID()) : null;
            if (info != null) return Math.max(0, info.getLatency());
        } catch (Throwable ignored) {}
        return 0;
    }
    private static net.minecraft.core.Direction getDirection(MinecraftWrapper mc, net.minecraft.core.BlockPos pos) {
        // face must match the vanilla dig packet
        if (mc.getHitResult() instanceof net.minecraft.world.phys.BlockHitResult bhr
                && bhr.getBlockPos().equals(pos)) {
            return bhr.getDirection();
        }
        net.minecraft.world.phys.Vec3 eye = mc.getPlayer().getEyePosition();
        net.minecraft.world.phys.Vec3 blockCenter = PhysicUtility.centerOf(pos);
        net.minecraft.world.phys.Vec3 diff = blockCenter.subtract(eye);
        double ax = Math.abs(diff.x), ay = Math.abs(diff.y), az = Math.abs(diff.z);
        if (ay >= ax && ay >= az) return diff.y > 0 ? net.minecraft.core.Direction.UP : net.minecraft.core.Direction.DOWN;
        if (ax >= az) return diff.x > 0 ? net.minecraft.core.Direction.EAST : net.minecraft.core.Direction.WEST;
        return diff.z > 0 ? net.minecraft.core.Direction.SOUTH : net.minecraft.core.Direction.NORTH;
    }
    private int findBestToolSlot(MinecraftWrapper mc, net.minecraft.core.BlockPos pos) {
        var state = BlockUtility.getState(mc.getLevel(), pos.getX(), pos.getY(), pos.getZ());
        int bestSlot = InventoryUtility.getSelectedSlot(mc.getPlayer());
        float bestSpeed = InventoryUtility.getItem(mc.getPlayer(), bestSlot).getDestroySpeed(state);
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(mc.getPlayer(), i);
            if (stack.isEmpty()) continue;
            float speed = stack.getDestroySpeed(state);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = i;
            }
        }
        return bestSlot != InventoryUtility.getSelectedSlot(mc.getPlayer()) ? bestSlot : -1;
    }
    private void applySwap(MinecraftWrapper mc) {
        String swap = swapMode;
        if (toolSlot < 0 || swap.equals("None")) return;
        if (swap.equals("Alternative")) {
            // server keeps the tool, local slot goes back
            if (altSwapped && InventoryUtility.getSelectedSlot(mc.getPlayer()) == restoreSlot) return;
            altSwapped = true;
            int prev = InventoryUtility.getSelectedSlot(mc.getPlayer());
            InventoryUtility.selectSlot(mc.getPlayer(), toolSlot);
            NetworkUtility.sendSetCarriedItem(toolSlot);
            lastSentSlot = toolSlot;
            InventoryUtility.selectSlot(mc.getPlayer(), prev);
            restoreSlot = prev;
            needRestore = true;
            return;
        }
        if (swap.equals("Silent")) return;
        // one held packet per tool, spam gets flagged
        int prev = InventoryUtility.getSelectedSlot(mc.getPlayer());
        if (swap.equals("Normal")) {
            InventoryUtility.selectSlot(mc.getPlayer(), toolSlot);
        }
        restoreSlot = prev;
        needRestore = true;
    }
    private void restoreSlotNow() {
        if (!needRestore || restoreSlot < 0) return;
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null) return;
        lastSentSlot = -2;
        altSwapped = false;
        String swap = swapMode;
        if (swap.equals("Silent") || swap.equals("Alternative")) {
            NetworkUtility.sendSetCarriedItem(restoreSlot);
            if (mc.getGameMode() != null) {
                ((ravex.mixin.network.AccessorMultiPlayerGameMode) (Object) mc.getGameMode().getRaw())
                    .setCarriedIndex(restoreSlot);
            }
        }
        if (swap.equals("Normal") || swap.equals("Alternative")) {
            InventoryUtility.selectSlot(mc.getPlayer(), restoreSlot);
        }
        needRestore = false;
    }
    private void rotateTo(MinecraftWrapper mc, net.minecraft.core.BlockPos pos) {
        // vanilla also turns the local model
        float[] angles = RotationUtility.anglesTo(mc.getPlayer().getEyePosition(), PhysicUtility.centerOf(pos));
        silentRotation.set(angles[0], angles[1]);
        if (rotate.equals("Vanilla")) {
            mc.getPlayer().setYRot(angles[0]);
            mc.getPlayer().setXRot(angles[1]);
        }
    }
    private boolean checkVisibility(MinecraftWrapper mc, net.minecraft.core.BlockPos pos) {
        if (!raycast || mc.getPlayer() == null || mc.getLevel() == null) return true;
        net.minecraft.world.phys.Vec3 eye = mc.getPlayer().getEyePosition();
        net.minecraft.world.phys.BlockHitResult hit = mc.getLevel().clip(new net.minecraft.world.level.ClipContext(
            eye,
            net.minecraft.world.phys.Vec3.atCenterOf(pos),
            net.minecraft.world.level.ClipContext.Block.COLLIDER,
            net.minecraft.world.level.ClipContext.Fluid.NONE,
            mc.getPlayer()));
        return hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS || hit.getBlockPos().equals(pos);
    }




}