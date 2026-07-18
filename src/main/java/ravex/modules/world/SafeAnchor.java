package ravex.modules.world;

import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.rotation.RotationUtility;
import ravex.utility.player.rotation.SilentRotation;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import java.util.List;

public class SafeAnchor extends Module {
    public final ModeParameter rotate = new ModeParameter("Rotate", "Normal", List.of("Silent", "Normal", "None"));
    public final NumberParameter range = new NumberParameter("Range", 4.5, 1.0, 6.0, 0.1);
    public final BooleanParameter autoCharge = new BooleanParameter("AutoCharge", true);
    public final BooleanParameter placeGlowstone = new BooleanParameter("PlaceGlowstone", true);
    public final BooleanParameter autoTrigger = new BooleanParameter("AutoTrigger", true);

    public static final SilentRotation silentRotation = new SilentRotation();
    private long lastActionTime = 0;
    private int stage = 0;
    private BlockPos anchorPos = null;

    public SafeAnchor() {
    }

    public static boolean maybeEnabled() {
        return maybeEnabled(SafeAnchor.class);
    }
    public static SafeAnchor itz() {
        return ModuleManager.get(SafeAnchor.class);
    }
    public static boolean hasSilentRotations() {
        return silentRotation.hasRotation;
    }

    @Override
    protected void onEnable() {
        lastActionTime = 0;
        stage = 0;
        anchorPos = null;
        silentRotation.reset();
    }

    @Override
    protected void onDisable() {
        silentRotation.reset();
        anchorPos = null;
        stage = 0;
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;

        long now = System.currentTimeMillis();
        if (now - lastActionTime < 150) return;

        if (anchorPos == null || stage == 0) {
            anchorPos = findNearbyAnchor(mc);
            if (anchorPos == null) return;
            stage = 1;
        }

        BlockState state = mc.level.getBlockState(anchorPos);
        if (!state.is(Blocks.RESPAWN_ANCHOR)) {
            anchorPos = null;
            stage = 0;
            return;
        }

        int charges = getAnchorCharges(state);

        switch (stage) {
            case 1:
                if (charges < 1 && autoCharge.getValue()) {
                    int glowstoneSlot = InventoryUtility.findHotbarSlot(mc.player, "glowstone");
                    if (glowstoneSlot == -1) {
                        glowstoneSlot = InventoryUtility.findSlot(mc.player, "glowstone", 9, 36);
                        if (glowstoneSlot == -1) return;
                        int hotbarSlot = InventoryUtility.getSelectedSlot(mc.player);
                        InventoryUtility.handleInventoryClick(mc, mc.player, glowstoneSlot, hotbarSlot, net.minecraft.world.inventory.ClickType.SWAP);
                        glowstoneSlot = hotbarSlot;
                    }
                    useItemOnBlock(mc, glowstoneSlot, anchorPos, Direction.UP, Vec3.atCenterOf(anchorPos));
                    stage = 2;
                } else {
                    stage = 2;
                }
                break;

            case 2:
                if (placeGlowstone.getValue()) {
                    BlockPos placePos = findGlowstonePlacePos(mc);
                    if (placePos != null) {
                        int glowstoneBlockSlot = InventoryUtility.findHotbarSlot(mc.player, "glowstone");
                        if (glowstoneBlockSlot == -1) {
                            glowstoneBlockSlot = InventoryUtility.findSlot(mc.player, "glowstone", 9, 36);
                            if (glowstoneBlockSlot != -1) {
                                int hotbarSlot = InventoryUtility.getSelectedSlot(mc.player);
                                InventoryUtility.handleInventoryClick(mc, mc.player, glowstoneBlockSlot, hotbarSlot, net.minecraft.world.inventory.ClickType.SWAP);
                                glowstoneBlockSlot = hotbarSlot;
                            }
                        }
                        if (glowstoneBlockSlot != -1) {
                            Vec3 center = Vec3.atCenterOf(placePos);
                            BlockPos neighbor = findPlaceNeighbor(mc, placePos);
                            if (neighbor != null) {
                                Direction face = Direction.UP;
                                for (Direction d : Direction.values()) {
                                    if (placePos.relative(d).equals(neighbor)) {
                                        face = d.getOpposite();
                                        break;
                                    }
                                }
                                Vec3 hitVec = center.add(new Vec3(face.getStepX(), face.getStepY(), face.getStepZ()).scale(0.5));
                                useItemOnBlock(mc, glowstoneBlockSlot, neighbor, face, hitVec);
                            }
                        }
                    }
                }
                stage = 3;
                break;

            case 3:
                if (autoTrigger.getValue() && charges >= 1) {
                    int swordSlot = findSwordSlot(mc);
                    if (swordSlot != -1) {
                        useItemOnBlock(mc, swordSlot, anchorPos, Direction.UP, Vec3.atCenterOf(anchorPos));
                    }
                }
                stage = 4;
                break;

            case 4:
                anchorPos = null;
                stage = 0;
                break;
        }
    }

    private BlockPos findNearbyAnchor(Minecraft mc) {
        BlockPos pPos = mc.player.blockPosition();
        double maxDist = range.getValue();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        int r = (int) Math.ceil(maxDist);
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    BlockPos pos = pPos.offset(dx, dy, dz);
                    if (mc.level.getBlockState(pos).is(Blocks.RESPAWN_ANCHOR)) {
                        double dist = Math.sqrt(pos.distToCenterSqr(mc.player.getX(), mc.player.getEyeY(), mc.player.getZ()));
                        if (dist <= maxDist && dist < bestDist) {
                            bestDist = dist;
                            best = pos;
                        }
                    }
                }
            }
        }
        return best;
    }

    private BlockPos findGlowstonePlacePos(Minecraft mc) {
        BlockPos pPos = mc.player.blockPosition();
        Direction[] dirs = {mc.player.getDirection(), mc.player.getDirection().getClockWise(), mc.player.getDirection().getCounterClockWise()};
        for (Direction dir : dirs) {
            BlockPos pos = pPos.relative(dir);
            if (mc.level.getBlockState(pos).isAir() && mc.level.getBlockState(pos.below()).isSolid()) {
                return pos;
            }
        }
        BlockPos front = pPos.relative(mc.player.getDirection());
        if (mc.level.getBlockState(front).isAir()) {
            return front;
        }
        return null;
    }

    private BlockPos findPlaceNeighbor(Minecraft mc, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockPos n = pos.relative(dir);
            if (!mc.level.getBlockState(n).isAir()) {
                return n;
            }
        }
        return pos.below();
    }

    private int findSwordSlot(Minecraft mc) {
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(mc.player, i);
            if (InventoryUtility.isSwordItem(stack)) return i;
        }
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(mc.player, i);
            if (!stack.isEmpty() && !stack.is(Items.GLOWSTONE) && !stack.is(Items.RESPAWN_ANCHOR)) return i;
        }
        return -1;
    }

    private void useItemOnBlock(Minecraft mc, int slot, BlockPos targetBlock, Direction face, Vec3 hitVec) {
        int originalSlot = InventoryUtility.getSelectedSlot(mc.player);
        String rotMode = rotate.getValue();

        if (!rotMode.equals("None")) {
            float[] angles = RotationUtility.anglesTo(mc.player.getEyePosition(), hitVec);
            if (rotMode.equals("Normal")) {
                mc.player.setYRot(angles[0]);
                mc.player.setXRot(angles[1]);
            } else if (rotMode.equals("Silent")) {
                silentRotation.set(angles[0], angles[1]);
            }
        }

        InventoryUtility.selectSlot(mc.player, slot);
        BlockHitResult hitResult = new BlockHitResult(hitVec, face, targetBlock, false);
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hitResult);
        mc.player.swing(InteractionHand.MAIN_HAND);
        lastActionTime = System.currentTimeMillis();

        if (slot != originalSlot) {
            InventoryUtility.selectSlot(mc.player, originalSlot);
        }
    }

    private int getAnchorCharges(BlockState state) {
        for (var prop : state.getProperties()) {
            if (prop.getName().equals("charges") && prop instanceof net.minecraft.world.level.block.state.properties.IntegerProperty intProp) {
                return state.getValue(intProp);
            }
        }
        return 0;
    }
}
