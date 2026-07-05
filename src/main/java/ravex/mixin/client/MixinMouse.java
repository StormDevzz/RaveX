package ravex.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.movement.GuiWalk;

@Mixin(MouseHandler.class)
public class MixinMouse {
    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void onMouseButton(long window, MouseButtonInfo buttonInfo, int action, CallbackInfo ci) {
        GuiWalk gw = GuiWalk.INSTANCE;
        if (!gw.getEnabled() || !"NoClick".equals(gw.mode.getValue())) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen == null) return;
        ci.cancel();
    }
}
