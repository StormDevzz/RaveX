package ravex.mixin.movement;
import ravex.utility.misc.ScreenUtility;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.gui.clickgui.ClickGUI;
import ravex.modules.movement.GuiMove;
import ravex.modules.Modules;
import ravex.mcwrapper.MinecraftWrapper;

@Mixin(Minecraft.class)
public abstract class MixinGuiMove {

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTickGuiMove(CallbackInfo ci) {
        GuiMove gw = Modules.get(GuiMove.class);
        if (!Modules.enabled(GuiMove.class)) return;

        Minecraft mc = (Minecraft)(Object)this;
        if (mc.screen == null || mc.player == null || mc.getWindow() == null) return;
        if (ScreenUtility.isChatScreen(ravex.mcwrapper.MinecraftWrapper.getWrapper())) return;
        if (gw.disableInClickGui && mc.screen instanceof ClickGUI) return;

        if ("NoClick".equals(gw.mode)) {
            handleNoClick(mc, gw);
        } else {
            handleMove(mc, gw);
        }
    }

    // container stays open server side, clicks keep working
    private void handleMove(Minecraft mc, GuiMove gw) {
        forceMoveKeys(mc);
        if (!gw.noJump) forceBinding(mc, mc.options.keyJump);
        if (!gw.noSprint) forceBinding(mc, mc.options.keySprint);
        if (gw.sneak) forceBinding(mc, mc.options.keyShift);
    }

    private void handleNoClick(Minecraft mc, GuiMove gw) {
        forceMoveKeys(mc);
        if (gw.sneak) forceBinding(mc, mc.options.keyShift);
    }

    private static void forceMoveKeys(Minecraft mc) {
        forceBinding(mc, mc.options.keyUp);
        forceBinding(mc, mc.options.keyDown);
        forceBinding(mc, mc.options.keyLeft);
        forceBinding(mc, mc.options.keyRight);
    }

    private static void forceBinding(Minecraft mc, KeyMapping mapping) {
        InputConstants.Key boundKey = mapping.key;
        if (boundKey.getType() == InputConstants.Type.KEYSYM) {
            boolean pressed = InputConstants.isKeyDown(mc.getWindow(), boundKey.getValue());
            KeyMapping.set(boundKey, pressed);
        }
    }
}
