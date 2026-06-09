package ravex.modules.world;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;

public class Igniter extends Module {
    public static final Igniter INSTANCE = new Igniter();

    // ── Параметры ─────────────────────────────────────────────────────────────
    public final NumberParameter  range        = new NumberParameter("Range",        4.0, 1.0, 6.0, 0.1);
    public final ModeParameter    swapMode     = new ModeParameter("Swap Mode", "Silent",
            java.util.List.of("Silent", "Normal", "None"));
    public final BooleanParameter autoDisable  = new BooleanParameter("Auto Disable",  false);
    public final BooleanParameter rotate       = new BooleanParameter("Rotate",       true);

    private Igniter() {
        super("Igniter", Category.WORLD);
        addParameter(range);
        addParameter(swapMode);
        addParameter(autoDisable);
        addParameter(rotate);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;

        BlockPos tntPos = findNearestTNT(mc);
        if (tntPos == null) return;

        int itemSlot = findIgnitionItem(mc);
        if (itemSlot == -1) return;

        // Поворот к блоку TNT
        Vec3 hitVec = Vec3.atCenterOf(tntPos);
        if (rotate.getValue()) {
            rotateTo(mc, hitVec);
        }

        // Выполняем свап
        int originalSlot = mc.player.getInventory().getSelectedSlot();
        String swap = swapMode.getValue();
        if (swap.equals("Normal")) {
            mc.player.getInventory().setSelectedSlot(itemSlot);
        } else if (swap.equals("Silent")) {
            if (mc.player.connection != null) {
                mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket(itemSlot));
            }
        } else if (swap.equals("None")) {
            if (mc.player.getInventory().getSelectedSlot() != itemSlot) {
                return;
            }
        }

        // Поджигаем TNT
        BlockHitResult hitResult = new BlockHitResult(hitVec, Direction.UP, tntPos, false);
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hitResult);
        mc.player.swing(InteractionHand.MAIN_HAND);

        // Возврат слота для Silent swap
        if (swap.equals("Silent") && originalSlot != -1) {
            if (mc.player.connection != null) {
                mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket(originalSlot));
            }
        }

        if (autoDisable.getValue()) {
            setEnabled(false);
        }
    }

    private BlockPos findNearestTNT(Minecraft mc) {
        BlockPos playerPos = mc.player.blockPosition();
        double r = range.getValue();
        int rx = (int) Math.ceil(r);
        int ry = (int) Math.ceil(r);
        int rz = (int) Math.ceil(r);

        BlockPos closest = null;
        double bestDistSqr = Double.MAX_VALUE;

        for (int dx = -rx; dx <= rx; dx++) {
            for (int dy = -ry; dy <= ry; dy++) {
                for (int dz = -rz; dz <= rz; dz++) {
                    BlockPos pos = playerPos.offset(dx, dy, dz);
                    if (mc.level.isLoaded(pos)) {
                        BlockState state = mc.level.getBlockState(pos);
                        if (state.is(Blocks.TNT)) {
                            double distSqr = mc.player.distanceToSqr(Vec3.atCenterOf(pos));
                            if (distSqr <= r * r && distSqr < bestDistSqr) {
                                bestDistSqr = distSqr;
                                closest = pos;
                            }
                        }
                    }
                }
            }
        }
        return closest;
    }

    private int findIgnitionItem(Minecraft mc) {
        // Ищем огниво или огненный шар в хотбаре
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                if (stack.is(Items.FLINT_AND_STEEL) || stack.is(Items.FIRE_CHARGE)) {
                    return i;
                }
            }
        }
        return -1;
    }

    private void rotateTo(Minecraft mc, Vec3 target) {
        Vec3 eyes = mc.player.getEyePosition();
        double dx = target.x - eyes.x;
        double dy = target.y - eyes.y;
        double dz = target.z - eyes.z;
        double dist = Math.sqrt(dx*dx + dz*dz);
        float targetYaw   = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
        float targetPitch = (float) -Math.toDegrees(Math.atan2(dy, dist));

        mc.player.setYRot(targetYaw);
        mc.player.setXRot(targetPitch);
    }
}
