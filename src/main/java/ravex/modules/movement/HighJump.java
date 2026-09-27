package ravex.modules.movement;
import ravex.utility.player.PlayerUtility;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import java.util.Random;
import ravex.utility.network.NetworkUtility;
import ravex.utility.player.InventoryUtility;
import ravex.utility.movement.MoveUtility;
import ravex.utility.misc.block.BlockUtility;
import ravex.utility.player.rotation.RotationUtility;
import ravex.mcwrapper.MinecraftWrapper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
@Module(name = "HighJump", category = "Movement")
public class HighJump {
    @Parameter(name = "Mode", modes = {"Vanilla", "GrimShulker", "NCP", "UNCP"})
    public String mode = "Vanilla";
    @Parameter(name = "Height", min = 0.5, max = 10.0, step = 0.1)
    public double height = 2.0;
    @Parameter(name = "NCPDelay", min = 1, max = 10, step = 1, visible = "mode=NCP")
    public int ncpDelay = 3;
    @Parameter(name = "UNCPDelay", min = 1, max = 10, step = 1, visible = "mode=UNCP")
    public int uncpDelay = 2;
    @Parameter(name = "BoostMode", modes = {"Strict", "Fast"}, visible = "mode=UNCP")
    public String boostMode = "Strict";

    private final Random random = new Random();
    private int ncpJumpTicks = 0;
    private boolean ncpJumping = false;
    private double ncpStartY = 0.0;
    private int uncpTicks = 0;
    private boolean uncpJumping = false;
    private double uncpStartY = 0.0;
    private int shulkerCooldown = 0;
    private int airBoostTicks = 0;
    private boolean forcedJump = false;
    private static final double INTERACT_REACH = 4.5;
    private net.minecraft.core.BlockPos worldShulkerPos;
    private int shulkerScanCooldown = 0;
    private boolean pendingBoost = false;
    private int openWaitTicks = 0;

    private Vec3 shulkerCenter(net.minecraft.core.BlockPos pos) {
        return new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }

    private boolean shulkerAccessible(MinecraftWrapper mc, net.minecraft.client.player.LocalPlayer player, net.minecraft.core.BlockPos pos) {
        var eye = player.getEyePosition();
        var center = shulkerCenter(pos);
        if (eye.distanceTo(center) > INTERACT_REACH) return false;
        var hit = mc.getLevel().clip(new ClipContext(eye, center, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(pos);
    }

    private void selectSlotSync(net.minecraft.client.player.LocalPlayer player, int slot) {
        InventoryUtility.selectSlot(player, slot);
        NetworkUtility.sendPacket(new net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket(slot));
    }

    private void forceJumpKey(MinecraftWrapper mc, boolean down) {
        if (mc.getOptions() == null) return;
        mc.getOptions().keyJump.setDown(down);
    }

    private void boostJump(MinecraftWrapper mc, net.minecraft.client.player.LocalPlayer player) {
        var delta = PlayerUtility.getDeltaMovement();
        if (PlayerUtility.isOnGround()) {
            MoveUtility.setMotion(delta.x, height, delta.z);
        } else if (delta.y < height) {
            MoveUtility.setMotion(delta.x, Math.min(height, delta.y + 0.5), delta.z);
        }
        forceJumpKey(mc, true);
    }

    private void airBoost(MinecraftWrapper mc, net.minecraft.client.player.LocalPlayer player) {
        if (airBoostTicks <= 0) return;
        airBoostTicks--;
        var delta = PlayerUtility.getDeltaMovement();
        if (!PlayerUtility.isOnGround() && delta.y < height) {
            MoveUtility.setMotion(delta.x, Math.min(height, delta.y + 0.4), delta.z);
        }
    }

    private void resetShulkerState() {
        shulkerCooldown = 0;
        airBoostTicks = 0;
        pendingBoost = false;
        openWaitTicks = 0;
        worldShulkerPos = null;
        shulkerScanCooldown = 0;
    }

    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player == null || mc.getLevel() == null) return;

        String modeVal = mode;

        if ("Vanilla".equals(modeVal)) {
            if (mc.isJumpKeyDown() && PlayerUtility.isOnGround()) {
                MoveUtility.setMotion(PlayerUtility.getDeltaMovement().x, height, PlayerUtility.getDeltaMovement().z);
            }
            return;
        }

        if ("GrimShulker".equals(modeVal)) {
            handleGrimShulker(mc, player);
            return;
        }
        if (forcedJump) {
            forceJumpKey(mc, false);
            forcedJump = false;
            resetShulkerState();
        }

        if ("NCP".equals(modeVal)) {
            handleNCP();
            return;
        }

        if ("UNCP".equals(modeVal)) {
            handleUNCP();
            return;
        }
    }

    private void handleNCP() {
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player == null) return;
        if (PlayerUtility.isOnGround() && mc.isJumpKeyDown()) {
            MoveUtility.setMotion(PlayerUtility.getDeltaMovement().x, 0.42, PlayerUtility.getDeltaMovement().z);
            ncpJumping = true;
            ncpJumpTicks = 0;
            ncpStartY = player.getY();
        }

        if (!ncpJumping) return;
        ncpJumpTicks++;

        double currentHeight = player.getY() - ncpStartY;
        if (currentHeight >= height || !mc.isJumpKeyDown() || PlayerUtility.isOnGround()) {
            ncpJumping = false;
            return;
        }

        double ox = (random.nextDouble() - 0.5) * 0.001;
        double oz = (random.nextDouble() - 0.5) * 0.001;
        NetworkUtility.sendMoveRelative(
            player.getX() + ox, player.getY() + 0.001, player.getZ() + oz,
            true, true
        );

        if (ncpJumpTicks % ncpDelay == 0) {
            MoveUtility.setMotion(
                PlayerUtility.getDeltaMovement().x,
                Math.min(0.42, PlayerUtility.getDeltaMovement().y + 0.08),
                PlayerUtility.getDeltaMovement().z
            );
        }
    }

    private void handleUNCP() {
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player == null) return;
        if (PlayerUtility.isOnGround() && mc.isJumpKeyDown()) {
            MoveUtility.setMotion(PlayerUtility.getDeltaMovement().x, 0.42, PlayerUtility.getDeltaMovement().z);
            uncpJumping = true;
            uncpStartY = player.getY();
            uncpTicks = 0;
        }

        if (!uncpJumping) return;
        uncpTicks++;

        double currentHeight = player.getY() - uncpStartY;
        if (currentHeight >= height || !mc.isJumpKeyDown()) {
            uncpJumping = false;
            return;
        }

        double incrementBase = "Fast".equals(boostMode) ? 0.06 : 0.04;
        double increment = Math.min(incrementBase + random.nextDouble() * 0.02, height - currentHeight);
        double ox = (random.nextDouble() - 0.5) * 0.005;
        double oz = (random.nextDouble() - 0.5) * 0.005;
        NetworkUtility.sendMoveRelative(
            player.getX() + ox, player.getY() + increment, player.getZ() + oz,
            true, true
        );

        if (uncpTicks % uncpDelay == 0) {
            MoveUtility.setMotion(
                PlayerUtility.getDeltaMovement().x,
                0.42,
                PlayerUtility.getDeltaMovement().z
            );
        }

        if (PlayerUtility.isOnGround() && PlayerUtility.getDeltaMovement().y <= 0.0) {
            uncpJumping = false;
        }
    }

    private net.minecraft.core.BlockPos findWorldShulker(MinecraftWrapper mc, net.minecraft.client.player.LocalPlayer player) {
        if (worldShulkerPos != null) {
            var st = BlockUtility.getState(mc.getLevel(), worldShulkerPos);
            if (st.getBlock() instanceof net.minecraft.world.level.block.ShulkerBoxBlock) {
                return worldShulkerPos;
            }
            worldShulkerPos = null;
        }
        if (shulkerScanCooldown > 0) {
            shulkerScanCooldown--;
            return null;
        }
        shulkerScanCooldown = 10;
        var eye = player.getEyePosition();
        var bp = player.blockPosition();
        int r = (int) Math.ceil(INTERACT_REACH);
        net.minecraft.core.BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (int x = bp.getX() - r; x <= bp.getX() + r; x++) {
            for (int y = bp.getY() - r; y <= bp.getY() + r; y++) {
                for (int z = bp.getZ() - r; z <= bp.getZ() + r; z++) {
                    var st = BlockUtility.getState(mc.getLevel(), x, y, z);
                    if (!(st.getBlock() instanceof net.minecraft.world.level.block.ShulkerBoxBlock)) continue;
                    var pos = BlockUtility.pos(x, y, z);
                    double dist = eye.distanceToSqr(shulkerCenter(pos));
                    if (dist <= INTERACT_REACH * INTERACT_REACH && dist < bestDist) {
                        bestDist = dist;
                        best = pos;
                    }
                }
            }
        }
        worldShulkerPos = best;
        return best;
    }

    private void handleGrimShulker(MinecraftWrapper mc, net.minecraft.client.player.LocalPlayer player) {
        if (!forcedJump) {
            forceJumpKey(mc, true);
            forcedJump = true;
        }
        airBoost(mc, player);
        if (pendingBoost) {
            if (mc.getCurrentScreen() instanceof net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen) {
                pendingBoost = false;
                openWaitTicks = 0;
                boostJump(mc, player);
                airBoostTicks = 10;
                shulkerCooldown = 6;
            } else if (++openWaitTicks > 5) {
                pendingBoost = false;
                openWaitTicks = 0;
                shulkerCooldown = 4;
            }
            return;
        }
        if (shulkerCooldown > 0) {
            shulkerCooldown--;
            return;
        }
        if (!PlayerUtility.isOnGround()) return;
        var shulker = findWorldShulker(mc, player);
        if (shulker == null) return;
        if (!shulkerAccessible(mc, player, shulker)) return;
        openShulker(mc, player, shulker);
        pendingBoost = true;
        openWaitTicks = 0;
    }

    private void openShulker(MinecraftWrapper mc, net.minecraft.client.player.LocalPlayer player, net.minecraft.core.BlockPos pos) {
        var center = shulkerCenter(pos);
        float[] ang = RotationUtility.anglesTo(player.getEyePosition(), center);
        player.setYRot(ang[0]);
        player.setXRot(ang[1]);
        NetworkUtility.sendRot(ang[0], ang[1], player.onGround(), player.horizontalCollision);
        int oldSlot = InventoryUtility.getSelectedSlot(player);
        int safeSlot = -1;
        if (InventoryUtility.isBlockItem(InventoryUtility.getItem(player, oldSlot))) {
            for (int i = 0; i < 9; i++) {
                if (i == oldSlot) continue;
                if (!InventoryUtility.isBlockItem(InventoryUtility.getItem(player, i))) {
                    safeSlot = i;
                    break;
                }
            }
        }
        if (safeSlot != -1) selectSlotSync(player, safeSlot);
        var openHit = new net.minecraft.world.phys.BlockHitResult(center, net.minecraft.core.Direction.UP, pos, false);
        NetworkUtility.sendUseItemOn(net.minecraft.world.InteractionHand.MAIN_HAND, openHit);
        BlockUtility.swing(mc);
        if (safeSlot != -1) selectSlotSync(player, oldSlot);
    }

    public void onDisable() {
        ncpJumping = false;
        uncpJumping = false;
        if (forcedJump) {
            forceJumpKey(MinecraftWrapper.getWrapper(), false);
            forcedJump = false;
        }
        resetShulkerState();
    }
}
