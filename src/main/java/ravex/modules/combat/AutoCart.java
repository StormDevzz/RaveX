package ravex.modules.combat;
<<<<<<< HEAD
import ravex.manager.ModuleManager;
=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
<<<<<<< HEAD
=======
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import ravex.RaveX;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.rotation.RotationUtility;
import ravex.utility.player.SwingUtility;
import java.util.List;
public class AutoCart extends Module {
<<<<<<< HEAD
=======
    public static final AutoCart INSTANCE = new AutoCart();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    public final NumberParameter range = new NumberParameter("Range", 6, 1, 10, 1);
    public final NumberParameter targetRange = new NumberParameter("TargetRange", 20, 5, 50, 1);
    public final ModeParameter cartType = new ModeParameter("CartType", "TNT",
            List.of("TNT", "Chest", "Furnace", "Hopper"));
    public final ModeParameter swapMode = new ModeParameter("SwapMode", "Normal",
            List.of("Normal", "Silent"));
    public final BooleanParameter rotate = new BooleanParameter("Rotate", true);
    public final BooleanParameter repeat = new BooleanParameter("Repeat", false);
    public final NumberParameter repeatDelay = new NumberParameter("RepeatDelay", 20, 5, 100, 5);
    public final BooleanParameter render = new BooleanParameter("Render", true);
    public final ColorParameter color = new ColorParameter("Color", 0x3FFF4444);
    public static BlockPos targetRenderPos = null;
    private boolean wasUsingBow = false;
    private int lastBowCharge = 0;
    private int repeatTimer = 0;
    private int originalSlot = -1;
    private BlockPos lastPlacedPos = null;
    private long lastPlaceTime = 0;

    @Override
    protected void onEnable() {
        wasUsingBow = false;
        lastBowCharge = 0;
        repeatTimer = 0;
        originalSlot = -1;
        lastPlacedPos = null;
        lastPlaceTime = 0;
        targetRenderPos = null;
    }
    @Override
    protected void onDisable() {
        if (originalSlot != -1 && Minecraft.getInstance().player != null) {
            selectSlot(originalSlot, Minecraft.getInstance());
            originalSlot = -1;
        }
        targetRenderPos = null;
    }
    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (repeat.getValue() && lastPlacedPos != null) {
            repeatTimer++;
            if (repeatTimer >= repeatDelay.getValue().intValue()) {
                repeatTimer = 0;
                if (shouldPlaceAgain(mc, lastPlacedPos)) {
                    placeCart(mc, lastPlacedPos);
                }
            }
        }
<<<<<<< HEAD
        boolean isUsingBow = mc.player.isUsingItem() && InventoryUtility.isBow(mc.player.getUseItem());
=======
        boolean isUsingBow = mc.player.isUsingItem() && mc.player.getUseItem().is(Items.BOW);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        if (isUsingBow) {
            lastBowCharge = mc.player.getTicksUsingItem();
        }
        if (wasUsingBow && !isUsingBow) {
            handleBowRelease(mc);
        }
        wasUsingBow = isUsingBow;
    }
    private void handleBowRelease(Minecraft mc) {
        Vec3 eyePos = mc.player.getEyePosition();
        Vec3 look = mc.player.getLookAngle();
        float f = Math.min(lastBowCharge / 20.0F, 1.0F);
        f = (f * f + f * 2.0F) / 3.0F;
        if (f < 0.1F) f = 0.1F;
        BlockPos landingPos = simulateTrajectory(mc, eyePos.add(look.scale(0.1)), look.scale(f * 3.0));
        if (landingPos == null) return;
        double dist = mc.player.getEyePosition().distanceTo(Vec3.atCenterOf(landingPos));
        if (dist > targetRange.getValue()) return;
<<<<<<< HEAD
        int railSlot = findItemSlot(mc, net.minecraft.world.item.Items.RAIL);
=======
        int railSlot = findItemSlot(mc, Items.RAIL);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        if (railSlot == -1) return;
        int cartSlot = findCartSlot(mc);
        if (cartSlot == -1) return;
        if (!canPlaceBlock(landingPos, mc)) return;
        if (!isWithinRange(landingPos, mc)) return;
        targetRenderPos = landingPos;
        placeCart(mc, landingPos);
    }
    private void placeCart(Minecraft mc, BlockPos pos) {
<<<<<<< HEAD
        int railSlot = findItemSlot(mc, net.minecraft.world.item.Items.RAIL);
=======
        int railSlot = findItemSlot(mc, Items.RAIL);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        int cartSlot = findCartSlot(mc);
        if (railSlot == -1 || cartSlot == -1) return;
        long now = System.currentTimeMillis();
        if (now - lastPlaceTime < 100) return;
        lastPlaceTime = now;
<<<<<<< HEAD
            originalSlot = InventoryUtility.getSelectedSlot(mc.player);
=======
        originalSlot = mc.player.getInventory().getSelectedSlot();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        RaveX.LOGGER.info("[AutoCart] Placing at {}", pos);
        if (rotate.getValue()) {
            faceBlock(mc, pos);
        }
        selectSlot(railSlot, mc);
        useItemOn(mc, pos, Direction.UP);
        BlockPos above = pos.above();
        if (rotate.getValue()) {
            faceBlock(mc, above);
        }
        selectSlot(cartSlot, mc);
        useItemOn(mc, above, Direction.UP);
            if (originalSlot != -1) selectSlot(originalSlot, mc);
        lastPlacedPos = pos;
        repeatTimer = 0;
    }
    private boolean shouldPlaceAgain(Minecraft mc, BlockPos pos) {
        BlockState state = mc.level.getBlockState(pos);
        if (state.isAir()) return true;
        BlockPos above = pos.above();
        BlockState aboveState = mc.level.getBlockState(above);
        return !aboveState.isAir();
    }
    private BlockPos simulateTrajectory(Minecraft mc, Vec3 startPos, Vec3 startVel) {
        Vec3 pos = startPos;
        Vec3 vel = startVel;
        Level level = mc.level;
        double gravity = 0.05;
        double drag = 0.99;
        for (int i = 0; i < 500; i++) {
            vel = vel.scale(drag);
            vel = vel.add(0, -gravity, 0);
            pos = pos.add(vel);
            BlockPos bp = BlockPos.containing(pos);
            if (!level.isLoaded(bp)) return null;
            BlockState state = level.getBlockState(bp);
            if (!state.isAir() && !state.canBeReplaced()) {
                return bp;
            }
            if (pos.y < level.getMinY()) return null;
        }
        return null;
    }
    private int findItemSlot(Minecraft mc, net.minecraft.world.item.Item item) {
        for (int i = 0; i < 9; i++) {
<<<<<<< HEAD
            var stack = InventoryUtility.getItem(mc.player, i);
            if (stack.is(item)) return i;
        }
        for (int i = 9; i < 36; i++) {
            var stack = InventoryUtility.getItem(mc.player, i);
=======
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.is(item)) return i;
        }
        for (int i = 9; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            if (stack.is(item)) {
                int freeSlot = findEmptySlot(mc);
                if (freeSlot != -1) {
                    mc.gameMode.handleInventoryMouseClick(
                        mc.player.containerMenu.containerId,
                        i,
                        freeSlot,
                        net.minecraft.world.inventory.ClickType.SWAP,
                        mc.player
                    );
                    return freeSlot;
                }
            }
        }
        return -1;
    }
    private int findCartSlot(Minecraft mc) {
        net.minecraft.world.item.Item targetItem = switch (cartType.getValue()) {
<<<<<<< HEAD
            case "Chest" -> net.minecraft.world.item.Items.CHEST_MINECART;
            case "Furnace" -> net.minecraft.world.item.Items.FURNACE_MINECART;
            case "Hopper" -> net.minecraft.world.item.Items.HOPPER_MINECART;
            default -> net.minecraft.world.item.Items.TNT_MINECART;
=======
            case "Chest" -> Items.CHEST_MINECART;
            case "Furnace" -> Items.FURNACE_MINECART;
            case "Hopper" -> Items.HOPPER_MINECART;
            default -> Items.TNT_MINECART;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        };
        return findItemSlot(mc, targetItem);
    }
    private int findEmptySlot(Minecraft mc) {
        for (int i = 0; i < 9; i++) {
<<<<<<< HEAD
            if (InventoryUtility.getItem(mc.player, i).isEmpty()) return i;
=======
            if (mc.player.getInventory().getItem(i).isEmpty()) return i;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        }
        return -1;
    }
    private void faceBlock(Minecraft mc, BlockPos pos) {
        float[] angles = RotationUtility.anglesTo(mc.player, Vec3.atCenterOf(pos));
        mc.player.setYRot(angles[0]);
        mc.player.setXRot(angles[1]);
    }
    private void selectSlot(int slot, Minecraft mc) {
        if (swapMode.getValue().equals("Silent")) {
            InventoryUtility.silentSelectSlot(mc.player, slot);
        } else {
            InventoryUtility.selectSlot(mc.player, slot);
        }
    }
    private void useItemOn(Minecraft mc, BlockPos pos, Direction face) {
        if (mc.gameMode == null) return;
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
            new BlockHitResult(Vec3.atCenterOf(pos), face, pos, false));
        SwingUtility.swing(mc.player, InteractionHand.MAIN_HAND);
    }
    private boolean canPlaceBlock(BlockPos pos, Minecraft mc) {
        Level level = mc.level;
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.canBeReplaced();
    }
    private boolean isWithinRange(BlockPos pos, Minecraft mc) {
        Vec3 playerPos = mc.player.getEyePosition();
        Vec3 targetPos = Vec3.atCenterOf(pos);
        return playerPos.distanceTo(targetPos) <= range.getValue();
    }
<<<<<<< HEAD
    public static boolean maybeEnabled() {
        return maybeEnabled(AutoCart.class);
    }
    public static AutoCart itz() {
        return ModuleManager.get(AutoCart.class);
    }

}
=======
}
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
