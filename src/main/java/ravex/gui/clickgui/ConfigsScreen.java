package ravex.gui.clickgui;
import ravex.utility.render.ColorUtility;

import net.minecraft.client.gui.GuiGraphics;
import ravex.mcwrapper.MinecraftWrapper;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import ravex.manager.ConfigManager;
import ravex.utility.render.FontRenderUtility;

import java.util.ArrayList;
import java.util.List;

public class ConfigsScreen extends Screen {

    private final Screen parent;
    private List<String> configs = new ArrayList<>();
    private int selectedIndex = -1;
    private int scrollOffset = 0;


    private boolean creatingNew = false;
    private String newName = "";


    private String status = "";
    private int statusTimer = 0;

    public ConfigsScreen(Screen parent) {
        super(Component.literal(ravex.utility.misc.LanguageUtility.t("cfg_title")));
        this.parent = parent;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    protected void init() {
        super.init();
        refreshList();
    }

    private void refreshList() {
        configs.clear();
        configs.addAll(ConfigManager.INSTANCE.list());
        configs.sort(String::compareToIgnoreCase);
    }



    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {

        SubScreenStyle.background(graphics, this.width, this.height);

        int activeColor = ColorUtility.getActiveColor();

        SubScreenStyle.header(graphics, this.width, ravex.utility.misc.LanguageUtility.t("cfg_title"), ravex.utility.misc.LanguageUtility.t("cfg_subtitle"), activeColor);

        int centerX = this.width / 2;


        int listX = 16;
        int listY = 48;
        int listW = centerX - 24;
        int listH = this.height - 90;
        int itemH = 26;

        SubScreenStyle.card(graphics, listX, listY, listW, listH, activeColor);

        FontRenderUtility.drawString(graphics, "§7" + ravex.utility.misc.LanguageUtility.t("cfg_saved") + "  §8[" + configs.size() + "]", listX + 6, listY + 5, 0xFF9090B0, false);


        int visibleItemsStart = 12;
        int maxVisible = (listH - visibleItemsStart) / itemH;
        int clampedScroll = Math.max(0, Math.min(scrollOffset, Math.max(0, configs.size() - maxVisible)));


        int ly = listY + visibleItemsStart;
        for (int i = clampedScroll; i < configs.size() && i < clampedScroll + maxVisible; i++) {
            String cfg = configs.get(i);
            boolean hovered = mouseX >= listX && mouseX <= listX + listW && mouseY >= ly && mouseY <= ly + itemH;
            boolean selected = i == selectedIndex;

            SubScreenStyle.row(graphics, listX + 3, ly + 1, listW - 6, itemH - 2, selected, hovered, activeColor);


            int textColor = selected ? 0xFFFFFFFF : (hovered ? 0xFFD0D0E8 : 0xFF909090);
            FontRenderUtility.drawString(graphics, cfg, listX + 10, ly + 7, textColor, false);


            if (cfg.equalsIgnoreCase("default")) {
                String badge = ravex.utility.misc.LanguageUtility.t("cfg_auto");
                int bw = FontRenderUtility.getStringWidth(badge) + 6;
                int bx = listX + listW - bw - 6;
                ravex.utility.render.Render2DUtility.drawRound(graphics, bx, ly + 6, bw, itemH - 13, 3, 0x55AA88FF);
                FontRenderUtility.drawString(graphics, badge, bx + 3, ly + 8, 0xFFCCBBFF, false);
            }

            ly += itemH;
        }

        if (configs.isEmpty()) {
            FontRenderUtility.drawString(graphics, ravex.utility.misc.LanguageUtility.t("cfg_empty"), listX + 10, listY + 24, 0xFF404060, false);
            FontRenderUtility.drawString(graphics, ravex.utility.misc.LanguageUtility.t("cfg_empty_hint"), listX + 10, listY + 36, 0xFF404060, false);
        }


        int rightX = centerX + 8;
        int rightW = this.width - rightX - 16;
        int rightY = 48;
        int rightH = this.height - 90;

        SubScreenStyle.card(graphics, rightX, rightY, rightW, rightH, activeColor);


        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() != null) {
            int modelX = rightX + rightW / 2;
            int modelY1 = rightY + 16;
            int modelY2 = rightY + 100;
            int scale = 35;
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                graphics, modelX - scale, modelY1, modelX + scale, modelY2,
                scale, 0.0625f, mouseX, mouseY, mc.getPlayer());


            String playerName = mc.getPlayer().getName().getString();
            int nameW = FontRenderUtility.getStringWidth(playerName);
            ravex.utility.render.Render2DUtility.drawRound(graphics, rightX + (rightW - nameW - 12) / 2, rightY + 100,
                          nameW + 12, 14, 4, 0x88000010);
            FontRenderUtility.drawString(graphics, playerName,
                rightX + (rightW - nameW) / 2, rightY + 102, activeColor, true);


            var skinType = mc.getPlayer().getSkin().model();
            String modelName = skinType.name().equals("slim") ? ravex.utility.misc.LanguageUtility.t("cfg_model_slim") : ravex.utility.misc.LanguageUtility.t("cfg_model_wide");
            int mnW = FontRenderUtility.getStringWidth(modelName);
            FontRenderUtility.drawString(graphics, modelName,
                rightX + (rightW - mnW) / 2, rightY + 113, 0xFF505070, false);
        } else {
            FontRenderUtility.drawString(graphics, ravex.utility.misc.LanguageUtility.t("cfg_not_in_world"), rightX + 8, rightY + 20, 0xFF505070, false);
        }


        graphics.fill(rightX + 8, rightY + 122, rightX + rightW - 8, rightY + 123, ColorUtility.withAlpha(activeColor, 40));


        if (selectedIndex >= 0 && selectedIndex < configs.size()) {
            String selCfg = configs.get(selectedIndex);
            FontRenderUtility.drawString(graphics, ravex.utility.misc.LanguageUtility.t("cfg_selected"), rightX + 8, rightY + 130, 0xFF707090, false);
            FontRenderUtility.drawString(graphics, selCfg, rightX + 8, rightY + 141, 0xFFD0D0FF, true);
        } else {
            FontRenderUtility.drawString(graphics, ravex.utility.misc.LanguageUtility.t("cfg_no_selected"), rightX + 8, rightY + 130, 0xFF404060, false);
        }


        if (statusTimer > 0 && !status.isEmpty()) {
            ravex.utility.render.Render2DUtility.drawRound(graphics, rightX + 6, rightY + 155, rightW - 12, 12, 4, 0x44001100);
            FontRenderUtility.drawString(graphics, status, rightX + 8, rightY + 157, 0xFFAAFFAA, false);
        }


        int toolbarY = this.height - 40;
        int tbBtnW = 80;
        int tbGap = 6;
        int tbTotalW = 4 * tbBtnW + 3 * tbGap;
        int tbStartX = (this.width - tbTotalW) / 2;
        SubScreenStyle.toolbarPill(graphics, tbStartX - 12, toolbarY + 4, tbTotalW + 24, 32);

        renderToolbar(graphics, mouseX, mouseY, activeColor, toolbarY);


        if (creatingNew) {
            renderNewDialog(graphics, mouseX, mouseY, activeColor);
        }

        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    private void renderToolbar(GuiGraphics graphics, int mouseX, int mouseY, int activeColor, int toolbarY) {
        int btnH = 20;
        int btnY = toolbarY + 10;
        int btnW = 80;
        int gap = 6;


        String[] labels = {ravex.utility.misc.LanguageUtility.t("cfg_load"), ravex.utility.misc.LanguageUtility.t("cfg_save_new"), ravex.utility.misc.LanguageUtility.t("cfg_delete"), ravex.utility.misc.LanguageUtility.t("cfg_back")};
        int[] xPositions = new int[labels.length];
        int totalBtns = labels.length;
        int totalW = totalBtns * btnW + (totalBtns - 1) * gap;
        int startX = (this.width - totalW) / 2;
        for (int i = 0; i < labels.length; i++) {
            xPositions[i] = startX + i * (btnW + gap);
        }

        for (int i = 0; i < labels.length; i++) {
            int bx = xPositions[i];
            boolean hov = mouseX >= bx && mouseX <= bx + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
            boolean isAction = i < 2;
            SubScreenStyle.button(graphics, bx, btnY, btnW, btnH, hov, isAction, activeColor);

            int textW = FontRenderUtility.getStringWidth(labels[i]);
            FontRenderUtility.drawString(graphics, labels[i], bx + (btnW - textW) / 2, btnY + 6, 0xFFFFFFFF, false);
        }
    }

    private void renderNewDialog(GuiGraphics graphics, int mouseX, int mouseY, int activeColor) {

        SubScreenStyle.modalBackdrop(graphics, this.width, this.height);

        int dlgW = 240;
        int dlgH = 110;
        int dlgX = (this.width - dlgW) / 2;
        int dlgY = (this.height - dlgH) / 2;

        SubScreenStyle.modal(graphics, dlgX, dlgY, dlgW, dlgH, activeColor);

            FontRenderUtility.drawString(graphics, ravex.utility.misc.LanguageUtility.t("cfg_save_as"), dlgX + 12, dlgY + 12, 0xFFFFFFFF, true);
            SubScreenStyle.titleAccent(graphics, dlgX + 12, dlgY + 24, ravex.utility.misc.LanguageUtility.t("cfg_save_as"), activeColor);


        int inputX = dlgX + 12;
        int inputY = dlgY + 32;
        int inputW = dlgW - 24;
        int inputH = 18;
        SubScreenStyle.input(graphics, inputX, inputY, inputW, inputH, activeColor);

        String display = newName.isEmpty() ? ravex.utility.misc.LanguageUtility.t("cfg_name_hint") : newName + "│";
        int textCol = newName.isEmpty() ? 0xFF404060 : 0xFFD0D0F0;
        FontRenderUtility.drawString(graphics, display, inputX + 5, inputY + 4, textCol, false);


        int btnY = dlgY + 64;
        int btnW = 90;
        int btnH = 18;

        boolean saveHov = mouseX >= dlgX + 10 && mouseX <= dlgX + 10 + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
        boolean cancelHov = mouseX >= dlgX + dlgW - 10 - btnW && mouseX <= dlgX + dlgW - 10 && mouseY >= btnY && mouseY <= btnY + btnH;

        SubScreenStyle.button(graphics, dlgX + 10, btnY, btnW, btnH, saveHov, true, activeColor);
        String dlgSave = ravex.utility.misc.LanguageUtility.t("cfg_save");
        FontRenderUtility.drawString(graphics, dlgSave,
            dlgX + 10 + (btnW - FontRenderUtility.getStringWidth(dlgSave)) / 2, btnY + 5, 0xFFFFFFFF, false);

        SubScreenStyle.button(graphics, dlgX + dlgW - 10 - btnW, btnY, btnW, btnH, cancelHov, false, activeColor);
        String dlgCancel = ravex.utility.misc.LanguageUtility.t("cfg_cancel");
        FontRenderUtility.drawString(graphics, dlgCancel,
            dlgX + dlgW - 10 - btnW + (btnW - FontRenderUtility.getStringWidth(dlgCancel)) / 2, btnY + 5, 0xFFD0D0D0, false);
    }



    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean handled) {
        int mx = (int) event.x();
        int my = (int) event.y();

        if (creatingNew) return handleDialogClick(mx, my);


        int toolbarY = this.height - 40;
        int btnH = 20;
        int btnY = toolbarY + 10;
        int btnW = 80;
        int gap = 6;
        int totalW = 4 * btnW + 3 * gap;
        int startX = (this.width - totalW) / 2;


        if (mx >= startX && mx <= startX + btnW && my >= btnY && my <= btnY + btnH) {
            if (selectedIndex >= 0 && selectedIndex < configs.size()) {
                String name = configs.get(selectedIndex);
                if (ConfigManager.INSTANCE.load(name)) {
                    status = "§aLoaded: " + name;
                } else {
                    status = "§cFailed to load: " + name;
                }
                statusTimer = 80;
            }
            return true;
        }

        int x1 = startX + btnW + gap;
        if (mx >= x1 && mx <= x1 + btnW && my >= btnY && my <= btnY + btnH) {
            creatingNew = true;
            newName = "";
            return true;
        }

        int x2 = startX + (btnW + gap) * 2;
        if (mx >= x2 && mx <= x2 + btnW && my >= btnY && my <= btnY + btnH) {
            if (selectedIndex >= 0 && selectedIndex < configs.size()) {
                String name = configs.get(selectedIndex);
                ConfigManager.INSTANCE.delete(name);
                refreshList();
                status = "§cDeleted: " + name;
                statusTimer = 80;
                if (selectedIndex >= configs.size()) selectedIndex = configs.size() - 1;
            }
            return true;
        }

        int x3 = startX + (btnW + gap) * 3;
        if (mx >= x3 && mx <= x3 + btnW && my >= btnY && my <= btnY + btnH) {
            this.minecraft.setScreen(parent);
            return true;
        }


        int centerX = this.width / 2;
        int listX = 16;
        int listY = 48;
        int listW = centerX - 24;
        int listH = this.height - 90;
        int itemH = 26;
        int visStart = 12;
        int maxVisible = (listH - visStart) / itemH;

        if (mx >= listX && mx <= listX + listW) {
            int ly = listY + visStart;
            for (int i = scrollOffset; i < configs.size() && i < scrollOffset + maxVisible; i++) {
                if (my >= ly && my <= ly + itemH) {
                    if (selectedIndex == i) {

                        if (ConfigManager.INSTANCE.load(configs.get(i))) {
                            status = "§aLoaded: " + configs.get(i);
                            statusTimer = 80;
                        }
                    }
                    selectedIndex = i;
                    return true;
                }
                ly += itemH;
            }
        }

        return super.mouseClicked(event, handled);
    }

    private boolean handleDialogClick(int mx, int my) {
        int dlgW = 240;
        int dlgX = (this.width - dlgW) / 2;
        int dlgY = (this.height - 110) / 2;
        int btnY = dlgY + 64;
        int btnW = 90;
        int btnH = 18;


        if (mx >= dlgX + 10 && mx <= dlgX + 10 + btnW && my >= btnY && my <= btnY + btnH) {
            saveNewConfig();
            return true;
        }

        if (mx >= dlgX + dlgW - 10 - btnW && mx <= dlgX + dlgW - 10 && my >= btnY && my <= btnY + btnH) {
            creatingNew = false;
            return true;
        }
        return true;
    }

    private void saveNewConfig() {
        String name = newName.isEmpty() ? "preset_" + (configs.size() + 1) : newName;
        if (ConfigManager.INSTANCE.save(name)) {
            refreshList();
            status = "§aSaved: " + name;
            statusTimer = 80;
            selectedIndex = configs.indexOf(name);
        } else {
            status = "§cFailed to save: " + name;
            statusTimer = 80;
        }
        creatingNew = false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scrollOffset = Math.max(0, scrollOffset - (int) Math.signum(scrollY));
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (creatingNew) {
            if (key == GLFW.GLFW_KEY_ESCAPE) { creatingNew = false; return true; }
            if (key == GLFW.GLFW_KEY_ENTER) { saveNewConfig(); return true; }
            if (key == GLFW.GLFW_KEY_BACKSPACE && !newName.isEmpty()) {
                newName = newName.substring(0, newName.length() - 1);
                return true;
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
    public boolean charTyped(CharacterEvent event) {
        if (creatingNew) {
            char c = event.codepointAsString().isEmpty() ? 0 : event.codepointAsString().charAt(0);
            if (c >= 32 && c <= 126 && newName.length() < 32) newName += c;
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    public void tick() {
        super.tick();
        if (statusTimer > 0) statusTimer--;
    }
}
