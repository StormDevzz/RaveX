package ravex.mixin.render;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.event.EventBusHolder;
import ravex.event.client.ScreenEvent;
import ravex.manager.NotificationManager;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.render.NoRender;
import ravex.modules.Modules;
import ravex.modules.client.MainMenu;

@Mixin(Screen.class)
public class MixinScreen {

    private static boolean ravex$menuBackgroundActive() {
        if (MinecraftWrapper.getWrapper().getLevel() != null) return false;
        MainMenu menu = Modules.get(MainMenu.class);
        return menu != null && Modules.enabled(MainMenu.class) && menu.menuBackground;
    }

    @Inject(method = "renderPanorama", at = @At("HEAD"), cancellable = true)
    private void onRenderPanorama(GuiGraphics guiGraphics, float partialTick, CallbackInfo ci) {
        if (!ravex$menuBackgroundActive()) return;
        ci.cancel();
        ravex.utility.render.MenuBackgroundUtility.render(guiGraphics);
    }

    @Inject(method = "renderBlurredBackground", at = @At("HEAD"), cancellable = true)
    private void onRenderBlurredBackground(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (ravex$menuBackgroundActive()) ci.cancel();
    }

    @Inject(method = "renderMenuBackground(Lnet/minecraft/client/gui/GuiGraphics;)V", at = @At("HEAD"), cancellable = true)
    private void onRenderMenuBackground(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (ravex$menuBackgroundActive()) ci.cancel();
    }

    @Inject(method = "renderMenuBackground(Lnet/minecraft/client/gui/GuiGraphics;IIII)V", at = @At("HEAD"), cancellable = true)
    private void onRenderMenuBackgroundRegion(GuiGraphics guiGraphics, int x, int y, int width, int height, CallbackInfo ci) {
        if (ravex$menuBackgroundActive()) ci.cancel();
    }

    @Inject(method = "renderTransparentBackground", at = @At("HEAD"), cancellable = true)
    private void onRenderTransparentBackground(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (Modules.enabled(NoRender.class) && Modules.get(NoRender.class).inventoryBackground) {
            if ((Object)this instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderTail(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        NotificationManager.renderToasts(guiGraphics);

        if ((Object)this instanceof net.minecraft.client.gui.screens.ChatScreen) {
            var drag = ravex.modules.client.Hud.draggingHud;
            if (drag != null) {
                int nx = mouseX - ravex.modules.client.Hud.dragOffX;
                int ny = mouseY - ravex.modules.client.Hud.dragOffY;
                nx = Math.max(0, Math.min(guiGraphics.guiWidth() - Math.round(drag.getWidth() * drag.getUserScale()), nx));
                ny = Math.max(0, Math.min(guiGraphics.guiHeight() - Math.round(drag.getHeight() * drag.getUserScale()), ny));
                drag.setX(nx);
                drag.setY(ny);
                float follow = 0.5f;
                float dx = drag.getDisplayX() + (nx - drag.getDisplayX()) * follow;
                float dy = drag.getDisplayY() + (ny - drag.getDisplayY()) * follow;
                if (Math.abs(dx - nx) < 0.4f) dx = nx;
                if (Math.abs(dy - ny) < 0.4f) dy = ny;
                drag.setDisplayX(dx);
                drag.setDisplayY(dy);
                drag.setHudPositionCustomized(true);
            }
            int activeColor = ravex.utility.render.ColorUtility.getActiveColor();
            for (var hm : ravex.manager.ModuleManager.INSTANCE.getHudModules()) {
                if (!hm.getEnabled()) continue;
                int x1 = hm.getX(), y1 = hm.getY();
                int x2 = x1 + Math.round(hm.getWidth() * hm.getUserScale());
                int y2 = y1 + Math.round(hm.getHeight() * hm.getUserScale());
                boolean hov = mouseX >= x1 && mouseX <= x2 && mouseY >= y1 && mouseY <= y2;
                if (hov || hm == drag) {
                    guiGraphics.fill(x1 - 1, y1 - 1, x2 + 1, y2 + 1, ravex.utility.render.ColorUtility.withAlpha(activeColor, 35));
                    ravex.utility.render.Render2DUtility.drawRoundBorder(guiGraphics, x1 - 1, y1 - 1, x2 - x1 + 2, y2 - y1 + 2, 2, 1, activeColor);
                }
            }
        }
    }

    @Inject(method = "<init>(Lnet/minecraft/network/chat/Component;)V", at = @At("TAIL"))
    private void onScreenOpen(CallbackInfo ci) {
        EventBusHolder.get().post(new ScreenEvent(ScreenEvent.ScreenAction.OPEN, (Screen)(Object)this));
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void onScreenClose(CallbackInfo ci) {
        if ((Object)this instanceof net.minecraft.client.gui.screens.ChatScreen) {
            ravex.modules.client.Hud.draggingHud = null;
            ravex.manager.ConfigManager.INSTANCE.save("default");
        }
        ravex.utility.misc.CursorUtility.reset();
        EventBusHolder.get().post(new ScreenEvent(ScreenEvent.ScreenAction.CLOSE, (Screen)(Object)this));
    }
}
