package ravex.mixin.render;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
<<<<<<< HEAD
import ravex.modules.render.Crosshair;
=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3

@Mixin(Gui.class)
public class MixinCrosshair {

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void onRenderCrosshair(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
<<<<<<< HEAD
        if (Crosshair.maybeEnabled()) {
=======
        if (ravex.modules.render.Crosshair.INSTANCE.getEnabled()) {
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            ci.cancel();
        }
    }
}
