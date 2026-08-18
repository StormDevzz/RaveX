package ravex.modules.combat;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.RaveX;
import ravex.utility.misc.PhysicUtility;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.rotation.RotationUtility;
import ravex.utility.player.rotation.AimUtility;
import ravex.utility.player.rotation.SilentRotationUtility;
import ravex.utility.player.SwingUtility;

import ravex.mcwrapper.MinecraftWrapper;
import org.jetbrains.annotations.Nullable;




@Module(name = "AutoCart", category = "Combat")
public class AutoCart {
    @Parameter(name = "Range", min = 1, max = 10, step = 1)
    public double range = 6;
    @Parameter(name = "TargetRange", min = 5, max = 50, step = 1)
    public double targetRange = 20;
    @Parameter(name = "CartType", modes = {"TNT", "Chest", "Furnace", "Hopper"})
    public String cartType = "TNT";
    @Parameter(name = "SwapMode", modes = {"Normal", "Silent"})
    public String swapMode = "Normal";
    @Parameter(name = "Rotate")
    public boolean rotate = true;
    @Parameter(name = "Bypass", modes = {"Vanilla", "Legit", "NCP"})
    public String bypass = "Vanilla";
    @Parameter(name = "RotateSpeed", min = 10, max = 180, step = 5)
    public double rotateSpeed = 180;
    @Parameter(name = "Repeat")
    public boolean repeat = false;
    @Parameter(name = "RepeatDelay", min = 5, max = 100, step = 5)
    public double repeatDelay = 20;
    @Parameter(name = "Render")
    public boolean render = true;
    @Parameter(name = "Color", color = true, visible = "render")
    public int color = 0x3FFF4444;
    public static net.minecraft.core.BlockPos targetRenderPos = null;
    public static final SilentRotationUtility silentRotation = new SilentRotationUtility();
    private boolean wasUsingBow = false;
    private int lastBowCharge = 0;
    private int repeatTimer = 0;
    private int originalSlot = -1;
    private net.minecraft.core.BlockPos lastPlacedPos = null;
    private long lastPlaceTime = 0;
    public static boolean hasSilentRotations() {
        return silentRotation.hasRotation;
    }
    public void onEnable() {
        wasUsingBow = false;
        lastBowCharge = 0;
        repeatTimer = 0;
        originalSlot = -1;
        lastPlacedPos = null;
        lastPlaceTime = 0;
        targetRenderPos = null;
    }
    public void onDisable() {
        if (originalSlot != -1 && MinecraftWrapper.getWrapper().getPlayer() != null) {
            selectSlot(originalSlot, MinecraftWrapper.getWrapper());
            originalSlot = -1;
        }
        targetRenderPos = null;
    }
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getLevel() == null) return;
        if (repeat && lastPlacedPos != null) {
            repeatTimer++;
            if (repeatTimer >= (int) repeatDelay) {
                repeatTimer = 0;
                if (shouldPlaceAgain(mc, lastPlacedPos)) {
                    placeCart(mc, lastPlacedPos);
                }
            }
        }
        boolean isUsingBow = mc.getPlayer().isUsingItem() && InventoryUtility.isBow(mc.getPlayer().getUseItem());
        if (isUsingBow) {
            lastBowCharge = mc.getPlayer().getTicksUsingItem();
        }
        if (wasUsingBow && !isUsingBow) {
            handleBowRelease(mc);
        }
        wasUsingBow = isUsingBow;
    }
    private void handleBowRelease(MinecraftWrapper mc) {
        net.minecraft.world.phys.Vec3 eyePos = mc.getPlayer().getEyePosition();
        net.minecraft.world.phys.Vec3 look = mc.getPlayer().getLookAngle();
        float f = Math.min(lastBowCharge / 20.0F, 1.0F);
        f = (f * f + f * 2.0F) / 3.0F;
        if (f < 0.1F) f = 0.1F;
        net.minecraft.core.BlockPos landingPos = simulateTrajectory(mc, eyePos.add(look.scale(0.1)), look.scale(f * 3.0));
        if (landingPos == null) return;
        double dist = mc.getPlayer().getEyePosition().distanceTo(PhysicUtility.centerOf(landingPos));
        if (dist > targetRange) return;
        int railSlot = findItemSlot(mc, net.minecraft.world.item.Items.RAIL);
        if (railSlot == -1) return;
        int cartSlot = findCartSlot(mc);
        if (cartSlot == -1) return;
        if (!canPlaceBlock(landingPos, mc)) return;
        if (!isWithinRange(landingPos, mc)) return;
        targetRenderPos = landingPos;
        placeCart(mc, landingPos);
    }
    private void placeCart(MinecraftWrapper mc, net.minecraft.core.BlockPos pos) {
        int railSlot = findItemSlot(mc, net.minecraft.world.item.Items.RAIL);
        int cartSlot = findCartSlot(mc);
        if (railSlot == -1 || cartSlot == -1) return;
        long now = System.currentTimeMillis();
        if (now - lastPlaceTime < 100) return;
        lastPlaceTime = now;
            originalSlot = InventoryUtility.getSelectedSlot(mc.getPlayer());
        RaveX.LOGGER.info("[AutoCart] Placing at {}", pos);
        if (rotate) {
            faceBlock(mc, pos);
        }
        selectSlot(railSlot, mc);
        useItemOn(mc, pos, net.minecraft.core.Direction.UP);
        net.minecraft.core.BlockPos above = pos.above();
        if (rotate) {
            faceBlock(mc, above);
        }
        selectSlot(cartSlot, mc);
        useItemOn(mc, above, net.minecraft.core.Direction.UP);
            if (originalSlot != -1) selectSlot(originalSlot, mc);
        lastPlacedPos = pos;
        repeatTimer = 0;
    }
    private boolean shouldPlaceAgain(MinecraftWrapper mc, net.minecraft.core.BlockPos pos) {
        net.minecraft.world.level.block.state.BlockState state = mc.getLevel().getBlockState(pos);
        if (state.isAir()) return true;
        net.minecraft.core.BlockPos above = pos.above();
        net.minecraft.world.level.block.state.BlockState aboveState = mc.getLevel().getBlockState(above);
        return !aboveState.isAir();
    }
    @Nullable
    private net.minecraft.core.BlockPos simulateTrajectory(MinecraftWrapper mc, net.minecraft.world.phys.Vec3 startPos, net.minecraft.world.phys.Vec3 startVel) {
        net.minecraft.world.phys.Vec3 pos = startPos;
        net.minecraft.world.phys.Vec3 vel = startVel;
        double gravity = 0.05;
        double drag = 0.99;
        for (int i = 0; i < 500; i++) {
            vel = vel.scale(drag);
            vel = vel.add(0, -gravity, 0);
            pos = pos.add(vel);
            net.minecraft.core.BlockPos bp = net.minecraft.core.BlockPos.containing(pos);
            if (!mc.getLevel().isLoaded(bp)) return null;
            net.minecraft.world.level.block.state.BlockState state = mc.getLevel().getBlockState(bp);
            if (!state.isAir() && !state.canBeReplaced()) {
                return bp;
            }
            if (pos.y < mc.getLevel().getMinY()) return null;
        }
        return null;
    }
    private int findItemSlot(MinecraftWrapper mc, net.minecraft.world.item.Item item) {
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(mc.getPlayer(), i);
            if (stack.is(item)) return i;
        }
        for (int i = 9; i < 36; i++) {
            var stack = InventoryUtility.getItem(mc.getPlayer(), i);
            if (stack.is(item)) {
                int freeSlot = findEmptySlot(mc);
                if (freeSlot != -1) {
                    mc.getGameMode().handleInventoryMouseClick(
                        mc.getPlayer().containerMenu.containerId,
                        i,
                        freeSlot,
                        InventoryUtility.SWAP,
                        mc.getPlayer()
                    );
                    return freeSlot;
                }
            }
        }
        return -1;
    }
    private int findCartSlot(MinecraftWrapper mc) {
        net.minecraft.world.item.Item targetItem = switch (cartType) {
            case "Chest" -> net.minecraft.world.item.Items.CHEST_MINECART;
            case "Furnace" -> net.minecraft.world.item.Items.FURNACE_MINECART;
            case "Hopper" -> net.minecraft.world.item.Items.HOPPER_MINECART;
            default -> net.minecraft.world.item.Items.TNT_MINECART;
        };
        return findItemSlot(mc, targetItem);
    }
    private int findEmptySlot(MinecraftWrapper mc) {
        for (int i = 0; i < 9; i++) {
            if (InventoryUtility.getItem(mc.getPlayer(), i).isEmpty()) return i;
        }
        return -1;
    }
    private void faceBlock(MinecraftWrapper mc, net.minecraft.core.BlockPos pos) {
        String mode = bypass;
        net.minecraft.world.phys.Vec3 target = PhysicUtility.centerOf(pos);
        float[] targetAngles = RotationUtility.anglesTo(mc.getPlayer().getEyePosition(), target);

        if ("NCP".equals(mode)) {
            float currentYaw = mc.getPlayer().getYRot();
            float currentPitch = mc.getPlayer().getXRot();
            if (!silentRotation.initialized) silentRotation.init(currentYaw, currentPitch);
            float maxSpeed = (float) rotateSpeed;
            float[] limited = AimUtility.limitAngles(silentRotation.lastYaw, targetAngles[0], silentRotation.lastPitch, targetAngles[1], maxSpeed);
            float finalYaw = RotationUtility.fixAngle(limited[0]);
            float finalPitch = RotationUtility.fixAngle(limited[1]);
            silentRotation.set(finalYaw, finalPitch);
            silentRotation.lastYaw = finalYaw;
            silentRotation.lastPitch = finalPitch;
        } else if ("Legit".equals(mode)) {
            float currentYaw = mc.getPlayer().getYRot();
            float currentPitch = mc.getPlayer().getXRot();
            float maxSpeed = (float) rotateSpeed;
            float[] limited = AimUtility.limitAngles(currentYaw, targetAngles[0], currentPitch, targetAngles[1], maxSpeed);
            float yawRand = (float) ((Math.random() - 0.5) * 1.5);
            float pitchRand = (float) ((Math.random() - 0.5) * 0.8);
            float finalYaw = RotationUtility.fixAngle(limited[0] + yawRand);
            float finalPitch = RotationUtility.fixAngle(limited[1] + pitchRand);
            mc.getPlayer().setYRot(finalYaw);
            mc.getPlayer().setXRot(finalPitch);
        } else {
            float yaw = RotationUtility.fixAngle(targetAngles[0]);
            float pitch = RotationUtility.fixAngle(targetAngles[1]);
            mc.getPlayer().setYRot(yaw);
            mc.getPlayer().setXRot(pitch);
        }
    }
    private void selectSlot(int slot, MinecraftWrapper mc) {
        if (swapMode.equals("Silent")) {
            InventoryUtility.silentSelectSlot(mc.getPlayer(), slot);
        } else {
            InventoryUtility.selectSlot(mc.getPlayer(), slot);
        }
    }
    private void useItemOn(MinecraftWrapper mc, net.minecraft.core.BlockPos pos, net.minecraft.core.Direction face) {
        if (mc.getGameMode() == null) return;
        mc.getGameMode().useItemOn(mc.getPlayer(), net.minecraft.world.InteractionHand.MAIN_HAND,
            new net.minecraft.world.phys.BlockHitResult(PhysicUtility.centerOf(pos), face, pos, false));
        SwingUtility.swing(mc.getPlayer(), net.minecraft.world.InteractionHand.MAIN_HAND);
    }
    private boolean canPlaceBlock(net.minecraft.core.BlockPos pos, MinecraftWrapper mc) {
        net.minecraft.world.level.block.state.BlockState state = mc.getLevel().getBlockState(pos);
        return state.isAir() || state.canBeReplaced();
    }
    private boolean isWithinRange(net.minecraft.core.BlockPos pos, MinecraftWrapper mc) {
        net.minecraft.world.phys.Vec3 playerPos = mc.getPlayer().getEyePosition();
        net.minecraft.world.phys.Vec3 targetPos = PhysicUtility.centerOf(pos);
        return playerPos.distanceTo(targetPos) <= range;
    }




}