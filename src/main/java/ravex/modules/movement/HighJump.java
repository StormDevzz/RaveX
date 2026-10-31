package ravex.modules.movement;
import ravex.utility.player.PlayerUtility;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import java.util.Random;
import ravex.utility.network.NetworkUtility;
import ravex.utility.player.InventoryUtility;
import ravex.utility.movement.MoveUtility;
import ravex.utility.misc.block.BlockUtility;
import ravex.utility.misc.PhysicUtility;
import ravex.utility.player.rotation.RotationUtility;
import ravex.utility.player.rotation.SilentRotationUtility;
import ravex.mcwrapper.MinecraftWrapper;
@Module(name = "HighJump", category = "Movement")
public class HighJump {
    @Parameter(name = "Mode", modes = {"Vanilla", "GrimShulker", "NCP", "UNCP"})
    public String mode = "Vanilla";
    @Parameter(name = "Height", min = 0.5, max = 10.0, step = 0.1, visible = "mode=Vanilla||mode=NCP||mode=UNCP")
    public double height = 2.0;
    // lid boost per tick, 1.0 is the safe value
    @Parameter(name = "ShulkerBoost", min = 0.5, max = 2.0, step = 0.1, visible = "mode=GrimShulker")
    public double shulkerBoost = 1.0;
    @Parameter(name = "NCPDelay", min = 1, max = 10, step = 1, visible = "mode=NCP")
    public int ncpDelay = 3;
    @Parameter(name = "UNCPDelay", min = 1, max = 10, step = 1, visible = "mode=UNCP")
    public int uncpDelay = 2;
    @Parameter(name = "BoostMode", modes = {"Strict", "Fast"}, visible = "mode=UNCP")
    public String boostMode = "Strict";
    // highlight the opened shulker
    @Parameter(name = "Highlight", visible = "mode=GrimShulker")
    public boolean highlight = true;
    @Parameter(name = "HighlightColor", color = true, visible = "mode=GrimShulker && highlight")
    public int highlightColor = 0xFFFFFFFF;

    public static volatile net.minecraft.core.BlockPos shulkerHighlightPos;
    public static volatile long shulkerHighlightMs;
    public static SilentRotationUtility silentRotation = new SilentRotationUtility();

    private final Random random = new Random();
    private int ncpJumpTicks = 0;
    private boolean ncpJumping = false;
    private double ncpStartY = 0.0;
    private int uncpTicks = 0;
    private boolean uncpJumping = false;
    private double uncpStartY = 0.0;
    private int shulkerCooldown = 0;
    private int silentRotationHold = 0;
    private static final double INTERACT_REACH = 4.5;
    private net.minecraft.core.BlockPos worldShulkerPos;
    private int shulkerScanCooldown = 0;
    private int lidScanCooldown = 0;
    private boolean shulkerGuiClosed = false;

    public static boolean hasSilentRotations() {
        return silentRotation.hasRotation;
    }

    private net.minecraft.world.phys.Vec3 shulkerCenter(net.minecraft.core.BlockPos pos) {
        return PhysicUtility.vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }

    private boolean shulkerAccessible(MinecraftWrapper mc, net.minecraft.client.player.LocalPlayer player, net.minecraft.core.BlockPos pos) {
        var eye = player.getEyePosition();
        var center = shulkerCenter(pos);
        if (eye.distanceTo(center) > INTERACT_REACH) return false;
        var hit = PhysicUtility.raycast(eye, center);
        return hit != null && hit.getBlockPos().equals(pos);
    }

    private void selectSlotSync(net.minecraft.client.player.LocalPlayer player, int slot) {
        InventoryUtility.selectSlot(player, slot);
        NetworkUtility.sendPacket(new net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket(slot));
    }

    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player == null || mc.getLevel() == null) return;

        if (silentRotationHold > 0 && --silentRotationHold == 0) {
            silentRotation.reset();
        }

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

        if ("NCP".equals(modeVal)) {
            handleNCP();
            return;
        }

        if ("UNCP".equals(modeVal)) {
            handleUNCP();
            return;
        }
    }

    // grim exempts motion next to an animating lid,
    // so a plain rise keeps lifting without flags
    private void handleGrimShulker(MinecraftWrapper mc, net.minecraft.client.player.LocalPlayer player) {
        // close the gui once per cycle, the lid still animates
        if (mc.getCurrentScreen() instanceof net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen) {
            if (!shulkerGuiClosed) {
                shulkerGuiClosed = true;
                player.closeContainer();
                if (shulkerCooldown < 2) shulkerCooldown = 2;
            }
        } else {
            shulkerGuiClosed = false;
        }
        if (shulkerCooldown > 0) shulkerCooldown--;

        var lid = findLidAnimating(mc, player);
        if (lid != null) {
            var motion = PlayerUtility.getDeltaMovement(player);
            MoveUtility.setMotion(motion.x, Math.min(shulkerBoost, 2.0), motion.z);
            return;
        }

        if (shulkerCooldown > 0) return;
        // open even in the air, never miss a fresh lid
        var shulker = findWorldShulker(mc, player);
        if (shulker == null) return;
        if (!shulkerAccessible(mc, player, shulker)) {
            worldShulkerPos = null;
            return;
        }
        openShulker(mc, player, shulker);
        shulkerCooldown = 5;
    }

    // remember the shulker, we rise out of a fresh search
    private net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity findLidAnimating(MinecraftWrapper mc,
            net.minecraft.client.player.LocalPlayer player) {
        var level = mc.getLevel();
        if (level == null) return null;

        if (worldShulkerPos != null) {
            var tracked = level.getBlockEntity(worldShulkerPos);
            if (tracked instanceof net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity shulker
                    && lidBoosting(player, worldShulkerPos, shulker)) {
                return shulker;
            }
        }

        if (lidScanCooldown > 0) {
            lidScanCooldown--;
            return null;
        }
        lidScanCooldown = 2;

        var bp = player.blockPosition();
        for (var pos : net.minecraft.core.BlockPos.betweenClosed(bp.offset(-4, -4, -4), bp.offset(4, 4, 4))) {
            if (worldShulkerPos != null && worldShulkerPos.equals(pos)) continue;
            if (!(level.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity shulker)) continue;
            if (lidBoosting(player, pos, shulker)) return shulker;
        }
        return null;
    }

    private boolean lidBoosting(net.minecraft.client.player.LocalPlayer player, net.minecraft.core.BlockPos pos,
            net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity shulker) {
        float progress = shulker.getProgress(1.0f);
        if (progress <= 0.0f || progress >= 1.0f) return false;
        double dist = Math.sqrt(Math.pow(player.getX() - (pos.getX() + 0.5), 2)
            + Math.pow(player.getZ() - (pos.getZ() + 0.5), 2));
        // standing on it or next to it works
        if (dist > 3.0) return false;
        double dy = player.getY() - (pos.getY() + 0.5);
        // only cap the height while not rising
        if (player.getDeltaMovement().y <= 1.0 && Math.abs(dy) > 4.5) return false;
        return true;
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
        climbTick(mc, player);
    }

    private void climbTick(MinecraftWrapper mc, net.minecraft.client.player.LocalPlayer player) {
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
        shulkerScanCooldown = 4;
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

    private void openShulker(MinecraftWrapper mc, net.minecraft.client.player.LocalPlayer player, net.minecraft.core.BlockPos pos) {
        var center = shulkerCenter(pos);
        float[] ang = RotationUtility.anglesTo(player.getEyePosition(), center);
        // silent rotation, camera stays still
        silentRotation.set(ang[0], ang[1]);
        silentRotationHold = 6;
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
        BlockUtility.useItemOn(mc, openHit);
        BlockUtility.swing(mc);
        if (safeSlot != -1) selectSlotSync(player, oldSlot);
        if (highlight) {
            shulkerHighlightPos = pos;
            shulkerHighlightMs = System.currentTimeMillis();
        }
    }

    public void onDisable() {
        ncpJumping = false;
        uncpJumping = false;
        shulkerCooldown = 0;
        worldShulkerPos = null;
        shulkerScanCooldown = 0;
        lidScanCooldown = 0;
        shulkerGuiClosed = false;
        silentRotationHold = 0;
        silentRotation.reset();
    }
}
