package ravex.modules.player;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;
import ravex.parameter.ModeParameter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SourceFiller extends Module {
    public static final SourceFiller INSTANCE = new SourceFiller();

    public final NumberParameter range = new NumberParameter("Range", 4.5, 1.0, 6.0, 0.1);
    public final ModeParameter mode = new ModeParameter("Mode", "Smart", List.of("Normal", "Smart"));
    public final BooleanParameter silent = new BooleanParameter("Silent Swap", true);
    public final BooleanParameter rotate = new BooleanParameter("Rotate", true);
    public final NumberParameter delay = new NumberParameter("Delay (ms)", 200.0, 0.0, 1000.0, 10.0);

    private long lastPlaceTime = 0;

    private SourceFiller() {
        super("SourceFiller", Category.PLAYER);
        addParameter(range);
        addParameter(mode);
        addParameter(silent);
        addParameter(rotate);
        addParameter(delay);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) return;

        if (System.currentTimeMillis() - lastPlaceTime < delay.getValue()) {
            return;
        }

        int spongeSlot = findSpongeSlot(p);
        if (spongeSlot == -1) return;

        BlockPos targetPos = findTargetWater(p, mc);
        if (targetPos == null) return;

        int prevSlot = p.getInventory().getSelectedSlot();

        
        p.getInventory().setSelectedSlot(spongeSlot);

        Vec3 hitVec = Vec3.atCenterOf(targetPos);
        BlockHitResult hit = new BlockHitResult(hitVec, Direction.UP, targetPos, false);

        if (rotate.getValue()) {
            float[] rots = rotationsTo(targetPos, p);
            p.setYRot(rots[0]);
            p.setXRot(rots[1]);
        }

        mc.gameMode.useItemOn(p, InteractionHand.MAIN_HAND, hit);
        p.swing(InteractionHand.MAIN_HAND);

        if (silent.getValue() && spongeSlot != prevSlot) {
            p.getInventory().setSelectedSlot(prevSlot);
        }

        lastPlaceTime = System.currentTimeMillis();
    }

    private int findSpongeSlot(LocalPlayer p) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = p.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() == Items.SPONGE) {
                return i;
            }
        }
        return -1;
    }

    private BlockPos findTargetWater(LocalPlayer p, Minecraft mc) {
        double r = range.getValue();
        List<BlockPos> candidates = new ArrayList<>();

        int minX = (int) Math.floor(p.getX() - r);
        int maxX = (int) Math.ceil(p.getX() + r);
        int minY = (int) Math.floor(p.getY() - r);
        int maxY = (int) Math.ceil(p.getY() + r);
        int minZ = (int) Math.floor(p.getZ() - r);
        int maxZ = (int) Math.ceil(p.getZ() + r);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos bp = new BlockPos(x, y, z);
                    if (p.getEyePosition().distanceToSqr(Vec3.atCenterOf(bp)) > r * r) {
                        continue;
                    }
                    if (mc.level.getFluidState(bp).is(FluidTags.WATER)) {
                        candidates.add(bp);
                    }
                }
            }
        }

        if (candidates.isEmpty()) return null;

        if ("Smart".equals(mode.getValue())) {
            
            return candidates.stream()
                .max(Comparator.comparingInt(bp -> countAdjacentWater(bp, mc)))
                .orElse(null);
        } else {
            
            return candidates.stream()
                .min(Comparator.comparingDouble(bp -> p.getEyePosition().distanceToSqr(Vec3.atCenterOf(bp))))
                .orElse(null);
        }
    }

    private int countAdjacentWater(BlockPos pos, Minecraft mc) {
        int count = 0;
        for (Direction dir : Direction.values()) {
            if (mc.level.getFluidState(pos.relative(dir)).is(FluidTags.WATER)) {
                count++;
            }
        }
        return count;
    }

    private float[] rotationsTo(BlockPos pos, LocalPlayer p) {
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
