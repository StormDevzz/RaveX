package ravex.gui.clickgui;

import ravex.utility.render.FontRenderUtility;
import net.minecraft.client.gui.GuiGraphics;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.modules.ModuleManager;
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

        int[] currentYOut = { iy + 16 };
        for (ModuleButton btn : visible) {
            btn.render(graphics, ix, iy, width, mouseX, mouseY, currentYOut, searchQuery);
        }
        int currentY = currentYOut[0];

        int shadowSize = 5;
        for (int s = shadowSize; s > 0; s--) {
            int alpha = 20 - s * 3;
            if (alpha > 0) {
                graphics.fill(ix + s, iy - 2 + s, ix + width - s, currentY + s, (alpha << 24));
            }
        }

        graphics.fillGradient(ix, iy - 2, ix + width, iy + 16, ColorUtility.HEADER_COLOR, ColorUtility.HEADER_GRADIENT_END);

        int activeColor = ColorUtility.getActiveColor();
        graphics.fill(ix, iy + 14, ix + width, iy + 16, activeColor);
        graphics.fill(ix, iy + 13, ix + 3, iy + 14, ColorUtility.darker(activeColor, 0.5f));

        String header = category.getDisplayName();
        FontRenderUtility.drawString(graphics, header, ix + 8, iy + 4, 0xFFE0E0F0, true);
        graphics.fill(ix + 8, iy + 13, ix + 8 + FontRenderUtility.getStringWidth(header) + 4, iy + 14, ColorUtility.withAlpha(activeColor, 30));

        graphics.fillGradient(ix, iy + 16, ix + width, currentY, ColorUtility.PANEL_BODY_START, ColorUtility.PANEL_BODY_END);
        graphics.fill(ix, iy + 16, ix + width, iy + 17, ColorUtility.withAlpha(activeColor, 15));

        currentYOut[0] = iy + 16;
        for (ModuleButton btn : visible) {
            btn.render(graphics, ix, iy, width, mouseX, mouseY, currentYOut, searchQuery);
        }
        currentY = currentYOut[0];

        if (ravex.modules.render.ClickGui.INSTANCE.outlines.getValue()) {
            int borderColor = ravex.modules.render.ClickGui.INSTANCE.outlineColor.getValue();
            graphics.fill(ix - 1, iy - 2, ix, currentY + 1, borderColor);
            graphics.fill(ix + width, iy - 2, ix + width + 1, currentY + 1, borderColor);
            graphics.fill(ix - 1, iy - 2, ix + width + 1, iy - 1, borderColor);
            graphics.fill(ix - 1, currentY, ix + width + 1, currentY + 1, borderColor);
        } else {
            graphics.fill(ix - 1, iy - 2, ix, currentY, ColorUtility.PANEL_BORDER_COLOR);
            graphics.fill(ix + width, iy - 2, ix + width + 1, currentY, ColorUtility.PANEL_BORDER_COLOR);
            graphics.fill(ix, currentY, ix + width, currentY + 1, ColorUtility.PANEL_BORDER_COLOR);
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

        int[] currentYOut = { iy + 16 };
        for (ModuleButton btn : allButtons) {
            if (btn.mouseClicked(mouseX, mouseY, button, ix, currentYOut, mc)) {
                return true;
            }
        }
        return false;
    }
}
