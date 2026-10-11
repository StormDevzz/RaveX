package ravex.gui.update;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import ravex.gui.clickgui.SubScreenStyle;
import ravex.manager.UpdateManager;
import ravex.utility.render.ColorUtility;
import ravex.utility.render.FontRenderUtility;
import ravex.utility.render.Render2DUtility;
import ravex.utility.misc.LanguageUtility;

import java.util.ArrayList;
import java.util.List;

/** Manual and automatic update UI: check, download, verify and install a new RaveX build. */
public class UpdateScreen extends Screen {
    private static final int CARD_PAD = 10;
    private static final int BTN_H = 18;
    private static final int GAP = 6;

    private final Screen parent;
    private final List<UiButton> buttons = new ArrayList<>();

    private int notesX;
    private int notesY;
    private int notesW;
    private int notesH;
    private int notesScroll;
    private int notesContentH;
    private boolean notesDragging;
    private double notesDragOrigin;
    private int notesDragScroll;

    private String statusLine = "";
    private boolean relaunchAfterInstall = true;

    public UpdateScreen(Screen parent) {
        super(Component.literal(LanguageUtility.t("upd_title")));
        this.parent = parent;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        buttons.clear();

        int panelW = Math.min(300, width - 40);
        int panelH = Math.min(190, height - 40);
        int panelX = (width - panelW) / 2;
        int panelY = (height - panelH) / 2;

        notesX = panelX + CARD_PAD;
        notesY = panelY + 46;
        notesW = panelW - CARD_PAD * 2;
        notesH = panelH - 46 - (BTN_H + GAP) - CARD_PAD;

        notesScroll = Math.max(0, Math.min(notesScroll, Math.max(0, notesContentH - notesH)));

        int buttonW = (notesW - GAP * 2) / 3;
        int buttonY = panelY + panelH - CARD_PAD - BTN_H;

        if (UpdateManager.INSTANCE.isUpdateAvailable()) {
            buttons.add(new UiButton(LanguageUtility.t("upd_open_release"), notesX, buttonY, buttonW,
                    () -> openReleasePage()));
            if (UpdateManager.INSTANCE.getState() == UpdateManager.State.DOWNLOADED) {
                buttons.add(new UiButton(LanguageUtility.t("upd_install"), notesX + buttonW + GAP, buttonY,
                        buttonW, () -> install()));
                buttons.add(new UiButton(LanguageUtility.t("upd_back"), notesX + (buttonW + GAP) * 2, buttonY,
                        buttonW, () -> onClose()));
            } else if (UpdateManager.INSTANCE.getState() != UpdateManager.State.DOWNLOADING
                    && UpdateManager.INSTANCE.getState() != UpdateManager.State.INSTALLING) {
                buttons.add(new UiButton(LanguageUtility.t("upd_download"), notesX + buttonW + GAP, buttonY,
                        buttonW, () -> UpdateManager.INSTANCE.downloadUpdate()));
                buttons.add(new UiButton(LanguageUtility.t("upd_back"), notesX + (buttonW + GAP) * 2, buttonY,
                        buttonW, () -> onClose()));
            } else {
                buttons.add(new UiButton(LanguageUtility.t("upd_downloading"), notesX + buttonW + GAP, buttonY,
                        buttonW, () -> { }));
                buttons.add(new UiButton(LanguageUtility.t("upd_back"), notesX + (buttonW + GAP) * 2, buttonY,
                        buttonW, () -> onClose()));
            }
        } else {
            buttons.add(new UiButton(LanguageUtility.t("upd_check_again"), notesX, buttonY, buttonW,
                    () -> UpdateManager.INSTANCE.checkForUpdates()));
            buttons.add(new UiButton(LanguageUtility.t("upd_back"), notesX + buttonW + GAP, buttonY, buttonW,
                    () -> onClose()));
        }
    }

    private void install() {
        statusLine = LanguageUtility.t("upd_preparing");
        UpdateManager.INSTANCE.installUpdate(relaunchAfterInstall);
    }

    private void openReleasePage() {
        String url = UpdateManager.INSTANCE.getReleasePageUrl();
        if (url == null || url.isEmpty()) return;
        try {
            Util.getPlatform().openUri(url);
        } catch (Throwable t) {
            statusLine = LanguageUtility.t("upd_no_browser");
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        SubScreenStyle.background(graphics, width, height);

        UpdateManager manager = UpdateManager.INSTANCE;
        int active = ColorUtility.getActiveColor();

        int panelW = Math.min(300, width - 40);
        int panelH = Math.min(190, height - 40);
        int panelX = (width - panelW) / 2;
        int panelY = (height - panelH) / 2;

        SubScreenStyle.card(graphics, panelX, panelY, panelW, panelH, active);
        SubScreenStyle.header(graphics, width, LanguageUtility.t("upd_title"),
                LanguageUtility.t("upd_subtitle"), active);

        statusLine = resolveStatus(manager);
        SubScreenStyle.statusPill(graphics, width, statusLine.isEmpty() ? null : statusLine, active);

        int infoY = panelY + 8;
        FontRenderUtility.drawString(graphics, LanguageUtility.t("upd_installed"), notesX, infoY, 0xFF7070A0, false);
        FontRenderUtility.drawString(graphics, manager.getCurrentVersion(),
                notesX + 52, infoY, 0xFFE0E0F0, false);

        infoY += 11;
        String latest = manager.getLatestVersion();
        FontRenderUtility.drawString(graphics, LanguageUtility.t("upd_latest"), notesX, infoY, 0xFF7070A0, false);
        int latestColor = manager.isUpdateAvailable() ? active : 0xFFE0E0F0;
        FontRenderUtility.drawString(graphics,
                latest == null || latest.isEmpty() ? "-" : latest,
                notesX + 52, infoY, latestColor, false);

        if (manager.getState() == UpdateManager.State.DOWNLOADING) {
            renderProgress(graphics, manager, active);
        } else if (manager.getState() == UpdateManager.State.ERROR
                && manager.getErrorMessage() != null) {
            String error = truncate(manager.getErrorMessage(), notesW - 8);
            FontRenderUtility.drawString(graphics, "§c" + error, notesX, notesY - 10, 0xFFFF8080, false);
        }

        renderNotes(graphics, manager, mouseX, mouseY, active);

        for (UiButton button : buttons) {
            button.render(graphics, mouseX, mouseY, active);
        }
        if (manager.getState() == UpdateManager.State.DOWNLOADED) {
            String hint = relaunchAfterInstall
                    ? LanguageUtility.t("upd_relaunch_on")
                    : LanguageUtility.t("upd_relaunch_off");
            FontRenderUtility.drawString(graphics, hint, notesX, notesY + notesH + 3,
                    relaunchOnHover(mouseX, mouseY, hint) ? 0xFFFFFFFF : 0xFF7070A0, false);
        }

        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    private String resolveStatus(UpdateManager manager) {
        return switch (manager.getState()) {
            case CHECKING -> LanguageUtility.t("upd_checking");
            case UP_TO_DATE -> LanguageUtility.t("upd_up_to_date");
            case AVAILABLE -> LanguageUtility.t("upd_available");
            case DOWNLOADING -> String.format("%.0f%%", manager.getDownloadProgress() * 100f);
            case DOWNLOADED -> LanguageUtility.t("upd_ready");
            case INSTALLING -> LanguageUtility.t("upd_installing");
            case ERROR -> LanguageUtility.t("upd_error");
            default -> "";
        };
    }

    private void renderProgress(GuiGraphics graphics, UpdateManager manager, int active) {
        int barY = notesY - 12;
        float progress = manager.getDownloadProgress();
        Render2DUtility.drawRound(graphics, notesX, barY, notesW, 4, 2,
                ColorUtility.withAlpha(0x000000, 150));
        if (progress > 0f) {
            Render2DUtility.drawRound(graphics, notesX, barY, (int) (notesW * progress), 4, 2,
                    ColorUtility.withAlpha(active, 220));
        }
        long done = manager.getDownloadDone();
        long total = manager.getDownloadTotal();
        String text = total > 0
                ? formatBytes(done) + " / " + formatBytes(total)
                : formatBytes(done);
        FontRenderUtility.drawString(graphics, text, notesX + notesW - FontRenderUtility.getStringWidth(text),
                barY - 10, 0xFFA0A0C0, false);
    }

    private void renderNotes(GuiGraphics graphics, UpdateManager manager, int mouseX, int mouseY, int active) {
        Render2DUtility.drawRound(graphics, notesX, notesY, notesW, notesH, 4,
                ColorUtility.withAlpha(0x0A0A18, 180));

        List<String[]> lines = wrapNotes(manager.getReleaseNotes(), notesW - 10);
        int lineH = FontRenderUtility.getFontHeight() + 2;
        notesContentH = lines.size() * lineH + 6;

        boolean hover = mouseX >= notesX && mouseX <= notesX + notesW
                && mouseY >= notesY && mouseY <= notesY + notesH;

        Render2DUtility.pushScissor(graphics, notesX + 1, notesY + 1, notesW - 2, notesH - 2);
        int y = notesY + 3 - notesScroll;
        for (String[] pair : lines) {
            if (y + lineH > notesY && y < notesY + notesH) {
                int color = pair[1].equals("b") ? 0xFFDDDDFF
                        : pair[1].equals("c") ? 0xFF8CD8FF
                        : 0xFFB0B0C0;
                FontRenderUtility.drawString(graphics, pair[0], notesX + 5, y, color, false);
            }
            y += lineH;
        }
        Render2DUtility.popScissor(graphics);

        if (notesContentH > notesH) {
            float ratio = (float) notesH / notesContentH;
            int barH = Math.max(20, (int) (notesH * ratio));
            int maxScroll = notesContentH - notesH;
            int barY = notesY + (int) ((notesH - barH) * ((float) notesScroll / maxScroll));
            Render2DUtility.drawRound(graphics, notesX + notesW - 3, barY, 2, barH, 1,
                    ColorUtility.withAlpha(active, hover ? 190 : 110));
        }

        if (lines.isEmpty()) {
            FontRenderUtility.drawString(graphics, LanguageUtility.t("upd_no_notes"),
                    notesX + 5, notesY + 5, 0xFF606070, false);
        }
    }

    /** Returns {text, style} pairs where style is "b" (bold), "c" (code/link) or "n". */
    private static List<String[]> wrapNotes(String notes, int maxWidth) {
        List<String[]> out = new ArrayList<>();
        if (notes == null || notes.isBlank()) return out;
        for (String raw : notes.split("\\r?\\n")) {
            String line = raw.strip();
            if (line.isEmpty()) {
                out.add(new String[]{" ", "n"});
                continue;
            }
            String style = "n";
            if (line.startsWith("#")) {
                line = line.replaceFirst("^#+\\s*", "");
                style = "b";
            } else if (line.startsWith("- ") || line.startsWith("* ")) {
                line = "\u2022 " + line.substring(2);
            }
            for (String chunk : wrapPlain(line, maxWidth)) {
                out.add(new String[]{chunk, style});
            }
        }
        return out;
    }

    private static List<String> wrapPlain(String text, int maxWidth) {
        List<String> out = new ArrayList<>();
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

    private static String truncate(String text, int maxWidth) {
        if (text == null) return "";
        if (FontRenderUtility.getStringWidth(text) <= maxWidth) return text;
        String result = text;
        while (result.length() > 1 && FontRenderUtility.getStringWidth(result + "...") > maxWidth) {
            result = result.substring(0, result.length() - 1);
        }
        return result + "...";
    }

    private static String formatBytes(long bytes) {
        if (bytes <= 0) return "0 B";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024f);
        return String.format("%.1f MB", bytes / (1024f * 1024f));
    }

    private boolean relaunchOnHover(int mouseX, int mouseY, String hint) {
        return mouseY >= notesY + notesH + 2 && mouseY <= notesY + notesH + 14
                && mouseX >= notesX
                && mouseX <= notesX + FontRenderUtility.getStringWidth(hint);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean handled) {
        int mx = (int) event.x();
        int my = (int) event.y();

        if (UpdateManager.INSTANCE.getState() == UpdateManager.State.DOWNLOADED
                && relaunchOnHover(mx, my, relaunchAfterInstall
                    ? LanguageUtility.t("upd_relaunch_on")
                    : LanguageUtility.t("upd_relaunch_off"))) {
            relaunchAfterInstall = !relaunchAfterInstall;
            return true;
        }

        if (mx >= notesX && mx <= notesX + notesW && my >= notesY && my <= notesY + notesH) {
            notesDragging = true;
            notesDragOrigin = event.y();
            notesDragScroll = notesScroll;
            return true;
        }

        for (UiButton button : buttons) {
            if (button.isHovered(mx, my)) {
                button.onClick.run();
                init();
                return true;
            }
        }
        return super.mouseClicked(event, handled);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (notesDragging) {
            int maxScroll = Math.max(0, notesContentH - notesH);
            notesScroll = Math.max(0, Math.min(maxScroll,
                    notesDragScroll + (int) (notesDragOrigin - event.y())));
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        notesDragging = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= notesX && mouseX <= notesX + notesW
                && mouseY >= notesY && mouseY <= notesY + notesH) {
            notesScroll = Math.max(0, Math.min(Math.max(0, notesContentH - notesH),
                    notesScroll - (int) (scrollY * 18)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void onClose() {
        UpdateManager manager = UpdateManager.INSTANCE;
        if (manager.getState() == UpdateManager.State.INSTALLING) return;
        if (parent != null) {
            ravex.mcwrapper.MinecraftWrapper.getWrapper().setScreen(parent);
        } else {
            super.onClose();
        }
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    private static final class UiButton {
        final String label;
        final int x;
        final int y;
        final int w;
        final Runnable onClick;

        UiButton(String label, int x, int y, int w, Runnable onClick) {
            this.label = label;
            this.x = x;
            this.y = y;
            this.w = w;
            this.onClick = onClick;
        }

        boolean isHovered(int mx, int my) {
            return mx >= x && mx <= x + w && my >= y && my <= y + BTN_H;
        }

        void render(GuiGraphics graphics, int mouseX, int mouseY, int active) {
            boolean hovered = isHovered(mouseX, mouseY);
            SubScreenStyle.button(graphics, x, y, w, BTN_H, hovered, true, active);
            int textW = FontRenderUtility.getStringWidth(label);
            FontRenderUtility.drawString(graphics, label,
                    x + (w - textW) / 2,
                    y + (BTN_H - FontRenderUtility.getFontHeight()) / 2,
                    0xFFFFFFFF, false);
        }
    }
}