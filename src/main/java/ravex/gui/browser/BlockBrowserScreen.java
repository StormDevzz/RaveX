package ravex.gui.browser;
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
import org.lwjgl.glfw.GLFW;
import ravex.gui.clickgui.ColorUtility;
import ravex.utility.render.FontRenderUtility;
import ravex.utility.render.Render2DEngine;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import ravex.manager.ModuleManager;

public class BlockBrowserScreen extends Screen {
    private final Screen parent;
    private final Predicate<Identifier> isSelected;
    private final BiConsumer<Block, Boolean> onToggle;
    private final Runnable onClear;
    private String searchQuery = "";
    private boolean searchFocused;
    private float scrollOffset;
    private float scrollAnim;
    private long openTime = -1;
    private boolean closing = false;
    private long closeStart = 0;
    private ItemStack hoveredStack = ItemStack.EMPTY;
    private List<Entry> previousFiltered = new ArrayList<>();
    private float previousScrollAnim;
    private float tabTransition = 1f;
    private float swipeDirection = 1f;
    private long selectedCount;
    private final Set<Identifier> selectedIds = new HashSet<>();

    private static class Entry {
        final Block block;
        final Identifier id;
        final String name;
        final ItemStack stack;
        Entry(Block block) {
            this.block = block;
            this.id = BuiltInRegistries.BLOCK.getKey(block);
            this.name = block.getName().getString();
            this.stack = new ItemStack(block.asItem());
        }
    }

    private final List<Entry> all = new ArrayList<>();
    private List<Entry> filtered = new ArrayList<>();

    private enum Tab { ALL, SELECTED }
    private Tab tab = Tab.ALL;

    public BlockBrowserScreen(Screen parent, Predicate<Identifier> isSelected,
                              BiConsumer<Block, Boolean> onToggle, Runnable onClear) {
        super(Component.literal("BlockBrowser"));
        this.parent = parent;
        this.isSelected = isSelected;
        this.onToggle = onToggle;
        this.onClear = onClear;
        for (Block b : BuiltInRegistries.BLOCK) {
            if (b.asItem() == null || b.asItem() == net.minecraft.world.item.Items.AIR) continue;
            all.add(new Entry(b));
        }
        refilter();
    }

    public BlockBrowserScreen(Screen parent, Predicate<Identifier> isSelected,
                              BiConsumer<Block, Boolean> onToggle) {
        this(parent, isSelected, onToggle, null);
    }

    @Override public boolean isPauseScreen() { return false; }

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
        g.fill(0, 0, w, h, (int)(0x60 * alpha) << 24);

        int pw = Math.min(680, w - 40);
        int ph = Math.min(480, h - 40);
        int px = (w - pw) / 2;
        int py = (h - ph) / 2;

        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(w / 2f, h / 2f);
        pose.scale(scale, scale);
        pose.translate(-w / 2f, -h / 2f);

        int amx = (int)((mx - w / 2f) / scale + w / 2f);
        int amy = (int)((my - h / 2f) / scale + h / 2f);

        int ac = ColorUtility.getActiveColor();
        int alpha255 = (int)(255 * alpha);
        int r = 8;

        Render2DEngine.drawRound(g, px, py, pw, ph, r, 0xF00A0A0E);
        Render2DEngine.drawRoundBorder(g, px, py, pw, ph, r, 1,
            ColorUtility.withAlpha(0xFF202020, alpha255));

        g.fill(px, py + 28, px + pw, py + 29, 0x22FFFFFF);

        FontRenderUtility.drawString(g, "BlockBrowser", px + 12, py + 8,
            ColorUtility.withAlpha(0xFFE0E0E0, alpha255), true);

        FontRenderUtility.drawString(g, selectedCount + "/" + all.size(), px + pw - 80, py + 10,
            ColorUtility.withAlpha(0xFF606060, alpha255), true);

        int cy = py + 34;
        int ch = ph - 64;

        int tabX = px + 10, tabY = cy, tabH = 16;
        int tabW = 60;
        for (int ti = 0; ti < 2; ti++) {
            int tix = tabX + ti * (tabW + 4);
            boolean ta = tab == (ti == 0 ? Tab.ALL : Tab.SELECTED);
            boolean th = !ta && amx >= tix && amx <= tix + tabW && amy >= tabY && amy <= tabY + tabH;
            Render2DEngine.drawRound(g, tix, tabY, tabW, tabH, 4,
                ta ? 0x20FFFFFF : (th ? 0x15151515 : 0x05000000));
            if (ta)
                Render2DEngine.drawRoundBorder(g, tix, tabY, tabW, tabH, 4, 1,
                    ColorUtility.withAlpha(0xFF404040, (int)(180 * alpha)));
            String tabLabel = ti == 0 ? "All" : "Selected";
            int tabLabelW = FontRenderUtility.getStringWidth(tabLabel);
            FontRenderUtility.drawString(g, tabLabel,
                tix + (tabW - tabLabelW) / 2,
                tabY + 3, ColorUtility.withAlpha(ta ? 0xFFFFFFFF : (th ? 0xFFB0B0B0 : 0xFF707070), alpha255), true);
        }

        int sx = px + pw - 170, sy2 = cy, sw = 160, sh = 16;
        boolean sHov = amx >= sx && amx <= sx + sw && amy >= sy2 && amy <= sy2 + sh;
        Render2DEngine.drawRound(g, sx, sy2, sw, sh, 4, searchFocused ? 0xFF141418 : (sHov ? 0xFF121216 : 0xFF0C0C10));
        Render2DEngine.drawRoundBorder(g, sx, sy2, sw, sh, 4, 1,
            searchFocused ? ColorUtility.withAlpha(0xFF555555, (int)(200 * alpha))
                : ColorUtility.withAlpha(0xFF222222, alpha255));
        String st = searchQuery.isEmpty() && !searchFocused ? "\u00A78Search..." :
            searchQuery + (searchFocused ? "\u00A78|" : "");
        FontRenderUtility.drawString(g, st, sx + 6, sy2 + 3,
            ColorUtility.withAlpha(0xFF909090, alpha255), true);

        int gx = px + 10, gy = cy + 24, gw = pw - 20, gh = ch - 28;
        int cols = Math.max(4, (gw + 2) / 24);
        int cell = 24;
        int rows = (filtered.size() + cols - 1) / cols;
        int maxScroll = Math.max(0, rows - (gh / cell));

        scrollAnim += (scrollOffset - scrollAnim) * 0.18f;
        if (Math.abs(scrollAnim - scrollOffset) < 0.01f) scrollAnim = scrollOffset;

        if (tabTransition < 1f) {
            tabTransition += (1f - tabTransition) * 0.20f;
            if (Math.abs(tabTransition - 1f) < 0.001f) tabTransition = 1f;
        }

        Render2DEngine.drawRound(g, gx, gy, gw, gh, 6, 0xAA06060A);
        Render2DEngine.drawRoundBorder(g, gx, gy, gw, gh, 6, 1,
            ColorUtility.withAlpha(0xFF1A1A1A, alpha255));

        g.enableScissor(gx, gy, gx + gw, gy + gh);

        hoveredStack = ItemStack.EMPTY;
        int sr = (int) scrollAnim;
        int er = Math.min(rows, sr + (gh / cell) + 1);

        int selColor = ColorUtility.withAlpha(ac, 80);
        int selHovColor = ColorUtility.withAlpha(ac, 110);
        int hovColor = ColorUtility.withAlpha(0x22FFFFFF, 100);

        boolean transitioning = tabTransition < 1f;

        if (transitioning && !previousFiltered.isEmpty()) {
            float oldOffX = -swipeDirection * tabTransition * gw;
            int oldCols = Math.max(4, (gw + 2) / 24);
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
                    if (selectedIds.contains(e.id)) {
                        Render2DEngine.drawPixelPerfectRound(g, gx + c * cell, gy + (r2 - oldSr) * cell,
                            cell, cell, 6, selColor);
                    }
                    g.renderItem(e.stack, gx + c * cell + 4, gy + (r2 - oldSr) * cell + 4);
                }
            }
            oldPose.popMatrix();
        }

        float newOffX = swipeDirection * (1f - tabTransition) * gw;
        if (transitioning) {
            var newPose = g.pose();
            newPose.pushMatrix();
            newPose.translate(newOffX, 0);
            renderGrid(g, sr, er, cols, gx, gy, cell, amx, amy, false, selColor, selHovColor, hovColor, transitioning);
            newPose.popMatrix();
        } else {
            renderGrid(g, sr, er, cols, gx, gy, cell, amx, amy, true, selColor, selHovColor, hovColor, false);
        }

        g.disableScissor();

        if (maxScroll > 0) {
            float ratio = (float) cell * rows / gh;
            int th = Math.max(12, (int)(gh / ratio));
            float sr2 = scrollOffset / Math.max(1, maxScroll);
            int ty2 = gy + (int)((gh - th) * sr2);
            Render2DEngine.drawRound(g, px + pw - 6, ty2, 3, th, 2,
                ColorUtility.withAlpha(0x44FFFFFF, alpha255));
        }

        if (onClear != null) {
            int cw = 60, ch2 = 14;
            int cxx = px + 10, cyy = py + ph - 20;
            boolean chov = amx >= cxx && amx <= cxx + cw && amy >= cyy && amy <= cyy + ch2;
            Render2DEngine.drawRound(g, cxx, cyy, cw, ch2, 4, chov ? 0xFF3A1010 : 0xFF1A0E0E);
            Render2DEngine.drawRoundBorder(g, cxx, cyy, cw, ch2, 4, 1, chov ? 0xFFFF4455 : 0xFF3A2020);
            FontRenderUtility.drawString(g, "Clear", cxx + 14, cyy + 2, 0xFFFF8888, true);
        }

        int bw = 50, bh = 14;
        int bx = px + pw - bw - 10, by2 = py + ph - 20;
        boolean bh2 = amx >= bx && amx <= bx + bw && amy >= by2 && amy <= by2 + bh;
        Render2DEngine.drawRound(g, bx, by2, bw, bh, 4, bh2 ? 0xFF1A1A1E : 0xFF0E0E12);
        Render2DEngine.drawRoundBorder(g, bx, by2, bw, bh, 4, 1,
            ColorUtility.withAlpha(0xFF303030, alpha255));
        FontRenderUtility.drawString(g, "Back", bx + 12, by2 + 2,
            ColorUtility.withAlpha(0xFFD0D0D0, alpha255), true);

        pose.popMatrix();

        if (!hoveredStack.isEmpty() && alpha > 0.8f) {
            String[] lines = {
                hoveredStack.getHoverName().getString(),
                "\u00A77" + BuiltInRegistries.ITEM.getKey(hoveredStack.getItem())
            };
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

    private void close() {
        if (closing) return;
        closing = true;
        closeStart = System.currentTimeMillis();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean handled) {
        int w = width, h = height;
        int pw = Math.min(680, w - 40), ph = Math.min(480, h - 40);
        int px = (w - pw) / 2, py = (h - ph) / 2;

        float scale = 0.85f + 0.15f * Math.min(1f, (System.currentTimeMillis() - openTime) / 150f);
        if (closing) scale *= (1f - 0.15f * Math.min(1f, (System.currentTimeMillis() - closeStart) / 120f));
        double amx = (event.x() - w / 2f) / scale + w / 2f;
        double amy = (event.y() - h / 2f) / scale + h / 2f;

        if (event.button() == 0) {
            int sx = px + pw - 170, sy2 = py + 34, sw = 160, sh = 16;
            if (amx >= sx && amx <= sx + sw && amy >= sy2 && amy <= sy2 + sh) {
                searchFocused = true;
                return true;
            }
            searchFocused = false;

            int tabW = 60, tabH = 16;
            int tabX = px + 10, tabY = py + 34;
            for (int ti = 0; ti < 2; ti++) {
                int tix = tabX + ti * (tabW + 4);
                if (amx >= tix && amx <= tix + tabW && amy >= tabY && amy <= tabY + tabH) {
                    if (tab != (ti == 0 ? Tab.ALL : Tab.SELECTED)) {
                        previousFiltered = new ArrayList<>(filtered);
                        previousScrollAnim = scrollAnim;
                        int oldIdx = tab.ordinal();
                        swipeDirection = ti > oldIdx ? 1f : -1f;
                        tab = ti == 0 ? Tab.ALL : Tab.SELECTED;
                        refilter();
                        tabTransition = 0f;
                    }
                    return true;
                }
            }

            if (onClear != null) {
                int cxx = px + 10, cyy = py + ph - 20, cw = 60, ch2 = 14;
                if (amx >= cxx && amx <= cxx + cw && amy >= cyy && amy <= cyy + ch2) {
                    onClear.run();
                    return true;
                }
            }

            int bx = px + pw - 60, by2 = py + ph - 20, bw = 50, bh = 14;
            if (amx >= bx && amx <= bx + bw && amy >= by2 && amy <= by2 + bh) {
                close();
                return true;
            }

            int gx = px + 10, gy = py + 58, cols = Math.max(4, (pw - 22) / 24), cell = 24;
            int col = (int)((amx - gx) / cell);
            int row = (int)((amy - gy) / cell) + (int)scrollAnim;
            if (col >= 0 && col < cols && row >= 0) {
                int idx = row * cols + col;
                if (idx < filtered.size() && amx >= gx + col * cell && amx <= gx + col * cell + cell
                    && amy >= gy + row * cell - (int)scrollAnim && amy <= gy + row * cell - (int)scrollAnim + cell) {
                    Entry e = filtered.get(idx);
                    onToggle.accept(e.block, !isSelected.test(e.id));
                    refilter();
                    return true;
                }
            }
        }

        return super.mouseClicked(event, handled);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmt, double vAmt) {
        int pw = Math.min(680, width - 40);
        int ph = Math.min(480, height - 40);
        int gw = pw - 20, cols = Math.max(4, (gw + 2) / 24);
        int rows = (filtered.size() + cols - 1) / cols;
        float gh = (ph - 64) - 28;
        int maxScroll = Math.max(0, rows - (int)(gh / 24));
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (float)vAmt * 1.5f));
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) { close(); return true; }
        if (searchFocused) {
            if (event.key() == GLFW.GLFW_KEY_BACKSPACE && !searchQuery.isEmpty()) {
                searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                scrollOffset = 0;
                refilter();
                return true;
            }
            if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_ESCAPE) {
                searchFocused = false;
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (searchFocused && event.codepoint() >= 32 && event.codepoint() < 127) {
            searchQuery += (char) event.codepoint();
            scrollOffset = 0;
            refilter();
            return true;
        }
        return super.charTyped(event);
    }

    private void refilter() {
        selectedIds.clear();
        for (Entry e : all) {
            if (isSelected.test(e.id)) selectedIds.add(e.id);
        }
        selectedCount = selectedIds.size();

        String q = searchQuery.toLowerCase();
        if (tab == Tab.SELECTED) {
            filtered = new ArrayList<>();
            for (Entry e : all) {
                if (!selectedIds.contains(e.id)) continue;
                if (q.isEmpty() || e.name.toLowerCase().contains(q) || e.id.toString().contains(q))
                    filtered.add(e);
            }
        } else if (q.isEmpty()) {
            filtered = all;
        } else {
            filtered = new ArrayList<>();
            for (Entry e : all) {
                if (e.name.toLowerCase().contains(q) || e.id.toString().contains(q))
                    filtered.add(e);
            }
        }
    }

    private void renderGrid(GuiGraphics g, int sr, int er, int cols, int gx, int gy, int cell,
                            int amx, int amy, boolean hoverable, int selColor, int selHovColor, int hovColor,
                            boolean transitioning) {
        for (int r2 = sr; r2 < er; r2++) {
            for (int c = 0; c < cols; c++) {
                int idx = r2 * cols + c;
                if (idx >= filtered.size()) break;
                Entry e = filtered.get(idx);
                int tx = gx + c * cell;
                int ty = gy + (r2 - sr) * cell;
                boolean hov = hoverable && amx >= tx && amx <= tx + cell && amy >= ty && amy <= ty + cell;

                if (hov) hoveredStack = e.stack;

                if (selectedIds.contains(e.id)) {
                    Render2DEngine.drawPixelPerfectRound(g, tx, ty, cell, cell, 6,
                        hov ? selHovColor : selColor);
                } else if (hov) {
                    Render2DEngine.drawRound(g, tx, ty, cell, cell, 6, hovColor);
                }

                g.renderItem(e.stack, tx + 4, ty + 4);
            }
        }
    }

    public static BlockBrowserScreen forNuker(Screen parent) {
        return new BlockBrowserScreen(parent,
            id -> ravex.modules.world.nuker.NukerData.INSTANCE.isSelected(id),
            (block, sel) -> ravex.modules.world.nuker.NukerData.INSTANCE
                .toggle(BuiltInRegistries.BLOCK.getKey(block)),
            () -> ravex.modules.world.nuker.NukerData.INSTANCE.clear());
    }

    public static BlockBrowserScreen forXray(Screen parent) {
        return new BlockBrowserScreen(parent,
            id -> ModuleManager.get(ravex.modules.player.Xray.class).isBlockSelected(id),
            (block, sel) -> {
                ModuleManager.get(ravex.modules.player.Xray.class).setBlockSelected(block, sel);
                var mc = Minecraft.getInstance();
                if (mc.levelRenderer != null) mc.levelRenderer.allChanged();
            },
            null);
    }
}
