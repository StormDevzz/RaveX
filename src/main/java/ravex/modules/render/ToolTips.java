package ravex.modules.render;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import ravex.utility.player.InventoryUtility;

@Module(name = "ToolTips", category = "Render")
public class ToolTips {
    @Parameter(name = "ShulkerReGear")
    public boolean shulkerRegear = true;
    @Parameter(name = "MiddleClickOpen")
    public boolean middleClickOpen = true;
    @Parameter(name = "ShowMaps")
    public boolean showMaps = true;
    @Parameter(name = "ShowBooks")
    public boolean showBooks = true;
    @Parameter(name = "ShowStorage")
    public boolean showStorage = true;
    @Parameter(name = "ShowId")
    public boolean showId = false;
    public net.minecraft.world.item.ItemStack lastStack = net.minecraft.world.item.ItemStack.EMPTY;
    public int lastX;
    public int lastY;

    public boolean isShulker(net.minecraft.world.item.ItemStack stack) {
        return InventoryUtility.isShulkerBox(stack) || stack.has(DataComponents.CONTAINER);
    }
    public boolean hasItems(net.minecraft.world.item.ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        var contents = stack.get(DataComponents.CONTAINER);
        if (contents == null) return false;
        for (ItemStack item : contents.nonEmptyItems()) {
            if (!item.isEmpty()) return true;
        }
        return false;
    }
    public boolean isMap(net.minecraft.world.item.ItemStack stack) {
        return stack.is(net.minecraft.world.item.Items.FILLED_MAP);
    }
    public boolean isBook(net.minecraft.world.item.ItemStack stack) {
        return stack.is(net.minecraft.world.item.Items.WRITTEN_BOOK) || stack.is(net.minecraft.world.item.Items.WRITABLE_BOOK);
    }
    public boolean isBundle(net.minecraft.world.item.ItemStack stack) {
        return stack.has(net.minecraft.core.component.DataComponents.BUNDLE_CONTENTS);
    }

    public boolean onMouseClicked(AbstractContainerScreen<?> screen, int mouseX, int mouseY, int button) { return false; }
    public boolean onMiddleClick(AbstractContainerScreen<?> screen, int mouseX, int mouseY) { return false; }

    public static int getBoxColor(net.minecraft.world.item.ItemStack stack) {
        if (stack.getItem() instanceof net.minecraft.world.item.BlockItem bi && bi.getBlock() instanceof net.minecraft.world.level.block.ShulkerBoxBlock sb && sb.getColor() != null) {
            int[] dye = {0xFFF0F0F0,0xFFF9801D,0xFFE87DFA,0xFF3AB3DA,0xFFFED83D,0xFF80C71F,0xFFF38BAA,0xFF474F52,0xFF9D9D97,0xFF169C9C,0xFF8932B8,0xFF3C44AA,0xFF835432,0xFF5E7C16,0xFFB02E26,0xFF1D1D21};
            return dye[sb.getColor().ordinal()];
        }
        return 0xFF9C7BB8;
    }
    public String idSuffix(net.minecraft.world.item.ItemStack stack) {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    public void onRenderContainer(AbstractContainerScreen<?> screen, GuiGraphics graphics, int mouseX, int mouseY) { }
}