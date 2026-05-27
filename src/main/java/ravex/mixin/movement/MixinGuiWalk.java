package ravex.mixin.movement;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.movement.GuiWalk;

/**
 * GuiWalk — keeps movement keys active while a GUI screen is open.
 *
 * Approach: on each tick, if a screen is open, we read the actual GLFW
 * state of each movement key via reflection (to avoid the protected-field
 * access issue) and feed it back through KeyMapping.set().
 *
 * KeyMapping.set() IS accessible because we declared it in the access
 * widener.  The protected field `key` is bypassed by using the existing
 * public KeyMapping.getKey() method available in Fabric-mapped jars.
 */
@Mixin(Minecraft.class)
public abstract class MixinGuiWalk {

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTickGuiWalk(CallbackInfo ci) {
        if (!GuiWalk.INSTANCE.getEnabled()) return;

        Minecraft mc = (Minecraft)(Object)this;
        if (mc.screen == null || mc.player == null || mc.getWindow() == null) return;

        // Re-apply real GLFW key state for every movement binding
        forceBinding(mc, mc.options.keyUp);
        forceBinding(mc, mc.options.keyDown);
        forceBinding(mc, mc.options.keyLeft);
        forceBinding(mc, mc.options.keyRight);
        forceBinding(mc, mc.options.keySprint);
        forceBinding(mc, mc.options.keyShift);
        forceBinding(mc, mc.options.keyJump);
    }

    /**
     * Reads the GLFW key code from a KeyMapping via its public getKey() method,
     * checks whether it is currently held down, and writes that back via the
     * Fabric-accessible KeyMapping.set(key, pressed) method.
     */
    private static void forceBinding(Minecraft mc, KeyMapping mapping) {
        // mapping.key is now accessible via the access widener
        com.mojang.blaze3d.platform.InputConstants.Key boundKey = mapping.key;
        if (boundKey.getType() == com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM) {
            boolean pressed = com.mojang.blaze3d.platform.InputConstants.isKeyDown(
                    mc.getWindow(), boundKey.getValue());
            KeyMapping.set(boundKey, pressed);
        }
    }
}
