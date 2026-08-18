package ravex.modules.movement;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import java.util.Random;
import ravex.utility.network.NetworkUtility;
import ravex.utility.player.InventoryUtility;
import ravex.utility.movement.MoveUtility;
import ravex.mcwrapper.MinecraftWrapper;
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

    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player == null || mc.getLevel() == null) return;

        String modeVal = mode;

        if ("Vanilla".equals(modeVal)) {
            if (mc.isJumpKeyDown() && mc.isPlayerOnGround()) {
                MoveUtility.setMotion(mc.getPlayerDeltaMovement().x, height, mc.getPlayerDeltaMovement().z);
            }
            return;
        }

        if ("GrimShulker".equals(modeVal)) {
            if (mc.isJumpKeyDown() && mc.isPlayerOnGround()) {
                int shulkerSlot = findShulkerBox();
                if (shulkerSlot != -1) {
                    int oldSlot = InventoryUtility.getSelectedSlot(player);
                    InventoryUtility.selectSlot(player, shulkerSlot);
                    var pos = player.blockPosition().below();
                    var hit = new net.minecraft.world.phys.BlockHitResult(
                        new net.minecraft.world.phys.Vec3(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5),
                        net.minecraft.core.Direction.UP, pos, false
                    );
                    NetworkUtility.sendUseItemOn(net.minecraft.world.InteractionHand.MAIN_HAND, hit);
                    var shulkerPos = pos.above();
                    var openHit = new net.minecraft.world.phys.BlockHitResult(
                        new net.minecraft.world.phys.Vec3(shulkerPos.getX() + 0.5, shulkerPos.getY() + 0.5, shulkerPos.getZ() + 0.5),
                        net.minecraft.core.Direction.UP, shulkerPos, false
                    );
                    NetworkUtility.sendUseItemOn(net.minecraft.world.InteractionHand.MAIN_HAND, openHit);
                    MoveUtility.setMotion(mc.getPlayerDeltaMovement().x, height, mc.getPlayerDeltaMovement().z);
                    InventoryUtility.selectSlot(player, oldSlot);
                }
            }
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

    private void handleNCP() {
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player == null) return;
        if (mc.isPlayerOnGround() && mc.isJumpKeyDown()) {
            MoveUtility.setMotion(mc.getPlayerDeltaMovement().x, 0.42, mc.getPlayerDeltaMovement().z);
            ncpJumping = true;
            ncpJumpTicks = 0;
            ncpStartY = player.getY();
        }

        if (!ncpJumping) return;
        ncpJumpTicks++;

        double currentHeight = player.getY() - ncpStartY;
        if (currentHeight >= height || !mc.isJumpKeyDown() || mc.isPlayerOnGround()) {
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
                mc.getPlayerDeltaMovement().x,
                Math.min(0.42, mc.getPlayerDeltaMovement().y + 0.08),
                mc.getPlayerDeltaMovement().z
            );
        }
    }

    private void handleUNCP() {
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player == null) return;
        if (mc.isPlayerOnGround() && mc.isJumpKeyDown()) {
            MoveUtility.setMotion(mc.getPlayerDeltaMovement().x, 0.42, mc.getPlayerDeltaMovement().z);
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
                mc.getPlayerDeltaMovement().x,
                0.42,
                mc.getPlayerDeltaMovement().z
            );
        }

        if (mc.isPlayerOnGround() && mc.getPlayerDeltaMovement().y <= 0.0) {
            uncpJumping = false;
        }
    }

    private int findShulkerBox() {
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player == null) return -1;
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(player, i);
            if (!stack.isEmpty() && stack.getItem() instanceof net.minecraft.world.item.BlockItem blockItem) {
                if (blockItem.getBlock() instanceof net.minecraft.world.level.block.ShulkerBoxBlock) {
                    return i;
                }
            }
        }
        return -1;
    }

    public void onDisable() {
        ncpJumping = false;
        uncpJumping = false;
    }
}
