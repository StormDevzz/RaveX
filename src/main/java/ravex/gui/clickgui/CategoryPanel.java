package ravex.gui.clickgui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import ravex.gui.clickgui.ClickGUI;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.manager.ModuleManager;
import ravex.utility.render.Render2DEngine;
import ravex.utility.render.animate.AnimationUtility;
import ravex.modules.client.ClickGui;
import ravex.utility.render.FontRenderUtility;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.Math.*;

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

    private final List<ModuleButton> allButtons = new ArrayList<>();
    private float headerAnim = 0f;
    private double scrollOffset = 0.0;

    public CategoryPanel(Category category, int x, int y) {
        this.category = category;
        this.x = x;
        this.y = y;
        this.targetX = x;
        this.targetY = y;
        List<Module> modules = new ArrayList<>(ModuleManager.INSTANCE.getByCategory(category));
        modules.removeIf(Module::isHud);
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
        double dragLerp = ModuleManager.get(ClickGui.class).smoothScroll.getValue()
            ? (ModuleManager.get(ClickGui.class).scrollSmoothness.getValue().doubleValue() / 100.0)
            : 1.0;

        if (Math.abs(targetX - x) > 0.05) {
            x += (targetX - x) * dragLerp;
        } else {
            x = targetX;
        }

        if (Math.abs(targetY - y) > 0.05) {
            y += (targetY - y) * dragLerp;
        } else {
            y = targetY;
        }

        int ix = (int) Math.round(x);
        int iy = (int) Math.round(y);
        int width = ModuleManager.get(ClickGui.class).panelWidth.getValue().intValue();
        int activeColor = ColorUtility.getActiveColor();

        boolean hasSearch = searchQuery != null && !searchQuery.isEmpty();
        for (ModuleButton btn : allButtons) {
            if (!btn.getModule().isVisible()) continue;
            boolean matches = !hasSearch || btn.getModule().getName().toLowerCase().contains(searchQuery.toLowerCase());
            btn.updateSearchReveal(matches, hasSearch);
        }

        List<ModuleButton> visible = filterButtons(searchQuery);
        if (visible.isEmpty() && hasSearch) return;
        if (visible.isEmpty() && allButtons.isEmpty()) return;

        int btnH = ModuleManager.get(ClickGui.class).buttonHeight.getValue().intValue();
        int listTop = iy + 22;
        int totalH = 22;
        for (ModuleButton btn : visible) {
            totalH += Math.max(1, (int)(btnH * btn.getSearchReveal())) + 2;
            if (btn.isExpanded()) totalH += (int)(btn.getExpandedHeight(width) * btn.getSearchReveal());
        }
        int maxPanelHeight = (int)(net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledHeight() * 0.55f);
        int viewportH = Math.min(totalH, maxPanelHeight);
        int panelBot = iy + viewportH;

        int maxScroll = max(0, totalH - viewportH);
        if (maxScroll <= 0) {
            scrollOffset = 0;
        } else {
            scrollOffset = max(-maxScroll, min(0.0, scrollOffset));
        }

        int panelH = panelBot - iy;
        int r = Math.min(ModuleManager.get(ClickGui.class).cornerRadius.getValue().intValue(), panelH / 2);

        int pAlpha = ModuleManager.get(ClickGui.class).panelOpacity.getValue().intValue();
        Render2DEngine.drawRound(graphics, ix, iy, width, panelH, r, ColorUtility.withAlpha(ColorUtility.PANEL_BODY_END, pAlpha));

        if (ModuleManager.get(ClickGui.class).outlines.getValue()) {
            int borderColor = ModuleManager.get(ClickGui.class).outlineColor.getValue();
            Render2DEngine.drawRound(graphics, ix - 1, iy - 1, width + 2, panelH + 2, r, borderColor);
        }

        Identifier catTexWhite = ravex.utility.render.TextureLoader.getCategoryTextureWhite(category);
        if (catTexWhite != null) {
            int iconSize = 14;
            int iconX = ix + 5;
            int iconY = iy + 2;
            graphics.blit(catTexWhite, iconX, iconY, iconX + iconSize, iconY + iconSize, 0.0f, 1.0f, 0.0f, 1.0f);
        }

        String header = category.getDisplayName();
        int headerY = iy + (18 - FontRenderUtility.getFontHeight()) / 2 + 1;
        FontRenderUtility.drawString(graphics, header,
            ix + 23, headerY, 0xFFFFFFFF, true);

        if (ModuleManager.get(ClickGui.class).moduleCounter.getValue()) {
            int enabled = 0;
            for (ModuleButton b : visible) {
                if (b.getModule().getEnabled()) enabled++;
            }
            int total = visible.size();
            String countText = enabled + "/" + total;
            int cw = FontRenderUtility.getStringWidth(countText);
            int pad = 4;
            int badgeX = ix + width - cw - pad - 8;
            int badgeY = iy + 4;
            int badgeH = 14;
            Render2DEngine.drawRound(graphics, badgeX, badgeY, cw + pad * 2, badgeH, 4, 0x22000000);
            FontRenderUtility.drawString(graphics, countText,
                badgeX + pad, badgeY + (badgeH - FontRenderUtility.getFontHeight()) / 2 + 1,
                enabled == total ? 0xFFA0E0A0 : 0xFFE0E0E0, true);
        }

        if (totalH > viewportH || scrollOffset != 0) {
            graphics.enableScissor(ix, listTop, ix + width, panelBot);
        }
        int[] renderYOut = { listTop + (int) Math.round(scrollOffset) };
        for (ModuleButton btn : visible)
            btn.render(graphics, ix, iy, width, mouseX, mouseY, renderYOut, searchQuery, listTop, panelBot);
        if (totalH > viewportH || scrollOffset != 0) {
            graphics.disableScissor();
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
        return allButtons.stream()
            .filter(b -> b.getModule().isVisible() && b.getSearchReveal() > 0.001f)
            .collect(Collectors.toList());
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button, net.minecraft.client.Minecraft mc) {
        int ix = (int) x;
        int iy = (int) y;
        int width = ModuleManager.get(ClickGui.class).panelWidth.getValue().intValue();

        if (button == 0 && mouseX >= ix && mouseX <= ix + width && mouseY >= iy && mouseY <= iy + 18) {
            dragging = true;
            dragOffsetX = mouseX - x;
            dragOffsetY = mouseY - y;
            return true;
        }

        String query = ClickGUI.searchQuery;
        int listTop = iy + 22;
        int totalH = getCurrentHeight(query);
        int maxPanelHeight = (int)(mc.getWindow().getGuiScaledHeight() * 0.55f);
        int panelBot = iy + Math.min(totalH, maxPanelHeight);

        if (mouseY < listTop || mouseY > panelBot) return false;

        List<ModuleButton> visible = filterButtons(query);
        int[] currentYOut = { listTop + (int) Math.round(scrollOffset) };
        for (ModuleButton btn : visible) {
            if (btn.mouseClicked(mouseX, mouseY, button, ix, width, currentYOut, mc))
                return true;
        }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount, String searchQuery) {
        int ix = (int) x;
        int iy = (int) y;
        int width = ModuleManager.get(ClickGui.class).panelWidth.getValue().intValue();
        int listTop = iy + 22;
        int totalH = getCurrentHeight(searchQuery);
        int maxPanelHeight = (int)(net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledHeight() * 0.55f);
        int panelBot = iy + Math.min(totalH, maxPanelHeight);

        if (mouseX >= ix && mouseX <= ix + width && mouseY >= iy && mouseY <= panelBot) {
            List<ModuleButton> visible = filterButtons(searchQuery);
            int btnH = ModuleManager.get(ClickGui.class).buttonHeight.getValue().intValue();
            int renderY = listTop + (int) Math.round(scrollOffset);
            for (ModuleButton btn : visible) {
                if (btn.isExpanded()) {
                    int expH = btn.getExpandedHeight(width);
                    if (mouseY >= renderY + btnH && mouseY < renderY + btnH + expH) {
                        if (btn.onInlineScroll(mouseX, mouseY, verticalAmount, ix, renderY + btnH, width)) {
                            return true;
                        }
                    }
                    renderY += btnH + 2 + expH;
                } else {
                    renderY += btnH + 2;
                }
            }

            scrollOffset += verticalAmount * 36;
            return true;
        }
        return false;
    }

    public int getCurrentHeight(String searchQuery) {
        int width = ModuleManager.get(ClickGui.class).panelWidth.getValue().intValue();
        List<ModuleButton> visible = filterButtons(searchQuery);
        if (visible.isEmpty() && !searchQuery.isEmpty()) return 0;
        if (visible.isEmpty() && allButtons.isEmpty()) return 0;

        int btnH = ModuleManager.get(ClickGui.class).buttonHeight.getValue().intValue();
        int h = 22;
        for (ModuleButton btn : visible) {
            h += Math.max(1, (int)(btnH * btn.getSearchReveal())) + 2;
            if (btn.isExpanded()) {
                h += (int)(btn.getExpandedHeight(width) * btn.getSearchReveal());
            }
        }
        return h;
    }

    public int getBaseHeight(String searchQuery) {
        return getCurrentHeight(searchQuery);
    }
}
