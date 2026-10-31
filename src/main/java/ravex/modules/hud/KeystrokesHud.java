package ravex.modules.hud;

import org.lwjgl.glfw.GLFW;
import ravex.modules.annotations.HudModule;
import ravex.modules.annotations.Parameter;
import net.minecraft.client.gui.GuiGraphics;

import ravex.modules.client.Hud;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.utility.render.ColorUtility;
import ravex.utility.render.FontRenderUtility;
import ravex.utility.render.Render2DUtility;
import ravex.utility.render.animate.AnimationUtility;
import ravex.gui.hudeditor.HudEditorScreen;
import ravex.modules.Modules;

// the classic wasd block with space bar and mouse buttons
// the size is set with the mouse wheel in the hud editor
@HudModule("KeystrokesHud")
public class KeystrokesHud extends ravex.modules.Module {
    @Parameter(name = "IdleColor", color = true)
    public int idleColor = 0x96101018;
    @Parameter(name = "PressedColor", color = true)
    public int pressedColor = 0xFF40A9F8;
    @Parameter(name = "TextColor", color = true)
    public int textColor = 0xFFE8E8F0;
    @Parameter(name = "Space")
    public boolean showSpace = true;
    @Parameter(name = "MouseButtons")
    public boolean showMouse = false;

    // w a s d space lmb rmb
    private static final int KEY_COUNT = 7;
    private final AnimationUtility.SpringAnimation[] press = new AnimationUtility.SpringAnimation[KEY_COUNT];
    private long lastFrameNanos = System.nanoTime();

    public KeystrokesHud() {
        super("KeystrokesHud", 10, 210, 72, 66);
        setX(10);
        setY(210);
        setWidth(72);
        setHeight(66);
        for (int i = 0; i < KEY_COUNT; i++) {
            press[i] = new AnimationUtility.SpringAnimation(0f);
        }
    }

    private boolean keyDown(long win, int key) {
        return GLFW.glfwGetKey(win, key) == GLFW.GLFW_PRESS;
    }

    private boolean mouseDown(long win, int button) {
        return GLFW.glfwGetMouseButton(win, button) == GLFW.GLFW_PRESS;
    }

    private void drawKey(GuiGraphics graphics, int x, int y, int w, int h, String label, float pressAmount) {
        float p = Math.max(0f, Math.min(1f, pressAmount));
        int bg = ColorUtility.interpolate(this.idleColor, this.pressedColor, p);
        Render2DUtility.drawRound(graphics, x, y, w, h, 3, bg);
        if (p < 0.99f) {
            Render2DUtility.drawRoundBorder(graphics, x, y, w, h, 3, 1, 0x33FFFFFF);
        }
        if (label != null && !label.isEmpty()) {
            int lw = FontRenderUtility.getStringWidth(label);
            int lh = FontRenderUtility.getFontHeight();
            int col = ColorUtility.interpolate(this.textColor, 0xFFFFFFFF, p);
            FontRenderUtility.drawString(graphics, label, x + (w - lw) / 2, y + (h - lh) / 2, col, false);
        }
    }

    public void render(GuiGraphics graphics, float partialTicks) {
        if (!Modules.enabled(Hud.class)) return;
        var mc = MinecraftWrapper.getWrapper();
        long win = mc.getWindowHandle();
        boolean inEditor = mc.getScreen() instanceof HudEditorScreen;

        long now = System.nanoTime();
        float dt = Math.min(50f, (now - lastFrameNanos) / 1_000_000f);
        lastFrameNanos = now;

        // no key highlighting while the hud editor is open, the mouse is busy dragging
        boolean[] down = {
            !inEditor && keyDown(win, GLFW.GLFW_KEY_W),
            !inEditor && keyDown(win, GLFW.GLFW_KEY_A),
            !inEditor && keyDown(win, GLFW.GLFW_KEY_S),
            !inEditor && keyDown(win, GLFW.GLFW_KEY_D),
            !inEditor && keyDown(win, GLFW.GLFW_KEY_SPACE),
            !inEditor && mouseDown(win, GLFW.GLFW_MOUSE_BUTTON_LEFT),
            !inEditor && mouseDown(win, GLFW.GLFW_MOUSE_BUTTON_RIGHT)
        };

        // critically damped springs, the key fades in and out instead of snapping
        float stiffness = 300f;
        for (int i = 0; i < KEY_COUNT; i++) {
            press[i].stiffness(stiffness).damping(2f * (float) Math.sqrt(stiffness)).mass(1f);
            press[i].setTarget(down[i] ? 1f : 0f);
            press[i].update(dt);
        }

        int kw = 22;
        int kh = 19;
        int gp = 3;
        int spaceH = 10;
        int gw = kw * 3 + gp * 2;

        int bx = getX();
        int by = getY();
        int cy = by;

        // w sits centered above the a s d row
        drawKey(graphics, bx + kw + gp, cy, kw, kh, "W", press[0].getValue());
        cy += kh + gp;

        drawKey(graphics, bx, cy, kw, kh, "A", press[1].getValue());
        drawKey(graphics, bx + kw + gp, cy, kw, kh, "S", press[2].getValue());
        drawKey(graphics, bx + (kw + gp) * 2, cy, kw, kh, "D", press[3].getValue());
        cy += kh + gp;

        if (this.showMouse) {
            int half = (gw - gp) / 2;
            drawKey(graphics, bx, cy, half, kh, "L", press[5].getValue());
            drawKey(graphics, bx + half + gp, cy, half, kh, "R", press[6].getValue());
            cy += kh + gp;
        }

        if (this.showSpace) {
            drawKey(graphics, bx, cy, gw, spaceH, null, press[4].getValue());
            cy += spaceH;
        }

        setWidth(gw);
        setHeight(cy - by);
    }
}
