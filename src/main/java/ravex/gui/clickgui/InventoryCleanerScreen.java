package ravex.gui.clickgui;

import net.minecraft.client.Minecraft;
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
import ravex.modules.player.inventorycleaner.InventoryCleanerData;
import ravex.modules.player.inventorycleaner.InventoryCleaner;
import ravex.utility.render.FontRenderUtility;
import ravex.utility.render.Render2DEngine;

import java.util.ArrayList;
import java.util.List;


public class InventoryCleanerScreen extends Screen {

    private static final int ITEM_SIZE   = 20;  
    private static final int COLS        = 8;   
    private static final int ROWS        = 6;   
    private static final int PAGE_SIZE   = COLS * ROWS;

    private final Screen parent;

    
    private final List<Item> allItems = new ArrayList<>();
    
    private final List<Item> filteredItems = new ArrayList<>();

    
    private String searchQuery  = "";
    private boolean searchFocus = false;

    
    private int currentPage = 0;

    
    private float selectedScroll = 0;

    
    private String hoveredTooltip = null;
    private int tooltipX, tooltipY;

    
    private long openTime = -1;

    
    private boolean cleanHovered = false;
    private boolean quitHovered  = false;
    private boolean prevHovered  = false;
    private boolean nextHovered  = false;

    
    private long cleanClickTime = -1;

    public InventoryCleanerScreen(Screen parent) {
        super(Component.literal("Inventory Cleaner"));
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
        
        int maxPage = Math.max(0, (filteredItems.size() - 1) / PAGE_SIZE);
        if (currentPage > maxPage) currentPage = maxPage;
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
        g.fill(0, 0, W, H, (bgA << 24) | 0x05050E);

        
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

        
        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xF0101020);
        Render2DEngine.drawBorder(g, panelX, panelY, panelW, panelH, 1, 0xFF2A1A4A);

        
        int headerH = 28;
        g.fill(panelX, panelY, panelX + panelW, panelY + headerH, 0xFF160E30);
        g.fill(panelX, panelY + headerH - 1, panelX + panelW, panelY + headerH, ColorUtility.getActiveColor());
        FontRenderUtility.drawString(g, "✦ Inventory Cleaner", panelX + 10, panelY + 6, ColorUtility.getActiveColor(), true);
        FontRenderUtility.drawString(g, "ESC / Quit", panelX + panelW - 72, panelY + 8, 0xFF606080, false);

        
        int leftW  = (panelW / 2) - 6;
        int rightW = panelW - leftW - 18;
        int leftX  = panelX + 6;
        int rightX = panelX + leftW + 12;
        int contentY = panelY + headerH + 6;
        int contentH = panelH - headerH - 50; 

        
        g.fill(leftX, contentY, leftX + leftW, contentY + contentH, 0xFF0C0920);
        Render2DEngine.drawBorder(g, leftX, contentY, leftW, contentH, 1, 0xFF281844);

        
        int colHeaderH = 18;
        g.fill(leftX, contentY, leftX + leftW, contentY + colHeaderH, 0xFF180E38);
        FontRenderUtility.drawString(g, "§7All Items §8(" + filteredItems.size() + ")", leftX + 5, contentY + 3, 0xFFAAAAAA, false);

        
        int searchY = contentY + colHeaderH + 2;
        int searchH = 14;
        int searchW = leftW - 8;
        boolean searchHov = mx >= leftX + 4 && mx <= leftX + 4 + searchW && my >= searchY && my <= searchY + searchH;
        g.fill(leftX + 4, searchY, leftX + 4 + searchW, searchY + searchH, searchFocus ? 0xFF1A1040 : 0xFF120B30);
        Render2DEngine.drawBorder(g, leftX + 4, searchY, searchW, searchH, 1,
            searchFocus ? ColorUtility.getActiveColor() : 0xFF2A1850);
        String searchDisplay = searchQuery.isEmpty() && !searchFocus ? "§8Search..." : searchQuery + (searchFocus ? "§8|" : "");
        FontRenderUtility.drawString(g, searchDisplay, leftX + 7, searchY + 2, 0xFFCCCCCC, false);

        
        int gridY = searchY + searchH + 3;
        int gridH = contentH - colHeaderH - searchH - 8;
        int gridX = leftX + 4;

        
        int startIdx = currentPage * PAGE_SIZE;
        int endIdx   = Math.min(startIdx + PAGE_SIZE, filteredItems.size());

        g.enableScissor(leftX, gridY, leftX + leftW, gridY + gridH);

        for (int i = startIdx; i < endIdx; i++) {
            int localIdx = i - startIdx;
            int col = localIdx % COLS;
            int row = localIdx / COLS;
            int ix = gridX + col * ITEM_SIZE;
            int iy = gridY + row * ITEM_SIZE;

            Item item = filteredItems.get(i);
            ItemStack stack = new ItemStack(item);
            Identifier rl = BuiltInRegistries.ITEM.getKey(item);
            String itemId = rl != null ? rl.toString() : "";

            boolean sel = InventoryCleanerData.INSTANCE.isSelected(itemId);
            boolean hov = mx >= ix && mx <= ix + ITEM_SIZE - 1 && my >= iy && my <= iy + ITEM_SIZE - 1;

            
            if (hov) {
                g.fill(ix, iy, ix + ITEM_SIZE, iy + ITEM_SIZE, 0xFF2A2040);
                hoveredTooltip = new ItemStack(item).getHoverName().getString() + "\n§8" + itemId;
                tooltipX = mx; tooltipY = my;
            } else if (sel) {
                g.fill(ix, iy, ix + ITEM_SIZE, iy + ITEM_SIZE, 0x44DA70D6);
            }

            
            g.renderItem(stack, ix + 2, iy + 2);

            
            if (sel) {
                FontRenderUtility.drawString(g, "§a✔", ix + ITEM_SIZE - 7, iy, 0xFF44FF88, false);
            }
        }

        g.disableScissor();

        
        int pageY = gridY + gridH + 2;
        int maxPage = Math.max(0, (filteredItems.size() - 1) / PAGE_SIZE);
        String pageStr = "Page " + (currentPage + 1) + " / " + (maxPage + 1);

        prevHovered = mx >= leftX + 4 && mx <= leftX + 26 && my >= pageY && my <= pageY + 12;
        nextHovered = mx >= leftX + leftW - 26 && mx <= leftX + leftW - 4 && my >= pageY && my <= pageY + 12;

        
        g.fill(leftX + 4, pageY, leftX + 26, pageY + 12, prevHovered ? 0xFF2A2050 : 0xFF180E38);
        FontRenderUtility.drawString(g, "◀", leftX + 10, pageY + 1, currentPage > 0 ? 0xFFCCCCCC : 0xFF444466, false);

        
        int ptw = FontRenderUtility.getStringWidth(pageStr);
        FontRenderUtility.drawString(g, pageStr, leftX + leftW / 2 - ptw / 2, pageY + 1, 0xFF9090B0, false);

        
        g.fill(leftX + leftW - 26, pageY, leftX + leftW - 4, pageY + 12, nextHovered ? 0xFF2A2050 : 0xFF180E38);
        FontRenderUtility.drawString(g, "▶", leftX + leftW - 22, pageY + 1, currentPage < maxPage ? 0xFFCCCCCC : 0xFF444466, false);

        
        g.fill(rightX, contentY, rightX + rightW, contentY + contentH, 0xFF0C0920);
        Render2DEngine.drawBorder(g, rightX, contentY, rightW, contentH, 1, 0xFF281844);

        
        g.fill(rightX, contentY, rightX + rightW, contentY + colHeaderH, 0xFF180E38);
        int selCount = InventoryCleanerData.INSTANCE.getSelectedItems().size();
        FontRenderUtility.drawString(g, "§dSelected §7(" + selCount + ")", rightX + 5, contentY + 3,
            selCount > 0 ? ColorUtility.getActiveColor() : 0xFF777777, false);

        
        int selContentY = contentY + colHeaderH + 3;
        int selContentH = contentH - colHeaderH - 6;
        g.enableScissor(rightX, selContentY, rightX + rightW, selContentY + selContentH);

        var selectedSet = InventoryCleanerData.INSTANCE.getSelectedItems();
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
            boolean rowHov = mx >= rightX + 3 && mx <= rightX + rightW - 3 && my >= sy && my <= sy + 16;
            if (rowHov) {
                g.fill(rightX + 3, sy, rightX + rightW - 3, sy + 16, 0xFF2A2040);
                hoveredTooltip = "§c✕ Remove: §f" + new ItemStack(item).getHoverName().getString();
                tooltipX = mx; tooltipY = my;
            }

            g.renderItem(stack, rightX + 4, sy);
            FontRenderUtility.drawString(g, new ItemStack(item).getHoverName().getString(), rightX + 22, sy + 3, 0xFFDDDDDD, false);

            
            FontRenderUtility.drawString(g, "§c✕", rightX + rightW - 12, sy + 3, 0xFFFF4455, false);

            sy += 18;
        }

        g.disableScissor();

        
        int btnY   = panelY + panelH - 36;
        int btnH   = 18;
        int cleanW = 100;
        int quitW  = 60;
        int cleanX = panelX + (panelW / 2) - cleanW - 6;
        int quitX  = panelX + (panelW / 2) + 6;

        
        cleanHovered = mx >= cleanX && mx <= cleanX + cleanW && my >= btnY && my <= btnY + btnH;
        boolean cleanFlash = (cleanClickTime > 0 && now - cleanClickTime < 300);
        int cleanBg = cleanFlash ? 0xFF44AA55 : cleanHovered ? 0xFF2A1850 : 0xFF1A0E38;
        g.fill(cleanX, btnY, cleanX + cleanW, btnY + btnH, cleanBg);
        Render2DEngine.drawBorder(g, cleanX, btnY, cleanW, btnH, 1,
            cleanHovered ? ColorUtility.getActiveColor() : 0xFF3A2060);
        int ctw = FontRenderUtility.getStringWidth("🗑 Clean Now");
        FontRenderUtility.drawString(g, "🗑 Clean Now", cleanX + cleanW / 2 - ctw / 2, btnY + 4, 0xFFFFFFFF, true);

        
        quitHovered = mx >= quitX && mx <= quitX + quitW && my >= btnY && my <= btnY + btnH;
        g.fill(quitX, btnY, quitX + quitW, btnY + btnH, quitHovered ? 0xFF3A1010 : 0xFF1A0E38);
        Render2DEngine.drawBorder(g, quitX, btnY, quitW, btnH, 1, quitHovered ? 0xFFFF4455 : 0xFF3A2060);
        int qtw = FontRenderUtility.getStringWidth("Quit");
        FontRenderUtility.drawString(g, "Quit", quitX + quitW / 2 - qtw / 2, btnY + 4, quitHovered ? 0xFFFF8888 : 0xFFCCCCCC, false);

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

        g.fill(tx - 1, ty - 1, tx + tw + 1, ty + th + 1, 0xCC1A0E30);
        g.fill(tx, ty, tx + tw, ty + th, 0xEE0E0920);
        Render2DEngine.drawBorder(g, tx, ty, tw, th, 1, ColorUtility.withAlpha(ColorUtility.getActiveColor(), 160));

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
        int quitW = 60;
        int quitX = panelX + (panelW / 2) + 6;
        if (mx >= quitX && mx <= quitX + quitW && my >= btnY && my <= btnY + btnH && btn == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            onClose();
            return true;
        }

        
        int cleanW = 100;
        int cleanX = panelX + (panelW / 2) - cleanW - 6;
        if (mx >= cleanX && mx <= cleanX + cleanW && my >= btnY && my <= btnY + btnH && btn == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            cleanClickTime = System.currentTimeMillis();
            InventoryCleaner.cleanInventory(Minecraft.getInstance());
            return true;
        }

        
        int searchY = contentY + colHeaderH + 2;
        int searchH = 14;
        if (mx >= leftX + 4 && mx <= leftX + leftW - 4 && my >= searchY && my <= searchY + searchH) {
            searchFocus = true;
            return true;
        } else if (btn == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            searchFocus = false;
        }

        
        int gridY = searchY + searchH + 3;
        int gridH = contentH - colHeaderH - searchH - 8;
        int pageY = gridY + gridH + 2;
        if (my >= pageY && my <= pageY + 12) {
            if (mx >= leftX + 4 && mx <= leftX + 26 && currentPage > 0) {
                currentPage--;
                return true;
            }
            int maxPage = Math.max(0, (filteredItems.size() - 1) / PAGE_SIZE);
            if (mx >= leftX + leftW - 26 && mx <= leftX + leftW - 4 && currentPage < maxPage) {
                currentPage++;
                return true;
            }
        }

        
        int gridX = leftX + 4;
        if (mx >= leftX && mx <= leftX + leftW && my >= gridY && my <= gridY + gridH) {
            int col = (mx - gridX) / ITEM_SIZE;
            int row = (my - gridY) / ITEM_SIZE;
            int idx = currentPage * PAGE_SIZE + row * COLS + col;
            if (col >= 0 && col < COLS && row >= 0 && row < ROWS && idx < filteredItems.size()) {
                Item item = filteredItems.get(idx);
                Identifier rl = BuiltInRegistries.ITEM.getKey(item);
                if (rl != null) InventoryCleanerData.INSTANCE.toggle(rl.toString());
                return true;
            }
        }

        
        int selContentY = contentY + colHeaderH + 3;
        int selContentH = contentH - colHeaderH - 6;
        if (mx >= rightX && mx <= rightX + rightW && my >= selContentY && my <= selContentY + selContentH) {
            var selectedArr = InventoryCleanerData.INSTANCE.getSelectedItems().toArray(new String[0]);
            int sy = selContentY + 3 - (int)selectedScroll;
            for (String itemId : selectedArr) {
                if (my >= sy && my <= sy + 16) {
                    InventoryCleanerData.INSTANCE.deselect(itemId);
                    return true;
                }
                sy += 18;
            }
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
        int rightX   = panelX + leftW + 12;
        int colHeaderH = 18;
        int selContentY = contentY + colHeaderH + 3;
        int selContentH = contentH - colHeaderH - 6;

        
        if (mouseX >= rightX && mouseX <= rightX + rightW && mouseY >= selContentY && mouseY <= selContentY + selContentH) {
            int selCount = InventoryCleanerData.INSTANCE.getSelectedItems().size();
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
            onClose();
            return true;
        }
        if (searchFocus) {
            if (key == GLFW.GLFW_KEY_BACKSPACE && !searchQuery.isEmpty()) {
                searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                currentPage = 0;
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
        if (searchFocus) {
            int codepoint = event.codepoint();
            if (codepoint >= 32 && codepoint < 127) {
                searchQuery += (char)codepoint;
                currentPage = 0;
                rebuildFiltered();
                return true;
            }
        }
        return super.charTyped(event);
    }

    @Override
    public void onClose() {
        
        if (InventoryCleaner.INSTANCE.getEnabled()) {
            InventoryCleaner.INSTANCE.setEnabled(false);
        }
        Minecraft.getInstance().setScreen(parent);
    }
}
