package ravex.modules.combat;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.block.BlockUtility;
import ravex.utility.misc.PhysicUtility;
import ravex.utility.nativelib.NativeLibraryUtility;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.SwingUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.BlockItem;




@ModuleInfo(name = "Burrow", category = "Combat")
public class Burrow implements ModuleAccess {
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
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (hasPlaced) return;
        tickCounter++;
        if (tickCounter < (int) delay) return;
        net.minecraft.core.BlockPos headPos = mc.player.blockPosition();
        if (!mc.level.getBlockState(headPos).isAir() && !mc.level.getBlockState(headPos).canBeReplaced()) return;
        int slot = findBlockSlot(mc);
        if (slot == -1) return;
        if (autoCenter) {
            double centerX = Math.floor(mc.player.getX()) + 0.5;
            double centerZ = Math.floor(mc.player.getZ()) + 0.5;
            mc.player.setPos(centerX, mc.player.getY(), centerZ);
            if (mc.player.connection != null) {
                mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(centerX, mc.player.getY(), centerZ, mc.player.onGround(), false));
            }
        }
        if (instant) {
            double h = height;
            net.minecraft.world.phys.Vec3 orig = mc.player.position();
            mc.player.setPos(orig.x, orig.y + h, orig.z);
            if (mc.player.connection != null) {
                mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(orig.x, orig.y + h, orig.z, false, false));
            }
        }
        int prevSlot = InventoryUtility.getSelectedSlot(mc.player);
        if (slot < 0 || slot > 8) return;
        InventoryUtility.selectSlot(mc.player, slot);
        net.minecraft.world.phys.BlockHitResult hit = new net.minecraft.world.phys.BlockHitResult(
            new net.minecraft.world.phys.Vec3(headPos.getX() + 0.5, headPos.getY() + 2, headPos.getZ() + 0.5),
            net.minecraft.core.Direction.DOWN, headPos, false
        );
        if (mc.gameMode != null) {
            mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
        }
        SwingUtility.swing(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND);
        InventoryUtility.selectSlot(mc.player, prevSlot);
        hasPlaced = true;
    }
    public void onDisable() {
        hasPlaced = false;
        tickCounter = 0;
    }
    private int findBlockSlot(Minecraft mc) {
        String b = block;
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(mc.player, i);
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
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("Burrow").getEnabled();
    }
    public static Burrow itz() {
        return ravex.manager.ModuleManager.delegate(Burrow.class);
    }


}