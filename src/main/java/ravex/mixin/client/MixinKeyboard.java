package ravex.mixin.client;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.Module;
import ravex.modules.ModuleManager;

@Mixin(KeyboardHandler.class)
public class MixinKeyboard {
    @Inject(method = "keyPress", at = @At("HEAD"))
    private void onKeyPress(long window, int key, KeyEvent event, CallbackInfo ci) {
        // Handled in RaveX.onClientTick for maximum reliability in-game!
    }
}
