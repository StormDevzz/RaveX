package ravex.mixin.render;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
<<<<<<< HEAD
import ravex.event.EventBusHolder;
import ravex.event.client.ScreenEvent;
import ravex.manager.NotificationManager;
=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import ravex.modules.render.NoRender;

@Mixin(Screen.class)
public class MixinScreen {

    @Inject(method = "renderTransparentBackground", at = @At("HEAD"), cancellable = true)
    private void onRenderTransparentBackground(GuiGraphics guiGraphics, CallbackInfo ci) {
<<<<<<< HEAD
        if (NoRender.maybeEnabled() && NoRender.itz().inventoryBackground.getValue()) {
=======
        if (NoRender.INSTANCE.getEnabled() && NoRender.INSTANCE.inventoryBackground.getValue()) {
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            if ((Object)this instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen) {
                ci.cancel();
            }
        }
    }
<<<<<<< HEAD

    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderTail(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        NotificationManager.renderToasts(guiGraphics);
    }

    @Inject(method = "<init>(Lnet/minecraft/network/chat/Component;)V", at = @At("TAIL"))
    private void onScreenOpen(CallbackInfo ci) {
        EventBusHolder.get().post(new ScreenEvent(ScreenEvent.ScreenAction.OPEN, (Screen)(Object)this));
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void onScreenClose(CallbackInfo ci) {
        EventBusHolder.get().post(new ScreenEvent(ScreenEvent.ScreenAction.CLOSE, (Screen)(Object)this));
    }
=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
}
