package ravex.modules.hud;
import ravex.modules.annotations.HudModule;
import ravex.modules.annotations.Parameter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import ravex.utility.render.ColorUtility;

import ravex.modules.client.Hud;
import ravex.utility.render.HudRendererUtility;
import ravex.utility.render.TextureLoaderUtility;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;

@HudModule("TpsHud")
public class TpsHud extends ravex.modules.Module {
    @Parameter(name = "Color", color = true)
    public int color = 0xFF44FF88;
    @Parameter(name = "Shadow")
    public boolean shadow = true;

private static final Identifier ICON = TextureLoaderUtility.HUD_TPS_WHITE;
    private static final int IS = HudRendererUtility.getIconSize();
    private long lastRealTime = 0;
    private long lastGameTick = -1;
    private float smoothedTPS = 20.0f;

    public void render(GuiGraphics graphics, float partialTicks) {
        if (!Modules.enabled(Hud.class)) return;
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getLevel() == null) return;
        updateTPS(mc);
        int bx = getX(), by = getY();
        int col = this.color;
        boolean shadow = this.shadow;
        String text = String.format("%.1f", smoothedTPS);
        int tw = ravex.utility.render.FontRenderUtility.getStringWidth(text);
        String label = "TPS";
        int lw = ravex.utility.render.FontRenderUtility.getStringWidth(label);
        int pw = 4 + Math.max(tw, lw) + 4 + IS + 4;
        int ph = 26;
        setWidth(pw);
        setHeight(ph);
        HudRendererUtility.drawBackground(graphics, bx, by, pw, ph);
        int cx = bx + 4;
        ravex.utility.render.FontRenderUtility.drawString(graphics, text, cx, by + 4, col, shadow);
        ravex.utility.render.FontRenderUtility.drawString(graphics, label, cx, by + 16, 0xFF8080A0, false);
        HudRendererUtility.drawIcon(graphics, ICON, bx + pw - 4 - IS, by + (ph - IS) / 2, ColorUtility.getActiveColor());
    }
    private void updateTPS(MinecraftWrapper mc) {
        long now = System.currentTimeMillis();
        long gameTick = mc.getLevel().getGameTime();
        if (lastGameTick < 0) {
            lastGameTick = gameTick;
            lastRealTime = now;
            return;
        }
        long elapsed = now - lastRealTime;
        if (elapsed >= 1000) {
            long ticks = gameTick - lastGameTick;
            float measured = (float)(ticks * 1000.0 / elapsed);
            smoothedTPS = smoothedTPS * 0.7f + Math.min(20f, Math.max(0f, measured)) * 0.3f;
            lastGameTick = gameTick;
            lastRealTime = now;
        }
    }
}
