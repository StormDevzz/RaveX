package ravex.gui.clickgui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import ravex.modules.ModuleManager;
import ravex.modules.HudModule;

public class HudEditorScreen extends Screen {
    private final Screen parentScreen;
    private HudModule draggingHud = null;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;
    private boolean snapToGrid = true;
    private final int gridSize = 8;

    public HudEditorScreen(Screen parentScreen) {
        super(Component.literal("RaveX HUD Editor"));
        this.parentScreen = parentScreen;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        // Draw grid background overlay
        graphics.fillGradient(0, 0, this.width, this.height, 0xD90A0A0F, 0xD9101017);

        // Draw grid lines
        int gridColor = 0x0CFFFFFF;
        for (int x = 0; x < this.width; x += 16) {
            graphics.fill(x, 0, x + 1, this.height, gridColor);
        }
        for (int y = 0; y < this.height; y += 16) {
            graphics.fill(0, y, this.width, y + 1, gridColor);
        }

        // Draw Header instructions
        graphics.drawString(this.font, "HUD LAYOUT EDITOR", 20, 20, 0xFFFFFFFF, true);
        graphics.drawString(this.font, "§7Drag active elements with Left Click. Press ESC or click Save to close.", 20, 32, 0xFF8F8FA0, true);

        // Snap to Grid indicator
        String snapText = snapToGrid ? "§aSnap to Grid: ON" : "§cSnap to Grid: OFF";
        graphics.drawString(this.font, snapText, 20, 48, 0xFFFFFFFF, true);

        // Render and handle dragging bounds for each active HUD module
        if (draggingHud != null) {
            int newX = mouseX - dragOffsetX;
            int newY = mouseY - dragOffsetY;
            if (snapToGrid) {
                newX = Math.round((float) newX / gridSize) * gridSize;
                newY = Math.round((float) newY / gridSize) * gridSize;
            }
            draggingHud.setX(newX);
            draggingHud.setY(newY);
        }

        int activeColor = ColorUtility.getActiveColor();

        for (HudModule hm : ModuleManager.INSTANCE.getHudModules()) {
            if (hm.getEnabled()) {
                int x1 = hm.getX();
                int y1 = hm.getY();
                int x2 = x1 + hm.getWidth();
                int y2 = y1 + hm.getHeight();

                boolean hovered = mouseX >= x1 && mouseX <= x2 && mouseY >= y1 && mouseY <= y2;

                // Draw translucent bounding box frame
                int borderColor = hovered ? activeColor : 0x44FFFFFF;
                graphics.fill(x1 - 2, y1 - 2, x2 + 2, y1, borderColor);
                graphics.fill(x1 - 2, y2, x2 + 2, y2 + 2, borderColor);
                graphics.fill(x1 - 2, y1, x1, y2, borderColor);
                graphics.fill(x2, y1, x2 + 2, y2, borderColor);

                // Draw translucent filling
                int fillColor = hovered ? 0x1Affffff : 0x08ffffff;
                graphics.fill(x1, y1, x2, y2, fillColor);

                // Label tag
                graphics.drawString(this.font, hm.getName() + " (" + x1 + "," + y1 + ")", x1 + 2, y1 + 2, hovered ? activeColor : 0xFFFFFFFF, false);
            }
        }

        // Draw a premium toolbar at the bottom
        int tbW = 220;
        int tbH = 32;
        int tbX = (this.width - tbW) / 2;
        int tbY = this.height - tbH - 20;

        // Draw glassy background with neon borders
        graphics.fill(tbX, tbY, tbX + tbW, tbY + tbH, 0xEE101015);
        graphics.fill(tbX, tbY, tbX + tbW, tbY + 1, activeColor); // top border

        // Save & Exit button bounds
        int btnX = tbX + 10;
        int btnY = tbY + 6;
        int btnW = 90;
        int btnH = 20;
        boolean btnHovered = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
        graphics.fill(btnX, btnY, btnX + btnW, btnY + btnH, btnHovered ? activeColor : 0xFF20202F);
        graphics.drawString(this.font, "Save Layout", btnX + (btnW - this.font.width("Save Layout")) / 2, btnY + 6, 0xFFFFFFFF, false);

        // Snap toggle button bounds
        int snapBtnX = tbX + tbW - 100;
        int snapBtnY = tbY + 6;
        int snapBtnW = 90;
        int snapBtnH = 20;
        boolean snapBtnHovered = mouseX >= snapBtnX && mouseX <= snapBtnX + snapBtnW && mouseY >= snapBtnY && mouseY <= snapBtnY + snapBtnH;
        graphics.fill(snapBtnX, snapBtnY, snapBtnX + snapBtnW, snapBtnY + snapBtnH, snapBtnHovered ? activeColor : 0xFF20202F);
        graphics.drawString(this.font, "Toggle Grid", snapBtnX + (snapBtnW - this.font.width("Toggle Grid")) / 2, snapBtnY + 6, 0xFFFFFFFF, false);

        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean handled) {
        if (event.button() == 0) { // Left Click
            int mx = (int) event.x();
            int my = (int) event.y();

            // Check Save button click
            int tbW = 220;
            int tbH = 32;
            int tbX = (this.width - tbW) / 2;
            int tbY = this.height - tbH - 20;
            int btnX = tbX + 10;
            int btnY = tbY + 6;
            int btnW = 90;
            int btnH = 20;

            if (mx >= btnX && mx <= btnX + btnW && my >= btnY && my <= btnY + btnH) {
                if (this.minecraft.player != null) {
                    this.minecraft.player.playSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 0.5f, 1.0f);
                }
                this.onClose();
                return true;
            }

            // Check Snap toggle button click
            int snapBtnX = tbX + tbW - 100;
            int snapBtnY = tbY + 6;
            int snapBtnW = 90;
            int snapBtnH = 20;

            if (mx >= snapBtnX && mx <= snapBtnX + snapBtnW && my >= snapBtnY && my <= snapBtnY + snapBtnH) {
                snapToGrid = !snapToGrid;
                if (this.minecraft.player != null) {
                    this.minecraft.player.playSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 0.5f, 1.2f);
                }
                return true;
            }

            // Check HUD modules click for drag
            for (HudModule hm : ModuleManager.INSTANCE.getHudModules()) {
                if (hm.getEnabled()) {
                    if (mx >= hm.getX() && mx <= hm.getX() + hm.getWidth() &&
                        my >= hm.getY() && my <= hm.getY() + hm.getHeight()) {
                        draggingHud = hm;
                        dragOffsetX = mx - hm.getX();
                        dragOffsetY = my - hm.getY();
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(event, handled);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0) {
            draggingHud = null;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        // If no parent (opened from HUD module), close fully back to game
        this.minecraft.setScreen(null);
    }
}
