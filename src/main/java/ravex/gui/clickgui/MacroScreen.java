package ravex.gui.clickgui;

import net.minecraft.client.Minecraft;
import ravex.utility.render.FontRenderUtility;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import ravex.macro.Macro;
import ravex.macro.MacroAction;
import ravex.macro.MacroAction.Type;
import ravex.manager.MacroManager;

import java.util.ArrayList;
import java.util.List;

public class MacroScreen extends Screen {
    private final Screen parent;
    private List<Macro> macros;
    private int scrollOffset;
    private int selectedIndex = -1;

    private boolean creatingNew;
    private String newName = "";
    private boolean bindingKey;
    private int bindingForIndex = -1;

    private boolean editingActions;
    private int editingIndex = -1;
    private String actionInput = "";
    private int actionTypeIndex;
    private String statusMessage = "";
    private int statusTimer;

    private static final String[] ACTION_TYPES = {"Toggle Module", "Send Chat", "Execute Command", "Delay (ms)"};
    private static final Type[] ACTION_TYPE_VALUES = {Type.TOGGLE_MODULE, Type.SEND_CHAT, Type.EXECUTE_COMMAND, Type.DELAY};

    public MacroScreen(Screen parent) {
        super(Component.literal("Macro Editor"));
        this.parent = parent;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    protected void init() {
        super.init();
        macros = new ArrayList<>(MacroManager.INSTANCE.getMacros());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        graphics.fillGradient(0, 0, this.width, this.height, 0xDD0A0A14, 0xDD10101F);

        int activeColor = ColorUtility.getActiveColor();

        FontRenderUtility.drawString(graphics, "Macro Editor", 20, 12, 0xFFFFFFFF, true);
        FontRenderUtility.drawString(graphics, "Add macros with keybinds that execute multiple actions", 20, 24, 0xFF8F8FA0, false);

        if (statusTimer > 0 && !statusMessage.isEmpty()) {
            FontRenderUtility.drawString(graphics, statusMessage, 20, 38, 0xFFAAFFAA, false);
        }

        int listX = 20;
        int listY = 50;
        int listW = 250;
        int itemH = 20;

        graphics.fill(listX, listY, listX + listW, this.height - 50, 0x44000000);

        int y = listY + 4 - scrollOffset;
        for (int i = 0; i < macros.size(); i++) {
            Macro m = macros.get(i);
            boolean hovered = mouseX >= listX && mouseX <= listX + listW && mouseY >= y && mouseY <= y + itemH;
            int bg = i == selectedIndex ? 0xFF202035 : (hovered ? 0xFF181828 : 0xFF0D0D14);
            graphics.fill(listX + 2, y, listX + listW - 2, y + itemH, bg);

            String keyName = m.getKeyBind() > 0 ? " [" + getKeyName(m.getKeyBind()) + "]" : "";
            FontRenderUtility.drawString(graphics, m.getName() + keyName, listX + 6, y + 5, 0xFFD0D0E0, false);

            int actionCount = m.getActions().size();
            String countStr = actionCount + " action" + (actionCount != 1 ? "s" : "");
            int cw = FontRenderUtility.getStringWidth(countStr);
            FontRenderUtility.drawString(graphics, countStr, listX + listW - cw - 8, y + 5, 0xFF707080, false);

            y += itemH;
        }
        if (macros.isEmpty()) {
            FontRenderUtility.drawString(graphics, "No macros yet. Click \"Create\" to add one.", listX + 6, listY + 8, 0xFF505060, false);
        }

        if (creatingNew) {
            renderCreateDialog(graphics, mouseX, mouseY);
        } else if (editingActions) {
            renderActionEditor(graphics, mouseX, mouseY);
        } else {
            renderToolbar(graphics, mouseX, mouseY, activeColor);
        }

        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    private void renderCreateDialog(GuiGraphics graphics, int mouseX, int mouseY) {
        int dlgX = this.width / 2 - 100;
        int dlgY = this.height / 2 - 60;
        int dlgW = 200;
        int dlgH = 120;

        graphics.fill(0, 0, this.width, this.height, 0x88000000);
        graphics.fill(dlgX, dlgY, dlgX + dlgW, dlgY + dlgH, 0xF510101A);
        graphics.fill(dlgX, dlgY, dlgX + dlgW, dlgY + 1, ColorUtility.getActiveColor());

        FontRenderUtility.drawString(graphics, "New Macro", dlgX + 10, dlgY + 10, 0xFFFFFFFF, true);
        FontRenderUtility.drawString(graphics, "Name:", dlgX + 10, dlgY + 32, 0xFF9E9EB0, false);

        int inputX = dlgX + 10;
        int inputY = dlgY + 46;
        int inputW = dlgW - 20;
        int inputH = 16;
        graphics.fill(inputX, inputY, inputX + inputW, inputY + inputH, 0xFF1A1A28);
        FontRenderUtility.drawString(graphics, newName.isEmpty() ? "MyMacro" : newName, inputX + 4, inputY + 3, newName.isEmpty() ? 0xFF505060 : 0xFFD0D0E0, false);

        int btnY = dlgY + 70;
        int btnW = 70;
        int btnH = 14;

        boolean okHovered = mouseX >= dlgX + 20 && mouseX <= dlgX + 20 + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
        graphics.fill(dlgX + 20, btnY, dlgX + 20 + btnW, btnY + btnH, okHovered ? ColorUtility.getActiveColor() : 0xFF202035);
        FontRenderUtility.drawString(graphics, "Create", dlgX + 20 + (btnW - FontRenderUtility.getStringWidth("Create")) / 2, btnY + 3, 0xFFFFFFFF, false);

        boolean cancelHovered = mouseX >= dlgX + dlgW - 20 - btnW && mouseX <= dlgX + dlgW - 20 && mouseY >= btnY && mouseY <= btnY + btnH;
        graphics.fill(dlgX + dlgW - 20 - btnW, btnY, dlgX + dlgW - 20, btnY + btnH, cancelHovered ? 0xFF303035 : 0xFF14141E);
        FontRenderUtility.drawString(graphics, "Cancel", dlgX + dlgW - 20 - btnW + (btnW - FontRenderUtility.getStringWidth("Cancel")) / 2, btnY + 3, 0xFFD0D0E0, false);
    }

    private void renderActionEditor(GuiGraphics graphics, int mouseX, int mouseY) {
        if (editingIndex < 0 || editingIndex >= macros.size()) return;
        Macro m = macros.get(editingIndex);

        int edX = this.width / 2 - 40;
        int edY = 50;
        int edW = this.width / 2 + 20;
        int edH = this.height - 100;

        graphics.fill(edX, edY, edX + edW, edY + edH, 0xF510101A);
        graphics.fill(edX, edY, edX + edW, edY + 1, ColorUtility.getActiveColor());

        FontRenderUtility.drawString(graphics, "Editing: " + m.getName(), edX + 10, edY + 8, 0xFFFFFFFF, true);

        int actionY = edY + 28;
        for (int i = 0; i < m.getActions().size(); i++) {
            MacroAction a = m.getActions().get(i);
            boolean hovered = mouseX >= edX + 4 && mouseX <= edX + edW - 30 && mouseY >= actionY && mouseY <= actionY + 14;
            graphics.fill(edX + 4, actionY, edX + edW - 30, actionY + 14, hovered ? 0xFF202035 : 0xFF14141E);
            FontRenderUtility.drawString(graphics, i + 1 + ". " + a.getDisplayString(), edX + 8, actionY + 3, 0xFFB0B0C0, false);

            boolean delHov = mouseX >= edX + edW - 26 && mouseX <= edX + edW - 6 && mouseY >= actionY + 1 && mouseY <= actionY + 13;
            graphics.fill(edX + edW - 26, actionY + 1, edX + edW - 6, actionY + 13, delHov ? 0xFF553333 : 0xFF1A1A28);
            FontRenderUtility.drawString(graphics, "X", edX + edW - 18, actionY + 3, 0xFFFF6666, false);

            actionY += 16;
        }

        int addY = actionY + 6;
        FontRenderUtility.drawString(graphics, "Add Action:", edX + 8, addY, 0xFF9E9EB0, false);
        int addInputY = addY + 14;

        String typeLabel = "Type: " + ACTION_TYPES[actionTypeIndex];
        FontRenderUtility.drawString(graphics, typeLabel, edX + 8, addInputY, 0xFFD0D0E0, false);

        int inputY = addInputY + 14;
        int inputW = edW - 20;
        graphics.fill(edX + 6, inputY, edX + 6 + inputW, inputY + 16, 0xFF1A1A28);
        FontRenderUtility.drawString(graphics, actionInput.isEmpty() ? "Enter value..." : actionInput, edX + 10, inputY + 3, actionInput.isEmpty() ? 0xFF505060 : 0xFFD0D0E0, false);

        int addBtnY = inputY + 22;
        boolean addHov = mouseX >= edX + 10 && mouseX <= edX + 90 && mouseY >= addBtnY && mouseY <= addBtnY + 14;
        graphics.fill(edX + 10, addBtnY, edX + 90, addBtnY + 14, addHov ? ColorUtility.getActiveColor() : 0xFF202035);
        FontRenderUtility.drawString(graphics, "Add Action", edX + 18, addBtnY + 3, 0xFFFFFFFF, false);

        boolean doneHov = mouseX >= edX + edW - 80 && mouseX <= edX + edW - 10 && mouseY >= addBtnY && mouseY <= addBtnY + 14;
        graphics.fill(edX + edW - 80, addBtnY, edX + edW - 10, addBtnY + 14, doneHov ? 0xFF303035 : 0xFF14141E);
        FontRenderUtility.drawString(graphics, "Done", edX + edW - 60, addBtnY + 3, 0xFFD0D0E0, false);
    }

    private void renderToolbar(GuiGraphics graphics, int mouseX, int mouseY, int activeColor) {
        int tbY = this.height - 40;
        int btnW = 80;
        int btnH = 20;

        int[] btnLabels = {-1, -1, -1, -1};
        String[] texts = {"Create", "Edit", "Delete", "Back"};

        for (int i = 0; i < 4; i++) {
            int bx = 20 + i * (btnW + 8);
            boolean hovered = mouseX >= bx && mouseX <= bx + btnW && mouseY >= tbY && mouseY <= tbY + btnH;
            int bg = hovered ? (i == 0 ? activeColor : 0xFF202035) : (i == 0 ? 0xFF252540 : 0xFF0D0D14);
            graphics.fill(bx, tbY, bx + btnW, tbY + btnH, bg);
            FontRenderUtility.drawString(graphics, texts[i], bx + (btnW - FontRenderUtility.getStringWidth(texts[i])) / 2, tbY + 6, 0xFFD0D0E0, false);
        }

        if (selectedIndex >= 0 && selectedIndex < macros.size()) {
            Macro m = macros.get(selectedIndex);
            String keyText = "Bind: " + (m.getKeyBind() > 0 ? getKeyName(m.getKeyBind()) : "None");
            int kx = this.width - 220;
            boolean bindHovered = mouseX >= kx && mouseX <= kx + 120 && mouseY >= tbY && mouseY <= tbY + btnH;
            graphics.fill(kx, tbY, kx + 120, tbY + btnH, bindHovered || bindingKey ? activeColor : 0xFF14141E);
            FontRenderUtility.drawString(graphics, bindingKey ? "Press a key..." : keyText, kx + 8, tbY + 6, 0xFFFFFFFF, false);

            boolean actionsHovered = mouseX >= this.width - 90 && mouseX <= this.width - 20 && mouseY >= tbY && mouseY <= tbY + btnH;
            graphics.fill(this.width - 90, tbY, this.width - 20, tbY + btnH, actionsHovered ? 0xFF303050 : 0xFF14141E);
            FontRenderUtility.drawString(graphics, "Actions", this.width - 80, tbY + 6, 0xFFD0D0E0, false);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean handled) {
        int mx = (int) event.x();
        int my = (int) event.y();
        int button = event.button();

        if (creatingNew) {
            return handleCreateDialogClick(mx, my, button);
        }
        if (editingActions) {
            return handleActionEditorClick(mx, my, button);
        }

        int tbY = this.height - 40;
        int btnW = 80;
        int btnH = 20;

        for (int i = 0; i < 4; i++) {
            int bx = 20 + i * (btnW + 8);
            if (mx >= bx && mx <= bx + btnW && my >= tbY && my <= tbY + btnH) {
                switch (i) {
                    case 0: creatingNew = true; newName = ""; return true;
                    case 1:
                        if (selectedIndex >= 0 && selectedIndex < macros.size()) {
                            editingActions = true;
                            editingIndex = selectedIndex;
                            actionInput = "";
                            actionTypeIndex = 0;
                        }
                        return true;
                    case 2:
                        if (selectedIndex >= 0 && selectedIndex < macros.size()) {
                            MacroManager.INSTANCE.removeMacro(macros.remove(selectedIndex));
                            selectedIndex = -1;
                            statusMessage = "Macro deleted";
                            statusTimer = 60;
                        }
                        return true;
                    case 3:
                        this.minecraft.setScreen(parent);
                        return true;
                }
            }
        }

        if (selectedIndex >= 0 && selectedIndex < macros.size()) {
            int kx = this.width - 220;
            if (mx >= kx && mx <= kx + 120 && my >= tbY && my <= tbY + btnH) {
                bindingKey = !bindingKey;
                return true;
            }
            if (mx >= this.width - 90 && mx <= this.width - 20 && my >= tbY && my <= tbY + btnH) {
                editingActions = true;
                editingIndex = selectedIndex;
                actionInput = "";
                actionTypeIndex = 0;
                return true;
            }
        }

        int listX = 20;
        int listY = 50;
        int listW = 250;
        int itemH = 20;

        int y = listY + 4 - scrollOffset;
        for (int i = 0; i < macros.size(); i++) {
            if (mx >= listX && mx <= listX + listW && my >= y && my <= y + itemH) {
                selectedIndex = i;
                return true;
            }
            y += itemH;
        }

        return super.mouseClicked(event, handled);
    }

    private boolean handleCreateDialogClick(int mx, int my, int button) {
        int dlgX = this.width / 2 - 100;
        int dlgY = this.height / 2 - 60;
        int dlgW = 200;
        int dlgH = 120;

        int btnY = dlgY + 70;
        int btnW = 70;
        int btnH = 14;

        if (mx >= dlgX + 20 && mx <= dlgX + 20 + btnW && my >= btnY && my <= btnY + btnH) {
            String name = newName.isEmpty() ? "Macro_" + (macros.size() + 1) : newName;
            Macro m = new Macro(name, -1, new ArrayList<>());
            macros.add(m);
            MacroManager.INSTANCE.addMacro(m);
            creatingNew = false;
            selectedIndex = macros.size() - 1;
            statusMessage = "Macro \"" + name + "\" created";
            statusTimer = 60;
            return true;
        }

        if (mx >= dlgX + dlgW - 20 - btnW && mx <= dlgX + dlgW - 20 && my >= btnY && my <= btnY + btnH) {
            creatingNew = false;
            return true;
        }

        int inputX = dlgX + 10;
        int inputY = dlgY + 46;
        int inputW = dlgW - 20;
        int inputH = 16;
        if (mx >= inputX && mx <= inputX + inputW && my >= inputY && my <= inputY + inputH) {
            return true;
        }

        return true;
    }

    private boolean handleActionEditorClick(int mx, int my, int button) {
        if (editingIndex < 0 || editingIndex >= macros.size()) return true;
        Macro m = macros.get(editingIndex);

        int edX = this.width / 2 - 40;
        int edY = 50;
        int edW = this.width / 2 + 20;

        int actionY = edY + 28;
        for (int i = 0; i < m.getActions().size(); i++) {
            if (mx >= edX + edW - 26 && mx <= edX + edW - 6 && my >= actionY + 1 && my <= actionY + 13) {
                m.getActions().remove(i);
                MacroManager.INSTANCE.save();
                return true;
            }
            actionY += 16;
        }

        int addY = actionY + 6;
        int inputY = addY + 28;
        int inputW = edW - 20;

        if (mx >= edX + 8 && mx <= edX + 8 + 120 && my >= addY + 14 && my <= addY + 26) {
            actionTypeIndex = (actionTypeIndex + 1) % ACTION_TYPES.length;
            return true;
        }

        if (mx >= edX + 6 && mx <= edX + 6 + inputW && my >= inputY && my <= inputY + 16) {
            return true;
        }

        int addBtnY = inputY + 22;
        if (mx >= edX + 10 && mx <= edX + 90 && my >= addBtnY && my <= addBtnY + 14) {
            if (!actionInput.isEmpty()) {
                m.getActions().add(new MacroAction(ACTION_TYPE_VALUES[actionTypeIndex], actionInput));
                actionInput = "";
                MacroManager.INSTANCE.save();
                return true;
            }
        }

        if (mx >= edX + edW - 80 && mx <= edX + edW - 10 && my >= addBtnY && my <= addBtnY + 14) {
            editingActions = false;
            editingIndex = -1;
            return true;
        }

        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();

        if (bindingKey && selectedIndex >= 0 && selectedIndex < macros.size()) {
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                macros.get(selectedIndex).setKeyBind(-1);
            } else {
                macros.get(selectedIndex).setKeyBind(key);
            }
            MacroManager.INSTANCE.save();
            bindingKey = false;
            return true;
        }

        if (creatingNew) {
            if (key == GLFW.GLFW_KEY_ESCAPE) { creatingNew = false; return true; }
            if (key == GLFW.GLFW_KEY_BACKSPACE && !newName.isEmpty()) { newName = newName.substring(0, newName.length() - 1); return true; }
            if (key == GLFW.GLFW_KEY_ENTER) {
                String name = newName.isEmpty() ? "Macro_" + (macros.size() + 1) : newName;
                Macro m = new Macro(name, -1, new ArrayList<>());
                macros.add(m);
                MacroManager.INSTANCE.addMacro(m);
                creatingNew = false;
                selectedIndex = macros.size() - 1;
                return true;
            }
            return true;
        }

        if (editingActions) {
            if (key == GLFW.GLFW_KEY_ESCAPE) { editingActions = false; editingIndex = -1; return true; }
            if (key == GLFW.GLFW_KEY_BACKSPACE && !actionInput.isEmpty()) { actionInput = actionInput.substring(0, actionInput.length() - 1); return true; }
            if (key == GLFW.GLFW_KEY_ENTER && !actionInput.isEmpty() && editingIndex >= 0 && editingIndex < macros.size()) {
                macros.get(editingIndex).getActions().add(new MacroAction(ACTION_TYPE_VALUES[actionTypeIndex], actionInput));
                actionInput = "";
                MacroManager.INSTANCE.save();
            }
            return true;
        }

        if (key == GLFW.GLFW_KEY_ESCAPE) {
            this.minecraft.setScreen(parent);
            return true;
        }

        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(net.minecraft.client.input.CharacterEvent event) {
        String text = event.codepointAsString();
        if (text.isEmpty()) return true;
        char c = text.charAt(0);
        if (c < 32 || c > 126) return true;

        if (creatingNew) {
            if (newName.length() < 24) newName += text;
            return true;
        }
        if (editingActions) {
            if (actionInput.length() < 64) actionInput += text;
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    public void tick() {
        super.tick();
        if (statusTimer > 0) statusTimer--;
    }

    private static String getKeyName(int key) {
        String name = GLFW.glfwGetKeyName(key, 0);
        if (name != null) return name.toUpperCase();
        if (key == GLFW.GLFW_KEY_RIGHT_SHIFT) return "RSHIFT";
        if (key == GLFW.GLFW_KEY_LEFT_SHIFT) return "LSHIFT";
        if (key == GLFW.GLFW_KEY_SPACE) return "SPACE";
        if (key == GLFW.GLFW_KEY_ESCAPE) return "ESC";
        if (key == GLFW.GLFW_KEY_ENTER) return "ENTER";
        if (key == GLFW.GLFW_KEY_TAB) return "TAB";
        return "KEY_" + key;
    }
}
