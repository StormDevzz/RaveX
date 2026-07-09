package ravex.modules.combat;
<<<<<<< HEAD
import ravex.manager.ModuleManager;
=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
<<<<<<< HEAD
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
=======
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import ravex.modules.Category;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import ravex.utility.nativelib.NativeLibrary;
<<<<<<< HEAD
import ravex.utility.player.InventoryUtility;
public class Burrow extends Module {
=======
public class Burrow extends Module {
    public static final Burrow INSTANCE = new Burrow();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    public final ModeParameter block = new ModeParameter("Block", "Obsidian",
        java.util.List.of("Obsidian", "Cobblestone", "Web", "Anvil"));
    public final BooleanParameter autoCenter = new BooleanParameter("AutoCenter", true);
    public final BooleanParameter rotate = new BooleanParameter("Rotate", true);
    public final BooleanParameter instant = new BooleanParameter("Instant", true);
    public final NumberParameter height = new NumberParameter("Height", 0.42, 0.2, 1.0, 0.01);
    public final NumberParameter delay = new NumberParameter("Delay", 0, 0, 5, 1);
    private static final NativeLibrary NATIVE = NativeLibrary.of("ravex_burrow");
    static {
        NATIVE.load();
    }
    private int tickCounter = 0;
    private boolean hasPlaced = false;

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (hasPlaced) return;
        tickCounter++;
        if (tickCounter < delay.getValue().intValue()) return;
        BlockPos headPos = mc.player.blockPosition();
        if (!mc.level.getBlockState(headPos).isAir() && !mc.level.getBlockState(headPos).canBeReplaced()) return;
        int slot = findBlockSlot(mc);
        if (slot == -1) return;
        if (autoCenter.getValue()) {
            double centerX = Math.floor(mc.player.getX()) + 0.5;
            double centerZ = Math.floor(mc.player.getZ()) + 0.5;
            mc.player.setPos(centerX, mc.player.getY(), centerZ);
            if (mc.player.connection != null) {
                mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(centerX, mc.player.getY(), centerZ, mc.player.onGround(), false));
            }
        }
        if (instant.getValue()) {
            double h = height.getValue();
            Vec3 orig = mc.player.position();
            mc.player.setPos(orig.x, orig.y + h, orig.z);
            if (mc.player.connection != null) {
                mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(orig.x, orig.y + h, orig.z, false, false));
            }
        }
<<<<<<< HEAD
        int prevSlot = InventoryUtility.getSelectedSlot(mc.player);
        if (slot < 0 || slot > 8) return;
        InventoryUtility.selectSlot(mc.player, slot);
=======
        int prevSlot = mc.player.getInventory().getSelectedSlot();
        if (slot < 0 || slot > 8) return;
        mc.player.getInventory().setSelectedSlot(slot);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        BlockHitResult hit = new BlockHitResult(
            new Vec3(headPos.getX() + 0.5, headPos.getY() + 2, headPos.getZ() + 0.5),
            Direction.DOWN, headPos, false
        );
        if (mc.gameMode != null) {
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
        }
        mc.player.swing(InteractionHand.MAIN_HAND);
<<<<<<< HEAD
        InventoryUtility.selectSlot(mc.player, prevSlot);
=======
        mc.player.getInventory().setSelectedSlot(prevSlot);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        hasPlaced = true;
    }
    @Override
    protected void onDisable() {
        hasPlaced = false;
        tickCounter = 0;
    }
    private int findBlockSlot(Minecraft mc) {
        String b = block.getValue();
        for (int i = 0; i < 9; i++) {
<<<<<<< HEAD
            var stack = InventoryUtility.getItem(mc.player, i);
=======
            ItemStack stack = mc.player.getInventory().getItem(i);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) continue;
            var blk = ((BlockItem) stack.getItem()).getBlock();
            if (b.equals("Obsidian") && blk == Blocks.OBSIDIAN) return i;
            if (b.equals("Cobblestone") && blk == Blocks.COBBLESTONE) return i;
            if (b.equals("Web") && blk == Blocks.COBWEB) return i;
            if (b.equals("Anvil") && blk instanceof net.minecraft.world.level.block.AnvilBlock) return i;
        }
        return -1;
    }
    private static native double[] nativeCalculate(double px, double py, double pz, double height, boolean autoCenter);
<<<<<<< HEAD
    public static boolean maybeEnabled() {
        return maybeEnabled(Burrow.class);
    }
    public static Burrow itz() {
        return ModuleManager.get(Burrow.class);
    }

}
=======
}
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
