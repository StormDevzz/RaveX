package ravex.manager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import ravex.utility.notification.Notification;
import java.util.concurrent.CopyOnWriteArrayList;

public class NotificationManager {
    private static final CopyOnWriteArrayList<Notification> notifications = new CopyOnWriteArrayList<>();
    private static final int PANEL_PADDING_X = 6;
    private static final int PANEL_PADDING_Y = 3;
    private static final int LINE_HEIGHT = 10;
    private static final int GAP = 4;
    private static final int ACCENT_HEIGHT = 1;

    public static void add(String text) {
        add(text, 0xFFD0D0E0, 3000);
    }

    public static void add(String text, int color) {
        add(text, color, 3000);
    }

    public static void add(String text, int color, int duration) {
        notifications.add(new Notification(text, color, duration));
        if (notifications.size() > 20) {
            notifications.remove(0);
        }
    }

    public static void render(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.font == null) return;

        int screenW = graphics.guiWidth();
        int screenH = graphics.guiHeight();
        int startY = screenH / 3;

        int currentY = startY;

        for (Notification n : notifications) {
            if (n.isExpired()) {
                notifications.remove(n);
                continue;
            }

            float alpha = n.getAlpha();
            if (alpha <= 0.01f) continue;

            int textW = mc.font.width(n.text);
            int panelW = textW + PANEL_PADDING_X * 2;
            int panelH = LINE_HEIGHT + PANEL_PADDING_Y * 2;
            int panelX = (screenW - panelW) / 2;

            int bgColor = (int)(0xBB * alpha) << 24;
            if (bgColor == 0) continue;

            int accentColor = n.color;
            int accentAlpha = (int)(0xFF * alpha) << 24;
            accentColor = (accentColor & 0x00FFFFFF) | accentAlpha;

            graphics.fill(panelX, currentY, panelX + panelW, currentY + panelH, bgColor);

            graphics.fill(panelX, currentY, panelX + panelW, currentY + ACCENT_HEIGHT, accentColor);

            int textColor = (n.color & 0x00FFFFFF) | ((int)(0xFF * alpha) << 24);
            int textX = panelX + PANEL_PADDING_X;
            int textY = currentY + PANEL_PADDING_Y;
            graphics.drawString(mc.font, n.text, textX, textY, textColor, false);

            currentY += panelH + GAP;
        }
    }

    public static void clear() {
        notifications.clear();
    }
}
