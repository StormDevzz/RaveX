package ravex.gui.clickgui;
import ravex.utility.render.ColorUtility;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import ravex.gui.profile.Profile;
import ravex.manager.ProfileManager;
import ravex.utility.render.FontRenderUtility;

import java.util.ArrayList;
import java.util.List;

public class ProfilesScreen extends Screen {
    private final Screen parent;
    private List<Profile> profiles;
    private int selectedIndex = -1;
    private int scrollOffset;

    private boolean creatingNew;
    private String newName = "";

    private String statusMessage = "";
    private int statusTimer;

    public ProfilesScreen(Screen parent) {
        super(Component.literal(ravex.utility.misc.LanguageUtility.t("prof_title")));
        this.parent = parent;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    protected void init() {
        super.init();
        profiles = new ArrayList<>(ProfileManager.INSTANCE.getProfiles());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        SubScreenStyle.background(graphics, this.width, this.height);

        int activeColor = ColorUtility.getActiveColor();

        SubScreenStyle.header(graphics, this.width, ravex.utility.misc.LanguageUtility.t("prof_title"), ravex.utility.misc.LanguageUtility.t("prof_subtitle"), activeColor);

        if (statusTimer > 0 && !statusMessage.isEmpty()) {
            SubScreenStyle.statusPill(graphics, this.width, statusMessage, activeColor);
        }

        int listX = 20;
        int listY = 50;
        int listW = 250;
        int itemH = 24;

        SubScreenStyle.card(graphics, listX, listY, listW, this.height - 50 - listY, activeColor);

        int y = listY + 8 - scrollOffset;
        for (int i = 0; i < profiles.size(); i++) {
            Profile p = profiles.get(i);
            boolean hovered = mouseX >= listX && mouseX <= listX + listW && mouseY >= y && mouseY <= y + itemH;
            SubScreenStyle.row(graphics, listX + 4, y, listW - 8, itemH - 2, i == selectedIndex, hovered, activeColor);

            FontRenderUtility.drawString(graphics, p.getName(), listX + 10, y + 4, 0xFFD0D0E0, false);

            int modCount = p.getModuleStates().size();
            String info = modCount + " modules";
            int iw = FontRenderUtility.getStringWidth(info);
            FontRenderUtility.drawString(graphics, info, listX + listW - iw - 12, y + 4, 0xFF707080, false);

            y += itemH;
        }
        if (profiles.isEmpty()) {
            FontRenderUtility.drawString(graphics, "No profiles yet. Capture current config to create one.", listX + 10, listY + 8, 0xFF505060, false);
        }

        if (creatingNew) {
            renderCreateDialog(graphics, mouseX, mouseY, activeColor);
        } else {
            renderToolbar(graphics, mouseX, mouseY, activeColor);
        }

        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    private void renderCreateDialog(GuiGraphics graphics, int mouseX, int mouseY, int activeColor) {
        int dlgX = this.width / 2 - 100;
        int dlgY = this.height / 2 - 60;
        int dlgW = 200;
        int dlgH = 120;

        SubScreenStyle.modalBackdrop(graphics, this.width, this.height);
        SubScreenStyle.modal(graphics, dlgX, dlgY, dlgW, dlgH, activeColor);

            FontRenderUtility.drawString(graphics, ravex.utility.misc.LanguageUtility.t("prof_save_as"), dlgX + 10, dlgY + 10, 0xFFFFFFFF, true);
            SubScreenStyle.titleAccent(graphics, dlgX + 10, dlgY + 22, ravex.utility.misc.LanguageUtility.t("prof_save_as"), activeColor);

        int inputX = dlgX + 10;
        int inputY = dlgY + 32;
        int inputW = dlgW - 20;
        int inputH = 16;
        SubScreenStyle.input(graphics, inputX, inputY, inputW, inputH, activeColor);
            String display = newName.isEmpty() ? ravex.utility.misc.LanguageUtility.t("prof_name_hint") : newName;
        FontRenderUtility.drawString(graphics, display, inputX + 4, inputY + 3, newName.isEmpty() ? 0xFF505060 : 0xFFD0D0E0, false);

        int btnY = dlgY + 60;
        int btnW = 80;
        int btnH = 14;

        boolean saveHov = mouseX >= dlgX + 15 && mouseX <= dlgX + 15 + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
        SubScreenStyle.button(graphics, dlgX + 15, btnY, btnW, btnH, saveHov, true, activeColor);
            String psv = ravex.utility.misc.LanguageUtility.t("prof_save");
            FontRenderUtility.drawString(graphics, psv, dlgX + 15 + (btnW - FontRenderUtility.getStringWidth(psv)) / 2, btnY + 3, 0xFFFFFFFF, false);

        boolean cancelHov = mouseX >= dlgX + dlgW - 15 - btnW && mouseX <= dlgX + dlgW - 15 && mouseY >= btnY && mouseY <= btnY + btnH;
        SubScreenStyle.button(graphics, dlgX + dlgW - 15 - btnW, btnY, btnW, btnH, cancelHov, false, activeColor);
            String pcv = ravex.utility.misc.LanguageUtility.t("cfg_cancel");
            FontRenderUtility.drawString(graphics, pcv, dlgX + dlgW - 15 - btnW + (btnW - FontRenderUtility.getStringWidth(pcv)) / 2, btnY + 3, 0xFFD0D0E0, false);
    }

    private static class ToolbarButton {
        final int x;
        final String label;
        final int id;

        ToolbarButton(int x, String label, int id) {
            this.x = x;
            this.label = label;
            this.id = id;
        }
    }

    private void renderToolbar(GuiGraphics graphics, int mouseX, int mouseY, int activeColor) {
        int tbY = this.height - 40;
        int btnW = 90;
        int btnH = 20;

        List<ToolbarButton> buttons = List.of(
            new ToolbarButton(20, ravex.utility.misc.LanguageUtility.t("prof_capture"), 0),
            new ToolbarButton(20 + btnW + 8, ravex.utility.misc.LanguageUtility.t("prof_apply"), 1),
            new ToolbarButton(20 + (btnW + 8) * 2, ravex.utility.misc.LanguageUtility.t("cfg_delete"), 2),
            new ToolbarButton(20 + (btnW + 8) * 3, ravex.utility.misc.LanguageUtility.t("prof_refresh"), 3)
        );

        int backX = this.width - 100;
        SubScreenStyle.toolbarPill(graphics, 12, tbY - 6, backX + btnW + 8 - 12, btnH + 12);

        for (ToolbarButton b : buttons) {
            int bx = b.x;
            boolean hovered = mouseX >= bx && mouseX <= bx + btnW && mouseY >= tbY && mouseY <= tbY + btnH;
            boolean isPrimary = b.id == 0 || b.id == 1;
            SubScreenStyle.button(graphics, bx, tbY, btnW, btnH, hovered, isPrimary, activeColor);
            FontRenderUtility.drawString(graphics, b.label, bx + (btnW - FontRenderUtility.getStringWidth(b.label)) / 2, tbY + 6, 0xFFFFFFFF, false);
        }

        boolean backHov = mouseX >= backX && mouseX <= backX + btnW && mouseY >= tbY && mouseY <= tbY + btnH;
        SubScreenStyle.button(graphics, backX, tbY, btnW, btnH, backHov, false, activeColor);
        String bkv = ravex.utility.misc.LanguageUtility.t("cfg_back");
        FontRenderUtility.drawString(graphics, bkv, backX + (btnW - FontRenderUtility.getStringWidth(bkv)) / 2, tbY + 6, 0xFFD0D0E0, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean handled) {
        int mx = (int) event.x();
        int my = (int) event.y();

        if (creatingNew) {
            return handleCreateDialogClick(mx, my);
        }

        int tbY = this.height - 40;
        int btnW = 90;
        int btnH = 20;

        if (mx >= 20 && mx <= 20 + btnW && my >= tbY && my <= tbY + btnH) {
            creatingNew = true;
            newName = "";
            return true;
        }

        if (mx >= 20 + btnW + 8 && mx <= 20 + (btnW + 8) * 2 && my >= tbY && my <= tbY + btnH) {
            if (selectedIndex >= 0 && selectedIndex < profiles.size()) {
                ProfileManager.INSTANCE.applyProfile(profiles.get(selectedIndex));
                statusMessage = ravex.utility.misc.LanguageUtility.t("prof_applied", profiles.get(selectedIndex).getName());
                statusTimer = 60;
            }
            return true;
        }

        if (mx >= 20 + (btnW + 8) * 2 && mx <= 20 + (btnW + 8) * 3 && my >= tbY && my <= tbY + btnH) {
            if (selectedIndex >= 0 && selectedIndex < profiles.size()) {
                String name = profiles.get(selectedIndex).getName();
                ProfileManager.INSTANCE.deleteProfile(profiles.get(selectedIndex));
                profiles.remove(selectedIndex);
                selectedIndex = -1;
                statusMessage = ravex.utility.misc.LanguageUtility.t("prof_deleted", name);
                statusTimer = 60;
            }
            return true;
        }

        if (mx >= 20 + (btnW + 8) * 3 && mx <= 20 + (btnW + 8) * 4 && my >= tbY && my <= tbY + btnH) {
            profiles.clear();
            profiles.addAll(ProfileManager.INSTANCE.getProfiles());
            statusMessage = ravex.utility.misc.LanguageUtility.t("prof_refreshed");
            statusTimer = 40;
            return true;
        }

        if (mx >= this.width - 100 && mx <= this.width - 100 + btnW && my >= tbY && my <= tbY + btnH) {
            this.minecraft.setScreen(parent);
            return true;
        }

        int listX = 20;
        int listY = 50;
        int listW = 250;
        int itemH = 24;

        int y = listY + 8 - scrollOffset;
        for (int i = 0; i < profiles.size(); i++) {
            if (mx >= listX && mx <= listX + listW && my >= y && my <= y + itemH) {
                selectedIndex = i;
                return true;
            }
            y += itemH;
        }

        return super.mouseClicked(event, handled);
    }

    private boolean handleCreateDialogClick(int mx, int my) {
        int dlgX = this.width / 2 - 100;
        int dlgY = this.height / 2 - 60;
        int dlgW = 200;

        int btnY = dlgY + 60;
        int btnW = 80;
        int btnH = 14;

        if (mx >= dlgX + 15 && mx <= dlgX + 15 + btnW && my >= btnY && my <= btnY + btnH) {
            String name = newName.isEmpty() ? "Profile_" + (profiles.size() + 1) : newName;
            Profile p = ProfileManager.INSTANCE.captureCurrent(name);
            ProfileManager.INSTANCE.saveProfile(p);
            profiles.add(p);
            creatingNew = false;
            selectedIndex = profiles.size() - 1;
            statusMessage = ravex.utility.misc.LanguageUtility.t("prof_saved", name);
            statusTimer = 60;
            return true;
        }

        if (mx >= dlgX + dlgW - 15 - btnW && mx <= dlgX + dlgW - 15 && my >= btnY && my <= btnY + btnH) {
            creatingNew = false;
            return true;
        }

        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();

        if (creatingNew) {
            if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) { creatingNew = false; return true; }
            if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE && !newName.isEmpty()) { newName = newName.substring(0, newName.length() - 1); return true; }
            if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER) {
                String name = newName.isEmpty() ? "Profile_" + (profiles.size() + 1) : newName;
                Profile p = ProfileManager.INSTANCE.captureCurrent(name);
                ProfileManager.INSTANCE.saveProfile(p);
                profiles.add(p);
                creatingNew = false;
                selectedIndex = profiles.size() - 1;
                statusMessage = ravex.utility.misc.LanguageUtility.t("prof_saved", name);
                statusTimer = 60;
            }
            return true;
        }

        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            this.minecraft.setScreen(parent);
            return true;
        }

        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        String text = event.codepointAsString();
        if (text.isEmpty()) return true;
        char c = text.charAt(0);
        if (c < 32 || c > 126) return true;

        if (creatingNew) {
            if (newName.length() < 32) newName += text;
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
