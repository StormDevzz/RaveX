package ravex.modules.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import ravex.utility.misc.NativeLoader;

public class AutoDrop extends Module {
    public static final AutoDrop INSTANCE = new AutoDrop();

    public final ModeParameter blockType = new ModeParameter("BlockType", "Gravel",
        java.util.List.of("Gravel", "Anvil", "Sand", "Both"));
    public final ModeParameter target = new ModeParameter("Target", "Self",
        java.util.List.of("Self", "Nearby", "Enemy"));
    public final NumberParameter range = new NumberParameter("Range", 4.0, 1.0, 6.0, 0.5);
    public final NumberParameter dropHeight = new NumberParameter("DropHeight", 3, 2, 6, 1);
    public final BooleanParameter airPlace = new BooleanParameter("AirPlace", true);
    public final BooleanParameter rotate = new BooleanParameter("Rotate", true);
    public final NumberParameter placeDelay = new NumberParameter("Delay", 2, 1, 10, 1);

    private static boolean nativeAvailable = false;
    static {
        try {
            nativeAvailable = NativeLoader.loadLibrary("ravex_autodrop");
        } catch (Throwable t) {
            nativeAvailable = false;
        }
    }

    private int tickCounter = 0;

    private AutoDrop() {
        super("AutoDrop", Category.COMBAT);
        addParameter(blockType);
        addParameter(target);
        addParameter(range);
        addParameter(dropHeight);
        addParameter(airPlace);
        addParameter(rotate);
        addParameter(placeDelay);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        tickCounter++;
        if (tickCounter < placeDelay.getValue().intValue()) return;
        tickCounter = 0;

        Entity targetEntity = findTarget(mc);
        if (targetEntity == null) return;

        int h = dropHeight.getValue().intValue();
        BlockPos placePos = targetEntity.blockPosition().above(h);
        if (!mc.level.getBlockState(placePos).isAir() && !mc.level.getBlockState(placePos).canBeReplaced()) return;

        int slot = findDropBlock(mc);
        if (slot == -1) return;

        int prevSlot = mc.player.getInventory().getSelectedSlot();
        if (slot < 0 || slot > 8) return;
        mc.player.getInventory().setSelectedSlot(slot);

        if (rotate.getValue()) {
            double dx = placePos.getX() + 0.5 - mc.player.getX();
            double dy = placePos.getY() + 0.5 - mc.player.getEyeY();
            double dz = placePos.getZ() + 0.5 - mc.player.getZ();
            Vec3 look = new Vec3(dx, dy, dz).normalize();
            float yaw = (float) Math.toDegrees(Math.atan2(-look.x, look.z));
            float pitch = (float) Math.toDegrees(-Math.asin(look.y));
            mc.player.setYRot(yaw);
            mc.player.setXRot(pitch);
        }

        BlockPos neighbor = null;
        Direction face = Direction.UP;

        if (airPlace.getValue() || mc.level.getBlockState(placePos.below()).isAir()) {
            neighbor = null;
            for (Direction dir : Direction.values()) {
                BlockPos side = placePos.relative(dir);
                if (!mc.level.getBlockState(side).isAir()) {
                    neighbor = side;
                    face = dir.getOpposite();
                    break;
                }
            }
            if (neighbor == null) {
                neighbor = placePos.above();
                face = Direction.DOWN;
            }
        } else {
            neighbor = placePos.below();
            face = Direction.UP;
        }

        Vec3 hitVec = new Vec3(
            neighbor.getX() + 0.5 + face.getStepX() * 0.5,
            neighbor.getY() + 0.5 + face.getStepY() * 0.5,
            neighbor.getZ() + 0.5 + face.getStepZ() * 0.5
        );

        BlockHitResult hit = new BlockHitResult(hitVec, face, neighbor, false);

        if (mc.gameMode != null) {
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
        }

        mc.player.swing(InteractionHand.MAIN_HAND);
        mc.player.getInventory().setSelectedSlot(prevSlot);
    }

    private Entity findTarget(Minecraft mc) {
        String t = target.getValue();
        if (t.equals("Self")) return mc.player;

        if (t.equals("Nearby") || t.equals("Enemy")) {
            Entity best = null;
            double bestDist = range.getValue();
            for (Entity e : mc.level.entitiesForRendering()) {
                if (e == mc.player) continue;
                if (!(e instanceof LivingEntity) || !e.isAlive()) continue;
                double dist = mc.player.distanceTo(e);
                if (dist < bestDist) {
                    bestDist = dist;
                    best = e;
                }
            }
            if (t.equals("Enemy") && best instanceof Player) return best;
            return best;
        }
        return null;
    }

    private int findDropBlock(Minecraft mc) {
        String type = blockType.getValue();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) continue;
            var block = ((BlockItem) stack.getItem()).getBlock();
            if (type.equals("Gravel") && block == Blocks.GRAVEL) return i;
            if (type.equals("Sand") && block == Blocks.SAND) return i;
            if (type.equals("Anvil") && block instanceof net.minecraft.world.level.block.AnvilBlock) return i;
            if (type.equals("Both") && (block instanceof FallingBlock || block instanceof net.minecraft.world.level.block.AnvilBlock)) return i;
        }
        return -1;
    }
}
