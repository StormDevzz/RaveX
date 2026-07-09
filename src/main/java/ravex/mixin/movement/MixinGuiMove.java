package ravex.mixin.movement;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.movement.GuiMove;

@Mixin(Minecraft.class)
public abstract class MixinGuiMove {

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTickGuiMove(CallbackInfo ci) {
        GuiMove gw = GuiMove.itz();
        if (!gw.getEnabled()) return;

        Minecraft mc = (Minecraft)(Object)this;
        if (mc.screen == null || mc.player == null || mc.getWindow() == null) return;
        if (mc.screen instanceof ChatScreen) return;

        switch (gw.mode.getValue()) {
            case "Grim" -> handleGrim(mc, gw);
            case "NCPStrict" -> handleNCPStrict(mc, gw);
            case "NoClick" -> handleNoClick(mc, gw);
            default -> handleVanilla(mc, gw);
        }
    }

    private void handleVanilla(Minecraft mc, GuiMove gw) {
        forceMoveKeys(mc);
        if (!gw.noJump.getValue()) forceBinding(mc, mc.options.keyJump);
        if (!gw.noSprint.getValue()) forceBinding(mc, mc.options.keySprint);
        if (gw.sneak.getValue()) forceBinding(mc, mc.options.keyShift);
    }

    private void handleNoClick(Minecraft mc, GuiMove gw) {
        forceMoveKeys(mc);
        if (gw.sneak.getValue()) forceBinding(mc, mc.options.keyShift);
    }

    private void handleNCPStrict(Minecraft mc, GuiMove gw) {
        Screen current = mc.screen;
        if (current instanceof AbstractContainerScreen && current != gw.closedScreen) {
            gw.closedScreen = current;
            int id = mc.player.containerMenu.containerId;
            mc.getConnection().send(new ServerboundContainerClosePacket(id));
        }
        forceMoveKeys(mc);
        if (!gw.noJump.getValue()) forceBinding(mc, mc.options.keyJump);
        if (!gw.noSprint.getValue()) forceBinding(mc, mc.options.keySprint);
        if (gw.sneak.getValue()) forceBinding(mc, mc.options.keyShift);
    }

    private void handleGrim(Minecraft mc, GuiMove gw) {
        Screen current = mc.screen;
        if (current instanceof AbstractContainerScreen && current != gw.closedScreen) {
            gw.closedScreen = current;
            int id = mc.player.containerMenu.containerId;
            mc.getConnection().send(new ServerboundContainerClosePacket(id));
            gw.grimCooldown = 2;
        }

        if (gw.grimCooldown > 0) {
            gw.grimCooldown--;
            return;
        }

        forceMoveKeys(mc);
        if (!gw.noJump.getValue()) forceBinding(mc, mc.options.keyJump);
        if (!gw.noSprint.getValue()) forceBinding(mc, mc.options.keySprint);
        if (gw.sneak.getValue()) forceBinding(mc, mc.options.keyShift);
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