package ravex.gui.hudeditor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import ravex.gui.clickgui.ColorUtility;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.modules.client.Hud;
import ravex.utility.misc.GuiOptimizer;
import ravex.utility.render.FontRenderUtility;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
public class HudEditorScreen extends Screen {
    private final Screen parentScreen;
    private final long initTime;
    private boolean closing;
    private long closingStartTime;
    private Module draggingHud;
    private int dragOffsetX;
    private int dragOffsetY;
    private long savedFlashMs = -1;
    private final HudPanel panel = new HudPanel();
    private final Map<Module, Float> hoverProgress = new HashMap<>();
    public HudEditorScreen(Screen parentScreen) {
        super(Component.literal("RaveX HUD Editor"));
        this.parentScreen = parentScreen;
        this.initTime = System.currentTimeMillis();
        panel.rebuildEntries();
    }
    @Override
    protected void init() {
        panel.init(this.width);
    }
    @Override
    public boolean isPauseScreen() { return false; }
    private float getAnimAlpha() {
        if (closing) {
            long elapsed = System.currentTimeMillis() - closingStartTime;
            if (elapsed >= 150) return 0f;
            return 1f - (elapsed / 150.0f);
        }
        long elapsed = System.currentTimeMillis() - initTime;
        return Math.min(1.0f, elapsed / 120.0f);
    }
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        GuiOptimizer.optimizeHudAnimations(ModuleManager.INSTANCE.getHudModules());
        if (closing && System.currentTimeMillis() - closingStartTime >= 150) {
            this.minecraft.setScreen(parentScreen);
            return;
        }
        float animAlpha = getAnimAlpha();
        int alpha = (int)(255 * animAlpha);
        int accentColor = ColorUtility.getActiveColor();
        long now = System.currentTimeMillis();
        if (ModuleManager.get(Hud.class).editorBackground.getValue()) {
            int bgOp = ModuleManager.get(Hud.class).editorOpacity.getValue().intValue();
            graphics.fillGradient(0, 0, this.width, this.height,
                ColorUtility.withAlpha(ColorUtility.BACKGROUND_START, bgOp),
                ColorUtility.withAlpha(ColorUtility.BACKGROUND_END, bgOp));
        }
        if (panel.isPanelDragging()) {
            panel.dragTo(mouseX, mouseY, this.width, this.height);
        }
        if (draggingHud != null) {
            int nx = mouseX - dragOffsetX;
            int ny = mouseY - dragOffsetY;
            nx = Math.max(0, Math.min(this.width - draggingHud.getWidth(), nx));
            ny = Math.max(0, Math.min(this.height - draggingHud.getHeight(), ny));
            draggingHud.setX(nx);
            draggingHud.setY(ny);
            draggingHud.setDisplayX(nx);
            draggingHud.setDisplayY(ny);
        }
        List<Module> hudMods = ModuleManager.INSTANCE.getHudModules();
        for (Module hm : hudMods) {
            if (!hm.getEnabled()) continue;
            int x1 = hm.getX(), y1 = hm.getY();
            int x2 = x1 + hm.getWidth(), y2 = y1 + hm.getHeight();
            boolean hov = mouseX >= x1 && mouseX <= x2 && mouseY >= y1 && mouseY <= y2;
            boolean dragging = (hm == draggingHud);
            float hp = hoverProgress.getOrDefault(hm, 0f);
            float target = (hov || dragging) ? 1f : 0f;
            hp += (target - hp) * 0.2f;
            if (Math.abs(hp - target) < 0.005f) hp = target;
            hoverProgress.put(hm, hp);
            try { hm.render(graphics, 0f); } catch (Throwable ignored) {}
            if (hp > 0.01f) {
                int glowAlpha = (int) (hp * 35);
                graphics.fill(x1 - 1, y1 - 1, x2 + 1, y2 + 1,
                    ColorUtility.withAlpha(accentColor, glowAlpha));
            }
            int borderAlpha = dragging ? 255 : (int) (60 + hp * 140);
            int borderCol = ColorUtility.withAlpha(
                dragging ? accentColor : (hov ? accentColor : 0xFFFFFFFF), borderAlpha);
            if (dragging) {
                long phase = now % 2000;
                float pulse = (float) (Math.sin(phase * Math.PI / 1000.0) * 0.5 + 0.5);
                for (int s = 1; s <= 3; s++) {
                    int al = (int) (pulse * 35 / s);
                    int gc = ColorUtility.withAlpha(accentColor, Math.max(1, al));
                    graphics.fill(x1 - s - 2, y1 - s - 2, x2 + s + 2, y1 - 2, gc);
                    graphics.fill(x1 - s - 2, y2 + 2, x2 + s + 2, y2 + s + 2, gc);
                    graphics.fill(x1 - s - 2, y1 - 2, x1 - 2, y2 + 2, gc);
                    graphics.fill(x2 + 2, y1 - 2, x2 + s + 2, y2 + 2, gc);
                }
                for (int s = 1; s <= 5; s++) {
                    int al = (int) ((1f - pulse) * 20 / s);
                    int gc = ColorUtility.withAlpha(0xFFFFFF, Math.max(1, al));
                    graphics.fill(x1 - s - 3, y1 - s - 3, x2 + s + 3, y1 - 3, gc);
                    graphics.fill(x1 - s - 3, y2 + 3, x2 + s + 3, y2 + s + 3, gc);
                    graphics.fill(x1 - s - 3, y1 - 3, x1 - 3, y2 + 3, gc);
                    graphics.fill(x2 + 3, y1 - 3, x2 + s + 3, y2 + 3, gc);
                }
            }
            int pad = 2;
            graphics.fill(x1 - pad, y1 - pad, x2 + pad, y1 - pad + 1, borderCol);
            graphics.fill(x1 - pad, y2 + pad - 1, x2 + pad, y2 + pad, borderCol);
            graphics.fill(x1 - pad, y1 - pad, x1 - pad + 1, y2 + pad, borderCol);
            graphics.fill(x2 + pad - 1, y1 - pad, x2 + pad, y2 + pad, borderCol);
            int cSize = 4;
            graphics.fill(x1 - pad, y1 - pad, x1 - pad + cSize, y1 - pad + 1, accentColor);
            graphics.fill(x1 - pad, y1 - pad, x1 - pad + 1, y1 - pad + cSize, accentColor);
            graphics.fill(x2 + pad - cSize, y2 + pad - 1, x2 + pad, y2 + pad, accentColor);
            graphics.fill(x2 + pad - 1, y2 + pad - cSize, x2 + pad, y2 + pad, accentColor);
            String tag = hm.getName();
            int tagX = x1, tagY = y1 - pad - 11;
            if (tagY < 0) tagY = y2 + pad + 2;
            FontRenderUtility.drawString(graphics, tag, tagX + 1, tagY + 1, 0xBB000000, false);
            FontRenderUtility.drawString(graphics, tag, tagX, tagY, hov ? accentColor : 0xFFAAAAAA, false);
        }
        panel.render(graphics, mouseX, mouseY, this.width, this.height, alpha, accentColor);
        super.render(graphics, mouseX, mouseY, partialTicks);
    }
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean handled) {
        if (closing) return true;
        int mx = (int) event.x(), my = (int) event.y();
        int btn = event.button();
        if (panel.mouseClicked(mx, my, btn, this.width, this.height)) return true;
        List<Module> huds = ModuleManager.INSTANCE.getHudModules();
        if (btn == 1) {
            for (Module hm : huds) {
                if (mx >= hm.getX() && mx <= hm.getX() + hm.getWidth() &&
                    my >= hm.getY() && my <= hm.getY() + hm.getHeight()) {
                    panel.toggleExpanded(hm);
                    return true;
                }
            }
        }
        if (btn == 0) {
            for (Module hm : huds) {
                if (!hm.getEnabled()) continue;
                if (mx >= hm.getX() && mx <= hm.getX() + hm.getWidth() &&
                    my >= hm.getY() && my <= hm.getY() + hm.getHeight()) {
                    draggingHud = hm;
                    dragOffsetX = mx - hm.getX();
                    dragOffsetY = my - hm.getY();
                    return true;
                }
            }
        }
        return super.mouseClicked(event, handled);
    }
    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0) {
            draggingHud = null;
            panel.mouseReleased();
        }
        return super.mouseReleased(event);
    }
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (closing) return true;
        int px = panel.getX(), py = panel.getY();
        int pw = HudPanel.PANEL_W;
        int tpy = panel.getY();
        boolean overPanel = mouseX >= px && mouseX <= px + pw && mouseY >= tpy;
        if (overPanel) {
            if (verticalAmount < 0) panel.scroll(-1, panel.getEntryCount());
            else if (verticalAmount > 0) panel.scroll(1, panel.getEntryCount());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
    @Override
    public boolean keyPressed(KeyEvent event) {
        if (closing) return true;
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) { doSave(); return true; }
        return super.keyPressed(event);
    }
    private void doSave() {
        savedFlashMs = System.currentTimeMillis();
        ravex.manager.ConfigManager.INSTANCE.save("default");
        startClosing();
    }
    private void startClosing() {
        if (!closing) {
            closing = true;
            closingStartTime = System.currentTimeMillis();
        }
    }
    @Override
    public void removed() {
        super.removed();
    }
}
