package ravex.gui.hudeditor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import ravex.gui.clickgui.ClickGUI;
import ravex.utility.render.ColorUtility;
import ravex.utility.render.Render2DUtility;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.modules.client.Hud;
import ravex.utility.misc.GuiOptimizerUtility;
import ravex.utility.misc.CursorUtility;
import ravex.utility.render.FontRenderUtility;
import ravex.utility.render.animate.AnimationUtility;
import ravex.mcwrapper.MinecraftWrapper;
import java.util.ArrayList;
import java.util.List;
public class HudEditorScreen extends Screen {
    private final Screen parentScreen;
    private final long initTime;
    private boolean closing;
    private long closingStartTime;
    private Module draggingHud;
    private int dragOffsetX;
    private int dragOffsetY;
    // the dragged element follows the cursor on a spring
    private AnimationUtility.SpringAnimation dragSpringX;
    private AnimationUtility.SpringAnimation dragSpringY;
    private long lastDragNanos = 0L;
    // the springs keep guiding the element to the drop point
    private boolean dragReleased = false;
    private int frozenTargetX;
    private int frozenTargetY;
    private long savedFlashMs = -1;
    private final HudPanel panel = new HudPanel();
    public HudEditorScreen(Screen parentScreen) {
        super(Component.literal(ravex.utility.misc.LanguageUtility.t("hud_editor_title")));
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
        GuiOptimizerUtility.optimizeHudAnimations(ModuleManager.INSTANCE.getHudModules());
        if (closing && System.currentTimeMillis() - closingStartTime >= 150) {
            this.minecraft.setScreen(parentScreen);
            return;
        }
        float animAlpha = getAnimAlpha();
        int alpha = (int)(255 * animAlpha);
        int accentColor = ColorUtility.getActiveColor();
        long now = System.currentTimeMillis();
        if (ModuleManager.get(Hud.class).editorBackground) {
            int bgOp = (int) ModuleManager.get(Hud.class).editorOpacity;
            graphics.fillGradient(0, 0, this.width, this.height,
                ColorUtility.withAlpha(ColorUtility.BACKGROUND_START, bgOp),
                ColorUtility.withAlpha(ColorUtility.BACKGROUND_END, bgOp));
        }
        if (panel.isPanelDragging()) {
            panel.dragTo(mouseX, mouseY, this.width, this.height);
        }
        if (draggingHud != null) {
            int nx;
            int ny;
            if (dragReleased) {
                // the mouse is gone, finish the move towards the drop point
                nx = frozenTargetX;
                ny = frozenTargetY;
            } else {
                nx = mouseX - dragOffsetX;
                ny = mouseY - dragOffsetY;
                nx = Math.max(0, Math.min(this.width - draggingHud.getWidth(), nx));
                ny = Math.max(0, Math.min(this.height - draggingHud.getHeight(), ny));
                draggingHud.setX(nx);
                draggingHud.setY(ny);
                frozenTargetX = nx;
                frozenTargetY = ny;
            }

            long nowNanos = System.nanoTime();
            float dragDt = lastDragNanos == 0L ? 16f : Math.min(50f, (nowNanos - lastDragNanos) / 1_000_000f);
            lastDragNanos = nowNanos;
            if (dragSpringX == null) {
                dragSpringX = new AnimationUtility.SpringAnimation(draggingHud.getDisplayX());
                dragSpringY = new AnimationUtility.SpringAnimation(draggingHud.getDisplayY());
            }
            float dragStiffness = 240f;
            dragSpringX.stiffness(dragStiffness).damping(2f * (float) Math.sqrt(dragStiffness)).mass(1f);
            dragSpringY.stiffness(dragStiffness).damping(2f * (float) Math.sqrt(dragStiffness)).mass(1f);
            dragSpringX.setTarget(nx);
            dragSpringY.setTarget(ny);
            dragSpringX.update(dragDt);
            dragSpringY.update(dragDt);
            draggingHud.setDisplayX(dragSpringX.getValue());
            draggingHud.setDisplayY(dragSpringY.getValue());
            draggingHud.setHudPositionCustomized(true);

            if (dragReleased && dragSpringX.isSettled() && dragSpringY.isSettled()) {
                draggingHud.setX(frozenTargetX);
                draggingHud.setY(frozenTargetY);
                draggingHud.setDisplayX(frozenTargetX);
                draggingHud.setDisplayY(frozenTargetY);
                draggingHud = null;
                dragReleased = false;
                dragSpringX = null;
                dragSpringY = null;
            }
        }
        List<Module> hudMods = ModuleManager.INSTANCE.getHudModules();
        for (Module hm : hudMods) {
            if (!hm.getEnabled()) continue;
            int x1 = hm.getX(), y1 = hm.getY();
            int x2 = x1 + Math.round(hm.getWidth() * hm.getUserScale());
            int y2 = y1 + Math.round(hm.getHeight() * hm.getUserScale());

            var pose = graphics.pose();
            pose.pushMatrix();
            pose.translate(x1, y1);
            pose.scale(hm.getUserScale(), hm.getUserScale());
            pose.translate(-x1, -y1);
            try { hm.render(graphics, 0f); } catch (Throwable ignored) {}
            pose.popMatrix();

            int pad = 2;
            String name = ravex.utility.misc.LanguageUtility.moduleName(hm.getName());
            String tag = name.endsWith("Hud") ? name.substring(0, name.length() - 3) : name;
            int tagX = x1, tagY = y1 - pad - 11;
            if (tagY < 0) tagY = y2 + pad + 2;
            FontRenderUtility.drawString(graphics, tag, tagX + 1, tagY + 1, 0xBB000000, false);
            FontRenderUtility.drawString(graphics, tag, tagX, tagY, 0xFFAAAAAA, false);
        }
        ClickGUI.hoveredDescription = null;
        panel.render(graphics, mouseX, mouseY, this.width, this.height, alpha, accentColor);
        renderDescription(graphics, mouseX, mouseY, alpha);
        // the parameter elements are the click gui ones
        if (ClickGUI.activeColorPalette != null) {
            ClickGUI.activeColorPalette.render(graphics, mouseX, mouseY, this.width, this.height);
        }

        String hint = ravex.utility.misc.LanguageUtility.t("hud_editor_scale_hint");
        int hw = FontRenderUtility.getStringWidth(hint);
        int hx = (this.width - hw) / 2;
        int hy = this.height - 15;
        Render2DUtility.drawRound(graphics, hx - 8, hy - 2, hw + 16, 13, 3, 0xA0080810);
        Render2DUtility.drawSmoothRoundOutline(graphics, hx - 8, hy - 2, hw + 16, 13, 3, 1,
            ColorUtility.applyAlpha(ModuleManager.get(ravex.modules.client.ClickGui.class).outlineColor, alpha / 255f));
        FontRenderUtility.drawString(graphics, hint, hx, hy, 0xFFE0E0F0, true);

        super.render(graphics, mouseX, mouseY, partialTicks);
    }
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean handled) {
        if (closing) return true;
        if (ClickGUI.activeColorPalette != null) {
            boolean result = ClickGUI.activeColorPalette.mouseClicked(event.x(), event.y(), event.button());
            if (!ClickGUI.activeColorPalette.isOpen()) {
                ClickGUI.activeColorPalette = null;
                ClickGUI.activeColorParameter = null;
            }
            return true;
        }
        int mx = (int) event.x(), my = (int) event.y();
        int btn = event.button();
        if (panel.mouseClicked(mx, my, btn, this.width, this.height)) return true;
        List<Module> huds = ModuleManager.INSTANCE.getHudModules();
        if (btn == 1) {
            for (Module hm : huds) {
                int x1 = hm.getX(), y1 = hm.getY();
                int x2 = x1 + Math.round(hm.getWidth() * hm.getUserScale());
                int y2 = y1 + Math.round(hm.getHeight() * hm.getUserScale());
                if (mx >= x1 && mx <= x2 && my >= y1 && my <= y2) {
                    panel.toggleExpanded(hm);
                    return true;
                }
            }
        }
        if (btn == 0) {
            for (Module hm : huds) {
                if (!hm.getEnabled()) continue;
                int x1 = hm.getX(), y1 = hm.getY();
                int x2 = x1 + Math.round(hm.getWidth() * hm.getUserScale());
                int y2 = y1 + Math.round(hm.getHeight() * hm.getUserScale());
                if (mx >= x1 && mx <= x2 && my >= y1 && my <= y2) {
                    draggingHud = hm;
                    dragOffsetX = mx - hm.getX();
                    dragOffsetY = my - hm.getY();
                    // re-seed the follow springs at the spot the element currently sits
                    dragSpringX = new AnimationUtility.SpringAnimation(hm.getDisplayX());
                    dragSpringY = new AnimationUtility.SpringAnimation(hm.getDisplayY());
                    lastDragNanos = System.nanoTime();
                    ravex.utility.misc.CursorUtility.setHand();
                    return true;
                }
            }
        }
        return super.mouseClicked(event, handled);
    }
    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0) {
            boolean wasDragging = draggingHud != null || panel.isPanelDragging();
            if (draggingHud != null) {
                // the springs finish the move, so the element does not snap on release
                dragReleased = true;
            }
            panel.mouseReleased();
            if (wasDragging) ravex.utility.misc.CursorUtility.reset();
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
        for (Module hm : ModuleManager.INSTANCE.getHudModules()) {
            if (!hm.getEnabled()) continue;
            int x1 = hm.getX(), y1 = hm.getY();
            int x2 = x1 + Math.round(hm.getWidth() * hm.getUserScale());
            int y2 = y1 + Math.round(hm.getHeight() * hm.getUserScale());
            if (mouseX >= x1 && mouseX <= x2 && mouseY >= y1 && mouseY <= y2) {
                if ("ChatHud".equals(hm.getName())) {
                    // wheel scrolls the chat, it does not scale it
                    var chat = MinecraftWrapper.getWrapper().getRaw().gui.getChat();
                    if (chat != null) chat.scrollChat(verticalAmount > 0 ? 1 : -1);
                    return true;
                }
                float step = 0.05f;
                float currentScale = hm.getUserScale();
                float newScale = verticalAmount > 0 ? currentScale + step : currentScale - step;
                newScale = Math.round(newScale * 20f) / 20f;
                newScale = Math.max(0.4f, Math.min(2.5f, newScale));
                hm.setUserScale(newScale);
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        int mx = (int) mouseX, my = (int) mouseY;
        boolean overHud = false;
        for (Module hm : ModuleManager.INSTANCE.getHudModules()) {
            if (!hm.getEnabled()) continue;
            int x1 = hm.getX(), y1 = hm.getY();
            int x2 = x1 + Math.round(hm.getWidth() * hm.getUserScale());
            int y2 = y1 + Math.round(hm.getHeight() * hm.getUserScale());
            if (mx >= x1 && mx <= x2 && my >= y1 && my <= y2) {
                overHud = true;
                break;
            }
        }
        if (overHud) {
            CursorUtility.setHand();
        } else {
            CursorUtility.reset();
        }
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (closing) return true;
        if (ClickGUI.activeColorPalette != null) {
            boolean result = ClickGUI.activeColorPalette.keyPressed(event.key());
            if (!ClickGUI.activeColorPalette.isOpen()) {
                ClickGUI.activeColorPalette = null;
                ClickGUI.activeColorParameter = null;
            }
            return true;
        }
        // the parameter elements are the click gui ones, so the same input states apply
        if (ClickGUI.activeKeybindElement != null) {
            ravex.parameter.KeybindParameter kp = (ravex.parameter.KeybindParameter) ClickGUI.activeKeybindElement.getParameter();
            if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
                kp.setValue(org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN);
                ravex.RaveX.suppressKey(GLFW.GLFW_KEY_ESCAPE);
            } else {
                kp.setValue(event.key());
                ravex.RaveX.suppressKey(event.key());
            }
            ClickGUI.activeKeybindElement = null;
            return true;
        }
        if (ClickGUI.activeNumberParameterElement != null) {
            if (event.key() == GLFW.GLFW_KEY_ESCAPE || event.key() == GLFW.GLFW_KEY_ENTER) {
                ClickGUI.activeNumberParameterElement.applyInput();
                ClickGUI.activeNumberParameterElement = null;
                return true;
            }
            if (event.key() == GLFW.GLFW_KEY_BACKSPACE) {
                ClickGUI.activeNumberParameterElement.removeLastChar();
                return true;
            }
            return true;
        }
        if (ClickGUI.activeStringParameterElement != null) {
            ravex.parameter.StringParameter sp = (ravex.parameter.StringParameter) ClickGUI.activeStringParameterElement.getParameter();
            if (event.key() == GLFW.GLFW_KEY_ESCAPE || event.key() == GLFW.GLFW_KEY_ENTER) {
                ClickGUI.activeStringParameterElement = null;
                return true;
            }
            if (event.key() == GLFW.GLFW_KEY_BACKSPACE) {
                String val = sp.getValue();
                if (!val.isEmpty()) {
                    sp.setValue(val.substring(0, val.length() - 1));
                }
                return true;
            }
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) { doSave(); return true; }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (closing) return true;
        if (ClickGUI.activeColorPalette != null) {
            String text = event.codepointAsString();
            if (!text.isEmpty()) {
                boolean result = ClickGUI.activeColorPalette.charTyped(text.charAt(0));
                if (!ClickGUI.activeColorPalette.isOpen()) {
                    ClickGUI.activeColorPalette = null;
                    ClickGUI.activeColorParameter = null;
                }
                return true;
            }
            return true;
        }
        if (ClickGUI.activeKeybindElement != null) {
            return true;
        }
        if (ClickGUI.activeNumberParameterElement != null) {
            String text = event.codepointAsString();
            if (!text.isEmpty()) {
                char ch = text.charAt(0);
                if ((ch >= '0' && ch <= '9') || ch == '.' || ch == '-') {
                    ClickGUI.activeNumberParameterElement.appendChar(ch);
                }
            }
            return true;
        }
        if (ClickGUI.activeStringParameterElement != null) {
            ravex.parameter.StringParameter sp = (ravex.parameter.StringParameter) ClickGUI.activeStringParameterElement.getParameter();
            String text = event.codepointAsString();
            if (!text.isEmpty() && text.charAt(0) >= 32 && text.charAt(0) < 127) {
                sp.setValue(sp.getValue() + text);
            }
            return true;
        }
        return super.charTyped(event);
    }
    // hovered hud module description, follows the mouse like the click gui tooltip
    private void renderDescription(GuiGraphics graphics, int mouseX, int mouseY, int alpha) {
        if (ClickGUI.activeColorPalette != null) return;
        Module hovered = panel.getHoveredModule();
        if (hovered == null) return;
        // a hovered mode option describes itself, same as in the click gui
        String desc = ClickGUI.hoveredDescription != null
            ? ClickGUI.hoveredDescription : hovered.getDescription();
        if (desc == null || desc.isEmpty()) return;

        int maxW = 190;
        List<String> lines = wrapText(desc, maxW);
        if (lines.isEmpty()) return;
        int lineH = FontRenderUtility.getFontHeight() + 2;
        int padX = 8;
        int padY = 6;
        int tw = 0;
        for (String line : lines) {
            tw = Math.max(tw, FontRenderUtility.getStringWidth(line));
        }
        tw = Math.min(tw + padX * 2, maxW + padX * 2);
        int th = lines.size() * lineH + padY * 2;

        int ox = (int) ModuleManager.get(ravex.modules.client.ClickGui.class).tooltipOffsetX;
        int oy = (int) ModuleManager.get(ravex.modules.client.ClickGui.class).tooltipOffsetY;
        int dx = mouseX + ox;
        int dy = mouseY + oy;
        if (dx + tw > this.width) dx = mouseX - tw - 4;
        if (dy + th > this.height) dy = mouseY - th - 4;
        if (dx < 0) dx = 2;
        if (dy < 0) dy = 2;

        float animAlpha = alpha / 255f;
        ClickGUI.drawDescriptionBackground(graphics, dx, dy, tw, th, animAlpha);

        int ly = dy + padY;
        int textAlpha = (int) (alpha * 0.9f);
        for (String line : lines) {
            FontRenderUtility.drawString(graphics, line, dx + padX, ly,
                ColorUtility.setAlpha(0xE0E0E0, textAlpha), true);
            ly += lineH;
        }
    }

    private static List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) return lines;
        StringBuilder line = new StringBuilder();
        for (String word : text.split("\\s+")) {
            if (word.isEmpty()) continue;
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (line.length() > 0 && FontRenderUtility.getStringWidth(candidate) > maxWidth) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (line.length() > 0) lines.add(line.toString());
        return lines;
    }

    private void doSave() {
        savedFlashMs = System.currentTimeMillis();
        ravex.manager.ConfigManager.INSTANCE.saveAsync("default");
        startClosing();
    }    private void startClosing() {
        if (!closing) {
            closing = true;
            closingStartTime = System.currentTimeMillis();
        }
    }
    @Override
    public void removed() {
        // the parameter elements are shared with the click gui, drop their input states
        ClickGUI.isDraggingSlider = false;
        if (ClickGUI.activeNumberParameterElement != null) {
            ClickGUI.activeNumberParameterElement.applyInput();
            ClickGUI.activeNumberParameterElement = null;
        }
        ClickGUI.activeStringParameterElement = null;
        ClickGUI.activeKeybindElement = null;
        ClickGUI.activeColorPalette = null;
        ClickGUI.activeColorParameter = null;
        super.removed();
    }
}
