package ravex.modules.combat;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.NumberParameter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionHand;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public class WitherRoseAura extends Module {
    public static final WitherRoseAura INSTANCE = new WitherRoseAura();

    public final NumberParameter range = new NumberParameter("Range", 4.0, 2.0, 6.0, 0.1);
    public final NumberParameter delayParam = new NumberParameter("Delay", 4.0, 0.0, 20.0, 1.0);

    private int delay = 0;
    private static boolean nativeAvailable = false;

    static {
        try {
            nativeAvailable = ravex.utility.misc.NativeLoader.loadLibrary("ravex_witherroseaura");
        } catch (UnsatisfiedLinkError e) {
            // Fallback to Java
        }
    }

    private static native double[] nativeCalculatePlacement(
        double playerX, double playerY, double playerZ,
        double targetX, double targetY, double targetZ,
        double range,
        boolean targetFeetIsReplaceable,
        boolean supportBlockIsSolid
    );

    private WitherRoseAura() {
        super("WitherRoseAura", Category.COMBAT);
        addParameter(range);
        addParameter(delayParam);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) return;

        if (delay > 0) {
            delay--;
            return;
        }

        double scanRange = range.getValue();

        // Find closest living entity target
        LivingEntity target = null;
        double closest = scanRange;
        for (net.minecraft.world.entity.Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living) || living == p || !living.isAlive()) continue;

            double dist = p.distanceTo(living);
            if (dist < closest) {
                closest = dist;
                target = living;
            }
        }

        if (target == null) return;

        // Find Wither Rose in hotbar
        int roseSlot = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = p.getInventory().getItem(i);
            if (stack.is(Items.WITHER_ROSE)) {
                roseSlot = i;
                break;
            }
        }

        if (roseSlot == -1) return;

        BlockPos targetPos = BlockPos.containing(target.getX(), target.getY(), target.getZ());
        BlockPos supportPos = targetPos.below();

        boolean targetFeetIsReplaceable = mc.level.getBlockState(targetPos).isAir();
        boolean supportBlockIsSolid = mc.level.getBlockState(supportPos).isSolid();

        boolean found = false;
        BlockPos neighbor = null;
        Direction face = Direction.UP;

        if (nativeAvailable) {
            try {
                double[] res = nativeCalculatePlacement(
                    p.getX(), p.getY(), p.getZ(),
                    target.getX(), target.getY(), target.getZ(),
                    scanRange,
                    targetFeetIsReplaceable,
                    supportBlockIsSolid
                );
                if (res != null && res[0] > 0.5) {
                    found = true;
                    neighbor = new BlockPos((int) res[1], (int) res[2], (int) res[3]);
                    face = Direction.values()[(int) res[4]];
                }
            } catch (UnsatisfiedLinkError e) {
                nativeAvailable = false;
            }
        }

        if (!nativeAvailable) {
            // Java fallback
            if (targetFeetIsReplaceable && supportBlockIsSolid && p.distanceTo(target) <= scanRange) {
                found = true;
                neighbor = supportPos;
                face = Direction.UP;
            }
        }

        if (found && neighbor != null) {
            int prevSlot = p.getInventory().getSelectedSlot();

            // Silent packet swap
            if (roseSlot != prevSlot) {
                p.connection.send(new ServerboundSetCarriedItemPacket(roseSlot));
            }

            BlockHitResult hit = new BlockHitResult(
                new Vec3(neighbor.getX() + 0.5, neighbor.getY() + 0.5, neighbor.getZ() + 0.5)
                    .add(new Vec3(face.getStepX(), face.getStepY(), face.getStepZ()).scale(0.5)),
                face,
                neighbor,
                false
            );

            // Use item on the position
            p.connection.send(new ServerboundUseItemOnPacket(InteractionHand.MAIN_HAND, hit, 0));
            p.connection.send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));

            if (roseSlot != prevSlot) {
                p.connection.send(new ServerboundSetCarriedItemPacket(prevSlot));
            }

            delay = delayParam.getValue().intValue();
        }
    }
}
