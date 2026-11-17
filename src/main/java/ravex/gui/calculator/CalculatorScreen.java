package ravex.gui.calculator;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import ravex.cmd.cmds.CalcUtility;
import ravex.gui.clickgui.SubScreenStyle;
import ravex.modules.client.Calculator;
import ravex.utility.render.ColorUtility;
import ravex.utility.render.FontRenderUtility;

/**
 * Minecraft GUI replacement for the old GTK-native calculator window.
 * Evaluation is done by CalcUtility (Rust core, Java fallback), so the
 * crashing C++ library is never involved.
 */
public class CalculatorScreen extends Screen {
    private static final int MAX_LENGTH = 96;

    private static final String[][] KEYPAD = {
        {"C", "(", ")", "DEL"},
        {"7", "8", "9", "/"},
        {"4", "5", "6", "*"},
        {"1", "2", "3", "-"},
        {"0", ".", "=", "+"}
    };

    private final Screen parent;
    private String expression = "";
    private String result = "";
    private boolean invalidResult;
    private double lastValue = Double.NaN;

    public CalculatorScreen(Screen parent) {
        super(Component.literal("Calculator"));
        this.parent = parent;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        SubScreenStyle.background(graphics, this.width, this.height);
        int activeColor = ColorUtility.getActiveColor();

        SubScreenStyle.header(graphics, this.width, "Calculator", "Rust expression engine", activeColor);

        int panelW = Math.min(340, this.width - 32);
        int panelH = Math.min(292, this.height - 32);
        int panelX = (this.width - panelW) / 2;
        int panelY = Math.max(36, (this.height - panelH) / 2);
        SubScreenStyle.card(graphics, panelX, panelY, panelW, panelH, activeColor);

        int inputX = panelX + 12;
        int inputY = panelY + 12;
        int inputW = panelW - 24;
        int inputH = 22;

        SubScreenStyle.input(graphics, inputX, inputY, inputW, inputH, activeColor);
        String shown = expression.isEmpty() ? "Enter an expression..." : expression;
        FontRenderUtility.drawString(graphics, shown, inputX + 6, inputY + 7,
            expression.isEmpty() ? 0xFF505060 : 0xFFE8E8F2, false);
        if ((System.currentTimeMillis() / 500) % 2 == 0) {
            int caretX = inputX + 6 + FontRenderUtility.getStringWidth(expression);
            graphics.fill(caretX + 1, inputY + 5, caretX + 2, inputY + 17, 0xFFE8E8F2);
        }

        int resultY = inputY + inputH + 8;
        if (!expression.isEmpty()) {
            FontRenderUtility.drawString(graphics,
                invalidResult ? "Invalid expression" : "= " + result,
                inputX + 2, resultY, invalidResult ? 0xFFFF6666 : 0xFF7AD4A0, false);
        }

        int cols = 4;
        int rows = KEYPAD.length;
        int gap = 5;
        int keypadY = resultY + 16;
        int keyW = (inputW - gap * (cols - 1)) / cols;
        int keyH = Math.min(24, Math.max(16, (panelY + panelH - 30 - keypadY - gap * (rows - 1)) / rows));

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                String label = KEYPAD[r][c];
                int bx = inputX + c * (keyW + gap);
                int by = keypadY + r * (keyH + gap);
                boolean hovered = mouseX >= bx && mouseX <= bx + keyW
                    && mouseY >= by && mouseY <= by + keyH;
                boolean primary = "=".equals(label);
                SubScreenStyle.button(graphics, bx, by, keyW, keyH, hovered, primary, activeColor);
                int tw = FontRenderUtility.getStringWidth(label);
                FontRenderUtility.drawString(graphics, label, bx + (keyW - tw) / 2, by + (keyH - 8) / 2,
                    primary ? 0xFFFFFFFF : 0xFFD0D0E0, false);
            }
        }

        FontRenderUtility.drawString(graphics, "Enter = evaluate  ·  Esc = close",
            inputX + 2, panelY + panelH - 16, 0xFF5A5A70, false);

        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean handled) {
        if (event.button() != 0) {
            return super.mouseClicked(event, handled);
        }
        int mx = (int) event.x();
        int my = (int) event.y();

        int panelW = Math.min(340, this.width - 32);
        int panelH = Math.min(292, this.height - 32);
        int panelX = (this.width - panelW) / 2;
        int panelY = Math.max(36, (this.height - panelH) / 2);
        int inputX = panelX + 12;
        int inputY = panelY + 12;
        int inputW = panelW - 24;
        int resultY = inputY + 22 + 8;

        int cols = 4;
        int rows = KEYPAD.length;
        int gap = 5;
        int keypadY = resultY + 16;
        int keyW = (inputW - gap * (cols - 1)) / cols;
        int keyH = Math.min(24, Math.max(16, (panelY + panelH - 30 - keypadY - gap * (rows - 1)) / rows));

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int bx = inputX + c * (keyW + gap);
                int by = keypadY + r * (keyH + gap);
                if (mx >= bx && mx <= bx + keyW && my >= by && my <= by + keyH) {
                    pressKey(KEYPAD[r][c]);
                    return true;
                }
            }
        }

        return super.mouseClicked(event, handled);
    }

    private void pressKey(String label) {
        switch (label) {
            case "C" -> setExpression("");
            case "DEL" -> {
                if (!expression.isEmpty()) {
                    setExpression(expression.substring(0, expression.length() - 1));
                }
            }
            case "=" -> commitResult();
            default -> setExpression(expression + label);
        }
    }

    private void setExpression(String value) {
        expression = value.length() > MAX_LENGTH ? value.substring(0, MAX_LENGTH) : value;
        evaluate();
    }

    private void evaluate() {
        if (expression.isEmpty()) {
            result = "";
            invalidResult = false;
            lastValue = Double.NaN;
            return;
        }
        Double value = CalcUtility.eval(expression);
        if (value == null) {
            result = "";
            invalidResult = true;
            lastValue = Double.NaN;
        } else {
            lastValue = value;
            result = format(value);
            invalidResult = false;
        }
    }

    private void commitResult() {
        if (!invalidResult && !Double.isNaN(lastValue)) {
            setExpression(format(lastValue));
        }
    }

    private static String format(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.valueOf((long) value);
        }
        String text = String.format("%.6f", value);
        text = text.replaceAll("0+$", "");
        return text.replaceAll("\\.$", "");
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            commitResult();
            return true;
        }
        if (key == GLFW.GLFW_KEY_BACKSPACE) {
            if (!expression.isEmpty()) {
                setExpression(expression.substring(0, expression.length() - 1));
            }
            return true;
        }
        if (key == GLFW.GLFW_KEY_DELETE) {
            setExpression("");
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        String text = event.codepointAsString();
        if (text.isEmpty()) {
            return true;
        }
        char c = text.charAt(0);
        boolean allowed = (c >= '0' && c <= '9')
            || (c >= 'a' && c <= 'z')
            || (c >= 'A' && c <= 'Z')
            || c == '.' || c == ',' || c == '+' || c == '-' || c == '*' || c == '/'
            || c == '%' || c == '^' || c == '(' || c == ')' || c == '!' || c == ' '
            || c == '\u00D7' || c == '\u00F7';
        if (!allowed) {
            return true;
        }
        if (c == ',') {
            c = '.';
        }
        if (expression.length() < MAX_LENGTH) {
            setExpression(expression + c);
        }
        return true;
    }

    @Override
    public void onClose() {
        Calculator.onScreenClosed();
        this.minecraft.setScreen(parent);
    }
}
