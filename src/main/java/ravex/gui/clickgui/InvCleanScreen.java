package ravex.gui.clickgui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.player.invclean.InvClean;
import ravex.modules.player.invclean.InvCleanData;
import ravex.utility.misc.LanguageUtility;
import ravex.utility.render.ColorUtility;
import ravex.utility.render.FontRenderUtility;
import ravex.utility.render.Render2DUtility;

import java.util.ArrayList;
import java.util.List;

// one page of native size icons, those stay crisp
public class InvCleanScreen extends Screen {
    private final Screen parent;

    // display name and search key cached per item
    private static final class InvEntry {
        final Identifier id;
        final String idStr;
        final ItemStack stack;
        private String name;
        private String nameLower;

        InvEntry(Identifier id, ItemStack stack) {
            this.id = id;
            this.idStr = id.toString();
            this.stack = stack;
        }

        String name() {
            if (name == null) name = stack.getHoverName().getString();
            return name;
        }

        String nameLower() {
            if (nameLower == null) nameLower = name().toLowerCase();
            return nameLower;
        }
    }

    private final List<InvEntry> allItems = new ArrayList<>();
    private List<InvEntry> filtered = new ArrayList<>();

    private enum Tab { ALL, SELECTED }
    private Tab tab = Tab.ALL;
    private int page = 0;

    private static final int MAX_COLS = 24;
    private static final int MIN_COLS = 6;
    private static final int MAX_ROWS = 10;
    private static final int MIN_ROWS = 4;
    private static final int CELL = 28;

    private String searchQuery = "";
    private boolean searchFocused = false;

    private InvEntry hovered = null;

    public InvCleanScreen(Screen parent) {
        super(net.minecraft.network.chat.Component.literal("InvClean"));
        this.parent = parent;
        ravex.gui.browser.SearchBrowserScreen.restoreAtlasFilter();
        // names stay lazy so the screen opens fast
        for (Item item : BuiltInRegistries.ITEM) {
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            if (id == null || id.getPath().equals("air")) continue;
            allItems.add(new InvEntry(id, new ItemStack(item)));
        }
        refilter();
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private void refilter() {
        String q = searchQuery.toLowerCase();
        filtered = new ArrayList<>(allItems.size());
        boolean selectedTab = tab == Tab.SELECTED;
        for (InvEntry e : allItems) {
            if (selectedTab && !InvCleanData.INSTANCE.isSelected(e.idStr)) continue;
            if (!q.isEmpty() && !e.nameLower().contains(q) && !e.idStr.contains(q)) continue;
            filtered.add(e);
        }
        int maxPage = Math.max(0, (filtered.size() - 1) / pageSize());
        if (page > maxPage) page = maxPage;
    }

    // shared layout so render and clicks agree
    private int panelX() { return (this.width - panelW()) / 2; }
    private int panelY() { return (this.height - panelH()) / 2; }
    private int panelW() { return Math.min(640, this.width - 60); }
    private int panelH() { return Math.min(400, this.height - 60); }

    private static final int TAB_W = 80;
    private static final int TAB_H = 16;

    private int cols() {
        int c = (panelW() - 20) / CELL;
        if (c < MIN_COLS) c = MIN_COLS;
        if (c > MAX_COLS) c = MAX_COLS;
        return c;
    }
    private int gridW() { return cols() * CELL; }
    private int rows() {
        int r = (panelH() - 94) / CELL;
        if (r < MIN_ROWS) r = MIN_ROWS;
        if (r > MAX_ROWS) r = MAX_ROWS;
        return r;
    }
    private int pageSize() { return cols() * rows(); }
    private int gridH() { return rows() * CELL; }
    private int gridX() { return panelX() + (panelW() - gridW()) / 2; }
    private int gridY() { return panelY() + 34 + TAB_H + 10; }

    private int maxPage() { return Math.max(0, (filtered.size() - 1) / pageSize()); }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float pt) {
        int px = panelX(), py = panelY(), pw = panelW(), ph = panelH();
        int ac = ColorUtility.getActiveColor();

        g.fill(0, 0, this.width, this.height, 0x8C000000);

        Render2DUtility.drawRound(g, px, py, pw, ph, 8, 0xFF0D0F14);
        Render2DUtility.drawRoundBorder(g, px, py, pw, ph, 8, 1, ColorUtility.withAlpha(ac, 160));

        FontRenderUtility.drawString(g, LanguageUtility.t("ic_title"), px + 12, py + 8, 0xFFE0E0E0, true);

        long selected = InvCleanData.INSTANCE.getSelectedItems().size();
        long total = tab == Tab.ALL ? allItems.size() : selected;
        FontRenderUtility.drawString(g, selected + "/" + total, px + pw - 80, py + 10, 0xFF606060, true);

        int tabX = px + 10, tabY = py + 34;
        for (int ti = 0; ti < 2; ti++) {
            int tix = tabX + ti * (TAB_W + 4);
            boolean ta = tab == (ti == 0 ? Tab.ALL : Tab.SELECTED);
            boolean th = !ta && mouseX >= tix && mouseX <= tix + TAB_W && mouseY >= tabY && mouseY <= tabY + TAB_H;
            Render2DUtility.drawRound(g, tix, tabY, TAB_W, TAB_H, 4,
                ta ? 0x20FFFFFF : (th ? 0x15151515 : 0x05000000));
            if (ta) Render2DUtility.drawRoundBorder(g, tix, tabY, TAB_W, TAB_H, 4, 1, ColorUtility.withAlpha(ac, 220));
            String label = LanguageUtility.t(ti == 0 ? "ic_all_items" : "ic_selected");
            int lw = FontRenderUtility.getStringWidth(label);
            FontRenderUtility.drawString(g, label, tix + (TAB_W - lw) / 2, tabY + 3,
                ta ? 0xFFFFFFFF : (th ? 0xFFB0B0B0 : 0xFF707070), true);
        }

        int sx = tabX + 2 * (TAB_W + 4) + 8;
        int sw = px + pw - 12 - sx;
        Render2DUtility.drawRound(g, sx, tabY, sw, TAB_H, 4, 0x14FFFFFF);
        if (searchFocused) Render2DUtility.drawRoundBorder(g, sx, tabY, sw, TAB_H, 4, 1, ColorUtility.withAlpha(ac, 200));
        if (searchQuery.isEmpty() && !searchFocused) {
            FontRenderUtility.drawString(g, "Search...", sx + 5, tabY + 3, 0xFF606070, false);
        } else {
            String shown = searchQuery + (((System.currentTimeMillis() / 500) % 2 == 0 && searchFocused) ? "|" : "");
            FontRenderUtility.drawString(g, shown, sx + 5, tabY + 3, 0xFFD0D0E0, false);
        }

        int gx = gridX(), gy = gridY(), gw = gridW(), gh = gridH();
        Render2DUtility.drawRound(g, gx - 6, gy - 6, gw + 12, gh + 12, 6, 0xFF06060A);

        hovered = null;
        if (page > maxPage()) page = maxPage();
        int start = page * pageSize();
        for (int i = 0; i < pageSize(); i++) {
            int idx = start + i;
            if (idx >= filtered.size()) break;
            InvEntry e = filtered.get(idx);
            int tx = gx + (i % cols()) * CELL;
            int ty = gy + (i / cols()) * CELL;
            boolean hov = mouseX >= tx && mouseX <= tx + CELL && mouseY >= ty && mouseY <= ty + CELL;
            boolean sel = InvCleanData.INSTANCE.isSelected(e.idStr);
            // cells skip their own fill, the panel behind is dark
            if (sel || hov) {
                int cellBg = sel ? ColorUtility.withAlpha(ac, hov ? 60 : 30) : 0xFF1A1A1E;
                Render2DUtility.drawRound(g, tx + 1, ty + 1, CELL - 2, CELL - 2, 6, cellBg);
                if (sel) Render2DUtility.drawRoundBorder(g, tx + 1, ty + 1, CELL - 2, CELL - 2, 6, 1, ColorUtility.withAlpha(ac, 220));
            }
            g.renderItem(e.stack, tx + 6, ty + 6);
            if (hov) hovered = e;
        }

        int bw = 110, bh = 18, by = py + ph - 28;
        String cleanLabel = LanguageUtility.t("ic_clean_now");
        boolean cleanHov = mouseX >= px + 10 && mouseX <= px + 10 + bw && mouseY >= by && mouseY <= by + bh;
        Render2DUtility.drawRound(g, px + 10, by, bw, bh, 5, cleanHov ? ColorUtility.withAlpha(ac, 90) : 0x18FFFFFF);
        Render2DUtility.drawRoundBorder(g, px + 10, by, bw, bh, 5, 1, ColorUtility.withAlpha(ac, 180));
        int clw = FontRenderUtility.getStringWidth(cleanLabel);
        FontRenderUtility.drawString(g, cleanLabel, px + 10 + (bw - clw) / 2, by + 4, 0xFFFFFFFF, false);

        String quitLabel = LanguageUtility.t("ic_quit");
        int qw = 80, qx = px + pw - 10 - qw;
        boolean quitHov = mouseX >= qx && mouseX <= qx + qw && mouseY >= by && mouseY <= by + bh;
        Render2DUtility.drawRound(g, qx, by, qw, bh, 5, quitHov ? 0x18FFFFFF : 0x10FFFFFF);
        int qlw = FontRenderUtility.getStringWidth(quitLabel);
        FontRenderUtility.drawString(g, quitLabel, qx + (qw - qlw) / 2, by + 4, 0xFFD0D0E0, false);

        int pgw = 64;
        int pgx = px + (pw - pgw) / 2;
        boolean prevHov = mouseX >= pgx - 22 && mouseX <= pgx - 6 && mouseY >= by && mouseY <= by + bh;
        boolean nextHov = mouseX >= pgx + pgw + 6 && mouseX <= pgx + pgw + 22 && mouseY >= by && mouseY <= by + bh;
        Render2DUtility.drawRound(g, pgx - 22, by, 16, bh, 4, prevHov ? 0x22FFFFFF : 0x12FFFFFF);
        Render2DUtility.drawRound(g, pgx + pgw + 6, by, 16, bh, 4, nextHov ? 0x22FFFFFF : 0x12FFFFFF);
        FontRenderUtility.drawString(g, "<", pgx - 16, by + 4, 0xFFC0C0C0, false);
        FontRenderUtility.drawString(g, ">", pgx + pgw + 12, by + 4, 0xFFC0C0C0, false);
        String pageLabel = (page + 1) + "/" + (maxPage() + 1);
        int plw = FontRenderUtility.getStringWidth(pageLabel);
        FontRenderUtility.drawString(g, pageLabel, pgx + (pgw - plw) / 2, by + 4, 0xFF909090, false);

        // name tag follows the cursor, flips at screen edges
        if (hovered != null) {
            String name = hovered.name();
            int nw = FontRenderUtility.getStringWidth(name);
            int boxW = nw + 16, boxH = 17;
            int tx = mouseX + 12, ty = mouseY - boxH - 8;
            if (tx + boxW > this.width - 2) tx = mouseX - boxW - 12;
            if (ty < 2) ty = mouseY + 14;
            if (tx < 2) tx = 2;
            if (ty + boxH > this.height - 2) ty = this.height - boxH - 2;
            Render2DUtility.drawRound(g, tx, ty, boxW, boxH, 4, 0xE6080A10);
            Render2DUtility.drawRoundBorder(g, tx, ty, boxW, boxH, 4, 1, ColorUtility.withAlpha(ac, 120));
            FontRenderUtility.drawString(g, name, tx + (boxW - nw) / 2, ty + 4, 0xFFCCCCCC, false);
        }

        super.render(g, mouseX, mouseY, pt);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean handled) {
        int mx = (int) event.x();
        int my = (int) event.y();
        int px = panelX(), py = panelY(), pw = panelW(), ph = panelH();

        int tabX = px + 10, tabY = py + 34;
        for (int ti = 0; ti < 2; ti++) {
            int tix = tabX + ti * (TAB_W + 4);
            if (mx >= tix && mx <= tix + TAB_W && my >= tabY && my <= tabY + TAB_H) {
                Tab nt = ti == 0 ? Tab.ALL : Tab.SELECTED;
                if (tab != nt) {
                    tab = nt;
                    page = 0;
                    refilter();
                }
                return true;
            }
        }

        int sx = tabX + 2 * (TAB_W + 4) + 8;
        int sw = px + pw - 12 - sx;
        if (mx >= sx && mx <= sx + sw && my >= tabY && my <= tabY + TAB_H) {
            searchFocused = true;
            return true;
        }
        searchFocused = false;

        int gx = gridX(), gy = gridY(), gw = gridW(), gh = gridH();
        if (mx >= gx && mx <= gx + gw && my >= gy && my <= gy + gh) {
            int col = (mx - gx) / CELL;
            int row = (my - gy) / CELL;
            int idx = page * pageSize() + row * cols() + col;
            if (row >= 0 && row < rows() && col >= 0 && col < cols() && idx >= 0 && idx < filtered.size()) {
                InvEntry e = filtered.get(idx);
                InvCleanData.INSTANCE.toggle(e.idStr);
                if (tab == Tab.SELECTED) {
                    page = 0;
                    refilter();
                }
            }
            return true;
        }

        int bh = 18, by = py + ph - 28;
        int pgw = 64;
        int pgx = px + (pw - pgw) / 2;
        if (mx >= pgx - 22 && mx <= pgx - 6 && my >= by && my <= by + bh) {
            if (page > 0) page--;
            return true;
        }
        if (mx >= pgx + pgw + 6 && mx <= pgx + pgw + 22 && my >= by && my <= by + bh) {
            if (page < maxPage()) page++;
            return true;
        }

        int bw = 110;
        if (mx >= px + 10 && mx <= px + 10 + bw && my >= by && my <= by + bh) {
            var mc = MinecraftWrapper.getWrapper();
            if (mc.getPlayer() != null) InvClean.cleanInventory(mc);
            return true;
        }
        int qw = 80, qx = px + pw - 10 - qw;
        if (mx >= qx && mx <= qx + qw && my >= by && my <= by + bh) {
            if (this.minecraft != null) this.minecraft.setScreen(parent);
            return true;
        }

        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY > 0) {
            if (page > 0) page--;
        } else if (scrollY < 0) {
            if (page < maxPage()) page++;
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            if (this.minecraft != null) this.minecraft.setScreen(parent);
            return true;
        }
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE && !searchQuery.isEmpty()) {
            searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
            page = 0;
            refilter();
            return true;
        }
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER) {
            searchFocused = false;
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(net.minecraft.client.input.CharacterEvent event) {
        String text = event.codepointAsString();
        if (text.isEmpty()) return true;
        char c = text.charAt(0);
        if (c < 32 || c > 126) return true;
        if (searchQuery.length() < 32) {
            searchQuery += text;
            page = 0;
            refilter();
        }
        return true;
    }

    @Override
    public void onClose() {
        ravex.gui.browser.SearchBrowserScreen.restoreAtlasFilter();
        if (this.minecraft != null) this.minecraft.setScreen(parent);
    }

    @Override
    public void removed() {
        ravex.gui.browser.SearchBrowserScreen.restoreAtlasFilter();
    }
}
