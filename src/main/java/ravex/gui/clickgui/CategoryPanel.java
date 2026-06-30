package ravex.gui.clickgui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import ravex.gui.clickgui.ClickGUI;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.modules.ModuleManager;
import ravex.modules.render.ClickGui;
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

    private final int width = 120;
    private final List<ModuleButton> allButtons = new ArrayList<>();
    private float headerAnim = 0f;
    private double scrollOffset = 0.0;
    private double scrollVelocity = 0.0;

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
        double dragLerp = ClickGui.INSTANCE.smoothScroll.getValue()
            ? (ClickGui.INSTANCE.scrollSmoothness.getValue().doubleValue() / 100.0)
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

        List<ModuleButton> visible = filterButtons(searchQuery);
        if (visible.isEmpty() && !searchQuery.isEmpty()) return;
        if (visible.isEmpty() && allButtons.isEmpty()) return;

        int btnH = ClickGui.INSTANCE.buttonHeight.getValue().intValue();
        int listTop = iy + 18;
        int totalH = getCurrentHeight(searchQuery);
        int viewportH = getBaseHeight(searchQuery);
        int panelBot = iy + totalH;

        
        scrollVelocity *= 0.92;
        if (Math.abs(scrollVelocity) < 0.5) scrollVelocity = 0;
        scrollOffset += scrollVelocity;
        int maxScroll = max(0, totalH - viewportH);
        scrollOffset = max(-maxScroll, min(0.0, scrollOffset));

        int activeColor = ColorUtility.getActiveColor();

        
        graphics.fill(ix + 3, iy + 3, ix + width + 3, panelBot + 3, 0x40000000);
        if (ClickGui.INSTANCE.outlines.getValue()) {
            int borderColor = ClickGui.INSTANCE.outlineColor.getValue();
            graphics.fill(ix - 1, iy - 1, ix + width + 1, panelBot + 1, borderColor);
        }
        graphics.fill(ix, iy, ix + width, panelBot, ColorUtility.PANEL_BODY_END);
        graphics.fill(ix, iy, ix + width, iy + 18, ColorUtility.HEADER_COLOR);
        if (ClickGui.INSTANCE.outlines.getValue()) {
            graphics.fill(ix, iy + 17, ix + width, iy + 18, ClickGui.INSTANCE.outlineColor.getValue());
        } else {
            graphics.fill(ix, iy + 17, ix + width, iy + 18, 0x15FFFFFF);
        }

        
        if (ClickGui.INSTANCE.headerGlow.getValue()) {
            int glowIntensity = ClickGui.INSTANCE.headerGlowIntensity.getValue().intValue();
            int glowCol = ColorUtility.withAlpha(activeColor, glowIntensity);
            int textGlowW = FontRenderUtility.getStringWidth(category.getDisplayName()) + 20;
            graphics.fill(ix + 20, iy, ix + 20 + textGlowW, iy + 17, glowCol);
            Identifier catTexGlow = ClickGUI.getCategoryTexture(category);
            if (catTexGlow != null)
                graphics.fill(ix + 3, iy + 1, ix + 19, iy + 17, glowCol);
        }

        
        Identifier catTexWhite = ravex.utility.render.TextureLoader.getCategoryTextureWhite(category);
        if (catTexWhite != null) {
            int iconSize = 18;
            int iconX = ix + 2;
            int iconY = iy;
            graphics.blit(catTexWhite, iconX, iconY, iconX + iconSize, iconY + iconSize, 0.0f, 1.0f, 0.0f, 1.0f);
        }

        
        String header = category.getDisplayName();
        int headerY = iy + (18 - FontRenderUtility.getFontHeight()) / 2 + 1;
        FontRenderUtility.drawString(graphics, FontRenderUtility.FontType.VANILLA, header,
            ix + ravex.modules.client.Settings.INSTANCE.headerTextX.getValue().intValue(), headerY, 0xFFFFFFFF, true);

        
        if (ClickGui.INSTANCE.moduleCounter.getValue()) {
            List<ModuleButton> visibleModules = filterButtons(searchQuery);
            int enabled = (int) visibleModules.stream().filter(b -> b.getModule().getEnabled()).count();
            int total = visibleModules.size();
            String countText = enabled + "/" + total;
            int cw = FontRenderUtility.getStringWidth(countText);
            int pad = 4;
            int badgeX = ix + width - cw - pad - 4;
            int badgeY = iy + 3;
            int badgeH = 12;
            graphics.fill(badgeX, badgeY, badgeX + cw + pad * 2, badgeY + badgeH, 0x30000000);
            graphics.fill(badgeX, badgeY, badgeX + cw + pad * 2, badgeY + badgeH, ColorUtility.withAlpha(activeColor, 20));
            graphics.fill(badgeX, badgeY + badgeH - 1, badgeX + cw + pad * 2, badgeY + badgeH, ColorUtility.withAlpha(activeColor, 60));
            FontRenderUtility.drawString(graphics, FontRenderUtility.FontType.VANILLA, countText,
                badgeX + pad, badgeY + (badgeH - FontRenderUtility.getFontHeight()) / 2 + 1,
                enabled == total ? 0xFFA0E0A0 : 0xFFE0E0E0, true);
        }

        
        if (totalH > viewportH || scrollOffset != 0) {
            graphics.enableScissor(ix, listTop, ix + width, panelBot);
        }
        int[] renderYOut = { listTop + (int) Math.round(scrollOffset) };
        for (ModuleButton btn : visible)
            btn.render(graphics, ix, iy, width, mouseX, mouseY, renderYOut, searchQuery);
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
        List<ModuleButton> visible = allButtons.stream()
            .filter(b -> b.getModule().isVisible())
            .collect(Collectors.toList());
        if (query == null || query.isEmpty()) return visible;
        String lower = query.toLowerCase();
        return visible.stream()
            .filter(b -> b.getModule().getName().toLowerCase().contains(lower))
            .collect(Collectors.toList());
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button, net.minecraft.client.Minecraft mc) {
        int ix = (int) x;
        int iy = (int) y;

        if (button == 0 && mouseX >= ix && mouseX <= ix + width && mouseY >= iy && mouseY <= iy + 18) {
            dragging = true;
            dragOffsetX = mouseX - x;
            dragOffsetY = mouseY - y;
            return true;
        }

        String query = ClickGUI.searchQuery;
        int listTop = iy + 18;
        int totalH = getCurrentHeight(query);
        int panelBot = iy + totalH;

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
        int listTop = iy + 18;
        int totalH = getCurrentHeight(searchQuery);
        int panelBot = iy + totalH;

        
        if (mouseX >= ix && mouseX <= ix + width && mouseY >= iy && mouseY <= panelBot) {
            
            if (!ClickGui.INSTANCE.separateSettings.getValue()) {
                List<ModuleButton> visible = filterButtons(searchQuery);
                int btnH = ClickGui.INSTANCE.buttonHeight.getValue().intValue();
                int renderY = listTop + (int) Math.round(scrollOffset);
                for (ModuleButton btn : visible) {
                    if (btn.isExpanded()) {
                        int expH = btn.getExpandedHeight(width);
                        if (mouseY >= renderY + btnH && mouseY < renderY + btnH + expH) {
                            if (btn.onInlineScroll(verticalAmount)) {
                                return true;
                            }
                        }
                        renderY += btnH + expH;
                    } else {
                        renderY += btnH;
                    }
                }
            }

            
            int screenH = net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledHeight();
            double minY = -totalH + 40;
            double maxY = screenH - 20;
            targetY = Math.max(minY, Math.min(maxY, targetY + verticalAmount * 16.0));
            return true;
        }
        return false;
    }

    
    public int getCurrentHeight(String searchQuery) {
        List<ModuleButton> visible = filterButtons(searchQuery);
        if (visible.isEmpty() && !searchQuery.isEmpty()) return 0;
        if (visible.isEmpty() && allButtons.isEmpty()) return 0;

        int btnH = ravex.modules.render.ClickGui.INSTANCE.buttonHeight.getValue().intValue();
        int h = 18 + visible.size() * btnH;

        boolean sepMode = ravex.modules.render.ClickGui.INSTANCE.separateSettings.getValue();
        if (!sepMode) {
            int panelW = this.width;
            for (ModuleButton btn : visible) {
                if (btn.isExpanded()) {
                    h += btn.getExpandedHeight(panelW);
                }
            }
        }
        return h;
    }

    
    public int getBaseHeight(String searchQuery) {
        return getCurrentHeight(searchQuery);
    }
}
