package ravex.gui.clickgui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import ravex.profile.Profile;
import ravex.profile.ProfileManager;

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
        super(Component.literal("Profile Manager"));
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
        graphics.fillGradient(0, 0, this.width, this.height, 0xDD0A0A14, 0xDD10101F);

        int activeColor = ColorUtility.getActiveColor();

        graphics.drawString(this.font, "Profile Manager", 20, 12, 0xFFFFFFFF, true);
        graphics.drawString(this.font, "Save and load module configurations", 20, 24, 0xFF8F8FA0, false);

        if (statusTimer > 0 && !statusMessage.isEmpty()) {
            graphics.drawString(this.font, statusMessage, 20, 38, 0xFFAAFFAA, false);
        }

        int listX = 20;
        int listY = 50;
        int listW = 250;
        int itemH = 24;

        graphics.fill(listX, listY, listX + listW, this.height - 50, 0x44000000);

        int y = listY + 4 - scrollOffset;
        for (int i = 0; i < profiles.size(); i++) {
            Profile p = profiles.get(i);
            boolean hovered = mouseX >= listX && mouseX <= listX + listW && mouseY >= y && mouseY <= y + itemH;
            int bg = i == selectedIndex ? 0xFF202035 : (hovered ? 0xFF181828 : 0xFF0D0D14);
            graphics.fill(listX + 2, y, listX + listW - 2, y + itemH, bg);

            graphics.drawString(this.font, p.getName(), listX + 6, y + 4, 0xFFD0D0E0, false);

            int modCount = p.getModuleStates().size();
            String info = modCount + " modules";
            int iw = this.font.width(info);
            graphics.drawString(this.font, info, listX + listW - iw - 8, y + 4, 0xFF707080, false);

            y += itemH;
        }
        if (profiles.isEmpty()) {
            graphics.drawString(this.font, "No profiles yet. Capture current config to create one.", listX + 6, listY + 8, 0xFF505060, false);
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

        graphics.fill(0, 0, this.width, this.height, 0x88000000);
        graphics.fill(dlgX, dlgY, dlgX + dlgW, dlgY + dlgH, 0xF510101A);
        graphics.fill(dlgX, dlgY, dlgX + dlgW, dlgY + 1, activeColor);

        graphics.drawString(this.font, "Save Profile As:", dlgX + 10, dlgY + 10, 0xFFFFFFFF, true);

        int inputX = dlgX + 10;
        int inputY = dlgY + 32;
        int inputW = dlgW - 20;
        int inputH = 16;
        graphics.fill(inputX, inputY, inputX + inputW, inputY + inputH, 0xFF1A1A28);
        String display = newName.isEmpty() ? "Profile name..." : newName;
        graphics.drawString(this.font, display, inputX + 4, inputY + 3, newName.isEmpty() ? 0xFF505060 : 0xFFD0D0E0, false);

        int btnY = dlgY + 60;
        int btnW = 80;
        int btnH = 14;

        boolean saveHov = mouseX >= dlgX + 15 && mouseX <= dlgX + 15 + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
        graphics.fill(dlgX + 15, btnY, dlgX + 15 + btnW, btnY + btnH, saveHov ? activeColor : 0xFF202035);
        graphics.drawString(this.font, "Save", dlgX + 15 + (btnW - this.font.width("Save")) / 2, btnY + 3, 0xFFFFFFFF, false);

        boolean cancelHov = mouseX >= dlgX + dlgW - 15 - btnW && mouseX <= dlgX + dlgW - 15 && mouseY >= btnY && mouseY <= btnY + btnH;
        graphics.fill(dlgX + dlgW - 15 - btnW, btnY, dlgX + dlgW - 15, btnY + btnH, cancelHov ? 0xFF303035 : 0xFF14141E);
        graphics.drawString(this.font, "Cancel", dlgX + dlgW - 15 - btnW + (btnW - this.font.width("Cancel")) / 2, btnY + 3, 0xFFD0D0E0, false);
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
            new ToolbarButton(20, "Capture", 0),
            new ToolbarButton(20 + btnW + 8, "Apply", 1),
            new ToolbarButton(20 + (btnW + 8) * 2, "Delete", 2),
            new ToolbarButton(20 + (btnW + 8) * 3, "Refresh", 3)
        );

        for (ToolbarButton b : buttons) {
            int bx = b.x;
            boolean hovered = mouseX >= bx && mouseX <= bx + btnW && mouseY >= tbY && mouseY <= tbY + btnH;
            boolean isPrimary = b.id == 0 || b.id == 1;
            int bg = hovered ? (isPrimary ? activeColor : 0xFF202035) : (isPrimary ? 0xFF252540 : 0xFF0D0D14);
            graphics.fill(bx, tbY, bx + btnW, tbY + btnH, bg);
            graphics.drawString(this.font, b.label, bx + (btnW - this.font.width(b.label)) / 2, tbY + 6, 0xFFD0D0E0, false);
        }

        int backX = this.width - 100;
        boolean backHov = mouseX >= backX && mouseX <= backX + btnW && mouseY >= tbY && mouseY <= tbY + btnH;
        graphics.fill(backX, tbY, backX + btnW, tbY + btnH, backHov ? 0xFF303035 : 0xFF0D0D14);
        graphics.drawString(this.font, "Back", backX + (btnW - this.font.width("Back")) / 2, tbY + 6, 0xFFD0D0E0, false);
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
                statusMessage = "Profile \"" + profiles.get(selectedIndex).getName() + "\" applied";
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
                statusMessage = "Profile \"" + name + "\" deleted";
                statusTimer = 60;
            }
            return true;
        }

        if (mx >= 20 + (btnW + 8) * 3 && mx <= 20 + (btnW + 8) * 4 && my >= tbY && my <= tbY + btnH) {
            profiles.clear();
            profiles.addAll(ProfileManager.INSTANCE.getProfiles());
            statusMessage = "Refreshed";
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

        int y = listY + 4 - scrollOffset;
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
            statusMessage = "Profile \"" + name + "\" saved";
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
                statusMessage = "Profile \"" + name + "\" saved";
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
