package ravex.gui.clickgui;
import ravex.utility.render.ColorUtility;

import net.minecraft.client.gui.GuiGraphics;
import ravex.mcwrapper.MinecraftWrapper;
import net.minecraft.resources.Identifier;
import ravex.gui.clickgui.ClickGUI;
import ravex.modules.Module;
import ravex.manager.ModuleManager;
import ravex.utility.render.Render2DUtility;
import ravex.modules.client.ClickGui;
import ravex.utility.render.FontRenderUtility;
import ravex.utility.render.animate.AnimationUtility;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.Math.*;

public class CategoryPanel {
    private final String category;
    private double x;
    private double y;
    private double targetX;
    private double targetY;
    private double vx = 0;
    private double vy = 0;

    private boolean dragging = false;
    private boolean customPosition = false;
    private double dragOffsetX = 0;
    private double dragOffsetY = 0;
    private float dragGlow = 0f;

    private final List<ModuleButton> allButtons = new ArrayList<>();
    private float headerAnim = 0f;
    private double scrollOffset = 0.0;
    private int lastEnabledCount = -1;
    private int oldEnabledCount = 0;
    private float countTransitionProgress = 1.0f;
    private boolean countDirectionUp = true;
    private float smoothBadgeWidth = -1f;

    public CategoryPanel(String category, int x, int y) {
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

    public String getCategory() { return category; }
    public int getX() { return (int) x; }
    public int getY() { return (int) y; }
    public boolean isCustomPosition() { return customPosition; }
    public void setCustomPosition(boolean customPosition) { this.customPosition = customPosition; }

    public void resetExpansion() {
        for (ModuleButton b : allButtons) {
            b.collapse();
        }
    }

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
                MinecraftWrapper.getWrapper().getWindow().handle(),
                org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT
            ) == org.lwjgl.glfw.GLFW.GLFW_PRESS;

            if (!isMouseDown) {
                dragging = false;
                ravex.utility.misc.CursorUtility.reset();
            } else {
                targetX = mouseX - dragOffsetX;
                targetY = mouseY - dragOffsetY;
                customPosition = true;
            }
        }
        double dragLerp = 0.12;

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
        int width = (int) ModuleManager.get(ClickGui.class).panelWidth;
        int activeColor = ColorUtility.getActiveColor();

        boolean hasSearch = searchQuery != null && !searchQuery.isEmpty();
        for (ModuleButton btn : allButtons) {
            if (!btn.getModule().isVisible()) continue;
            boolean matches = !hasSearch || ravex.utility.misc.SearchUtility.matches(btn.getModule().getName() + " " + ravex.utility.misc.LanguageUtility.moduleName(btn.getModule().getName()), searchQuery);
            btn.updateSearchReveal(matches, hasSearch);
        }

        List<ModuleButton> visible = filterButtons(searchQuery);
        if (visible.isEmpty() && hasSearch) return;
        if (visible.isEmpty() && allButtons.isEmpty()) return;

        int btnH = (int) ModuleManager.get(ClickGui.class).buttonHeight;
        int listTop = iy + 22;
        int totalH = 22;
        for (ModuleButton btn : visible) {
            totalH += Math.max(1, (int)(btnH * btn.getSearchReveal())) + 2;
            if (btn.isExpanded()) totalH += (int)(btn.getExpandedHeight(width) * btn.getSearchReveal());
        }
        int maxPanelHeight = (int)(MinecraftWrapper.getWrapper().getWindow().getGuiScaledHeight() * 0.75f);
        int viewportH = Math.min(totalH, maxPanelHeight);
        int panelBot = iy + viewportH;

        int maxScroll = max(0, totalH - viewportH);
        double spring = 0.2;
        if (maxScroll <= 0) {
            if (Math.abs(scrollOffset) > 0.5) {
                scrollOffset -= scrollOffset * spring;
            } else {
                scrollOffset = 0;
            }
        } else {
            if (scrollOffset > 0) {
                scrollOffset -= scrollOffset * spring;
                if (scrollOffset < 0.5) scrollOffset = 0;
            } else if (scrollOffset < -maxScroll) {
                scrollOffset += (-maxScroll - scrollOffset) * spring;
                if (Math.abs(scrollOffset + maxScroll) < 0.5) scrollOffset = -maxScroll;
            }
        }

        int panelH = panelBot - iy;
        int r = Math.min((int) ModuleManager.get(ClickGui.class).cornerRadius, panelH / 2);

        int pAlpha = (int) ModuleManager.get(ClickGui.class).panelOpacity;
        Render2DUtility.drawRound(graphics, ix, iy, width, panelH, r, ColorUtility.withAlpha(ColorUtility.PANEL_BODY_END, pAlpha));

        if (ModuleManager.get(ClickGui.class).outlines) {
            int borderColor = ModuleManager.get(ClickGui.class).outlineColor;
            Render2DUtility.drawRound(graphics, ix - 1, iy - 1, width + 2, panelH + 2, r, borderColor);
        }

        float dragTarget = dragging ? 1f : 0f;
        dragGlow += (dragTarget - dragGlow) * 0.18f;
        if (Math.abs(dragTarget - dragGlow) < 0.01f) dragGlow = dragTarget;
        if (dragGlow > 0.01f) {
            float eased = dragGlow * dragGlow * (3f - 2f * dragGlow);
            int coreAlpha = (int) (150 * eased);
            int midAlpha = (int) (70 * eased);
            int haloAlpha = (int) (28 * eased);
            Render2DUtility.drawRoundBorder(graphics, ix - 3, iy - 3, width + 6, panelH + 6, r + 2, 1, ColorUtility.setAlpha(0xFFFFFF, haloAlpha));
            Render2DUtility.drawRoundBorder(graphics, ix - 2, iy - 2, width + 4, panelH + 4, r + 1, 1, ColorUtility.setAlpha(0xFFFFFF, midAlpha));
            Render2DUtility.drawRoundBorder(graphics, ix - 1, iy - 1, width + 2, panelH + 2, r, 1, ColorUtility.setAlpha(0xFFFFFF, coreAlpha));
        }

        Identifier catTexWhite = ravex.utility.render.TextureLoaderUtility.getCategoryTextureWhite(category);
        if (catTexWhite != null) {
            int iconSize = 14;
            int iconX = ix + 5;
            int iconY = iy + 2;
            graphics.blit(catTexWhite, iconX, iconY, iconX + iconSize, iconY + iconSize, 0.0f, 1.0f, 0.0f, 1.0f);
        }

        String header = ravex.utility.misc.LanguageUtility.categoryName(category);
        int headerY = iy + (18 - FontRenderUtility.getFontHeight()) / 2 + 1;
        FontRenderUtility.drawString(graphics, header,
            ix + 23, headerY, 0xFFFFFFFF, true);

        if (ModuleManager.get(ClickGui.class).moduleCounter) {
            int enabled = 0;
            for (ModuleButton b : visible) {
                if (b.getModule().getEnabled()) enabled++;
            }
            int total = visible.size();
            if (lastEnabledCount == -1) {
                lastEnabledCount = enabled;
                oldEnabledCount = enabled;
                countTransitionProgress = 1.0f;
            } else if (enabled != lastEnabledCount) {
                if (countTransitionProgress < 1.0f) {
                    oldEnabledCount = Math.round(oldEnabledCount + (lastEnabledCount - oldEnabledCount) * AnimationUtility.Easing.CUBIC_OUT.apply(countTransitionProgress));
                } else {
                    oldEnabledCount = lastEnabledCount;
                }
                countDirectionUp = enabled > oldEnabledCount;
                lastEnabledCount = enabled;
                countTransitionProgress = 0.0f;
            }

            if (countTransitionProgress < 1.0f) {
                countTransitionProgress = Math.min(1.0f, countTransitionProgress + 0.12f);
            }

            float eased = AnimationUtility.Easing.CUBIC_OUT.apply(countTransitionProgress);
            String totalPart = "/" + total;
            String curEnabledStr = String.valueOf(enabled);
            String oldEnabledStr = String.valueOf(oldEnabledCount);
            int curNumW = FontRenderUtility.getStringWidth(curEnabledStr);
            int oldNumW = FontRenderUtility.getStringWidth(oldEnabledStr);
            float targetNumW = curNumW + (oldNumW - curNumW) * (1.0f - eased);
            int totalPartW = FontRenderUtility.getStringWidth(totalPart);
            float targetCw = targetNumW + totalPartW;
            if (smoothBadgeWidth < 0f) {
                smoothBadgeWidth = targetCw;
            } else {
                smoothBadgeWidth += (targetCw - smoothBadgeWidth) * 0.25f;
            }

            int pad = 4;
            int badgeW = Math.round(smoothBadgeWidth) + pad * 2;
            int badgeX = ix + width - badgeW - 8;
            int badgeY = iy + 4;
            int badgeH = 14;
            Render2DUtility.drawRound(graphics, badgeX, badgeY, badgeW, badgeH, 4, 0x22000000);

            Render2DUtility.pushScissor(graphics, badgeX, badgeY, badgeW, badgeH);
            int baseY = badgeY + (badgeH - FontRenderUtility.getFontHeight()) / 2 + 1;
            int color = enabled == total ? 0xFFA0E0A0 : 0xFFE0E0E0;

            int numX = badgeX + pad;
            int totalX = numX + Math.round(targetNumW);
            FontRenderUtility.drawString(graphics, totalPart, totalX, baseY, color, true);

            if (countTransitionProgress < 1.0f && !curEnabledStr.equals(oldEnabledStr)) {
                float slideDist = badgeH * 0.85f;
                float oldY = countDirectionUp ? (baseY - eased * slideDist) : (baseY + eased * slideDist);
                float newY = countDirectionUp ? (baseY + (1.0f - eased) * slideDist) : (baseY - (1.0f - eased) * slideDist);
                int oldAlpha = (int) ((1.0f - eased) * 255);
                int newAlpha = (int) (eased * 255);
                FontRenderUtility.drawString(graphics, oldEnabledStr, numX, (int) oldY, ColorUtility.withAlpha(color, oldAlpha), true);
                FontRenderUtility.drawString(graphics, curEnabledStr, numX, (int) newY, ColorUtility.withAlpha(color, newAlpha), true);
            } else {
                FontRenderUtility.drawString(graphics, curEnabledStr, numX, baseY, color, true);
            }
            Render2DUtility.popScissor(graphics);
        }

        if (totalH > viewportH || scrollOffset != 0) {
            Render2DUtility.pushScissor(graphics, ix, listTop, width, panelBot - listTop);
        }
        int[] renderYOut = { listTop + (int) Math.round(scrollOffset) };
        for (ModuleButton btn : visible)
            btn.render(graphics, ix, iy, width, mouseX, mouseY, renderYOut, searchQuery, listTop, panelBot);
        if (totalH > viewportH || scrollOffset != 0) {
            Render2DUtility.popScissor(graphics);
        }
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        render(graphics, mouseX, mouseY, "");
    }

    public int getMatchCount(String query) {
        return (int) allButtons.stream()
            .filter(b -> ravex.utility.misc.SearchUtility.matches(b.getModule().getName() + " " + ravex.utility.misc.LanguageUtility.moduleName(b.getModule().getName()), query))
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
        int width = (int) ModuleManager.get(ClickGui.class).panelWidth;

        if (button == 0 && mouseX >= ix && mouseX <= ix + width && mouseY >= iy && mouseY <= iy + 18) {
            dragging = true;
            customPosition = true;
            dragOffsetX = mouseX - x;
            dragOffsetY = mouseY - y;
            ravex.utility.misc.CursorUtility.setHand();
            return true;
        }

        String query = ClickGUI.searchQuery;
        int listTop = iy + 22;
        int totalH = getCurrentHeight(query);
        int maxPanelHeight = (int)(mc.getWindow().getGuiScaledHeight() * 0.75f);
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
        int width = (int) ModuleManager.get(ClickGui.class).panelWidth;
        int listTop = iy + 22;
        int totalH = getCurrentHeight(searchQuery);
        int maxPanelHeight = (int)(MinecraftWrapper.getWrapper().getWindow().getGuiScaledHeight() * 0.75f);
        int panelBot = iy + Math.min(totalH, maxPanelHeight);

        if (mouseX >= ix && mouseX <= ix + width && mouseY >= iy && mouseY <= panelBot) {
            List<ModuleButton> visible = filterButtons(searchQuery);
            int btnH = (int) ModuleManager.get(ClickGui.class).buttonHeight;
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
        int width = (int) ModuleManager.get(ClickGui.class).panelWidth;
        List<ModuleButton> visible = filterButtons(searchQuery);
        if (visible.isEmpty() && !searchQuery.isEmpty()) return 0;
        if (visible.isEmpty() && allButtons.isEmpty()) return 0;

        int btnH = (int) ModuleManager.get(ClickGui.class).buttonHeight;
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
