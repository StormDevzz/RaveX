package ravex.mixin.player;

import net.minecraft.core.BlockPos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.misc.FastItem;
import ravex.modules.misc.StashFinder;
import ravex.modules.player.ChestHelper;
import ravex.utility.player.InventoryUtility;

@Mixin(AbstractContainerScreen.class)
public class MixinAbstractContainerScreen {

    private int lastHoveredSlot = -1;
    private long fastItemLastMove = 0;

    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderTail(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>)(Object)this;
        ChestHelper.itz().onRenderButtons(screen, graphics, mouseX, mouseY);

        if (StashFinder.maybeEnabled()) {
            var menu = screen.getMenu();
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && menu instanceof net.minecraft.world.inventory.ChestMenu) {
                java.util.List<ItemStack> contents = new java.util.ArrayList<>();
                int containerSize = Math.min(menu.slots.size() - 36, 54);
                for (int i = 0; i < containerSize; i++) {
                    contents.add(menu.slots.get(i).getItem());
                }
                BlockPos pos = mc.player.blockPosition();
                StashFinder.itz().onContainerOpened(pos, contents);
            }
        }

        if (FastItem.maybeEnabled()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.gameMode == null) return;
            long handle = GLFW.glfwGetCurrentContext();
            if (handle == 0) return;
            boolean shift = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                         || GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
            boolean lmb = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_1) == GLFW.GLFW_PRESS;
            if (!shift || !lmb) {
                lastHoveredSlot = -1;
                return;
            }
            long delay = FastItem.itz().getDelayMs();
            long now = System.currentTimeMillis();
            if (delay > 0 && now - fastItemLastMove < delay) return;
            Slot slot = ((AccessorContainerScreen)screen).getHoveredSlot();
            if (slot == null) {
                int left = ((AccessorContainerScreen)screen).getLeftPos();
                int top = ((AccessorContainerScreen)screen).getTopPos();
                for (Slot s : screen.getMenu().slots) {
                    if (mouseX >= left + s.x && mouseX < left + s.x + 18
                     && mouseY >= top + s.y && mouseY < top + s.y + 18) {
                        slot = s;
                        break;
                    }
                }
            }
            if (slot == null || !slot.hasItem()) return;
            if (slot.index == lastHoveredSlot && delay > 0) return;
            lastHoveredSlot = slot.index;
            fastItemLastMove = now;
            InventoryUtility.quickMoveSlot(mc, screen.getMenu().containerId, slot.index);
        }
    }
}
