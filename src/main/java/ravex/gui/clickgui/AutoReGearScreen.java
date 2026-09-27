package ravex.gui.clickgui;
import ravex.utility.render.ColorUtility;

import ravex.mcwrapper.MinecraftWrapper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;
import ravex.modules.player.autoregear.AutoReGearData;
import ravex.modules.player.autoregear.AutoReGear;
import ravex.utility.render.FontRenderUtility;
import ravex.utility.render.Render2DUtility;

import java.util.ArrayList;
import java.util.List;

public class AutoReGearScreen extends Screen {

    private static final int ITEM_SIZE   = 20;

    private final Screen parent;
    private final List<Item> allItems = new ArrayList<>();
    private final List<Item> filteredItems = new ArrayList<>();

    private String searchQuery  = "";
    private boolean searchFocus = false;
    private float gridScroll = 0;
    private float selectedScroll = 0;
    private String hoveredTooltip = null;
    private int tooltipX, tooltipY;
    private long openTime = -1;

    private boolean quitHovered  = false;

    private String editingItemId = null;
    private String editingText = "";

    private void saveEditingTargetCount() {
        if (editingItemId == null) return;
        try {
            int count = editingText.isEmpty() ? 1 : Integer.parseInt(editingText);
            count = Math.max(1, Math.min(256, count));
            AutoReGearData.INSTANCE.setTargetCount(editingItemId, count);
        } catch (NumberFormatException e) {

        }
        editingItemId = null;
    }

    public AutoReGearScreen(Screen parent) {
        super(Component.literal(ravex.utility.misc.LanguageUtility.t("arg_subtitle")));
        this.parent = parent;

        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) continue;
            allItems.add(item);
        }
        rebuildFiltered();
    }

    private void rebuildFiltered() {
        filteredItems.clear();
        String q = searchQuery.toLowerCase().trim();
        for (Item item : allItems) {
            if (q.isEmpty()) {
                filteredItems.add(item);
            } else {
                String name = new ItemStack(item).getHoverName().getString().toLowerCase();
                Identifier rl = BuiltInRegistries.ITEM.getKey(item);
                if (rl == null) continue;
                String id = rl.toString();
                if (name.contains(q) || id.contains(q)) {
                    filteredItems.add(item);
                }
            }
        }
        gridScroll = 0;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float pt) {
        long now = System.currentTimeMillis();
        if (openTime < 0) openTime = now;

        float elapsed = (now - openTime);
        float progress = Math.min(1.0f, elapsed / 200f);
        float scale = progress * (2f - progress);

        int W = this.width;
        int H = this.height;

        int bgA = (int)(progress * 0x99);
        g.fill(0, 0, W, H, ColorUtility.setAlpha(0x05050E, bgA));

        int panelW = 680;
        int panelH = 420;
        if (W < panelW + 40) panelW = W - 40;
        if (H < panelH + 40) panelH = H - 40;

        int panelX = (W - panelW) / 2;
        int panelY = (H - panelH) / 2;

        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(W / 2.0f, H / 2.0f);
        pose.scale(scale, scale);
        pose.translate(-W / 2.0f, -H / 2.0f);

        float cx = W / 2.0f, cy = H / 2.0f;
        int mx = scale > 0.01f ? (int)((mouseX - cx) / scale + cx) : mouseX;
        int my = scale > 0.01f ? (int)((mouseY - cy) / scale + cy) : mouseY;

        hoveredTooltip = null;

        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xF0121218);
        Render2DUtility.drawBorder(g, panelX, panelY, panelW, panelH, 1, 0xFF33333F);

        int headerH = 28;
        g.fill(panelX, panelY, panelX + panelW, panelY + headerH, 0xFF1A1A22);
        g.fill(panelX, panelY + headerH - 1, panelX + panelW, panelY + headerH, ColorUtility.getActiveColor());
        FontRenderUtility.drawString(g, ravex.utility.misc.LanguageUtility.t("arg_title"), panelX + 10, panelY + 6, ColorUtility.getActiveColor(), true);
        FontRenderUtility.drawString(g, ravex.utility.misc.LanguageUtility.t("arg_back"), panelX + panelW - 72, panelY + 8, 0xFF606080, false);

        int leftW  = (panelW / 2) - 6;
        int rightW = panelW - leftW - 18;
        int leftX  = panelX + 6;
        int rightX = panelX + leftW + 12;
        int contentY = panelY + headerH + 6;
        int contentH = panelH - headerH - 50;


        g.fill(leftX, contentY, leftX + leftW, contentY + contentH, 0xFF121218);
        Render2DUtility.drawBorder(g, leftX, contentY, leftW, contentH, 1, 0xFF2E2E3A);

        int colHeaderH = 18;
        g.fill(leftX, contentY, leftX + leftW, contentY + colHeaderH, 0xFF1A1A22);
        FontRenderUtility.drawString(g, "§7All Items §8(" + filteredItems.size() + ")", leftX + 5, contentY + 3, 0xFFAAAAAA, false);

        int searchY = contentY + colHeaderH + 2;
        int searchH = 14;
        int searchW = leftW - 8;
        g.fill(leftX + 4, searchY, leftX + 4 + searchW, searchY + searchH, searchFocus ? 0xFF20202A : 0xFF16161C);
        Render2DUtility.drawBorder(g, leftX + 4, searchY, searchW, searchH, 1, searchFocus ? ColorUtility.getActiveColor() : 0xFF3A3A46);
        String searchDisplay = searchQuery.isEmpty() && !searchFocus ? "§8Search..." : searchQuery + (searchFocus ? "§8|" : "");
        FontRenderUtility.drawString(g, searchDisplay, leftX + 7, searchY + 2, 0xFFCCCCCC, false);

        int gridY = searchY + searchH + 3;
        int gridH = contentH - colHeaderH - searchH - 8;
        int gridX = leftX + 4;
        int gridW = leftW - 8;
        int cols = Math.max(8, gridW / ITEM_SIZE);

        int totalRows = (filteredItems.size() + cols - 1) / cols;
        int maxGridScroll = Math.max(0, totalRows * ITEM_SIZE - gridH);
        gridScroll = Math.max(0, Math.min(maxGridScroll, gridScroll));

        Render2DUtility.pushScissor(g, leftX, gridY, leftW, gridH);

        for (int i = 0; i < filteredItems.size(); i++) {
            int col = i % cols;
            int row = i / cols;
            int ix = gridX + col * ITEM_SIZE;
            int iy = gridY + row * ITEM_SIZE - (int) gridScroll;
            if (iy + ITEM_SIZE < gridY || iy > gridY + gridH) continue;

            Item item = filteredItems.get(i);
            ItemStack stack = new ItemStack(item);
            Identifier rl = BuiltInRegistries.ITEM.getKey(item);
            String itemId = rl != null ? rl.toString() : "";

            boolean sel = AutoReGearData.INSTANCE.isSelected(itemId);
            boolean hov = mx >= ix && mx <= ix + ITEM_SIZE - 1 && my >= iy && my <= iy + ITEM_SIZE - 1;

            if (hov) {
                g.fill(ix, iy, ix + ITEM_SIZE, iy + ITEM_SIZE, 0xFF2A2C38);
                hoveredTooltip = new ItemStack(item).getHoverName().getString() + "\n§8" + itemId + "\n§eClick to toggle in kit";
                tooltipX = mx; tooltipY = my;
            } else if (sel) {
                g.fill(ix, iy, ix + ITEM_SIZE, iy + ITEM_SIZE, 0x44DA70D6);
            }

            g.renderItem(stack, ix + 2, iy + 2);

            if (sel) {
                Render2DUtility.drawBorder(g, ix, iy, ITEM_SIZE, ITEM_SIZE, 1, 0xFF44FF88);
            }
        }

        Render2DUtility.popScissor(g);

        if (maxGridScroll > 0) {
            int barH = Math.max(20, gridH * gridH / (totalRows * ITEM_SIZE));
            int barY = gridY + (int) ((gridH - barH) * (gridScroll / maxGridScroll));
            int barX = leftX + leftW - 4;
            g.fill(barX, gridY, barX + 2, gridY + gridH, 0xFF1E1E26);
            g.fill(barX, barY, barX + 2, barY + barH, ColorUtility.getActiveColor());
        }


        g.fill(rightX, contentY, rightX + rightW, contentY + contentH, 0xFF121218);
        Render2DUtility.drawBorder(g, rightX, contentY, rightW, contentH, 1, 0xFF2E2E3A);

        g.fill(rightX, contentY, rightX + rightW, contentY + colHeaderH, 0xFF1A1A22);
        int selCount = AutoReGearData.INSTANCE.getSelectedItems().size();
        FontRenderUtility.drawString(g, "§dKit Items §7(" + selCount + ")", rightX + 5, contentY + 3,
            selCount > 0 ? ColorUtility.getActiveColor() : 0xFF777777, false);

        int selContentY = contentY + colHeaderH + 3;
        int selContentH = contentH - colHeaderH - 6;
        Render2DUtility.pushScissor(g, rightX, selContentY, rightW, selContentH);

        var selectedSet = AutoReGearData.INSTANCE.getSelectedItems().keySet();
        String[] selectedArr = selectedSet.toArray(new String[0]);
        int maxSelScroll = Math.max(0, selectedArr.length * 18 - selContentH);
        selectedScroll = Math.max(0, Math.min(maxSelScroll, selectedScroll));

        int sy = selContentY + 3 - (int)selectedScroll;
        for (String itemId : selectedArr) {
            Identifier rlId = Identifier.tryParse(itemId);
            if (rlId == null) continue;
            Item item = null;
            for (Item candidate : BuiltInRegistries.ITEM) {
                Identifier key = BuiltInRegistries.ITEM.getKey(candidate);
                if (rlId.equals(key)) { item = candidate; break; }
            }
            if (item == null || item == Items.AIR) continue;

            ItemStack stack = new ItemStack(item);
            int targetCount = AutoReGearData.INSTANCE.getTargetCount(itemId);

            boolean rowHov = mx >= rightX + 3 && mx <= rightX + rightW - 3 && my >= sy && my <= sy + 16;
            if (rowHov) {
                g.fill(rightX + 3, sy, rightX + rightW - 3, sy + 16, 0xFF23232C);
                hoveredTooltip = "§eLeft-click: +8 Target Count\n§cRight-click: -8 Target Count\n"
                                 + "§bScroll over count: +/-1 count\n"
                                 + "§aMiddle-click: type count manually\n"
                                 + "§7Item: " + new ItemStack(item).getHoverName().getString();
                tooltipX = mx; tooltipY = my;
            }

            g.renderItem(stack, rightX + 4, sy);
            FontRenderUtility.drawString(g, new ItemStack(item).getHoverName().getString(), rightX + 22, sy + 3, 0xFFDDDDDD, false);


            if (itemId.equals(editingItemId)) {
                String amountStr = editingText + (System.currentTimeMillis() % 1000 < 500 ? "|" : "");
                int amW = FontRenderUtility.getStringWidth(amountStr);
                g.fill(rightX + rightW - 85, sy + 1, rightX + rightW - 20, sy + 15, 0xFF16161C);
                Render2DUtility.drawBorder(g, rightX + rightW - 85, sy + 1, 65, 14, 1, ColorUtility.getActiveColor());
                FontRenderUtility.drawString(g, amountStr, rightX + rightW - 25 - amW, sy + 3, 0xFFFFFFFF, false);
            } else {
                String amountStr = "x" + targetCount;
                int amW = FontRenderUtility.getStringWidth(amountStr);
                FontRenderUtility.drawString(g, amountStr, rightX + rightW - 32 - amW, sy + 3, ColorUtility.getActiveColor(), false);
            }


            FontRenderUtility.drawString(g, "x", rightX + rightW - 14, sy + 3, 0xFFFF4455, false);

            sy += 18;
        }

        Render2DUtility.popScissor(g);


        int btnY   = panelY + panelH - 36;
        int btnH   = 18;
        int quitW  = 120;
        int quitX  = panelX + (panelW - quitW) / 2;

        quitHovered = mx >= quitX && mx <= quitX + quitW && my >= btnY && my <= btnY + btnH;
        g.fill(quitX, btnY, quitX + quitW, btnY + btnH, quitHovered ? 0xFF2A2A35 : 0xFF1C1C24);
        Render2DUtility.drawBorder(g, quitX, btnY, quitW, btnH, 1, quitHovered ? ColorUtility.getActiveColor() : 0xFF3A3A46);
        String scv = ravex.utility.misc.LanguageUtility.t("arg_save_close");
        int qtw = FontRenderUtility.getStringWidth(scv);
        FontRenderUtility.drawString(g, scv, quitX + quitW / 2 - qtw / 2, btnY + 4, 0xFFFFFFFF, true);

        pose.popMatrix();

        if (hoveredTooltip != null && scale > 0.8f) {
            renderTooltip(g, hoveredTooltip, mouseX, mouseY);
        }

        super.render(g, mouseX, mouseY, pt);
    }

    private void renderTooltip(GuiGraphics g, String text, int mx, int my) {
        String[] lines = text.split("\n");
        int maxW = 0;
        for (String line : lines) {
            int lw = FontRenderUtility.getStringWidth(line);
            if (lw > maxW) maxW = lw;
        }
        int tw = maxW + 10;
        int th = lines.length * 10 + 6;
        int tx = mx + 8;
        int ty = my - th - 4;
        if (tx + tw > this.width) tx = this.width - tw - 4;
        if (ty < 2) ty = my + 12;

        g.fill(tx - 1, ty - 1, tx + tw + 1, ty + th + 1, 0xCC23232C);
        g.fill(tx, ty, tx + tw, ty + th, 0xEE121216);
        Render2DUtility.drawBorder(g, tx, ty, tw, th, 1, ColorUtility.withAlpha(ColorUtility.getActiveColor(), 160));

        int ly = ty + 3;
        for (String line : lines) {
            FontRenderUtility.drawString(g, line, tx + 5, ly, 0xFFEEEEEE, false);
            ly += 10;
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean handled) {
        int mx = (int) event.x();
        int my = (int) event.y();
        int btn = event.button();

        int W = this.width, H = this.height;
        int panelW = Math.max(300, Math.min(680, W - 40));
        int panelH = Math.max(250, Math.min(420, H - 40));
        int panelX = (W - panelW) / 2;
        int panelY = (H - panelH) / 2;

        int headerH  = 28;
        int contentY = panelY + headerH + 6;
        int contentH = panelH - headerH - 50;
        int leftW    = (panelW / 2) - 6;
        int rightW   = panelW - leftW - 18;
        int leftX    = panelX + 6;
        int rightX   = panelX + leftW + 12;
        int colHeaderH = 18;

        int btnY  = panelY + panelH - 36;
        int btnH  = 18;
        int quitW = 120;
        int quitX = panelX + (panelW - quitW) / 2;


        if (mx >= quitX && mx <= quitX + quitW && my >= btnY && my <= btnY + btnH && btn == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            saveEditingTargetCount();
            onClose();
            return true;
        }


        int searchY = contentY + colHeaderH + 2;
        int searchH = 14;
        if (mx >= leftX + 4 && mx <= leftX + leftW - 4 && my >= searchY && my <= searchY + searchH) {
            saveEditingTargetCount();
            searchFocus = true;
            return true;
        } else if (btn == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            searchFocus = false;
        }


        int gridY = searchY + searchH + 3;
        int gridH = contentH - colHeaderH - searchH - 8;

        int gridX = leftX + 4;
        int gridW = leftW - 8;
        int cols = Math.max(8, gridW / ITEM_SIZE);
        if (mx >= leftX && mx <= leftX + leftW && my >= gridY && my <= gridY + gridH) {
            int col = (mx - gridX) / ITEM_SIZE;
            int row = (my - gridY + (int) gridScroll) / ITEM_SIZE;
            int idx = row * cols + col;
            if (col >= 0 && col < cols && row >= 0 && idx < filteredItems.size()) {
                saveEditingTargetCount();
                Item item = filteredItems.get(idx);
                Identifier rl = BuiltInRegistries.ITEM.getKey(item);
                if (rl != null) {
                    AutoReGearData.INSTANCE.toggle(rl.toString(), item.getDefaultMaxStackSize());
                }
                return true;
            }
        }


        int selContentY = contentY + colHeaderH + 3;
        int selContentH = contentH - colHeaderH - 6;
        if (mx >= rightX && mx <= rightX + rightW && my >= selContentY && my <= selContentY + selContentH) {
            var selectedArr = AutoReGearData.INSTANCE.getSelectedItems().keySet().toArray(new String[0]);
            int sy = selContentY + 3 - (int)selectedScroll;
            for (String itemId : selectedArr) {
                if (my >= sy && my <= sy + 16) {

                    if (mx >= rightX + rightW - 16 && mx <= rightX + rightW - 4) {
                        AutoReGearData.INSTANCE.deselect(itemId);
                        if (itemId.equals(editingItemId)) {
                            editingItemId = null;
                        }
                    } else if (btn == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
                        if (editingItemId != null && !editingItemId.equals(itemId)) {
                            saveEditingTargetCount();
                        }
                        editingItemId = itemId;
                        editingText = String.valueOf(AutoReGearData.INSTANCE.getTargetCount(itemId));
                    } else {
                        if (editingItemId != null && !editingItemId.equals(itemId)) {
                            saveEditingTargetCount();
                        }

                        int count = AutoReGearData.INSTANCE.getTargetCount(itemId);
                        int maxStack = 64;
                        Identifier rId = Identifier.tryParse(itemId);
                        if (rId != null) {
                            for (Item candidate : BuiltInRegistries.ITEM) {
                                if (rId.equals(BuiltInRegistries.ITEM.getKey(candidate))) {
                                    maxStack = candidate.getDefaultMaxStackSize();
                                    break;
                                }
                            }
                        }
                        int step = (maxStack == 1) ? 1 : (maxStack == 16) ? 4 : 8;

                        if (btn == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                            count = Math.min(256, count + step);
                        } else if (btn == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                            count = Math.max(1, count - step);
                        }
                        AutoReGearData.INSTANCE.setTargetCount(itemId, count);
                    }
                    return true;
                }
                sy += 18;
            }
            if (editingItemId != null) {
                saveEditingTargetCount();
            }
        } else if (editingItemId != null) {
            saveEditingTargetCount();
        }

        return super.mouseClicked(event, handled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double hAmt, double vAmt) {
        int W = this.width, H = this.height;
        int panelW = Math.max(300, Math.min(680, W - 40));
        int panelH = Math.max(250, Math.min(420, H - 40));
        int panelX = (W - panelW) / 2;
        int panelY = (H - panelH) / 2;
        int headerH  = 28;
        int contentY = panelY + headerH + 6;
        int contentH = panelH - headerH - 50;
        int leftW    = (panelW / 2) - 6;
        int rightW   = panelW - leftW - 18;
        int leftX    = panelX + 6;
        int rightX   = panelX + leftW + 12;
        int colHeaderH = 18;
        int selContentY = contentY + colHeaderH + 3;
        int selContentH = contentH - colHeaderH - 6;

        int gridTopY = contentY + colHeaderH + 2 + 14 + 3;
        int gridBotH = contentH - colHeaderH - 14 - 8;
        if (mouseX >= leftX && mouseX <= leftX + leftW && mouseY >= gridTopY && mouseY <= gridTopY + gridBotH) {
            int gridCols = Math.max(8, (leftW - 8) / ITEM_SIZE);
            int gridRows = (filteredItems.size() + gridCols - 1) / gridCols;
            int maxScroll = Math.max(0, gridRows * ITEM_SIZE - gridBotH);
            gridScroll = Math.max(0, Math.min(maxScroll, gridScroll - (float) (vAmt * 40)));
            return true;
        }

        if (mouseX >= rightX && mouseX <= rightX + rightW && mouseY >= selContentY && mouseY <= selContentY + selContentH) {

            var selectedArr = AutoReGearData.INSTANCE.getSelectedItems().keySet().toArray(new String[0]);
            int sy = selContentY + 3 - (int)selectedScroll;
            for (String itemId : selectedArr) {
                if (mouseY >= sy && mouseY <= sy + 16) {
                    if (mouseX >= rightX + rightW - 85 && mouseX <= rightX + rightW - 18) {
                        int count = AutoReGearData.INSTANCE.getTargetCount(itemId);
                        int change = (vAmt > 0) ? 1 : -1;
                        count = Math.max(1, Math.min(256, count + change));
                        AutoReGearData.INSTANCE.setTargetCount(itemId, count);

                        if (itemId.equals(editingItemId)) {
                            editingText = String.valueOf(count);
                        }
                        return true;
                    }
                }
                sy += 18;
            }

            int selCount = AutoReGearData.INSTANCE.getSelectedItems().size();
            int maxSelScroll = Math.max(0, selCount * 18 - selContentH);
            selectedScroll = (float) Math.max(0, Math.min(maxSelScroll, selectedScroll - vAmt * 12));
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, hAmt, vAmt);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (editingItemId != null) {
                saveEditingTargetCount();
                return true;
            }
            onClose();
            return true;
        }
        if (editingItemId != null) {
            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                if (!editingText.isEmpty()) {
                    editingText = editingText.substring(0, editingText.length() - 1);
                }
                return true;
            }
            if (key == GLFW.GLFW_KEY_ENTER) {
                saveEditingTargetCount();
                return true;
            }
        }
        if (searchFocus) {
              if (key == GLFW.GLFW_KEY_BACKSPACE && !searchQuery.isEmpty()) {
                  searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                  rebuildFiltered();
                return true;
            }
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_ESCAPE) {
                searchFocus = false;
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (editingItemId != null) {
            int codepoint = event.codepoint();
            if (Character.isDigit((char) codepoint)) {
                if (editingText.length() < 3) {
                    editingText += (char) codepoint;
                }
                return true;
            }
        }
        if (searchFocus) {
            int codepoint = event.codepoint();
              if (codepoint >= 32 && codepoint < 127) {
                  searchQuery += (char)codepoint;
                  rebuildFiltered();
                return true;
            }
        }
        return super.charTyped(event);
    }

    @Override
    public void onClose() {
        saveEditingTargetCount();
        MinecraftWrapper.getWrapper().setScreen(parent);
    }
}
