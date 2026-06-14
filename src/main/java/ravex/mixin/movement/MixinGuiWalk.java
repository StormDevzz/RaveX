package ravex.mixin.movement;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.movement.GuiWalk;

@Mixin(Minecraft.class)
public abstract class MixinGuiWalk {

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTickGuiWalk(CallbackInfo ci) {
        if (!GuiWalk.INSTANCE.getEnabled()) return;

        Minecraft mc = (Minecraft)(Object)this;
        if (mc.screen == null || mc.player == null || mc.getWindow() == null) return;

        forceBinding(mc, mc.options.keyUp);
        forceBinding(mc, mc.options.keyDown);
        forceBinding(mc, mc.options.keyLeft);
        forceBinding(mc, mc.options.keyRight);
        forceBinding(mc, mc.options.keySprint);
        if (GuiWalk.INSTANCE.sneak.getValue()) {
            forceBinding(mc, mc.options.keyShift);
        }
        forceBinding(mc, mc.options.keyJump);
    }

    private static void forceBinding(Minecraft mc, KeyMapping mapping) {
        InputConstants.Key boundKey = mapping.key;
        if (boundKey.getType() == InputConstants.Type.KEYSYM) {
            boolean pressed = InputConstants.isKeyDown(mc.getWindow(), boundKey.getValue());
            KeyMapping.set(boundKey, pressed);
        }
    }
}
