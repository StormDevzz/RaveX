package ravex.mixin.client;

import net.minecraft.client.MouseHandler;
import ravex.mcwrapper.MinecraftWrapper;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.client.Hud;
import ravex.modules.movement.GuiMove;
import ravex.modules.Modules;

@Mixin(MouseHandler.class)
public class MixinMouse {
    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void onMouseButton(long window, MouseButtonInfo buttonInfo, int action, CallbackInfo ci) {
        GuiMove gw = Modules.get(GuiMove.class);
        if (!Modules.enabled(GuiMove.class) || !"NoClick".equals(gw.mode)) return;
        var mc = MinecraftWrapper.getInstance();
        if (!(mc.screen instanceof AbstractContainerScreen<?> screen)) return;
        double mx = mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth() / mc.getWindow().getWidth();
        double my = mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight() / mc.getWindow().getHeight();
        for (Slot slot : screen.getMenu().slots) {
            if (mx >= slot.x && mx < slot.x + 18 && my >= slot.y && my < slot.y + 18) {
                ci.cancel();
                return;
            }
        }
    }

    @Inject(method = "onButton", at = @At("TAIL"))
    private void onMouseButtonPost(long window, MouseButtonInfo buttonInfo, int action, CallbackInfo ci) {
        var mc = MinecraftWrapper.getInstance();
        if (mc.player == null) return;

        if (Modules.get(Hud.class).dragEnabled && buttonInfo.button() == 0) {
            if (action == 1) {
                if (mc.screen == null) {
                    double mx = mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth() / mc.getWindow().getWidth();
                    double my = mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight() / mc.getWindow().getHeight();
                    for (var hm : ravex.manager.ModuleManager.INSTANCE.getHudModules()) {
                        if (!hm.getEnabled()) continue;
                        if (mx >= hm.getX() && mx <= hm.getX() + hm.getWidth() &&
                            my >= hm.getY() && my <= hm.getY() + hm.getHeight()) {
                            Hud.draggingHud = hm;
                            Hud.dragOffX = (int)mx - hm.getX();
                            Hud.dragOffY = (int)my - hm.getY();
                            break;
                        }
                    }
                }
            } else if (action == 0) {
                Hud.draggingHud = null;
            }
        }
    }
}
