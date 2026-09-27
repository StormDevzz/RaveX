package ravex.gui.clickgui;
import ravex.utility.render.ColorUtility;

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
        super(Component.literal(ravex.utility.misc.LanguageUtility.t("macro_title")));
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
        SubScreenStyle.background(graphics, this.width, this.height);

        int activeColor = ColorUtility.getActiveColor();

        SubScreenStyle.header(graphics, this.width, ravex.utility.misc.LanguageUtility.t("macro_title"), ravex.utility.misc.LanguageUtility.t("macro_subtitle"), activeColor);

        if (statusTimer > 0 && !statusMessage.isEmpty()) {
            SubScreenStyle.statusPill(graphics, this.width, statusMessage, activeColor);
        }

        int listX = 20;
        int listY = 50;
        int listW = 250;
        int itemH = 20;

        SubScreenStyle.card(graphics, listX, listY, listW, this.height - 50 - listY, activeColor);

        int y = listY + 8 - scrollOffset;
        for (int i = 0; i < macros.size(); i++) {
            Macro m = macros.get(i);
            boolean hovered = mouseX >= listX && mouseX <= listX + listW && mouseY >= y && mouseY <= y + itemH;
            SubScreenStyle.row(graphics, listX + 4, y, listW - 8, itemH - 2, i == selectedIndex, hovered, activeColor);

            String keyName = m.getKeyBind() > 0 ? " [" + getKeyName(m.getKeyBind()) + "]" : "";
            FontRenderUtility.drawString(graphics, m.getName() + keyName, listX + 10, y + 5, 0xFFD0D0E0, false);

            int actionCount = m.getActions().size();
            String countStr = actionCount + " action" + (actionCount != 1 ? "s" : "");
            int cw = FontRenderUtility.getStringWidth(countStr);
            FontRenderUtility.drawString(graphics, countStr, listX + listW - cw - 12, y + 5, 0xFF707080, false);

            y += itemH;
        }
        if (macros.isEmpty()) {
            FontRenderUtility.drawString(graphics, ravex.utility.misc.LanguageUtility.t("macro_empty"), listX + 10, listY + 8, 0xFF505060, false);
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
        int activeColor = ColorUtility.getActiveColor();
        int dlgX = this.width / 2 - 100;
        int dlgY = this.height / 2 - 60;
        int dlgW = 200;
        int dlgH = 120;

        SubScreenStyle.modalBackdrop(graphics, this.width, this.height);
        SubScreenStyle.modal(graphics, dlgX, dlgY, dlgW, dlgH, activeColor);

        FontRenderUtility.drawString(graphics, ravex.utility.misc.LanguageUtility.t("macro_new"), dlgX + 10, dlgY + 10, 0xFFFFFFFF, true);
        SubScreenStyle.titleAccent(graphics, dlgX + 10, dlgY + 22, ravex.utility.misc.LanguageUtility.t("macro_new"), activeColor);
        FontRenderUtility.drawString(graphics, ravex.utility.misc.LanguageUtility.t("macro_name"), dlgX + 10, dlgY + 32, 0xFF9E9EB0, false);

        int inputX = dlgX + 10;
        int inputY = dlgY + 46;
        int inputW = dlgW - 20;
        int inputH = 16;
        SubScreenStyle.input(graphics, inputX, inputY, inputW, inputH, activeColor);
        FontRenderUtility.drawString(graphics, newName.isEmpty() ? "MyMacro" : newName, inputX + 4, inputY + 3, newName.isEmpty() ? 0xFF505060 : 0xFFD0D0E0, false);

        int btnY = dlgY + 70;
        int btnW = 70;
        int btnH = 14;

        boolean okHovered = mouseX >= dlgX + 20 && mouseX <= dlgX + 20 + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
        SubScreenStyle.button(graphics, dlgX + 20, btnY, btnW, btnH, okHovered, true, activeColor);
        String crv = ravex.utility.misc.LanguageUtility.t("macro_create");
        FontRenderUtility.drawString(graphics, crv, dlgX + 20 + (btnW - FontRenderUtility.getStringWidth(crv)) / 2, btnY + 3, 0xFFFFFFFF, false);

        boolean cancelHovered = mouseX >= dlgX + dlgW - 20 - btnW && mouseX <= dlgX + dlgW - 20 && mouseY >= btnY && mouseY <= btnY + btnH;
        SubScreenStyle.button(graphics, dlgX + dlgW - 20 - btnW, btnY, btnW, btnH, cancelHovered, false, activeColor);
        String ccv = ravex.utility.misc.LanguageUtility.t("cfg_cancel");
        FontRenderUtility.drawString(graphics, ccv, dlgX + dlgW - 20 - btnW + (btnW - FontRenderUtility.getStringWidth(ccv)) / 2, btnY + 3, 0xFFD0D0E0, false);
    }

    private void renderActionEditor(GuiGraphics graphics, int mouseX, int mouseY) {
        if (editingIndex < 0 || editingIndex >= macros.size()) return;
        Macro m = macros.get(editingIndex);
        int activeColor = ColorUtility.getActiveColor();

        int edX = this.width / 2 - 40;
        int edY = 50;
        int edW = this.width / 2 + 20;
        int edH = this.height - 100;

        SubScreenStyle.modal(graphics, edX, edY, edW, edH, activeColor);

        FontRenderUtility.drawString(graphics, ravex.utility.misc.LanguageUtility.t("macro_editing") + m.getName(), edX + 10, edY + 8, 0xFFFFFFFF, true);

        int actionY = edY + 28;
        for (int i = 0; i < m.getActions().size(); i++) {
            MacroAction a = m.getActions().get(i);
            boolean hovered = mouseX >= edX + 4 && mouseX <= edX + edW - 30 && mouseY >= actionY && mouseY <= actionY + 14;
            ravex.utility.render.Render2DUtility.drawRound(graphics, edX + 4, actionY, edW - 34, 14, 4, hovered ? ColorUtility.withAlpha(activeColor, 55) : 0xB014141E);
            FontRenderUtility.drawString(graphics, i + 1 + ". " + a.getDisplayString(), edX + 8, actionY + 3, 0xFFB0B0C0, false);

            boolean delHov = mouseX >= edX + edW - 26 && mouseX <= edX + edW - 6 && mouseY >= actionY + 1 && mouseY <= actionY + 13;
            ravex.utility.render.Render2DUtility.drawRound(graphics, edX + edW - 26, actionY + 1, 20, 12, 3, delHov ? 0xAA553333 : 0xB01A1A28);
            FontRenderUtility.drawString(graphics, "X", edX + edW - 18, actionY + 3, 0xFFFF6666, false);

            actionY += 16;
        }

        int addY = actionY + 6;
        FontRenderUtility.drawString(graphics, ravex.utility.misc.LanguageUtility.t("macro_add_action_label"), edX + 8, addY, 0xFF9E9EB0, false);
        int addInputY = addY + 14;

        String actionTypeLabel = switch (actionTypeIndex) {
            case 0 -> ravex.utility.misc.LanguageUtility.t("macro_toggle");
            case 1 -> ravex.utility.misc.LanguageUtility.t("macro_chat");
            case 2 -> ravex.utility.misc.LanguageUtility.t("macro_cmd");
            default -> ravex.utility.misc.LanguageUtility.t("macro_delay");
        };
        String typeLabel = ravex.utility.misc.LanguageUtility.t("macro_type") + actionTypeLabel;
        FontRenderUtility.drawString(graphics, typeLabel, edX + 8, addInputY, 0xFFD0D0E0, false);

        int inputY = addInputY + 14;
        int inputW = edW - 20;
        SubScreenStyle.input(graphics, edX + 6, inputY, inputW, 16, activeColor);
        FontRenderUtility.drawString(graphics, actionInput.isEmpty() ? ravex.utility.misc.LanguageUtility.t("macro_enter_value") : actionInput, edX + 10, inputY + 3, actionInput.isEmpty() ? 0xFF505060 : 0xFFD0D0E0, false);

        int addBtnY = inputY + 22;
        boolean addHov = mouseX >= edX + 10 && mouseX <= edX + 90 && mouseY >= addBtnY && mouseY <= addBtnY + 14;
        SubScreenStyle.button(graphics, edX + 10, addBtnY, 80, 14, addHov, true, activeColor);
        FontRenderUtility.drawString(graphics, ravex.utility.misc.LanguageUtility.t("macro_add_action"), edX + 18, addBtnY + 3, 0xFFFFFFFF, false);

        boolean doneHov = mouseX >= edX + edW - 80 && mouseX <= edX + edW - 10 && mouseY >= addBtnY && mouseY <= addBtnY + 14;
        SubScreenStyle.button(graphics, edX + edW - 80, addBtnY, 70, 14, doneHov, false, activeColor);
        FontRenderUtility.drawString(graphics, ravex.utility.misc.LanguageUtility.t("macro_done"), edX + edW - 60, addBtnY + 3, 0xFFD0D0E0, false);
    }

    private void renderToolbar(GuiGraphics graphics, int mouseX, int mouseY, int activeColor) {
        int tbY = this.height - 40;
        int btnW = 80;
        int btnH = 20;

        String[] texts = {ravex.utility.misc.LanguageUtility.t("macro_create"), ravex.utility.misc.LanguageUtility.t("macro_edit"), ravex.utility.misc.LanguageUtility.t("cfg_delete"), ravex.utility.misc.LanguageUtility.t("cfg_back")};

        int actionsX = this.width - 90;
        int pillEnd = (selectedIndex >= 0 && selectedIndex < macros.size()) ? actionsX + btnW + 8 : 20 + 3 * (btnW + 8) + btnW + 8;
        SubScreenStyle.toolbarPill(graphics, 12, tbY - 6, pillEnd - 12, btnH + 12);

        for (int i = 0; i < 4; i++) {
            int bx = 20 + i * (btnW + 8);
            boolean hovered = mouseX >= bx && mouseX <= bx + btnW && mouseY >= tbY && mouseY <= tbY + btnH;
            SubScreenStyle.button(graphics, bx, tbY, btnW, btnH, hovered, i == 0, activeColor);
            FontRenderUtility.drawString(graphics, texts[i], bx + (btnW - FontRenderUtility.getStringWidth(texts[i])) / 2, tbY + 6, 0xFFFFFFFF, false);
        }

        if (selectedIndex >= 0 && selectedIndex < macros.size()) {
            Macro m = macros.get(selectedIndex);
            String keyText = ravex.utility.misc.LanguageUtility.t("macro_bind") + (m.getKeyBind() > 0 ? getKeyName(m.getKeyBind()) : ravex.utility.misc.LanguageUtility.t("macro_none"));
            int kx = this.width - 220;
            boolean bindHovered = mouseX >= kx && mouseX <= kx + 120 && mouseY >= tbY && mouseY <= tbY + btnH;
            SubScreenStyle.button(graphics, kx, tbY, 120, btnH, bindHovered || bindingKey, bindingKey || bindHovered, activeColor);
            FontRenderUtility.drawString(graphics, bindingKey ? ravex.utility.misc.LanguageUtility.t("macro_press_key") : keyText, kx + 8, tbY + 6, 0xFFFFFFFF, false);

            boolean actionsHovered = mouseX >= actionsX && mouseX <= actionsX + 70 && mouseY >= tbY && mouseY <= tbY + btnH;
            SubScreenStyle.button(graphics, actionsX, tbY, 70, btnH, actionsHovered, false, activeColor);
            FontRenderUtility.drawString(graphics, ravex.utility.misc.LanguageUtility.t("macro_actions"), actionsX + 10, tbY + 6, 0xFFFFFFFF, false);
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
                            statusMessage = ravex.utility.misc.LanguageUtility.t("macro_deleted");
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

        int y = listY + 8 - scrollOffset;
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
            statusMessage = ravex.utility.misc.LanguageUtility.t("macro_created", name);
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
