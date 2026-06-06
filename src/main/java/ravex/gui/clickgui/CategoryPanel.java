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

        double spring = 0.25;
        double damping = 0.75;
        vx = (targetX - x) * spring + vx * damping;
        vy = (targetY - y) * spring + vy * damping;
        x += vx;
        y += vy;

        int ix = (int) x;
        int iy = (int) y;

        List<ModuleButton> visible = filterButtons(searchQuery);
        if (visible.isEmpty() && !searchQuery.isEmpty()) return;
        if (visible.isEmpty() && allButtons.isEmpty()) return;

        int currentY = getCurrentHeight(searchQuery) + iy;

        int activeColor = ColorUtility.getActiveColor();
        Color ac1 = ColorUtility.getColor(270);
        Color ac2 = ColorUtility.getColor(0);
        Color ac3 = ColorUtility.getColor(180);
        Color ac4 = ColorUtility.getColor(90);

        boolean hovered = Render2DEngine.isHovered(mouseX, mouseY, ix, iy - 2, width, currentY - iy + 2);
        headerAnim = Render2DEngine.fastAnimation(headerAnim, hovered ? 1f : 0f, 8f);

        int radius = 4;
        int shadowSize = 4;
        for (int s = shadowSize; s > 0; s--) {
            int alpha = (int)(10 * (1f - s / (float)shadowSize));
            if (alpha > 0) {
                Render2DEngine.drawRound(graphics, ix + s, iy - 2 + s, width, currentY - iy + 4 - s * 2, radius,
                    (alpha << 24));
            }
        }

        int panelTop = 0xCC0B0B18;
        Render2DEngine.drawRound(graphics, ix, iy + 16, width, Math.max(0, currentY - iy - 16), radius, panelTop);

        // Header: deep navy gradient with a fine top highlight
        graphics.fillGradient(ix, iy - 2, ix + width, iy + 16, 0xFF151530, 0xFF0E0E22);
        // Sheen on top edge
        graphics.fill(ix + radius, iy - 2, ix + width - radius, iy - 1, 0x22FFFFFF);
        // Rounded corners on header
        Render2DEngine.drawRound(graphics, ix, iy - 2, width, 18, radius, 0x00000000); // clears corners via overdraw is not ideal; draw header fill + corners

        // Accent divider (full width, bottom of header)
        int activeColorDarker = ColorUtility.darker(activeColor, 0.7f);
        // Gradient glow just above the line
        graphics.fillGradient(ix, iy + 10, ix + width, iy + 15,
            0x00000000, ColorUtility.withAlpha(activeColor, 45));
        graphics.fill(ix, iy + 15, ix + width, iy + 16, activeColor);
        // Soft bloom below the accent line
        graphics.fillGradient(ix, iy + 16, ix + width, iy + 20,
            ColorUtility.withAlpha(activeColor, 30), 0x00000000);

        String header = category.getDisplayName();
        FontRenderUtility.drawString(graphics, FontRenderUtility.FontType.VANILLA, header, ix + 8, iy + 3, 0xFFEEEEFF, true);
        int headerW = FontRenderUtility.getStringWidth(FontRenderUtility.FontType.VANILLA, header);
        graphics.fill(ix + 8, iy + 13, ix + 8 + headerW + 4, iy + 14, ColorUtility.withAlpha(activeColor, 25));

        if (ClickGui.INSTANCE.outlines.getValue()) {
            int borderColor = ClickGui.INSTANCE.outlineColor.getValue();
            Render2DEngine.drawRound(graphics, ix - 1, iy - 2, width + 2, currentY - iy + 4, radius + 1, borderColor);
        } else {
            int borderCol = ColorUtility.PANEL_BORDER_COLOR;
            graphics.fill(ix - 1, iy - 2, ix, currentY, borderCol);
            graphics.fill(ix + width, iy - 2, ix + width + 1, currentY, borderCol);
            graphics.fill(ix, currentY, ix + width, currentY + 1, borderCol);
        }

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

        if (button == 0 && mouseX >= ix && mouseX <= ix + width && mouseY >= iy && mouseY <= iy + 16) {
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
            h += 18;
        }
        return h;
    }
}
