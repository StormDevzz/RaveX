package ravex.gui.changelog;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import ravex.gui.clickgui.SubScreenStyle;
import ravex.manager.ChangelogManager;
import ravex.utility.render.ColorUtility;
import ravex.utility.render.FontRenderUtility;
import ravex.utility.render.Render2DUtility;
import ravex.utility.misc.LanguageUtility;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders the changelog, sourced from {@code .github/RELEASE_NOTES.md} of the repository
 * (bundled into the jar and refreshed from the GitHub Releases API when online).
 */
public class ChangelogScreen extends Screen {
    private static final int BTN_H = 18;
    private static final int PAD = 8;
    private static final int ENTRY_GAP = 10;

    private final Screen parent;

    private List<ChangelogManager.Entry> entries = new ArrayList<>();
    private List<RenderedLine> rendered = new ArrayList<>();
    private int contentH;

    private int listX;
    private int listY;
    private int listW;
    private int listH;
    private int scroll;
    private int maxTextW;
    private boolean dragging;
    private double dragOrigin;
    private int dragScroll;

    private boolean loading = true;
    private boolean loadStarted;

    public ChangelogScreen(Screen parent) {
        super(Component.literal(LanguageUtility.t("cl_title")));
        this.parent = parent;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        loading = ChangelogManager.INSTANCE.getEntries().isEmpty();
        entries = ChangelogManager.INSTANCE.getEntries();

        listW = Math.min(340, width - 40);
        listH = Math.min(200, height - 70);
        listX = (width - listW) / 2;
        listY = (height - listH) / 2 + 6;

        rebuild();
        clampScroll();

        if (loading && !loadStarted) {
            loadStarted = true;
            Thread thread = new Thread(() -> {
                ChangelogManager.INSTANCE.loadLocal();
                ChangelogManager.INSTANCE.loadRemote();
                onMainThread(() -> {
                    loading = false;
                    entries = ChangelogManager.INSTANCE.getEntries();
                    rebuild();
                    clampScroll();
                });
            }, "RaveX-Changelog");
            thread.setDaemon(true);
            thread.start();
        }
    }

    private void onMainThread(Runnable runnable) {
        try {
            ravex.mcwrapper.MinecraftWrapper.getWrapper().execute(runnable);
        } catch (Throwable t) {
            runnable.run();
        }
    }

    private void clampScroll() {
        scroll = Math.max(0, Math.min(scroll, Math.max(0, contentH - listH)));
    }

    /** Flattens the entries into drawable lines with wrapping. */
    private void rebuild() {
        rendered = new ArrayList<>();
        maxTextW = listW - PAD * 2 - 12;

        for (int i = 0; i < entries.size(); i++) {
            ChangelogManager.Entry entry = entries.get(i);
            if (i > 0) rendered.add(RenderedLine.gap(ENTRY_GAP));

            String heading = "RaveX " + (entry.version == null ? "" : entry.version);
            rendered.add(RenderedLine.title(heading, entry.date));

            for (ChangelogManager.Line line : entry.lines) {
                switch (line.type) {
                    case TITLE -> rendered.add(RenderedLine.title(line.plain(), null));
                    case HEADING -> {
                        rendered.add(RenderedLine.gap(4));
                        rendered.add(RenderedLine.heading(line.plain()));
                    }
                    case QUOTE -> rendered.add(RenderedLine.body("  " + line.plain(), RenderedLine.QUOTE));
                    case BULLET -> {
                        int indent = Math.min(2, line.indent) * 8;
                        rendered.add(RenderedLine.body("\u2022 " + line.plain(),
                                RenderedLine.NORMAL, indent));
                    }
                    default -> rendered.add(RenderedLine.body(line.plain(), RenderedLine.NORMAL));
                }
            }
        }

        int y = 0;
        for (RenderedLine line : rendered) {
            if (line.style == RenderedLine.BULLET || line.style == RenderedLine.NORMAL
                    || line.style == RenderedLine.QUOTE) {
                line.chunks.addAll(wrap(line.text, maxTextW - line.indent));
            }
            line.y = y;
            y += line.height();
        }
        contentH = y + PAD;
    }

    private static List<String> wrap(String text, int maxWidth) {
        List<String> out = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            out.add("");
            return out;
        }
        StringBuilder current = new StringBuilder();
        for (String word : text.split("\\s+")) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (FontRenderUtility.getStringWidth(candidate) > maxWidth && !current.isEmpty()) {
                out.add(current.toString());
                current = new StringBuilder(word);
            } else {
                current = new StringBuilder(candidate);
            }
        }
        if (!current.isEmpty()) out.add(current.toString());
        return out;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        SubScreenStyle.background(graphics, width, height);
        int active = ColorUtility.getActiveColor();

        SubScreenStyle.card(graphics, listX - PAD, listY - 26, listW + PAD * 2, listH + 26 + BTN_H + PAD, active);
        SubScreenStyle.header(graphics, width, "RaveX Changelog", subtitle(), active);

        if (loading) {
            FontRenderUtility.drawString(graphics, LanguageUtility.t("cl_loading_body"),
                    listX + PAD, listY + 6, 0xFF8080A0, false);
            renderFooter(graphics, active, mouseX, mouseY);
            super.render(graphics, mouseX, mouseY, partialTicks);
            return;
        }

        if (entries.isEmpty()) {
            FontRenderUtility.drawString(graphics, LanguageUtility.t("cl_none"),
                    listX + PAD, listY + 6, 0xFF808090, false);
        }

        boolean hover = mouseX >= listX && mouseX <= listX + listW
                && mouseY >= listY && mouseY <= listY + listH;

        Render2DUtility.drawRound(graphics, listX, listY, listW, listH, 4,
                ColorUtility.withAlpha(0x0A0A18, 190));

        Render2DUtility.pushScissor(graphics, listX + 1, listY + 1, listW - 2, listH - 2);
        int cursorY = listY + PAD - scroll;
        for (RenderedLine line : rendered) {
            int y = cursorY + line.y;
            if (y + line.height() > listY && y < listY + listH) {
                line.draw(graphics, listX + PAD, y, active, maxTextW);
            }
        }
        Render2DUtility.popScissor(graphics);

        if (contentH > listH) {
            float ratio = (float) listH / contentH;
            int barH = Math.max(24, (int) (listH * ratio));
            int maxScroll = contentH - listH;
            int barY = listY + (int) ((listH - barH) * ((float) scroll / maxScroll));
            Render2DUtility.drawRound(graphics, listX + listW - 3, barY, 2, barH, 1,
                    ColorUtility.withAlpha(active, hover ? 190 : 100));
        }

        renderFooter(graphics, active, mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    private String subtitle() {
        if (loading) return LanguageUtility.t("cl_loading");
        if (entries.isEmpty()) return LanguageUtility.t("cl_unavailable");
        ChangelogManager.Entry latest = entries.get(0);
        return latest.date.isEmpty()
                ? LanguageUtility.t("cl_latest", latest.version)
                : LanguageUtility.t("cl_latest_date", latest.version, latest.date);
    }

    private void renderFooter(GuiGraphics graphics, int active, int mouseX, int mouseY) {
        int buttonY = listY + listH + 8;
        int buttonW = 80;
        String releasesLabel = LanguageUtility.t("cl_releases");
        String backLabel = LanguageUtility.t("cl_back");

        boolean openHovered = Render2DUtility.isHovered(mouseX, mouseY, listX, buttonY, buttonW, BTN_H);
        SubScreenStyle.button(graphics, listX, buttonY, buttonW, BTN_H, openHovered, true, active);
        FontRenderUtility.drawString(graphics, releasesLabel,
                listX + (buttonW - FontRenderUtility.getStringWidth(releasesLabel)) / 2,
                buttonY + (BTN_H - FontRenderUtility.getFontHeight()) / 2, 0xFFFFFFFF, false);

        boolean backHovered = Render2DUtility.isHovered(mouseX, mouseY,
                listX + listW - buttonW, buttonY, buttonW, BTN_H);
        SubScreenStyle.button(graphics, listX + listW - buttonW, buttonY, buttonW, BTN_H,
                backHovered, true, active);
        FontRenderUtility.drawString(graphics, backLabel,
                listX + listW - buttonW + (buttonW - FontRenderUtility.getStringWidth(backLabel)) / 2,
                buttonY + (BTN_H - FontRenderUtility.getFontHeight()) / 2, 0xFFFFFFFF, false);

        if (contentH > listH) {
            String hint = LanguageUtility.t("cl_scroll");
            FontRenderUtility.drawString(graphics, hint,
                    listX + (listW - FontRenderUtility.getStringWidth(hint)) / 2,
                    buttonY + (BTN_H - FontRenderUtility.getFontHeight()) / 2, 0xFF606070, false);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean handled) {
        int mx = (int) event.x();
        int my = (int) event.y();
        int buttonY = listY + listH + 8;
        int buttonW = 80;

        if (Render2DUtility.isHovered(mx, my, listX, buttonY, buttonW, BTN_H)) {
            try {
                Util.getPlatform().openUri("https://github.com/StormDevzz/RaveX/releases");
            } catch (Throwable ignored) {
            }
            return true;
        }
        if (Render2DUtility.isHovered(mx, my, listX + listW - buttonW, buttonY, buttonW, BTN_H)) {
            onClose();
            return true;
        }

        if (mx >= listX && mx <= listX + listW && my >= listY && my <= listY + listH) {
            dragging = true;
            dragOrigin = event.y();
            dragScroll = scroll;
            return true;
        }
        return super.mouseClicked(event, handled);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (dragging) {
            scroll = Math.max(0, Math.min(Math.max(0, contentH - listH),
                    dragScroll + (int) (dragOrigin - event.y())));
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        dragging = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= listX && mouseX <= listX + listW && mouseY >= listY && mouseY <= listY + listH) {
            scroll = Math.max(0, Math.min(Math.max(0, contentH - listH),
                    scroll - (int) (scrollY * 20)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (entries.size() > 1) {
            int maxScroll = Math.max(0, contentH - listH);
            if (event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_PAGE_DOWN) {
                scroll = Math.min(maxScroll, scroll + listH - 20);
                return true;
            }
            if (event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_PAGE_UP) {
                scroll = Math.max(0, scroll - (listH - 20));
                return true;
            }
            if (event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_HOME) {
                scroll = 0;
                return true;
            }
            if (event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_END) {
                scroll = maxScroll;
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        if (parent != null) {
            ravex.mcwrapper.MinecraftWrapper.getWrapper().setScreen(parent);
        } else {
            super.onClose();
        }
    }

    private static class RenderedLine {
        static final int TITLE = 0;
        static final int HEADING = 1;
        static final int BULLET = 2;
        static final int NORMAL = 3;
        static final int QUOTE = 4;

        final String text;
        final int style;
        final int indent;
        final List<String> chunks = new ArrayList<>();
        int y;

        private RenderedLine(String text, int style, int indent) {
            this.text = text;
            this.style = style;
            this.indent = indent;
        }

        static RenderedLine title(String text, String date) {
            return new RenderedLine(date == null || date.isEmpty() ? text : text + "  \u00b7  " + date, TITLE, 0);
        }

        static RenderedLine heading(String text) {
            return new RenderedLine(text, HEADING, 0);
        }

        static RenderedLine body(String text, int style) {
            return new RenderedLine(text, style, 0);
        }

        static RenderedLine body(String text, int style, int indent) {
            return new RenderedLine(text, style, indent);
        }

        static RenderedLine gap(int height) {
            return new RenderedLine("", NORMAL, 0) {
                @Override
                int height() {
                    return height;
                }
            };
        }

        int height() {
            int lineH = FontRenderUtility.getFontHeight() + 2;
            if (style == TITLE) return lineH + 4;
            if (style == HEADING) return lineH + 1;
            if (text.isEmpty()) return 3;
            return Math.max(lineH, chunks.size() * lineH);
        }

        void draw(GuiGraphics graphics, int x, int y, int active, int ruleWidth) {
            if (text.isEmpty()) return;
            int lineH = FontRenderUtility.getFontHeight() + 2;
            int color = switch (style) {
                case TITLE -> 0xFFFFFFFF;
                case HEADING -> ColorUtility.setAlpha(active, 235);
                case QUOTE -> 0xFF9098B0;
                default -> 0xFFB8B8CC;
            };
            boolean bold = style == TITLE || style == HEADING;
            int drawX = x + indent;

            if (style == TITLE) {
                Render2DUtility.drawRect(graphics, x, y + lineH - 1, ruleWidth, 1,
                        ColorUtility.withAlpha(active, 60));
            }
            if (chunks.isEmpty()) {
                FontRenderUtility.drawString(graphics, text, drawX, y, color, bold);
            } else {
                int cy = y;
                for (String chunk : chunks) {
                    FontRenderUtility.drawString(graphics, chunk, drawX, cy, color, bold);
                    cy += lineH;
                }
            }
        }
    }
}