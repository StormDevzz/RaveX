package ravex.gui.clickgui;

import ravex.utility.render.FontRenderUtility;
import ravex.utility.render.Render2DEngine;
import net.minecraft.client.gui.GuiGraphics;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.modules.ModuleManager;
import ravex.modules.render.ClickGui;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CategoryPanel {
    private final Category category;
    private double x;
    private double y;
    private double targetX;
    private double targetY;
    private double vx = 0;
    private double vy = 0;
    
    private boolean dragging = false;
    private double dragOffsetX = 0;
    private double dragOffsetY = 0;

    private final int width = 120;
    private final List<ModuleButton> allButtons = new ArrayList<>();
    private float headerAnim = 0f;

    public CategoryPanel(Category category, int x, int y) {
        this.category = category;
        this.x = x;
        this.y = y;
        this.targetX = x;
        this.targetY = y;
        List<Module> modules = new ArrayList<>(ModuleManager.INSTANCE.getByCategory(category));
        modules.sort((m1, m2) -> m1.getName().compareToIgnoreCase(m2.getName()));
        for (Module m : modules) {
            allButtons.add(new ModuleButton(m));
        }
    }

    public int getX() { return (int) x; }
    public int getY() { return (int) y; }

    public void setX(int x) {
        this.x = x;
        this.targetX = x;
        this.vx = 0;
    }

    public void setY(int y) {
        this.y = y;
        this.targetY = y;
        this.vy = 0;
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, String searchQuery) {
        if (dragging) {
            boolean isMouseDown = org.lwjgl.glfw.GLFW.glfwGetMouseButton(
                net.minecraft.client.Minecraft.getInstance().getWindow().handle(),
                org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT
            ) == org.lwjgl.glfw.GLFW.GLFW_PRESS;

            if (!isMouseDown) {
                dragging = false;
            } else {
                targetX = mouseX - dragOffsetX;
                targetY = mouseY - dragOffsetY;
            }
        }
        // Snap immediately to target position (no spring animations)
        x = targetX;
        y = targetY;

        int ix = (int) x;
        int iy = (int) y;

        List<ModuleButton> visible = filterButtons(searchQuery);
        if (visible.isEmpty() && !searchQuery.isEmpty()) return;
        if (visible.isEmpty() && allButtons.isEmpty()) return;

        int currentY = getCurrentHeight(searchQuery) + iy;
        int activeColor = ColorUtility.getActiveColor();

        // 1. Draw flat Category Panel Body
        graphics.fill(ix, iy + 16, ix + width, currentY, 0xEE141414);

        // 2. Draw flat Category Panel Header
        graphics.fill(ix, iy - 2, ix + width, iy + 16, 0xFF1C1C1C);

        // 3. Draw Accent Line (2px active color at the bottom of header)
        graphics.fill(ix, iy + 14, ix + width, iy + 16, activeColor);

        // 4. Draw Header Title Text
        String header = category.getDisplayName();
        FontRenderUtility.drawString(graphics, FontRenderUtility.FontType.VANILLA, header, ix + 6, iy + 2, 0xFFFFFFFF, true);

        // 5. Draw Border Outline
        int borderColor = ClickGui.INSTANCE.outlines.getValue() ? ClickGui.INSTANCE.outlineColor.getValue() : 0xFF2C2C2C;
        graphics.fill(ix - 1, iy - 3, ix + width + 1, iy - 2, borderColor); // Top
        graphics.fill(ix - 1, iy - 2, ix, currentY, borderColor); // Left
        graphics.fill(ix + width, iy - 2, ix + width + 1, currentY, borderColor); // Right
        graphics.fill(ix - 1, currentY, ix + width + 1, currentY + 1, borderColor); // Bottom

        int[] renderYOut = { iy + 16 };
        for (ModuleButton btn : visible) {
            btn.render(graphics, ix, iy, width, mouseX, mouseY, renderYOut, searchQuery);
        }
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        render(graphics, mouseX, mouseY, "");
    }

    public int getMatchCount(String query) {
        return (int) allButtons.stream()
            .filter(b -> b.getModule().getName().toLowerCase().contains(query.toLowerCase()))
            .count();
    }

    private List<ModuleButton> filterButtons(String query) {
        if (query == null || query.isEmpty()) return allButtons;
        String lower = query.toLowerCase();
        return allButtons.stream()
            .filter(b -> b.getModule().getName().toLowerCase().contains(lower))
            .collect(Collectors.toList());
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button, net.minecraft.client.Minecraft mc) {
        int ix = (int) x;
        int iy = (int) y;

        if (button == 0 && mouseX >= ix && mouseX <= ix + width && mouseY >= iy - 2 && mouseY <= iy + 16) {
            dragging = true;
            dragOffsetX = mouseX - x;
            dragOffsetY = mouseY - y;
            return true;
        }

        List<ModuleButton> visible = filterButtons(ClickGUI.searchQuery);
        int[] currentYOut = { iy + 16 };
        for (ModuleButton btn : visible) {
            if (btn.mouseClicked(mouseX, mouseY, button, ix, width, currentYOut, mc)) {
                return true;
            }
        }
        return false;
    }

    public int getCurrentHeight(String searchQuery) {
        List<ModuleButton> visible = filterButtons(searchQuery);
        if (visible.isEmpty() && !searchQuery.isEmpty()) return 0;
        if (visible.isEmpty() && allButtons.isEmpty()) return 0;

        int h = 16;
        for (ModuleButton ignored : visible) {
            h += 15;
        }
        return h;
    }
}
