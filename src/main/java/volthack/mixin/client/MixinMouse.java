package volthack.mixin.client;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import volthack.manager.InputManager;

@Mixin(MouseHandler.class)
public class MixinMouse {
    @Inject(method = "method_22684", at = @At("HEAD"))
    private void onMouseRaw(long window, int button, int action, int mods, CallbackInfo ci) {
        InputManager.INSTANCE.onMouse(button, action);
    }

    @Inject(method = "onScroll", at = @At("HEAD"))
    private void onScrollRaw(long window, double xoffset, double yoffset, CallbackInfo ci) {
        InputManager.INSTANCE.onScroll(yoffset);
    }
}
