package ravex.mixin.player;

import net.minecraft.core.BlockPos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ravex.modules.misc.ItemScroller;
import ravex.modules.misc.StashFinder;
import ravex.modules.player.ExtraChest;

@Mixin(AbstractContainerScreen.class)
public abstract class MixinAbstractContainerScreen {

    @Shadow protected Slot hoveredSlot;

    @Invoker("getHoveredSlot")
    public abstract Slot invokeGetHoveredSlot(double mouseX, double mouseY);

    private long scrollerLastTransfer = 0;
    private boolean scrollerWasHolding = false;

    
    
    
    @Inject(method = "mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z",
            at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(MouseButtonEvent event, boolean z, CallbackInfoReturnable<Boolean> cir) {
        if (event.button() != 0) return;
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>)(Object)this;
        if (ExtraChest.INSTANCE.onMouseClicked(screen, (int) event.x(), (int) event.y())) {
            cir.setReturnValue(true);
        }
    }

    
    
    
    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderTail(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        
        if (ItemScroller.INSTANCE.getEnabled()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.player != null && mc.getWindow() != null) {
                long handle = mc.getWindow().handle();
                if (handle != 0) {
                    boolean shift = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                                 || GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
                    boolean lmb = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

                    Slot slot = invokeGetHoveredSlot(mouseX, mouseY);
                    if (slot == null) slot = this.hoveredSlot;

                    if (shift && lmb && slot != null && slot.hasItem()) {
                        long now = System.currentTimeMillis();
                        long delayMs = ItemScroller.INSTANCE.delay.getValue().longValue();
                        if (!scrollerWasHolding || now - scrollerLastTransfer >= delayMs) {
                            scrollerLastTransfer = now;
                            scrollerWasHolding = true;
                            mc.gameMode.handleInventoryMouseClick(
                                ((AbstractContainerScreen<?>)(Object)this).getMenu().containerId,
                                slot.index, 0, ClickType.QUICK_MOVE, mc.player);
                        }
                    } else {
                        scrollerWasHolding = false;
                    }
                }
            }
        }

        
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>)(Object)this;
        ExtraChest.INSTANCE.onRenderButtons(screen, graphics, mouseX, mouseY);

        
        if (StashFinder.INSTANCE.getEnabled()) {
            var menu = screen.getMenu();
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && menu instanceof net.minecraft.world.inventory.ChestMenu) {
                java.util.List<ItemStack> contents = new java.util.ArrayList<>();
                int containerSize = Math.min(menu.slots.size() - 36, 54);
                for (int i = 0; i < containerSize; i++) {
                    contents.add(menu.slots.get(i).getItem());
                }
                BlockPos pos = mc.player.blockPosition();
                StashFinder.INSTANCE.onContainerOpened(pos, contents);
            }
        }
    }

    
    
    
    @Inject(method = "mouseScrolled(DDDD)Z", at = @At("HEAD"), cancellable = true)
    private void onMouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY,
                                  CallbackInfoReturnable<Boolean> cir) {
        if (!ItemScroller.INSTANCE.getEnabled()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || mc.getWindow() == null) return;

        long handle = mc.getWindow().handle();
        if (handle == 0) return;
        boolean shift = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                     || GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        if (!shift) return;

        Slot slot = invokeGetHoveredSlot(mouseX, mouseY);
        if (slot == null) slot = this.hoveredSlot;
        if (slot != null && slot.hasItem()) {
            mc.gameMode.handleInventoryMouseClick(
                ((AbstractContainerScreen<?>)(Object)this).getMenu().containerId,
                slot.index, 0, ClickType.QUICK_MOVE, mc.player);
            cir.setReturnValue(true);
        }
    }
}
