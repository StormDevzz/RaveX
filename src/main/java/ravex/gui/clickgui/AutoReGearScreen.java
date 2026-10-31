package ravex.gui.clickgui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;
import ravex.gui.browser.SearchBrowserScreen;
import ravex.modules.player.autoregear.AutoReGearData;
import ravex.utility.misc.LanguageUtility;
import ravex.utility.render.ColorUtility;
import ravex.utility.render.FontRenderUtility;
import ravex.utility.render.Render2DUtility;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// kit editor for AutoReGear - the SearchBrowser layout, adapted to items.
public class AutoReGearScreen extends Screen {
    private final Screen parent;

    private static class Entry {
        final Identifier id;
        final String name;
        final ItemStack stack;
        Entry(Item item) {
            this.id = BuiltInRegistries.ITEM.getKey(item);
            this.stack = new ItemStack(item);
            this.name = stack.getHoverName().getString();
        }
    }

    private enum Tab { ITEMS, SELECTED }

    private final List<Entry> allItems = new ArrayList<>();
    private List<Entry> filtered = new ArrayList<>();
    private List<Entry> previousFiltered = new ArrayList<>();
    private Tab tab = Tab.ITEMS;

    private String searchQuery = "";
    private boolean searchFocused;
    private float scrollOffset;
    private float scrollAnim;
    private float previousScrollAnim;
    private float tabTransition = 1f;
    private float swipeDirection = 1f;
    private long openTime = -1;
    private boolean closing = false;
    private long closeStart = 0;
    private ItemStack hoveredStack = ItemStack.EMPTY;
    private String hoveredName = "";
    private String hoveredId = "";
    private final Set<Identifier> selectedIds = new HashSet<>();

    private String editingKey = null;
    private String editingText = "";

    public AutoReGearScreen(Screen parent) {
        super(Component.literal(LanguageUtility.t("arg_subtitle")));
        this.parent = parent;
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) continue;
            allItems.add(new Entry(item));
        }
        refilter();
        SearchBrowserScreen.restoreAtlasFilter();
    }

    @Override public boolean isPauseScreen() { return false; }

    private void saveEditing() {
        if (editingKey == null) return;
        try {
            int count = editingText.isEmpty() ? 1 : Integer.parseInt(editingText);
            count = Math.max(1, Math.min(256, count));
            AutoReGearData.INSTANCE.setTargetCount(editingKey, count);
        } catch (NumberFormatException ignored) {
        }
        editingKey = null;
        editingText = "";
    }

    private void close() {
        if (closing) return;
        saveEditing();
        closing = true;
        closeStart = System.currentTimeMillis();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        long now = System.currentTimeMillis();
        if (openTime < 0) openTime = now;

        float elapsed = now - openTime;
        float openAlpha = Math.min(1f, elapsed / 150f);
        float openScale = 0.85f + 0.15f * openAlpha;

        float closeAlpha = 1f;
        float closeScale = 1f;
        if (closing) {
            float cElapsed = (now - closeStart) / 120f;
            closeAlpha = Math.max(0f, 1f - cElapsed);
            closeScale = 1f - 0.15f * cElapsed;
            if (cElapsed >= 1f) {
                minecraft.setScreen(parent);
                return;
            }
        }

        float alpha = openAlpha * closeAlpha;
        float scale = openScale * closeScale;

        int w = width, h = height;
        g.fill(0, 0, w, h, (int) (0x60 * alpha) << 24);

        int pw = Math.min(480, w - 40);
        int ph = Math.min(360, h - 40);
        int px = (w - pw) / 2;
        int py = (h - ph) / 2;

        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(w / 2f, h / 2f);
        pose.scale(scale, scale);
        pose.translate(-w / 2f, -h / 2f);

        int amx = (int) ((mx - w / 2f) / scale + w / 2f);
        int amy = (int) ((my - h / 2f) / scale + h / 2f);

        int ac = ColorUtility.getActiveColor();
        int alpha255 = (int) (255 * alpha);
        int r = 8;

        Render2DUtility.drawRound(g, px, py, pw, ph, r, 0xF00A0A0E);
        Render2DUtility.drawRoundBorder(g, px, py, pw, ph, r, 1, ColorUtility.withAlpha(ac, alpha255));

        g.fill(px, py + 28, px + pw, py + 29, 0x22FFFFFF);

        FontRenderUtility.drawString(g, LanguageUtility.t("arg_title"), px + 12, py + 8,
            ColorUtility.withAlpha(0xFFE0E0E0, alpha255), true);

        int selCount = AutoReGearData.INSTANCE.getSelectedItems().size();
        FontRenderUtility.drawString(g, selCount + "/" + allItems.size(), px + pw - 80, py + 10,
            ColorUtility.withAlpha(0xFF606060, alpha255), true);

        int cy = py + 34;
        int ch = ph - 64;

        int tabX = px + 10, tabY = cy, tabH = 16;
        int tabW = 60;
        String[] labels = { LanguageUtility.t("arg_all_items"), LanguageUtility.t("arg_kit_items") };
        for (int ti = 0; ti < 2; ti++) {
            int tix = tabX + ti * (tabW + 4);
            boolean ta = tab == (ti == 0 ? Tab.ITEMS : Tab.SELECTED);
            boolean th = !ta && amx >= tix && amx <= tix + tabW && amy >= tabY && amy <= tabY + tabH;
            Render2DUtility.drawRound(g, tix, tabY, tabW, tabH, 4, ta ? 0x20FFFFFF : (th ? 0x15151515 : 0x05000000));
            if (ta) {
                Render2DUtility.drawRoundBorder(g, tix, tabY, tabW, tabH, 4, 1, ColorUtility.withAlpha(ac, (int) (220 * alpha)));
            }
            String tabLabel = labels[ti];
            int tabLabelW = FontRenderUtility.getStringWidth(tabLabel);
            FontRenderUtility.drawString(g, tabLabel, tix + (tabW - tabLabelW) / 2, tabY + 3,
                ColorUtility.withAlpha(ta ? 0xFFFFFFFF : (th ? 0xFFB0B0B0 : 0xFF707070), alpha255), true);
        }

        int sx = px + pw - 170, sy2 = cy, sw = 160, sh = 16;
        boolean sHov = amx >= sx && amx <= sx + sw && amy >= sy2 && amy <= sy2 + sh;
        Render2DUtility.drawRound(g, sx, sy2, sw, sh, 4, searchFocused ? 0xFF141418 : (sHov ? 0xFF121216 : 0xFF0C0C10));
        Render2DUtility.drawRoundBorder(g, sx, sy2, sw, sh, 4, 1,
            searchFocused ? ColorUtility.withAlpha(ac, (int) (255 * alpha)) : ColorUtility.withAlpha(0xFF222222, alpha255));
        String st = searchQuery.isEmpty() && !searchFocused ? "\u00A78Search..." : searchQuery + (searchFocused ? "\u00A78|" : "");
        FontRenderUtility.drawString(g, st, sx + 6, sy2 + 3, ColorUtility.withAlpha(0xFF909090, alpha255), true);

        int gx = px + 10, gy = cy + 24, gw = pw - 20, gh = ch - 28;
        int cols = Math.max(4, (gw + 2) / 28);
        int cell = 28;
        int rows = (filtered.size() + cols - 1) / cols;
        int maxScroll = Math.max(0, rows - (gh / cell));

        scrollAnim += (scrollOffset - scrollAnim) * 0.18f;
        if (Math.abs(scrollAnim - scrollOffset) < 0.01f) scrollAnim = scrollOffset;

        if (tabTransition < 1f) {
            tabTransition += (1f - tabTransition) * 0.20f;
            if (Math.abs(tabTransition - 1f) < 0.001f) tabTransition = 1f;
        }

        Render2DUtility.drawRound(g, gx, gy, gw, gh, 6, 0xFF06060A);
        Render2DUtility.pushScissor(g, gx, gy, gw, gh);

        hoveredStack = ItemStack.EMPTY;
        hoveredName = "";
        hoveredId = "";
        int sr = (int) scrollAnim;
        int er = Math.min(rows, sr + (gh / cell) + 1);

        int selColor = ColorUtility.withAlpha(ac, 80);
        int selHovColor = ColorUtility.withAlpha(ac, 110);
        int hovColor = ColorUtility.withAlpha(0x22FFFFFF, 100);
        boolean transitioning = tabTransition < 1f;

        if (transitioning && !previousFiltered.isEmpty()) {
            float oldOffX = -swipeDirection * tabTransition * gw;
            int oldCols = Math.max(4, (gw + 2) / 28);
            int oldRows = (previousFiltered.size() + oldCols - 1) / oldCols;
            int oldSr = (int) previousScrollAnim;
            int oldEr = Math.min(oldRows, oldSr + (gh / cell) + 1);
            var oldPose = g.pose();
            oldPose.pushMatrix();
            oldPose.translate(oldOffX, 0);
            for (int r2 = oldSr; r2 < oldEr; r2++) {
                for (int c = 0; c < oldCols; c++) {
                    int idx = r2 * oldCols + c;
                    if (idx >= previousFiltered.size()) break;
                    Entry e = previousFiltered.get(idx);
                    int tx = gx + c * cell;
                    int ty = gy + (r2 - oldSr) * cell;
                    int cellBg2 = selectedIds.contains(e.id) ? ColorUtility.withAlpha(ac, (int) (30 * alpha)) : 0xFF06060A;
                    Render2DUtility.drawRound(g, tx, ty, cell, cell, 6, cellBg2);
                    renderIcon(g, e, tx + 6, ty + 6);
                }
            }
            oldPose.popMatrix();
        }

        float newOffX = swipeDirection * (1f - tabTransition) * gw;
        if (transitioning) {
            var newPose = g.pose();
            newPose.pushMatrix();
            newPose.translate(newOffX, 0);
            renderGrid(g, sr, er, cols, gx, gy, cell, amx, amy, false, selColor, selHovColor, hovColor, alpha);
            newPose.popMatrix();
        } else {
            renderGrid(g, sr, er, cols, gx, gy, cell, amx, amy, true, selColor, selHovColor, hovColor, alpha);
        }
        Render2DUtility.popScissor(g);

        if (maxScroll > 0) {
            float ratio = (float) cell * rows / gh;
            int th = Math.max(12, (int) (gh / ratio));
            float sr2 = scrollOffset / Math.max(1, maxScroll);
            int ty2 = gy + (int) ((gh - th) * sr2);
            Render2DUtility.drawRound(g, px + pw - 6, ty2, 3, th, 2, ColorUtility.withAlpha(0x44FFFFFF, alpha255));
        }

        int cw = 60, ch2 = 14;
        int cxx = px + 10, cyy = py + ph - 20;
        boolean chov = amx >= cxx && amx <= cxx + cw && amy >= cyy && amy <= cyy + ch2;
        boolean hasSelection = selCount > 0;
        Render2DUtility.drawRound(g, cxx, cyy, cw, ch2, 4, hasSelection ? (chov ? 0xFF3A1010 : 0xFF1A0E0E) : 0xFF101014);
        Render2DUtility.drawRoundBorder(g, cxx, cyy, cw, ch2, 4, 1,
            hasSelection ? (chov ? 0xFFFF4455 : 0xFF3A2020) : 0xFF202028);
        FontRenderUtility.drawString(g, "Clear", cxx + 14, cyy + 2, hasSelection ? 0xFFFF8888 : 0xFF505058, true);

        int bw = 50, bh = 14;
        int bx = px + pw - bw - 10, by2 = py + ph - 20;
        boolean bh2 = amx >= bx && amx <= bx + bw && amy >= by2 && amy <= by2 + bh;
        Render2DUtility.drawRound(g, bx, by2, bw, bh, 4, bh2 ? 0xFF1A1A1E : 0xFF0E0E12);
        Render2DUtility.drawRoundBorder(g, bx, by2, bw, bh, 4, 1, ColorUtility.withAlpha(0xFF303030, alpha255));
        FontRenderUtility.drawString(g, "Back", bx + 12, by2 + 2, ColorUtility.withAlpha(0xFFD0D0D0, alpha255), true);

        pose.popMatrix();

        if (!hoveredName.isEmpty() && alpha > 0.8f) {
            List<String> lines = new ArrayList<>();
            lines.add(hoveredName);
            lines.add("\u00A77" + hoveredId);
            if (selectedIds.contains(Identifier.tryParse(hoveredId)) && tab == Tab.SELECTED) {
                lines.add("\u00A7eLMB: +count  \u00A7cRMB: -count  \u00A7bMMB: type");
            }
            int mw = 0;
            for (String l : lines) mw = Math.max(mw, FontRenderUtility.getStringWidth(l));
            int tx2 = mx + 10, ty3 = my - 6;
            if (tx2 + mw > width) tx2 = mx - mw - 10;
            if (ty3 < 2) ty3 = my + 10;
            for (String l : lines) {
                FontRenderUtility.drawString(g, l, tx2 + 6, ty3, 0xFFCCCCCC, false);
                ty3 += 10;
            }
        }

        super.render(g, mx, my, pt);
    }

    private void renderIcon(GuiGraphics g, Entry e, int x, int y) {
        g.renderItem(e.stack, x, y);
    }

    private void renderGrid(GuiGraphics g, int sr, int er, int cols, int gx, int gy, int cell,
                            int amx, int amy, boolean hoverable, int selColor, int selHovColor, int hovColor,
                            float alpha) {
        int ac = ColorUtility.getActiveColor();
        int a255 = (int) (255 * alpha);
        for (int r2 = sr; r2 < er; r2++) {
            for (int c = 0; c < cols; c++) {
                int idx = r2 * cols + c;
                if (idx >= filtered.size()) break;
                Entry e = filtered.get(idx);
                int tx = gx + c * cell;
                int ty = gy + (r2 - sr) * cell;
                boolean hov = hoverable && amx >= tx && amx <= tx + cell && amy >= ty && amy <= ty + cell;

                if (hov) {
                    hoveredStack = e.stack;
                    hoveredName = e.name;
                    hoveredId = e.id.toString();
                }

                boolean sel = selectedIds.contains(e.id);
                int cellBg;
                if (sel && hov) {
                    cellBg = ColorUtility.withAlpha(ac, (int) (60 * alpha));
                } else if (sel) {
                    cellBg = ColorUtility.withAlpha(ac, (int) (30 * alpha));
                } else if (hov) {
                    cellBg = ColorUtility.withAlpha(0xFF1A1A1E, a255);
                } else {
                    cellBg = ColorUtility.withAlpha(0xFF06060A, a255);
                }
                Render2DUtility.drawRound(g, tx, ty, cell, cell, 6, cellBg);
                renderIcon(g, e, tx + 6, ty + 6);

                if (sel) {
                    String badge;
                    if (editingKey != null && editingKey.equals(e.id.toString())) {
                        badge = editingText + (System.currentTimeMillis() % 1000 < 500 ? "|" : "");
                    } else {
                        badge = String.valueOf(AutoReGearData.INSTANCE.getTargetCount(e.id.toString()));
                    }
                    int bwid = FontRenderUtility.getStringWidth(badge);
                    int bxx = tx + cell - bwid - 2;
                    int byy = ty + cell - 9;
                    g.fill(bxx - 1, byy - 1, tx + cell - 1, ty + cell - 1, 0x99000000);
                    FontRenderUtility.drawString(g, badge, bxx, byy, 0xFFFFE680, false);
                    Render2DUtility.drawRoundBorder(g, tx, ty, cell, cell, 6, 1, ColorUtility.withAlpha(ac, 200));
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean handled) {
        int w = width, h = height;
        int pw = Math.min(480, w - 40), ph = Math.min(360, h - 40);
        int px = (w - pw) / 2, py = (h - ph) / 2;

        float scale = 0.85f + 0.15f * Math.min(1f, (System.currentTimeMillis() - openTime) / 150f);
        if (closing) scale *= (1f - 0.15f * Math.min(1f, (System.currentTimeMillis() - closeStart) / 120f));
        double amx = (event.x() - w / 2f) / scale + w / 2f;
        double amy = (event.y() - h / 2f) / scale + h / 2f;

        int sx = px + pw - 170, sy2 = py + 34, sw = 160, sh = 16;
        int tabW = 60, tabH = 16;
        int tabX = px + 10, tabY = py + 34;
        int cxx = px + 10, cyy = py + ph - 20, cw = 60, ch2 = 14;
        int bx = px + pw - 60, by2 = py + ph - 20, bw = 50, bh = 14;
        int gx = px + 10, gy = py + 58, cols = Math.max(4, (pw - 22) / 28), cell = 28;

        if (amx >= sx && amx <= sx + sw && amy >= sy2 && amy <= sy2 + sh) {
            searchFocused = true;
            return true;
        }
        searchFocused = false;
        if (editingKey != null) saveEditing();

        for (int ti = 0; ti < 2; ti++) {
            int tix = tabX + ti * (tabW + 4);
            if (amx >= tix && amx <= tix + tabW && amy >= tabY && amy <= tabY + tabH) {
                Tab target = ti == 0 ? Tab.ITEMS : Tab.SELECTED;
                if (tab != target) {
                    previousFiltered = new ArrayList<>(filtered);
                    previousScrollAnim = scrollAnim;
                    swipeDirection = ti > tab.ordinal() ? 1f : -1f;
                    tab = target;
                    refilter();
                    tabTransition = 0f;
                }
                return true;
            }
        }

        if (AutoReGearData.INSTANCE.getSelectedItems().size() > 0
                && amx >= cxx && amx <= cxx + cw && amy >= cyy && amy <= cyy + ch2) {
            AutoReGearData.INSTANCE.clear();
            refilter();
            return true;
        }

        if (amx >= bx && amx <= bx + bw && amy >= by2 && amy <= by2 + bh) {
            close();
            return true;
        }

        int col = (int) ((amx - gx) / cell);
        int row = (int) ((amy - gy) / cell) + (int) scrollAnim;
        if (col >= 0 && col < cols && row >= 0) {
            int idx = row * cols + col;
            if (idx < filtered.size() && amx >= gx + col * cell && amx <= gx + col * cell + cell
                && amy >= gy + row * cell - (int) scrollAnim && amy <= gy + row * cell - (int) scrollAnim + cell) {
                Entry e = filtered.get(idx);
                String key = e.id.toString();
                if (tab == Tab.ITEMS) {
                    AutoReGearData.INSTANCE.toggle(key, e.stack.getItem().getDefaultMaxStackSize());
                } else {
                    int count = AutoReGearData.INSTANCE.getTargetCount(key);
                    int maxStack = Math.max(1, e.stack.getItem().getDefaultMaxStackSize());
                    int step = maxStack == 1 ? 1 : maxStack == 16 ? 4 : 8;
                    if (event.button() == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
                        editingKey = key;
                        editingText = String.valueOf(count);
                    } else if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                        AutoReGearData.INSTANCE.setTargetCount(key, Math.min(256, count + step));
                    } else if (event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                        AutoReGearData.INSTANCE.setTargetCount(key, Math.max(1, count - step));
                    }
                }
                refilter();
                return true;
            }
        }

        return super.mouseClicked(event, handled);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmt, double vAmt) {
        int pw = Math.min(480, width - 40);
        int ph = Math.min(360, height - 40);
        int gw = pw - 20, cols = Math.max(4, (gw + 2) / 28);
        int rows = (filtered.size() + cols - 1) / cols;
        float gh = (ph - 64) - 28;
        int maxScroll = Math.max(0, rows - (int) (gh / 28));
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (float) vAmt * 1.5f));
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (editingKey != null) {
            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                if (!editingText.isEmpty()) editingText = editingText.substring(0, editingText.length() - 1);
                return true;
            }
            if (key == GLFW.GLFW_KEY_ENTER) {
                saveEditing();
                return true;
            }
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                editingKey = null;
                editingText = "";
                return true;
            }
        }
        if (key == GLFW.GLFW_KEY_ESCAPE) { close(); return true; }
        if (searchFocused) {
            if (key == GLFW.GLFW_KEY_BACKSPACE && !searchQuery.isEmpty()) {
                searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                scrollOffset = 0;
                refilter();
                return true;
            }
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_ESCAPE) {
                searchFocused = false;
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        int codepoint = event.codepoint();
        if (editingKey != null) {
            if (Character.isDigit((char) codepoint) && editingText.length() < 3) {
                editingText += (char) codepoint;
            }
            return true;
        }
        if (searchFocused && codepoint >= 32 && codepoint < 127) {
            searchQuery += (char) codepoint;
            scrollOffset = 0;
            refilter();
            return true;
        }
        return super.charTyped(event);
    }

    private void refilter() {
        selectedIds.clear();
        for (String id : AutoReGearData.INSTANCE.getSelectedItems().keySet()) {
            Identifier rl = Identifier.tryParse(id);
            if (rl != null) selectedIds.add(rl);
        }

        String q = searchQuery.toLowerCase();
        filtered = new ArrayList<>();
        for (Entry e : allItems) {
            boolean sel = selectedIds.contains(e.id);
            if (tab == Tab.SELECTED && !sel) continue;
            if (!q.isEmpty() && !e.name.toLowerCase().contains(q) && !e.id.toString().contains(q)) continue;
            filtered.add(e);
        }
    }

    @Override
    public void onClose() {
        saveEditing();
        close();
    }
}
