package ravex.modules.movement;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import java.util.List;
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
        var mc = MinecraftWrapper.getInstance();
        if (mc.player == null || mc.level == null) return;

        String modeVal = mode;

        if ("Vanilla".equals(modeVal)) {
            if (mc.options.keyJump.isDown() && mc.player.onGround()) {
                MoveUtility.setMotion(mc.player.getDeltaMovement().x, height, mc.player.getDeltaMovement().z);
            }
            return;
        }

        if ("GrimShulker".equals(modeVal)) {
            if (mc.options.keyJump.isDown() && mc.player.onGround()) {
                int shulkerSlot = findShulkerBox();
                if (shulkerSlot != -1) {
                    int oldSlot = InventoryUtility.getSelectedSlot(mc.player);
                    InventoryUtility.selectSlot(mc.player, shulkerSlot);
                    net.minecraft.core.BlockPos pos = mc.player.blockPosition().below();
                    net.minecraft.world.phys.BlockHitResult hit = new net.minecraft.world.phys.BlockHitResult(
                        new net.minecraft.world.phys.Vec3(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5),
                        net.minecraft.core.Direction.UP, pos, false
                    );
                    NetworkUtility.sendUseItemOn(net.minecraft.world.InteractionHand.MAIN_HAND, hit);
                    net.minecraft.core.BlockPos shulkerPos = pos.above();
                    net.minecraft.world.phys.BlockHitResult openHit = new net.minecraft.world.phys.BlockHitResult(
                        new net.minecraft.world.phys.Vec3(shulkerPos.getX() + 0.5, shulkerPos.getY() + 0.5, shulkerPos.getZ() + 0.5),
                        net.minecraft.core.Direction.UP, shulkerPos, false
                    );
                    NetworkUtility.sendUseItemOn(net.minecraft.world.InteractionHand.MAIN_HAND, openHit);
                    MoveUtility.setMotion(mc.player.getDeltaMovement().x, height, mc.player.getDeltaMovement().z);
                    InventoryUtility.selectSlot(mc.player, oldSlot);
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
        var mc = MinecraftWrapper.getInstance();
        if (mc.player.onGround() && mc.options.keyJump.isDown()) {
            MoveUtility.setMotion(mc.player.getDeltaMovement().x, 0.42, mc.player.getDeltaMovement().z);
            ncpJumping = true;
            ncpJumpTicks = 0;
            ncpStartY = mc.player.getY();
        }

        if (!ncpJumping) return;
        ncpJumpTicks++;

        double currentHeight = mc.player.getY() - ncpStartY;
        if (currentHeight >= height || !mc.options.keyJump.isDown() || mc.player.onGround()) {
            ncpJumping = false;
            return;
        }

        double ox = (random.nextDouble() - 0.5) * 0.001;
        double oz = (random.nextDouble() - 0.5) * 0.001;
        NetworkUtility.sendMoveRelative(
            mc.player.getX() + ox, mc.player.getY() + 0.001, mc.player.getZ() + oz,
            true, true
        );

        if (ncpJumpTicks % ncpDelay == 0) {
            MoveUtility.setMotion(
                mc.player.getDeltaMovement().x,
                Math.min(0.42, mc.player.getDeltaMovement().y + 0.08),
                mc.player.getDeltaMovement().z
            );
        }
    }

    private void handleUNCP() {
        var mc = MinecraftWrapper.getInstance();
        if (mc.player.onGround() && mc.options.keyJump.isDown()) {
            MoveUtility.setMotion(mc.player.getDeltaMovement().x, 0.42, mc.player.getDeltaMovement().z);
            uncpJumping = true;
            uncpStartY = mc.player.getY();
            uncpTicks = 0;
        }

        if (!uncpJumping) return;
        uncpTicks++;

        double currentHeight = mc.player.getY() - uncpStartY;
        if (currentHeight >= height || !mc.options.keyJump.isDown()) {
            uncpJumping = false;
            return;
        }

        double incrementBase = "Fast".equals(boostMode) ? 0.06 : 0.04;
        double increment = Math.min(incrementBase + random.nextDouble() * 0.02, height - currentHeight);
        double ox = (random.nextDouble() - 0.5) * 0.005;
        double oz = (random.nextDouble() - 0.5) * 0.005;
        NetworkUtility.sendMoveRelative(
            mc.player.getX() + ox, mc.player.getY() + increment, mc.player.getZ() + oz,
            true, true
        );

        if (uncpTicks % uncpDelay == 0) {
            MoveUtility.setMotion(
                mc.player.getDeltaMovement().x,
                0.42,
                mc.player.getDeltaMovement().z
            );
        }

        if (mc.player.onGround() && mc.player.getDeltaMovement().y <= 0.0) {
            uncpJumping = false;
        }
    }

    private int findShulkerBox() {
        var mc = MinecraftWrapper.getInstance();
        for (int i = 0; i < 9; i++) {
            net.minecraft.world.item.ItemStack stack = InventoryUtility.getItem(mc.player, i);
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
