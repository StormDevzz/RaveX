package ravex.modules.combat;
<<<<<<< HEAD
import ravex.manager.ModuleManager;
=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
<<<<<<< HEAD
import net.minecraft.world.item.BlockItem;
import ravex.utility.misc.MobUtility;
=======
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import ravex.utility.nativelib.NativeLibrary;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.rotation.RotationUtility;
import ravex.utility.player.SwingUtility;
public class AutoDrop extends Module {
<<<<<<< HEAD
=======
    public static final AutoDrop INSTANCE = new AutoDrop();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    public final ModeParameter blockType = new ModeParameter("BlockType", "Gravel",
        java.util.List.of("Gravel", "Anvil", "Sand", "Both"));
    public final ModeParameter target = new ModeParameter("Target", "Self",
        java.util.List.of("Self", "Nearby", "Enemy"));
    public final NumberParameter range = new NumberParameter("Range", 4.0, 1.0, 6.0, 0.5);
    public final NumberParameter dropHeight = new NumberParameter("DropHeight", 3, 2, 6, 1);
    public final BooleanParameter airPlace = new BooleanParameter("AirPlace", true);
    public final BooleanParameter rotate = new BooleanParameter("Rotate", true);
    public final NumberParameter placeDelay = new NumberParameter("Delay", 2, 1, 10, 1);
    private static final NativeLibrary NATIVE = NativeLibrary.of("ravex_autodrop");
    static {
        NATIVE.load();
    }
    private int tickCounter = 0;

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        tickCounter++;
        if (tickCounter < placeDelay.getValue().intValue()) return;
        tickCounter = 0;
        Entity targetEntity = findTarget(mc);
        if (targetEntity == null) return;
        BlockPos placePos = targetEntity.blockPosition().above(dropHeight.getValue().intValue());
        if (!mc.level.getBlockState(placePos).isAir() && !mc.level.getBlockState(placePos).canBeReplaced()) return;
        int slot = findDropBlock(mc);
        if (slot == -1) return;
<<<<<<< HEAD
        int prevSlot = InventoryUtility.getSelectedSlot(mc.player);
=======
        int prevSlot = mc.player.getInventory().getSelectedSlot();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        InventoryUtility.selectSlot(mc.player, slot);
        if (rotate.getValue()) {
            Vec3 center = Vec3.atCenterOf(placePos);
            mc.player.setYRot(RotationUtility.yawTo(mc.player, center));
            mc.player.setXRot(RotationUtility.pitchTo(mc.player, center));
        }
        BlockPos neighbor;
        Direction face = Direction.UP;
        if (airPlace.getValue() || mc.level.getBlockState(placePos.below()).isAir()) {
            neighbor = null;
            for (Direction dir : Direction.values()) {
                BlockPos side = placePos.relative(dir);
                if (!mc.level.getBlockState(side).isAir()) { neighbor = side; face = dir.getOpposite(); break; }
            }
            if (neighbor == null) { neighbor = placePos.above(); face = Direction.DOWN; }
        } else { neighbor = placePos.below(); }

        Vec3 hitVec = new Vec3(
            neighbor.getX() + 0.5 + face.getStepX() * 0.5,
            neighbor.getY() + 0.5 + face.getStepY() * 0.5,
            neighbor.getZ() + 0.5 + face.getStepZ() * 0.5
        );
        if (mc.gameMode != null)
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(hitVec, face, neighbor, false));
        SwingUtility.swing(mc.player, InteractionHand.MAIN_HAND);
        InventoryUtility.selectSlot(mc.player, prevSlot);
    }
    private Entity findTarget(Minecraft mc) {
        String t = target.getValue();
        if (t.equals("Self")) return mc.player;
        Entity best = null;
        double bestDist = range.getValue();
        for (Entity e : mc.level.entitiesForRendering()) {
<<<<<<< HEAD
            LivingEntity le = MobUtility.asLivingEntity(e);
            if (le == null || MobUtility.isSelf(le) || !e.isAlive()) continue;
            double dist = MobUtility.distanceToPlayer(e);
            if (dist < bestDist) { bestDist = dist; best = e; }
        }
        if (t.equals("Enemy") && !MobUtility.isPlayer(MobUtility.asLivingEntity(best))) return null;
=======
            if (e == mc.player || !(e instanceof LivingEntity) || !e.isAlive()) continue;
            double dist = mc.player.distanceTo(e);
            if (dist < bestDist) { bestDist = dist; best = e; }
        }
        if (t.equals("Enemy") && !(best instanceof Player)) return null;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        return best;
    }
    private int findDropBlock(Minecraft mc) {
        String type = blockType.getValue();
        for (int i = 0; i < 36; i++) {
<<<<<<< HEAD
            var stack = InventoryUtility.getItem(mc.player, i);
=======
            ItemStack stack = mc.player.getInventory().getItem(i);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) continue;
            var block = ((BlockItem) stack.getItem()).getBlock();
            if (type.equals("Gravel") && block == Blocks.GRAVEL) return i;
            if (type.equals("Sand") && block == Blocks.SAND) return i;
            if (type.equals("Anvil") && block instanceof net.minecraft.world.level.block.AnvilBlock) return i;
            if (type.equals("Both") && (block instanceof FallingBlock || block instanceof net.minecraft.world.level.block.AnvilBlock)) return i;
        }
        return -1;
    }
<<<<<<< HEAD
    public static boolean maybeEnabled() {
        return maybeEnabled(AutoDrop.class);
    }
    public static AutoDrop itz() {
        return ModuleManager.get(AutoDrop.class);
    }

}
=======
}
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
