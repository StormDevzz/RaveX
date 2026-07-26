package ravex.modules.combat;

import ravex.modules.annotations.ModuleInfo;
import net.minecraft.client.Minecraft;
import ravex.utility.misc.block.BlockUtility;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import ravex.utility.player.SwingUtility;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import ravex.utility.misc.PhysicUtility;
import ravex.RaveX;

import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.rotation.RotationUtility;
import java.util.List;
@ModuleInfo(name = "AutoCart", category = "Combat")
public class AutoCart extends ravex.modules.Module {
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
    public static net.minecraft.core.BlockPos targetRenderPos = null;
    private boolean wasUsingBow = false;
    private int lastBowCharge = 0;
    private int repeatTimer = 0;
    private int originalSlot = -1;
    private net.minecraft.core.BlockPos lastPlacedPos = null;
    private long lastPlaceTime = 0;
    protected void onEnable() {
        wasUsingBow = false;
        lastBowCharge = 0;
        repeatTimer = 0;
        originalSlot = -1;
        lastPlacedPos = null;
        lastPlaceTime = 0;
        targetRenderPos = null;
    }
    protected void onDisable() {
        if (originalSlot != -1 && Minecraft.getInstance().player != null) {
            selectSlot(originalSlot, Minecraft.getInstance());
            originalSlot = -1;
        }
        targetRenderPos = null;
    }
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
        boolean isUsingBow = mc.player.isUsingItem() && InventoryUtility.isBow(mc.player.getUseItem());
        if (isUsingBow) {
            lastBowCharge = mc.player.getTicksUsingItem();
        }
        if (wasUsingBow && !isUsingBow) {
            handleBowRelease(mc);
        }
        wasUsingBow = isUsingBow;
    }
    private void handleBowRelease(Minecraft mc) {
        net.minecraft.world.phys.Vec3 eyePos = mc.player.getEyePosition();
        net.minecraft.world.phys.Vec3 look = mc.player.getLookAngle();
        float f = Math.min(lastBowCharge / 20.0F, 1.0F);
        f = (f * f + f * 2.0F) / 3.0F;
        if (f < 0.1F) f = 0.1F;
        net.minecraft.core.BlockPos landingPos = simulateTrajectory(mc, eyePos.add(look.scale(0.1)), look.scale(f * 3.0));
        if (landingPos == null) return;
        double dist = mc.player.getEyePosition().distanceTo(net.minecraft.world.phys.Vec3.atCenterOf(landingPos));
        if (dist > targetRange.getValue()) return;
        int railSlot = findItemSlot(mc, net.minecraft.world.item.Items.RAIL);
        if (railSlot == -1) return;
        int cartSlot = findCartSlot(mc);
        if (cartSlot == -1) return;
        if (!canPlaceBlock(landingPos, mc)) return;
        if (!isWithinRange(landingPos, mc)) return;
        targetRenderPos = landingPos;
        placeCart(mc, landingPos);
    }
    private void placeCart(Minecraft mc, net.minecraft.core.BlockPos pos) {
        int railSlot = findItemSlot(mc, net.minecraft.world.item.Items.RAIL);
        int cartSlot = findCartSlot(mc);
        if (railSlot == -1 || cartSlot == -1) return;
        long now = System.currentTimeMillis();
        if (now - lastPlaceTime < 100) return;
        lastPlaceTime = now;
            originalSlot = InventoryUtility.getSelectedSlot(mc.player);
        RaveX.LOGGER.info("[AutoCart] Placing at {}", pos);
        if (rotate.getValue()) {
            faceBlock(mc, pos);
        }
        selectSlot(railSlot, mc);
        useItemOn(mc, pos, net.minecraft.core.Direction.UP);
        net.minecraft.core.BlockPos above = pos.above();
        if (rotate.getValue()) {
            faceBlock(mc, above);
        }
        selectSlot(cartSlot, mc);
        useItemOn(mc, above, net.minecraft.core.Direction.UP);
            if (originalSlot != -1) selectSlot(originalSlot, mc);
        lastPlacedPos = pos;
        repeatTimer = 0;
    }
    private boolean shouldPlaceAgain(Minecraft mc, net.minecraft.core.BlockPos pos) {
        BlockState state = mc.level.getBlockState(pos);
        if (state.isAir()) return true;
        net.minecraft.core.BlockPos above = pos.above();
        BlockState aboveState = mc.level.getBlockState(above);
        return !aboveState.isAir();
    }
    private net.minecraft.core.BlockPos simulateTrajectory(Minecraft mc, net.minecraft.world.phys.Vec3 startPos, net.minecraft.world.phys.Vec3 startVel) {
        net.minecraft.world.phys.Vec3 pos = startPos;
        net.minecraft.world.phys.Vec3 vel = startVel;
        Level level = mc.level;
        double gravity = 0.05;
        double drag = 0.99;
        for (int i = 0; i < 500; i++) {
            vel = vel.scale(drag);
            vel = vel.add(0, -gravity, 0);
            pos = pos.add(vel);
            net.minecraft.core.BlockPos bp = net.minecraft.core.BlockPos.containing(pos);
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
            var stack = InventoryUtility.getItem(mc.player, i);
            if (stack.is(item)) return i;
        }
        for (int i = 9; i < 36; i++) {
            var stack = InventoryUtility.getItem(mc.player, i);
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
            case "Chest" -> net.minecraft.world.item.Items.CHEST_MINECART;
            case "Furnace" -> net.minecraft.world.item.Items.FURNACE_MINECART;
            case "Hopper" -> net.minecraft.world.item.Items.HOPPER_MINECART;
            default -> net.minecraft.world.item.Items.TNT_MINECART;
        };
        return findItemSlot(mc, targetItem);
    }
    private int findEmptySlot(Minecraft mc) {
        for (int i = 0; i < 9; i++) {
            if (InventoryUtility.getItem(mc.player, i).isEmpty()) return i;
        }
        return -1;
    }
    private void faceBlock(Minecraft mc, net.minecraft.core.BlockPos pos) {
        float[] angles = RotationUtility.anglesTo(mc.player, net.minecraft.world.phys.Vec3.atCenterOf(pos));
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
    private void useItemOn(Minecraft mc, net.minecraft.core.BlockPos pos, net.minecraft.core.Direction face) {
        if (mc.gameMode == null) return;
        mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND,
            new BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos), face, pos, false));
        SwingUtility.swing(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND);
    }
    private boolean canPlaceBlock(net.minecraft.core.BlockPos pos, Minecraft mc) {
        Level level = mc.level;
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.canBeReplaced();
    }
    private boolean isWithinRange(net.minecraft.core.BlockPos pos, Minecraft mc) {
        net.minecraft.world.phys.Vec3 playerPos = mc.player.getEyePosition();
        net.minecraft.world.phys.Vec3 targetPos = net.minecraft.world.phys.Vec3.atCenterOf(pos);
        return playerPos.distanceTo(targetPos) <= range.getValue();
    }
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("AutoCart").getEnabled();
    }
    public static AutoCart itz() {
        return ravex.manager.ModuleManager.delegate(AutoCart.class);
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