package ravex.modules.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import ravex.RaveX;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;

public class AutoCart extends Module {
    public static final AutoCart INSTANCE = new AutoCart();

    public final NumberParameter range = new NumberParameter("Range", 6, 1, 10, 1);
    public final BooleanParameter rotate = new BooleanParameter("Rotate", true);

    private boolean wasUsingBow = false;
    private int lastBowCharge = 0;

    private AutoCart() {
        super("AutoCart", Category.COMBAT);
        addParameter(range);
        addParameter(rotate);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        boolean isUsingBow = mc.player.isUsingItem() && mc.player.getUseItem().is(Items.BOW);

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
        if (dist > range.getValue()) return;

        int railSlot = -1;
        int cartSlot = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.is(Items.RAIL)) railSlot = i;
            if (stack.is(Items.TNT_MINECART)) cartSlot = i;
        }
        if (railSlot == -1 || cartSlot == -1) return;

        RaveX.LOGGER.info("[AutoCart] Placing at {}", landingPos);

        int prevSlot = mc.player.getInventory().getSelectedSlot();

        faceBlock(mc, landingPos);
        selectSlot(railSlot, mc);
        useItemOn(mc, landingPos, Direction.UP);

        faceBlock(mc, landingPos.above());
        selectSlot(cartSlot, mc);
        useItemOn(mc, landingPos.above(), Direction.UP);

        selectSlot(prevSlot, mc);
    }

    private BlockPos simulateTrajectory(Minecraft mc, Vec3 startPos, Vec3 startVel) {
        Vec3 pos = startPos;
        Vec3 vel = startVel;
        Level level = mc.level;

        for (int i = 0; i < 500; i++) {
            Vec3 newPos = pos.add(vel);
            double maxDist = vel.length();
            int subSteps = Math.max(1, (int) Math.ceil(maxDist));
            for (int j = 0; j < subSteps; j++) {
                Vec3 checkPos = pos.add(vel.scale((j + 1.0) / subSteps));
                BlockPos bp = BlockPos.containing(checkPos);
                if (!level.isLoaded(bp)) return null;
                BlockState state = level.getBlockState(bp);
                if (!state.isAir()) return bp;
            }
            pos = newPos;
            vel = vel.scale(0.99);
            vel = vel.add(0, -0.05, 0);
            if (pos.y < level.getMinY()) return null;
        }
        return null;
    }

    private void faceBlock(Minecraft mc, BlockPos pos) {
        if (!rotate.getValue()) return;
        Vec3 target = Vec3.atCenterOf(pos);
        Vec3 eyes = mc.player.getEyePosition();
        double dx = target.x - eyes.x;
        double dy = target.y - eyes.y;
        double dz = target.z - eyes.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        mc.player.setYRot((float) Math.toDegrees(Math.atan2(dz, dx)) - 90f);
        mc.player.setXRot((float) -Math.toDegrees(Math.atan2(dy, dist)));
    }

    private void selectSlot(int slot, Minecraft mc) {
        mc.player.getInventory().setSelectedSlot(slot);
    }

    private void useItemOn(Minecraft mc, BlockPos pos, Direction face) {
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), face, pos, false));
    }
}
