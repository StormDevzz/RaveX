package ravex.gui.blockbrowser;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.lwjgl.glfw.GLFW;
import ravex.gui.clickgui.ColorUtility;
import ravex.modules.world.NukerData;
import ravex.utility.render.FontRenderUtility;
import ravex.utility.render.Render2DEngine;

import java.util.ArrayList;
import java.util.List;

public class NukerBlockBrowserScreen extends Screen {
    private final Screen parent;

    private static final int ITEM_SIZE = 22;
    private static final int COLS = 9;
    private static final int ROWS = 6;
    private static final int PAGE_SIZE = COLS * ROWS;

    private final List<Block> allBlocks = new ArrayList<>();
    private final List<Block> filteredBlocks = new ArrayList<>();

    private String searchQuery = "";
    private boolean searchFocus = false;
    private int currentPage = 0;
    private long openTime = -1;
    private String tooltip = null;

    public NukerBlockBrowserScreen(Screen parent) {
        super(Component.literal("Nuker Block Selector"));
        this.parent = parent;

        for (Block block : BuiltInRegistries.BLOCK) {
            if (block == Blocks.AIR) continue;
            allBlocks.add(block);
        }
        rebuildFiltered();
    }

    private void rebuildFiltered() {
        filteredBlocks.clear();
        String q = searchQuery.toLowerCase().trim();
        for (Block block : allBlocks) {
            if (q.isEmpty()) {
                filteredBlocks.add(block);
            } else {
                String name = block.getName().getString().toLowerCase();
                Identifier id = BuiltInRegistries.BLOCK.getKey(block);
                if (name.contains(q) || id.toString().contains(q)) {
                    filteredBlocks.add(block);
                }
            }
        }
        int maxPage = Math.max(0, (filteredBlocks.size() - 1) / PAGE_SIZE);
        if (currentPage > maxPage) currentPage = maxPage;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float pt) {
        long now = System.currentTimeMillis();
        if (openTime < 0) openTime = now;

        float elapsed = now - openTime;
        float progress = Math.min(1.0f, elapsed / 200f);
        float scale = progress * (2f - progress);

        int W = width;
        int H = height;

        int bgA = (int)(progress * 0x99);
        g.fill(0, 0, W, H, (bgA << 24) | 0x05050E);

        int panelW = Math.min(640, W - 40);
        int panelH = Math.min(440, H - 40);
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
        tooltip = null;

        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xF0101020);

        int headerH = 28;
        g.fill(panelX, panelY, panelX + panelW, panelY + headerH, 0xFF160E30);
        g.fill(panelX, panelY + headerH - 1, panelX + panelW, panelY + headerH, ColorUtility.getActiveColor());
        FontRenderUtility.drawString(g, "⚡ Nuker Block Selector", panelX + 10, panelY + 6, ColorUtility.getActiveColor(), true);
        FontRenderUtility.drawString(g, "ESC / Back", panelX + panelW - 68, panelY + 8, 0xFF606080, false);

        int contentY = panelY + headerH + 6;
        int contentH = panelH - headerH - 40;
        int leftX = panelX + 6;
        int leftW = panelW - 12;

        g.fill(leftX, contentY, leftX + leftW, contentY + contentH, 0xFF0C0920);
        Render2DEngine.drawBorder(g, leftX, contentY, leftW, contentH, 1, 0xFF281844);

        int colH = 16;
        g.fill(leftX, contentY, leftX + leftW, contentY + colH, 0xFF180E38);
        int selCount = NukerData.INSTANCE.getSelectedBlocks().size();
        FontRenderUtility.drawString(g, "§7Blocks §8(" + filteredBlocks.size() + ")  §dSelected: §f" + selCount,
            leftX + 5, contentY + 3, 0xFFAAAAAA, false);

        int searchY = contentY + colH + 2;
        int searchH = 14;
        int searchW = leftW - 8;
        boolean searchHov = mx >= leftX + 4 && mx <= leftX + 4 + searchW && my >= searchY && my <= searchY + searchH;
        g.fill(leftX + 4, searchY, leftX + 4 + searchW, searchY + searchH, searchFocus ? 0xFF1A1040 : 0xFF120B30);
        Render2DEngine.drawBorder(g, leftX + 4, searchY, searchW, searchH, 1,
            searchFocus ? ColorUtility.getActiveColor() : 0xFF2A1850);
        String searchDisplay = searchQuery.isEmpty() && !searchFocus ? "§8Search..." :
            searchQuery + (searchFocus ? "§8|" : "");
        FontRenderUtility.drawString(g, searchDisplay, leftX + 7, searchY + 2, 0xFFCCCCCC, false);

        int gridY = searchY + searchH + 3;
        int gridH = contentH - colH - searchH - 8;
        int gridX = leftX + 4;

        int startIdx = currentPage * PAGE_SIZE;
        int endIdx = Math.min(startIdx + PAGE_SIZE, filteredBlocks.size());

        g.enableScissor(leftX, gridY, leftX + leftW, gridY + gridH);

        for (int i = startIdx; i < endIdx; i++) {
            int localIdx = i - startIdx;
            int col = localIdx % COLS;
            int row = localIdx / COLS;
            int ix = gridX + col * ITEM_SIZE;
            int iy = gridY + row * ITEM_SIZE;

            Block block = filteredBlocks.get(i);
            Identifier id = BuiltInRegistries.BLOCK.getKey(block);
            boolean selected = NukerData.INSTANCE.isSelected(id);
            boolean hover = mx >= ix && mx <= ix + ITEM_SIZE && my >= iy && my <= iy + ITEM_SIZE;

            if (hover) {
                g.fill(ix, iy, ix + ITEM_SIZE, iy + ITEM_SIZE, 0xFF2A2040);
                tooltip = block.getName().getString() + "\n§8" + id;
            } else if (selected) {
                g.fill(ix, iy, ix + ITEM_SIZE, iy + ITEM_SIZE, 0x44FF4444);
            }

            g.renderItem(new ItemStack(block.asItem()), ix + 3, iy + 3);

            if (selected) {
                FontRenderUtility.drawString(g, "§a✔", ix + ITEM_SIZE - 8, iy + 1, 0xFF44FF88, false);
            }
        }

        g.disableScissor();

        int pageY = gridY + gridH + 2;
        int maxPage = Math.max(0, (filteredBlocks.size() - 1) / PAGE_SIZE);
        String pageStr = (currentPage + 1) + " / " + (maxPage + 1);

        g.fill(leftX + 4, pageY, leftX + 26, pageY + 12, 0xFF180E38);
        FontRenderUtility.drawString(g, "◀", leftX + 10, pageY + 1, currentPage > 0 ? 0xFFCCCCCC : 0xFF444466, false);

        int ptw = FontRenderUtility.getStringWidth(pageStr);
        FontRenderUtility.drawString(g, pageStr, leftX + leftW / 2 - ptw / 2, pageY + 1, 0xFF9090B0, false);

        g.fill(leftX + leftW - 26, pageY, leftX + leftW - 4, pageY + 12, 0xFF180E38);
        FontRenderUtility.drawString(g, "▶", leftX + leftW - 22, pageY + 1, currentPage < maxPage ? 0xFFCCCCCC : 0xFF444466, false);

        int btnY = panelY + panelH - 28;
        int btnH = 16;
        int clearW = 80;
        int clearX = panelX + panelW / 2 - clearW / 2;
        boolean clearHov = mx >= clearX && mx <= clearX + clearW && my >= btnY && my <= btnY + btnH;
        g.fill(clearX, btnY, clearX + clearW, btnY + btnH, clearHov ? 0xFF3A1010 : 0xFF1A0E38);
        Render2DEngine.drawBorder(g, clearX, btnY, clearW, btnH, 1, clearHov ? 0xFFFF4455 : 0xFF3A2060);
        int ctw = FontRenderUtility.getStringWidth("Clear All");
        FontRenderUtility.drawString(g, "Clear All", clearX + clearW / 2 - ctw / 2, btnY + 3, 0xFFFF8888, false);

        pose.popMatrix();

        if (tooltip != null && scale > 0.8f) {
            renderTip(g, tooltip, mouseX, mouseY);
        }

        super.render(g, mouseX, mouseY, pt);
    }

    private void renderTip(GuiGraphics g, String text, int mx, int my) {
        String[] lines = text.split("\n");
        int maxW = 0;
        for (String l : lines) {
            int lw = FontRenderUtility.getStringWidth(l);
            if (lw > maxW) maxW = lw;
        }
        int tw = maxW + 10;
        int th = lines.length * 10 + 6;
        int tx = mx + 8;
        int ty = my - th - 4;
        if (tx + tw > width) tx = width - tw - 4;
        if (ty < 2) ty = my + 12;
        g.fill(tx - 1, ty - 1, tx + tw + 1, ty + th + 1, 0xCC1A0E30);
        g.fill(tx, ty, tx + tw, ty + th, 0xEE0E0920);
        Render2DEngine.drawBorder(g, tx, ty, tw, th, 1, ColorUtility.withAlpha(ColorUtility.getActiveColor(), 160));
        int ly = ty + 3;
        for (String l : lines) {
            FontRenderUtility.drawString(g, l, tx + 5, ly, 0xFFEEEEEE, false);
            ly += 10;
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean handled) {
        int mx = (int) event.x();
        int my = (int) event.y();
        int btn = event.button();

        int panelW = Math.min(640, width - 40);
        int panelH = Math.min(440, height - 40);
        int panelX = (width - panelW) / 2;
        int panelY = (height - panelH) / 2;
        int headerH = 28;
        int contentY = panelY + headerH + 6;
        int contentH = panelH - headerH - 40;
        int leftW = panelW - 12;
        int leftX = panelX + 6;
        int colH = 16;

        int btnY = panelY + panelH - 28;
        int btnH = 16;
        int clearW = 80;
        int clearX = panelX + panelW / 2 - clearW / 2;
        if (mx >= clearX && mx <= clearX + clearW && my >= btnY && my <= btnY + btnH && btn == 0) {
            NukerData.INSTANCE.clear();
            return true;
        }

        if (btn == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            int searchY = contentY + colH + 2;
            int searchH = 14;
            int searchW = leftW - 8;
            if (mx >= leftX + 4 && mx <= leftX + 4 + searchW && my >= searchY && my <= searchY + searchH) {
                searchFocus = true;
                return true;
            }
            searchFocus = false;
        }

        int searchY = contentY + colH + 2;
        int searchH = 14;
        int gridY = searchY + searchH + 3;
        int gridH = contentH - colH - searchH - 8;
        int gridX = leftX + 4;

        int pageY = gridY + gridH + 2;
        if (my >= pageY && my <= pageY + 12 && btn == 0) {
            int maxPage = Math.max(0, (filteredBlocks.size() - 1) / PAGE_SIZE);
            if (mx >= leftX + 4 && mx <= leftX + 26 && currentPage > 0) {
                currentPage--;
                return true;
            }
            if (mx >= leftX + leftW - 26 && mx <= leftX + leftW - 4 && currentPage < maxPage) {
                currentPage++;
                return true;
            }
        }

        if (mx >= leftX && mx <= leftX + leftW && my >= gridY && my <= gridY + gridH && btn == 0) {
            int col = (mx - gridX) / ITEM_SIZE;
            int row = (my - gridY) / ITEM_SIZE;
            int idx = currentPage * PAGE_SIZE + row * COLS + col;
            if (col >= 0 && col < COLS && row >= 0 && row < ROWS && idx < filteredBlocks.size()) {
                Block block = filteredBlocks.get(idx);
                Identifier id = BuiltInRegistries.BLOCK.getKey(block);
                NukerData.INSTANCE.toggle(id);
                return true;
            }
        }

        if (btn == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            searchFocus = false;
        }

        return super.mouseClicked(event, handled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double hAmt, double vAmt) {
        if (vAmt < 0) {
            int maxPage = Math.max(0, (filteredBlocks.size() - 1) / PAGE_SIZE);
            if (currentPage < maxPage) currentPage++;
        } else if (vAmt > 0) {
            if (currentPage > 0) currentPage--;
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            Minecraft.getInstance().setScreen(parent);
            return true;
        }
        if (searchFocus) {
            if (key == GLFW.GLFW_KEY_BACKSPACE && !searchQuery.isEmpty()) {
                searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                currentPage = 0;
                rebuildFiltered();
                return true;
            }
            if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_ENTER) {
                searchFocus = false;
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (searchFocus) {
            int cp = event.codepoint();
            if (cp >= 32 && cp < 127) {
                searchQuery += (char) cp;
                currentPage = 0;
                rebuildFiltered();
                return true;
            }
        }
        return super.charTyped(event);
    }
}
