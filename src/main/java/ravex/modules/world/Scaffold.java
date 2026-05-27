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

    public final ModeParameter    mode      = new ModeParameter("Mode", "Normal",
            List.of("Normal", "Expand", "Spoof"));
    public final BooleanParameter tower     = new BooleanParameter("Tower", true);
    public final BooleanParameter silentRot = new BooleanParameter("Silent Rot", true);

    private int lastSlot = -1;
    private boolean placed = false;

    private Scaffold() {
        super("Scaffold", Category.WORLD);
        addParameter(mode);
        addParameter(tower);
        addParameter(silentRot);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) return;

        if (tower.getValue() && mc.options.keyJump.isDown() && !p.onGround()) {
            p.setDeltaMovement(p.getDeltaMovement().x, 0.42, p.getDeltaMovement().z);
        }

        BlockPos target = findTargetBlock(p);
        if (target == null) return;

        int slot = findBlockSlot(p);
        if (slot == -1) return;

        int prev = p.getInventory().getSelectedSlot();
        p.getInventory().setSelectedSlot(slot);

        Vec3 placePos = Vec3.atCenterOf(target);

        Direction dir = Direction.DOWN;
        Vec3 hitPos = new Vec3(0.5, 0.0, 0.5);

        BlockHitResult hit = new BlockHitResult(
            placePos.add(hitPos), dir, target.relative(dir.getOpposite()), false
        );

        if (silentRot.getValue()) {
            float[] rots = rotationsTo(target);
            p.setYRot(rots[0]);
            p.setXRot(rots[1]);
        }

        mc.gameMode.useItemOn(p, InteractionHand.MAIN_HAND, hit);
        p.swing(InteractionHand.MAIN_HAND);

        if (slot != prev) {
            p.getInventory().setSelectedSlot(prev);
        }

        placed = true;
    }

    private BlockPos findTargetBlock(LocalPlayer p) {
        BlockPos below = BlockPos.containing(p.getX(), p.getY() - 1, p.getZ());
        if (isAir(below)) return below;

        if ("Expand".equals(mode.getValue())) {
            BlockPos[] checks = {
                below.north(), below.south(), below.east(), below.west(),
                below.north().east(), below.north().west(),
                below.south().east(), below.south().west()
            };
            for (BlockPos bp : checks) {
                if (isAir(bp)) return bp;
            }
        }

        return null;
    }

    private boolean isAir(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;
        BlockState state = mc.level.getBlockState(pos);
        return state.isAir() || state.getBlock() == Blocks.SNOW;
    }

    private int findBlockSlot(LocalPlayer p) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = p.getInventory().getItem(i);
            if (stack.getItem() instanceof BlockItem && !stack.isEmpty()) return i;
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
