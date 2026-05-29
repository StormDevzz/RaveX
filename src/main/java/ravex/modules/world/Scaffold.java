package ravex.modules.world;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import java.util.List;

public class Scaffold extends Module {
    public static final Scaffold INSTANCE = new Scaffold();

    public final ModeParameter mode = new ModeParameter("Mode", "Normal", List.of("Normal", "Expand"));
    public final BooleanParameter tower = new BooleanParameter("Tower", true);
    public final BooleanParameter silentRot = new BooleanParameter("Silent Rot", true);
    public final BooleanParameter keepY = new BooleanParameter("Keep Y", false);

    private int lastSlot = -1;
    private double targetY = -1;

    private Scaffold() {
        super("Scaffold", Category.WORLD);
        addParameter(mode);
        addParameter(tower);
        addParameter(silentRot);
        addParameter(keepY);
    }

    @Override
    protected void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            targetY = Math.floor(mc.player.getY());
        } else {
            targetY = -1;
        }
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) return;

        // Reset or adjust Keep Y level when player is on ground or jumps higher
        if (p.onGround()) {
            targetY = Math.floor(p.getY());
        }

        // 1. Jump/Tower Helper
        if (tower.getValue() && mc.options.keyJump.isDown()) {
            p.setDeltaMovement(p.getDeltaMovement().x, 0.42, p.getDeltaMovement().z);
            targetY = Math.floor(p.getY());
        }

        // 2. Select block slot from hotbar
        int slot = findBlockSlot(p);
        if (slot == -1) return;

        // 3. Find placement target block below player
        BlockPos below = BlockPos.containing(
            p.getX(), 
            (keepY.getValue() && targetY != -1) ? (targetY - 1) : (p.getY() - 1), 
            p.getZ()
        );

        if (!isAir(below)) return; // Already placed

        // Try expanding scaffold slightly if moving in Expand mode
        BlockPos targetPos = below;
        if ("Expand".equals(mode.getValue())) {
            double dx = p.getDeltaMovement().x;
            double dz = p.getDeltaMovement().z;
            BlockPos dirOffset = below.offset(
                dx > 0.05 ? 1 : (dx < -0.05 ? -1 : 0),
                0,
                dz > 0.05 ? 1 : (dz < -0.05 ? -1 : 0)
            );
            if (isAir(dirOffset)) {
                targetPos = dirOffset;
            }
        }

        // 4. Find valid adjacent block face to interact with
        BlockPos neighbor = null;
        Direction placeFace = null;
        for (Direction face : Direction.values()) {
            BlockPos side = targetPos.relative(face);
            if (!isAir(side)) {
                neighbor = side;
                placeFace = face.getOpposite();
                break;
            }
        }

        // If no neighbor exists, try looking down/below the neighbor block structure
        if (neighbor == null) {
            neighbor = targetPos.below();
            placeFace = Direction.UP;
        }

        // 5. Silent Rotations
        if (silentRot.getValue()) {
            float[] rots = rotationsTo(neighbor);
            p.setYRot(rots[0]);
            p.setXRot(rots[1]);
        }

        // 6. Vanilla-Like Silent Hand Swap & Interaction
        int prevSlot = p.getInventory().getSelectedSlot();
        p.getInventory().setSelectedSlot(slot);

        Vec3 hitVec = Vec3.atCenterOf(neighbor).add(
            new Vec3(placeFace.getStepX(), placeFace.getStepY(), placeFace.getStepZ()).scale(0.5)
        );

        BlockHitResult blockHit = new BlockHitResult(hitVec, placeFace, neighbor, false);

        mc.gameMode.useItemOn(p, InteractionHand.MAIN_HAND, blockHit);
        p.swing(InteractionHand.MAIN_HAND);

        // Silent swap back
        if (slot != prevSlot) {
            p.getInventory().setSelectedSlot(prevSlot);
        }
    }

    private boolean isAir(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;
        BlockState state = mc.level.getBlockState(pos);
        return state.isAir() || state.getBlock() == Blocks.SNOW || !state.getFluidState().isEmpty();
    }

    private int findBlockSlot(LocalPlayer p) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = p.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem) return i;
        }
        return -1;
    }

    private float[] rotationsTo(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) return new float[]{0, 0};

        Vec3 target = Vec3.atCenterOf(pos);
        double dx = target.x - p.getX();
        double dy = (target.y + 0.5) - (p.getY() + p.getEyeHeight());
        double dz = target.z - p.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, dist));

        return new float[]{yaw, pitch};
    }
}
