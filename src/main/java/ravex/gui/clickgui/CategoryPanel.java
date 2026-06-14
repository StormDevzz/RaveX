package ravex.gui.clickgui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import ravex.gui.clickgui.ClickGUI;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.modules.ModuleManager;
import ravex.modules.render.ClickGui;
import ravex.utility.render.FontRenderUtility;
import ravex.utility.render.Render2DEngine;

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
    private int scrollIndex = 0;

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

    public Category getCategory() { return category; }
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
        x = targetX;
        y = targetY;

        int ix = (int) x;
        int iy = (int) y;

        List<ModuleButton> visible = filterButtons(searchQuery);
        if (visible.isEmpty() && !searchQuery.isEmpty()) return;
        if (visible.isEmpty() && allButtons.isEmpty()) return;

        // Reset/clamp scroll index if list is smaller
        if (visible.size() <= 20) {
            scrollIndex = 0;
        } else {
            scrollIndex = Math.max(0, Math.min(scrollIndex, visible.size() - 20));
        }

        int currentY = getCurrentHeight(searchQuery) + iy;
        int activeColor = ColorUtility.getActiveColor();
        int r = 3;

        // ── Shadow ───────────────────────────────────────────────────────────────
        graphics.fill(ix + 2, iy + 2, ix + width + 2, currentY + 2, 0x40000000);

        // ── Body (gradient) ──────────────────────────────────────────────────────
        graphics.fillGradient(ix, iy + 16, ix + width, currentY, 0xE60E0E22, 0xE60A0A16);

        // ── Header (gradient with accent) ────────────────────────────────────────
        graphics.fillGradient(ix, iy - 2, ix + width, iy + 16, ColorUtility.withAlpha(activeColor, 8), 0xFF18182E);

        // ── Accent line at header bottom ─────────────────────────────────────────
        graphics.fill(ix, iy + 14, ix + width, iy + 15, ColorUtility.withAlpha(activeColor, 90));
        graphics.fill(ix, iy + 15, ix + width, iy + 16, activeColor);

        // ── Category Icon ────────────────────────────────────────────────────────
        Identifier catTex = ClickGUI.getCategoryTexture(category);
        if (catTex != null) {
            int iconSize = 14;
            int iconX = ix + 4;
            int iconY = iy + (16 - iconSize) / 2;
            graphics.blit(catTex, iconX, iconY, iconX + iconSize, iconY + iconSize, 0.0f, 1.0f, 0.0f, 1.0f);
        }

        // ── Header Title ─────────────────────────────────────────────────────────
        String header = category.getDisplayName();
        int headerY = iy - 2 + (18 - FontRenderUtility.getFontHeight()) / 2 + 1;
        FontRenderUtility.drawString(graphics, FontRenderUtility.FontType.VANILLA, header, ix + 24, headerY, 0xFFFFFFFF, true);

        // ── Module Counter Badge ────────────────────────────────────────────────
        if (ClickGui.INSTANCE.moduleCounter.getValue()) {
            int enabled = (int) allButtons.stream().filter(b -> b.getModule().getEnabled()).count();
            int total = allButtons.size();
            String countText = enabled + "/" + total;
            int cw = FontRenderUtility.getStringWidth(countText);
            int pad = 4;
            int badgeX = ix + width - cw - pad - 4;
            int badgeY = iy + 1;
            int badgeH = 12;
            graphics.fill(badgeX, badgeY, badgeX + cw + pad * 2, badgeY + badgeH, 0x30000000);
            graphics.fill(badgeX, badgeY, badgeX + cw + pad * 2, badgeY + badgeH, ColorUtility.withAlpha(activeColor, 20));
            graphics.fill(badgeX, badgeY + badgeH - 1, badgeX + cw + pad * 2, badgeY + badgeH, ColorUtility.withAlpha(activeColor, 60));
            int badgeTextY = badgeY + (badgeH - FontRenderUtility.getFontHeight()) / 2 + 1;
            FontRenderUtility.drawString(graphics, FontRenderUtility.FontType.VANILLA, countText, badgeX + pad, badgeTextY,
                enabled == total ? 0xFFA0E0A0 : 0xFFE0E0E0, true);
        }

        // ── Border Outline ───────────────────────────────────────────────────────
        int borderColor = ClickGui.INSTANCE.outlines.getValue() ? ClickGui.INSTANCE.outlineColor.getValue() : 0xFF1C1C2C;
        graphics.fill(ix - 1, iy - 3, ix + width + 1, iy - 2, borderColor);
        graphics.fill(ix - 1, iy - 2, ix, currentY, borderColor);
        graphics.fill(ix + width, iy - 2, ix + width + 1, currentY, borderColor);
        graphics.fill(ix - 1, currentY, ix + width + 1, currentY + 1, borderColor);

        // ── Module Buttons ───────────────────────────────────────────────────────
        List<ModuleButton> rendered = visible;
        int btnWidth = width;
        if (visible.size() > 20) {
            rendered = visible.subList(scrollIndex, scrollIndex + 20);
            btnWidth = width - 4; // leave 4px space for scrollbar on the right
        }

        int[] renderYOut = { iy + 16 };
        for (ModuleButton btn : rendered) {
            btn.render(graphics, ix, iy, btnWidth, mouseX, mouseY, renderYOut, searchQuery);
        }

        // ── Scrollbar ────────────────────────────────────────────────────────────
        if (visible.size() > 20) {
            int trackX = ix + width - 4;
            int trackY = iy + 16;
            int trackW = 2;
            int trackH = currentY - trackY - 2;

            // Track background
            graphics.fill(trackX, trackY, trackX + trackW, trackY + trackH, 0x1A000000);

            // Thumb
            int thumbH = Math.max(8, trackH * 20 / visible.size());
            int thumbY = trackY + (trackH - thumbH) * scrollIndex / (visible.size() - 20);
            graphics.fill(trackX, thumbY, trackX + trackW, thumbY + thumbH, ColorUtility.withAlpha(activeColor, 180));
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
        List<ModuleButton> rendered = visible;
        int btnWidth = width;
        if (visible.size() > 20) {
            scrollIndex = Math.max(0, Math.min(scrollIndex, visible.size() - 20));
            rendered = visible.subList(scrollIndex, scrollIndex + 20);
            btnWidth = width - 4;
        }

        int[] currentYOut = { iy + 16 };
        for (ModuleButton btn : rendered) {
            if (btn.mouseClicked(mouseX, mouseY, button, ix, btnWidth, currentYOut, mc)) {
                return true;
            }
        }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount, String searchQuery) {
        int ix = (int) x;
        int iy = (int) y;
        int currentY = getCurrentHeight(searchQuery) + iy;

        if (mouseX >= ix && mouseX <= ix + width && mouseY >= iy + 16 && mouseY <= currentY) {
            List<ModuleButton> visible = filterButtons(searchQuery);
            if (visible.size() > 20) {
                if (verticalAmount > 0) {
                    scrollIndex = Math.max(0, scrollIndex - 1);
                } else if (verticalAmount < 0) {
                    scrollIndex = Math.min(visible.size() - 20, scrollIndex + 1);
                }
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
        int btnH = ravex.modules.render.ClickGui.INSTANCE.buttonHeight.getValue().intValue();
        int count = Math.min(visible.size(), 20);
        h += count * btnH;
        return h;
    }
}
