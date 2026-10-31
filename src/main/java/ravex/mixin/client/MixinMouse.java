package ravex.mixin.client;

import net.minecraft.client.MouseHandler;
import ravex.mcwrapper.MinecraftWrapper;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.manager.ConfigManager;
import ravex.manager.ModuleManager;
import ravex.modules.client.Hud;
import ravex.modules.hud.Radar;
import ravex.modules.movement.GuiMove;
import ravex.modules.render.FreeCam;
import ravex.modules.render.FreeLook;
import ravex.modules.Modules;

@Mixin(MouseHandler.class)
public class MixinMouse {
    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void onScrollHead(long window, double xOffset, double yOffset, CallbackInfo ci) {
        var scrollMc = MinecraftWrapper.getInstance();
        if (scrollMc.player != null && scrollMc.screen == null && yOffset != 0) {
            double rx = scrollMc.mouseHandler.xpos() * scrollMc.getWindow().getGuiScaledWidth() / scrollMc.getWindow().getWidth();
            double ry = scrollMc.mouseHandler.ypos() * scrollMc.getWindow().getGuiScaledHeight() / scrollMc.getWindow().getHeight();
            for (var hm : ModuleManager.INSTANCE.getHudModules()) {
                if (!(hm instanceof Radar radar) || !radar.getEnabled()) continue;
                int w = Math.round(radar.getWidth() * radar.getUserScale());
                int h = Math.round(radar.getHeight() * radar.getUserScale());
                if (rx >= radar.getX() && rx <= radar.getX() + w && ry >= radar.getY() && ry <= radar.getY() + h) {
                    // scroll over the radar zooms it, one notch is 2 blocks
                    radar.range = Math.max(8, Math.min(32, radar.range + (yOffset > 0 ? -2 : 2)));
                    ci.cancel();
                    return;
                }
            }
        }
        if (scrollMc.player != null && scrollMc.screen instanceof ChatScreen) {
            double mx = scrollMc.mouseHandler.xpos() * scrollMc.getWindow().getGuiScaledWidth() / scrollMc.getWindow().getWidth();
            double my = scrollMc.mouseHandler.ypos() * scrollMc.getWindow().getGuiScaledHeight() / scrollMc.getWindow().getHeight();
            for (var hm : ModuleManager.INSTANCE.getHudModules()) {
                if (!hm.getEnabled()) continue;
                // the chat scrolls under the wheel, it never resizes
                if ("ChatHud".equals(hm.getName())) continue;
                int x1 = hm.getX(), y1 = hm.getY();
                int x2 = x1 + Math.round(hm.getWidth() * hm.getUserScale());
                int y2 = y1 + Math.round(hm.getHeight() * hm.getUserScale());
                if (mx >= x1 && mx <= x2 && my >= y1 && my <= y2) {
                    float step = 0.05f;
                    float newScale = hm.getUserScale() + (yOffset > 0 ? step : -step);
                    newScale = Math.round(newScale * 20f) / 20f;
                    newScale = Math.max(0.4f, Math.min(2.5f, newScale));
                    hm.setUserScale(newScale);
                    ci.cancel();
                    return;
                }
            }
        }
        if (Modules.enabled(ravex.modules.render.Zoom.class)) {
            ravex.modules.render.Zoom zoom = Modules.get(ravex.modules.render.Zoom.class);
            if (zoom != null && scrollMc.player != null && zoom.isHeld()) {
                zoom.adjustScroll(yOffset);
                ci.cancel();
                return;
            }
        }
        if (Modules.enabled(FreeLook.class)) {
            FreeLook fl = Modules.get(FreeLook.class);
            if (fl != null && fl.zoom && scrollMc.player != null && scrollMc.screen == null) {
                fl.adjustZoom(yOffset);
                ci.cancel();
                return;
            }
        }
        if (!Modules.enabled(FreeCam.class)) return;
        FreeCam fc = Modules.get(FreeCam.class);
        if (fc == null || !fc.scrollSpeed) return;
        var mc = MinecraftWrapper.getInstance();
        if (mc.player == null || mc.screen != null) return;
        fc.adjustSpeedScroll(yOffset);
        ci.cancel();
    }
    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void onMouseButton(long window, MouseButtonInfo buttonInfo, int action, CallbackInfo ci) {
        GuiMove gw = Modules.get(GuiMove.class);
        var mc = MinecraftWrapper.getInstance();
        if (!Modules.enabled(GuiMove.class) || !"NoClick".equals(gw.mode)) return;
        if (!(mc.screen instanceof AbstractContainerScreen<?>)) return;
        // only while walking
        if (!mc.options.keyUp.isDown() && !mc.options.keyDown.isDown()
                && !mc.options.keyLeft.isDown() && !mc.options.keyRight.isDown()) return;
        // no inventory or container clicking at all while moving
        ci.cancel();
    }

    @Inject(method = "onButton", at = @At("TAIL"))
    private void onMouseButtonPost(long window, MouseButtonInfo buttonInfo, int action, CallbackInfo ci) {
        var mc = MinecraftWrapper.getInstance();
        if (mc.player == null) return;

        boolean chatOpen = mc.screen instanceof ChatScreen;
        if (buttonInfo.button() == 0 && (chatOpen || Modules.get(Hud.class).dragEnabled)) {
            if (action == 1) {
                if (mc.screen == null || chatOpen) {
                    double mx = mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth() / mc.getWindow().getWidth();
                    double my = mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight() / mc.getWindow().getHeight();
                    for (var hm : ModuleManager.INSTANCE.getHudModules()) {
                        if (!hm.getEnabled()) continue;
                        // chatHud may only be moved inside the HudEditor
                        if ("ChatHud".equals(hm.getName())) continue;
                        int w = Math.round(hm.getWidth() * hm.getUserScale());
                        int h = Math.round(hm.getHeight() * hm.getUserScale());
                        if (mx >= hm.getX() && mx <= hm.getX() + w &&
                            my >= hm.getY() && my <= hm.getY() + h) {
                            Hud.draggingHud = hm;
                            Hud.dragOffX = (int)mx - hm.getX();
                            Hud.dragOffY = (int)my - hm.getY();
                            if (chatOpen) {
                                ravex.utility.misc.CursorUtility.setCrosshair();
                            } else {
                                ravex.utility.misc.CursorUtility.setHand();
                            }
                            break;
                        }
                    }
                }
            } else if (action == 0) {
                if (Hud.draggingHud != null && mc.screen instanceof ChatScreen) {
                    ConfigManager.INSTANCE.save("default");
                }
                Hud.draggingHud = null;
                ravex.utility.misc.CursorUtility.reset();
            }
        }
    }
}
