package ravex.modules.combat;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;

import ravex.utility.nativelib.NativeLibraryUtility;
import ravex.utility.network.NetworkUtility;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.SwingUtility;
import net.minecraft.world.item.BlockItem;
import ravex.mcwrapper.MinecraftWrapper;




@Module(name = "Burrow", category = "Combat")
public class Burrow {
    @Parameter(name = "Block", modes = {"Obsidian", "Cobblestone", "Web", "Anvil"})
    public String block = "Obsidian";
    @Parameter(name = "AutoCenter")
    public boolean autoCenter = true;
    @Parameter(name = "Rotate")
    public boolean rotate = true;
    @Parameter(name = "Instant")
    public boolean instant = true;
    @Parameter(name = "Height", min = 0.2, max = 1.0, step = 0.01)
    public double height = 0.42;
    @Parameter(name = "Delay", min = 0, max = 5, step = 1)
    public double delay = 0;
    private static final NativeLibraryUtility NATIVE = NativeLibraryUtility.of("ravex_burrow");
    static {
        NATIVE.load();
    }
    private int tickCounter = 0;
    private boolean hasPlaced = false;
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getLevel() == null) return;
        if (hasPlaced) return;
        tickCounter++;
        if (tickCounter < (int) delay) return;
        net.minecraft.core.BlockPos headPos = mc.getPlayer().blockPosition();
        if (!mc.getLevel().getBlockState(headPos).isAir() && !mc.getLevel().getBlockState(headPos).canBeReplaced()) return;
        int slot = findBlockSlot(mc);
        if (slot == -1) return;
        if (autoCenter) {
            double centerX = Math.floor(mc.getPlayer().getX()) + 0.5;
            double centerZ = Math.floor(mc.getPlayer().getZ()) + 0.5;
            mc.getPlayer().setPos(centerX, mc.getPlayer().getY(), centerZ);
            NetworkUtility.sendMoveRelative(centerX, mc.getPlayer().getY(), centerZ, mc.getPlayer().onGround(), false);
        }
        if (instant) {
            double h = height;
            net.minecraft.world.phys.Vec3 orig = mc.getPlayer().position();
            mc.getPlayer().setPos(orig.x, orig.y + h, orig.z);
            NetworkUtility.sendMoveRelative(orig.x, orig.y + h, orig.z, false, false);
        }
        int prevSlot = InventoryUtility.getSelectedSlot(mc.getPlayer());
        if (slot < 0 || slot > 8) return;
        InventoryUtility.selectSlot(mc.getPlayer(), slot);
        net.minecraft.world.phys.BlockHitResult hit = new net.minecraft.world.phys.BlockHitResult(
            new net.minecraft.world.phys.Vec3(headPos.getX() + 0.5, headPos.getY() + 2, headPos.getZ() + 0.5),
            net.minecraft.core.Direction.DOWN, headPos, false
        );
        if (mc.getGameMode() != null) {
            mc.getGameMode().useItemOn(mc.getPlayer(), net.minecraft.world.InteractionHand.MAIN_HAND, hit);
        }
        SwingUtility.swing(mc.getPlayer(), net.minecraft.world.InteractionHand.MAIN_HAND);
        InventoryUtility.selectSlot(mc.getPlayer(), prevSlot);
        hasPlaced = true;
    }
    public void onDisable() {
        hasPlaced = false;
        tickCounter = 0;
    }
    private int findBlockSlot(MinecraftWrapper mc) {
        String b = block;
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(mc.getPlayer(), i);
            if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) continue;
            var blk = ((BlockItem) stack.getItem()).getBlock();
            if (b.equals("Obsidian") && blk == net.minecraft.world.level.block.Blocks.OBSIDIAN) return i;
            if (b.equals("Cobblestone") && blk == net.minecraft.world.level.block.Blocks.COBBLESTONE) return i;
            if (b.equals("Web") && blk == net.minecraft.world.level.block.Blocks.COBWEB) return i;
            if (b.equals("Anvil") && blk instanceof net.minecraft.world.level.block.AnvilBlock) return i;
        }
        return -1;
    }
    private static native double[] nativeCalculate(double px, double py, double pz, double height, boolean autoCenter);




}