package ravex.mixin.render;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.manager.NotificationManager;
import ravex.modules.render.NoRender;

@Mixin(Screen.class)
public class MixinScreen {

    @Inject(method = "renderTransparentBackground", at = @At("HEAD"), cancellable = true)
    private void onRenderTransparentBackground(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (NoRender.INSTANCE.getEnabled() && NoRender.INSTANCE.inventoryBackground.getValue()) {
            if ((Object)this instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderTail(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        NotificationManager.renderToasts(guiGraphics);
    }
}
