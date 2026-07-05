package ravex.modules.world;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.FurnaceScreen;
import net.minecraft.client.gui.screens.inventory.BlastFurnaceScreen;
import net.minecraft.client.gui.screens.inventory.SmokerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.state.BlockState;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.NumberParameter;
public class AutoSmelt extends Module {
    public static final AutoSmelt INSTANCE = new AutoSmelt();
    public final NumberParameter range = new NumberParameter("Range", 5.0, 1.0, 6.0, 0.5);
    public final BooleanParameter autoFuel = new BooleanParameter("AutoFuel", true);
    public final BooleanParameter render = new BooleanParameter("Render", true);
    public final ColorParameter color = new ColorParameter("Color", 0x3FFF8800);
    public static BlockPos currentTarget = null;

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;
        if (!(mc.screen instanceof FurnaceScreen)
            && !(mc.screen instanceof BlastFurnaceScreen)
            && !(mc.screen instanceof SmokerScreen)) {
            currentTarget = null;
            return;
        }
        if (currentTarget == null) {
            if (mc.hitResult != null && mc.hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                BlockPos pos = ((net.minecraft.world.phys.BlockHitResult) mc.hitResult).getBlockPos();
                BlockState st = mc.level.getBlockState(pos);
                if (st.getBlock() instanceof AbstractFurnaceBlock) {
                    currentTarget = pos;
                }
            }
        }
        if (!(mc.player.containerMenu instanceof AbstractFurnaceMenu furnace)) {
            currentTarget = null;
            return;
        }
        int containerId = furnace.containerId;
        int playerInvStart = 3;
        int hotbarStart = playerInvStart + 27;
        if (furnace.getSlot(2).hasItem()) {
            mc.gameMode.handleInventoryMouseClick(
                containerId, 2, 0, ClickType.QUICK_MOVE, mc.player
            );
            return;
        }
        if (furnace.getBurnProgress() > 0.01f) return;
        if (!furnace.getSlot(0).hasItem()) {
            for (int i = playerInvStart; i < furnace.slots.size(); i++) {
                ItemStack stack = furnace.slots.get(i).getItem();
                if (stack.isEmpty()) continue;
                if (furnace.canSmelt(stack)) {
                    mc.gameMode.handleInventoryMouseClick(
                        containerId, i, 0, ClickType.QUICK_MOVE, mc.player
                    );
                    return;
                }
            }
            for (int i = hotbarStart; i < furnace.slots.size(); i++) {
                ItemStack stack = furnace.slots.get(i).getItem();
                if (stack.isEmpty()) continue;
                if (furnace.canSmelt(stack)) {
                    mc.gameMode.handleInventoryMouseClick(
                        containerId, i, 0, ClickType.QUICK_MOVE, mc.player
                    );
                    return;
                }
            }
        }
        if (autoFuel.getValue() && !furnace.getSlot(1).hasItem()) {
            for (int i = playerInvStart; i < furnace.slots.size(); i++) {
                ItemStack stack = furnace.slots.get(i).getItem();
                if (stack.isEmpty()) continue;
                if (furnace.isFuel(stack)) {
                    mc.gameMode.handleInventoryMouseClick(
                        containerId, i, 0, ClickType.QUICK_MOVE, mc.player
                    );
                    return;
                }
            }
        }
    }
    @Override
    protected void onDisable() {
        currentTarget = null;
    }
}
