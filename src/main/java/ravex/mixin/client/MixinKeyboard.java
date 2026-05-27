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
        if (event.input() == 1) { // 1 = GLFW_PRESS
            // Check global modules keybinds!
            for (Module m : ModuleManager.INSTANCE.getModules()) {
                if (m.getKeyBind() != GLFW.GLFW_KEY_UNKNOWN && m.getKeyBind() == event.key()) {
                    m.toggle();
                }
            }

            if (event.key() == GLFW.GLFW_KEY_RIGHT_SHIFT) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    if (mc.screen == null) {
                        mc.setScreen(new ravex.gui.clickgui.ClickGUI());
                    } else if (mc.screen instanceof ravex.gui.clickgui.ClickGUI) {
                        mc.setScreen(null);
                    }
                }
            }
        }
    }
}
