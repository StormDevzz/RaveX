package ravex.gui.clickgui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
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

    // New config input
    private boolean creatingNew = false;
    private String newName = "";

    // Status message
    private String status = "";
    private int statusTimer = 0;

    public ConfigsScreen(Screen parent) {
        super(Component.literal("RaveX Configurations"));
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

    // ─── Rendering ───────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        // Blurred dark background
        graphics.fillGradient(0, 0, this.width, this.height, 0xEA06060E, 0xEA0C0C1A);

        int activeColor = ColorUtility.getActiveColor();

        // ── Title bar ───────────────────────────────────────────────────────────
        graphics.fill(0, 0, this.width, 38, 0xCC08081A);
        graphics.fill(0, 37, this.width, 38, activeColor);
        FontRenderUtility.drawString(graphics, "Configurations", 18, 8, 0xFFFFFFFF, true);
        FontRenderUtility.drawString(graphics, "Manage saved client presets", 18, 21, 0xFF7070A0, false);

        int centerX = this.width / 2;

        // ── Left panel: config list ──────────────────────────────────────────────
        int listX = 16;
        int listY = 48;
        int listW = centerX - 24;
        int listH = this.height - 90;
        int itemH = 26;

        graphics.fill(listX, listY, listX + listW, listY + listH, 0xBB08081A);
        graphics.fill(listX, listY, listX + listW, listY + 1, ColorUtility.withAlpha(activeColor, 80));

        FontRenderUtility.drawString(graphics, "§7Saved Presets  §8[" + configs.size() + "]", listX + 6, listY + 5, 0xFF9090B0, false);

        // Scrollable config list
        int visibleItemsStart = 12;
        int maxVisible = (listH - visibleItemsStart) / itemH;
        int clampedScroll = Math.max(0, Math.min(scrollOffset, Math.max(0, configs.size() - maxVisible)));

        // Use scissor to clip
        int ly = listY + visibleItemsStart;
        for (int i = clampedScroll; i < configs.size() && i < clampedScroll + maxVisible; i++) {
            String cfg = configs.get(i);
            boolean hovered = mouseX >= listX && mouseX <= listX + listW && mouseY >= ly && mouseY <= ly + itemH;
            boolean selected = i == selectedIndex;

            int bg = selected ? ColorUtility.withAlpha(activeColor, 50) :
                     hovered ? 0xCC141422 : 0x00000000;
            graphics.fill(listX, ly, listX + listW, ly + itemH - 1, bg);

            if (selected) {
                graphics.fill(listX, ly, listX + 2, ly + itemH - 1, activeColor);
            }

            // Config name
            int textColor = selected ? 0xFFFFFFFF : (hovered ? 0xFFD0D0E8 : 0xFF909090);
            FontRenderUtility.drawString(graphics, cfg, listX + 10, ly + 7, textColor, false);

            // Active badge if this is the "default" config
            if (cfg.equalsIgnoreCase("default")) {
                String badge = "AUTO";
                int bw = FontRenderUtility.getStringWidth(badge) + 6;
                int bx = listX + listW - bw - 6;
                graphics.fill(bx, ly + 6, bx + bw, ly + itemH - 7, 0x55AA88FF);
                FontRenderUtility.drawString(graphics, badge, bx + 3, ly + 8, 0xFFCCBBFF, false);
            }

            ly += itemH;
        }

        if (configs.isEmpty()) {
            FontRenderUtility.drawString(graphics, "No presets saved yet.", listX + 10, listY + 24, 0xFF404060, false);
            FontRenderUtility.drawString(graphics, "Click \"Save New\" below.", listX + 10, listY + 36, 0xFF404060, false);
        }

        // ── Right panel: player profile + selected config info ────────────────────
        int rightX = centerX + 8;
        int rightW = this.width - rightX - 16;
        int rightY = 48;
        int rightH = this.height - 90;

        graphics.fill(rightX, rightY, rightX + rightW, rightY + rightH, 0xBB08081A);
        graphics.fill(rightX, rightY, rightX + rightW, rightY + 1, ColorUtility.withAlpha(activeColor, 80));

        // Player model
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            int modelX = rightX + rightW / 2;
            int modelY1 = rightY + 16;
            int modelY2 = rightY + 100;
            int scale = 35;
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                graphics, modelX - scale, modelY1, modelX + scale, modelY2,
                scale, 0.0625f, mouseX, mouseY, mc.player);

            // Player name + skin type
            String playerName = mc.player.getName().getString();
            int nameW = FontRenderUtility.getStringWidth(playerName);
            graphics.fill(rightX + (rightW - nameW - 12) / 2, rightY + 100,
                          rightX + (rightW + nameW + 12) / 2, rightY + 114,
                          0x88000010);
            FontRenderUtility.drawString(graphics, playerName,
                rightX + (rightW - nameW) / 2, rightY + 102, activeColor, true);

            // Check skin type
            var skinType = mc.player.getSkin().model();
            String modelName = skinType.name().equals("slim") ? "Alex model" : "Steve model";
            int mnW = FontRenderUtility.getStringWidth(modelName);
            FontRenderUtility.drawString(graphics, modelName,
                rightX + (rightW - mnW) / 2, rightY + 113, 0xFF505070, false);
        } else {
            FontRenderUtility.drawString(graphics, "Not in a world", rightX + 8, rightY + 20, 0xFF505070, false);
        }

        // Divider
        graphics.fill(rightX + 8, rightY + 122, rightX + rightW - 8, rightY + 123, ColorUtility.withAlpha(activeColor, 40));

        // Selected config details
        if (selectedIndex >= 0 && selectedIndex < configs.size()) {
            String selCfg = configs.get(selectedIndex);
            FontRenderUtility.drawString(graphics, "Selected:", rightX + 8, rightY + 130, 0xFF707090, false);
            FontRenderUtility.drawString(graphics, selCfg, rightX + 8, rightY + 141, 0xFFD0D0FF, true);
        } else {
            FontRenderUtility.drawString(graphics, "No preset selected", rightX + 8, rightY + 130, 0xFF404060, false);
        }

        // Status message
        if (statusTimer > 0 && !status.isEmpty()) {
            graphics.fill(rightX + 6, rightY + 155, rightX + rightW - 6, rightY + 167, 0x44001100);
            FontRenderUtility.drawString(graphics, status, rightX + 8, rightY + 157, 0xFFAAFFAA, false);
        }

        // ── Bottom toolbar ───────────────────────────────────────────────────────
        int toolbarY = this.height - 40;
        graphics.fill(0, toolbarY, this.width, toolbarY + 1, ColorUtility.withAlpha(activeColor, 60));
        graphics.fill(0, toolbarY + 1, this.width, this.height, 0xBB06060F);

        renderToolbar(graphics, mouseX, mouseY, activeColor, toolbarY);

        // ── New config dialog ────────────────────────────────────────────────────
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

        // Buttons: Load, Save New, Delete, Back
        String[] labels = {"Load", "Save New", "Delete", "Back"};
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
            int bg = hov ? (isAction ? activeColor : 0xFF202038) : (isAction ? 0xFF16162A : 0xFF0C0C1A);
            graphics.fill(bx, btnY, bx + btnW, btnY + btnH, bg);
            if (hov) graphics.fill(bx, btnY + btnH - 1, bx + btnW, btnY + btnH, activeColor);

            int textW = FontRenderUtility.getStringWidth(labels[i]);
            FontRenderUtility.drawString(graphics, labels[i], bx + (btnW - textW) / 2, btnY + 6, 0xFFD0D0E8, false);
        }
    }

    private void renderNewDialog(GuiGraphics graphics, int mouseX, int mouseY, int activeColor) {
        // Dim background
        graphics.fill(0, 0, this.width, this.height, 0x99000000);

        int dlgW = 240;
        int dlgH = 110;
        int dlgX = (this.width - dlgW) / 2;
        int dlgY = (this.height - dlgH) / 2;

        // Dialog window
        graphics.fill(dlgX, dlgY, dlgX + dlgW, dlgY + dlgH, 0xF50C0C1C);
        graphics.fill(dlgX, dlgY, dlgX + dlgW, dlgY + 1, activeColor);
        graphics.fill(dlgX, dlgY, dlgX + 1, dlgY + dlgH, ColorUtility.withAlpha(activeColor, 80));
        graphics.fill(dlgX + dlgW - 1, dlgY, dlgX + dlgW, dlgY + dlgH, ColorUtility.withAlpha(activeColor, 80));

        FontRenderUtility.drawString(graphics, "Save Preset As:", dlgX + 12, dlgY + 12, 0xFFFFFFFF, true);

        // Text input
        int inputX = dlgX + 12;
        int inputY = dlgY + 32;
        int inputW = dlgW - 24;
        int inputH = 18;
        graphics.fill(inputX, inputY, inputX + inputW, inputY + inputH, 0xFF181830);
        graphics.fill(inputX, inputY + inputH - 1, inputX + inputW, inputY + inputH, activeColor);

        String display = newName.isEmpty() ? "Preset name..." : newName + "│";
        int textCol = newName.isEmpty() ? 0xFF404060 : 0xFFD0D0F0;
        FontRenderUtility.drawString(graphics, display, inputX + 5, inputY + 4, textCol, false);

        // Buttons
        int btnY = dlgY + 64;
        int btnW = 90;
        int btnH = 18;

        boolean saveHov = mouseX >= dlgX + 10 && mouseX <= dlgX + 10 + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
        boolean cancelHov = mouseX >= dlgX + dlgW - 10 - btnW && mouseX <= dlgX + dlgW - 10 && mouseY >= btnY && mouseY <= btnY + btnH;

        graphics.fill(dlgX + 10, btnY, dlgX + 10 + btnW, btnY + btnH, saveHov ? activeColor : 0xFF14142A);
        FontRenderUtility.drawString(graphics, "Save",
            dlgX + 10 + (btnW - FontRenderUtility.getStringWidth("Save")) / 2, btnY + 5, 0xFFFFFFFF, false);

        graphics.fill(dlgX + dlgW - 10 - btnW, btnY, dlgX + dlgW - 10, btnY + btnH, cancelHov ? 0xFF303040 : 0xFF181820);
        FontRenderUtility.drawString(graphics, "Cancel",
            dlgX + dlgW - 10 - btnW + (btnW - FontRenderUtility.getStringWidth("Cancel")) / 2, btnY + 5, 0xFFD0D0D0, false);
    }

    // ─── Input handling ──────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean handled) {
        int mx = (int) event.x();
        int my = (int) event.y();

        if (creatingNew) return handleDialogClick(mx, my);

        // Toolbar buttons
        int toolbarY = this.height - 40;
        int btnH = 20;
        int btnY = toolbarY + 10;
        int btnW = 80;
        int gap = 6;
        int totalW = 4 * btnW + 3 * gap;
        int startX = (this.width - totalW) / 2;

        // Load
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
        // Save New
        int x1 = startX + btnW + gap;
        if (mx >= x1 && mx <= x1 + btnW && my >= btnY && my <= btnY + btnH) {
            creatingNew = true;
            newName = "";
            return true;
        }
        // Delete
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
        // Back
        int x3 = startX + (btnW + gap) * 3;
        if (mx >= x3 && mx <= x3 + btnW && my >= btnY && my <= btnY + btnH) {
            this.minecraft.setScreen(parent);
            return true;
        }

        // Config list clicks
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
                        // Double-click effect: load
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

        // Save
        if (mx >= dlgX + 10 && mx <= dlgX + 10 + btnW && my >= btnY && my <= btnY + btnH) {
            saveNewConfig();
            return true;
        }
        // Cancel
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
