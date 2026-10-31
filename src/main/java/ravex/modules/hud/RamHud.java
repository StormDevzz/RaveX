package ravex.modules.hud;

import net.minecraft.client.gui.GuiGraphics;
import ravex.event.Subscribe;
import ravex.event.client.TickEvent;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Module;
import ravex.modules.Modules;
import ravex.modules.annotations.HudModule;
import ravex.modules.annotations.Parameter;
import ravex.modules.client.Hud;
import ravex.utility.render.FontRenderUtility;
import ravex.utility.render.Render2DUtility;
import ravex.utility.render.HudRendererUtility;

@HudModule("RamHud")
public class RamHud extends Module {

    @Parameter(name = "ShowGraph")
    public boolean showGraph = true;

    @Parameter(name = "ShowUsage")
    public boolean showUsage = true;

    @Parameter(name = "WarnAt", min = 50, max = 95, step = 5)
    public double warnAt = 85;

    @Parameter(name = "Samples", min = 10, max = 60, step = 5)
    public int samples = 30;

    private float[] graphSamples;
    private int graphIndex = 0;
    private int tickCounter = 0;
    private long lastUsed = 0;
    private long lastMax = 1;

    private RamHud() {
        super("RamHud", 10, 80, 144, 30);
        setX(10);
        setY(80);
        setWidth(144);
        setHeight(30);
        graphSamples = new float[samples];
    }

    @Subscribe
    public void onClientTick(TickEvent.Client event) {
        if (!getEnabled()) {
            lastUsed = 0;
            lastMax = 1;
            return;
        }
        Runtime rt = Runtime.getRuntime();
        long maxMem = rt.maxMemory();
        long totalMem = rt.totalMemory();
        long freeMem = rt.freeMemory();
        if (maxMem <= 0) maxMem = 1;
        lastUsed = totalMem - freeMem;
        lastMax = maxMem;

        if (graphSamples.length != samples) {
            graphSamples = new float[samples];
            graphIndex = 0;
        }

        if (++tickCounter % 10 == 0) {
            double pct = (double) lastUsed * 100.0 / lastMax;
            graphSamples[graphIndex % graphSamples.length] = (float) pct;
            graphIndex++;
        }
    }

    public void render(GuiGraphics graphics, float partialTicks) {
        if (!Modules.enabled(Hud.class)) return;
        int bx = getX();
        int by = getY();
        int pw = 144;
        int ph = showGraph ? 44 : (showUsage ? 30 : 24);
        setWidth(pw);
        setHeight(ph);

        HudRendererUtility.drawBackground(graphics, bx, by, pw, ph);

        double pct = (lastMax > 0) ? (double) lastUsed * 100.0 / lastMax : 0.0;
        int color = (pct >= warnAt) ? 0xFFFF4545 : ((pct >= 70) ? 0xFFFFB830 : 0xFF38BDF8);

        int iconX = bx + 8;
        int iconY = by + 6;
        Render2DUtility.drawRound(graphics, iconX, iconY, 10, 10, 2, 0xFF1C2230);
        Render2DUtility.drawRoundBorder(graphics, iconX, iconY, 10, 10, 2, 1, 0xFF334155);
        graphics.fill(iconX + 3, iconY + 3, iconX + 7, iconY + 7, 0xFF2D3748);
        graphics.fill(iconX - 2, iconY + 2, iconX, iconY + 4, 0xFFE5A93C);
        graphics.fill(iconX - 2, iconY + 6, iconX, iconY + 8, 0xFFE5A93C);
        graphics.fill(iconX + 10, iconY + 2, iconX + 12, iconY + 4, 0xFFE5A93C);
        graphics.fill(iconX + 10, iconY + 6, iconX + 12, iconY + 8, 0xFFE5A93C);

        FontRenderUtility.drawString(graphics, "RAM", iconX + 14, by + 6, 0xFFFFFFFF, true);

        String pctStr = Math.round(pct) + "%";
        int pctW = FontRenderUtility.getStringWidth(pctStr);
        int pctX = bx + pw - 8 - pctW;
        FontRenderUtility.drawString(graphics, pctStr, pctX, by + 6, color, true);

        if (showUsage) {
            long usedMb = lastUsed / (1024L * 1024L);
            long maxMb = lastMax / (1024L * 1024L);
            String usageStr = usedMb + " / " + maxMb + " MB";
            int usageW = FontRenderUtility.getStringWidth(usageStr);
            int usageX = pctX - 6 - usageW;
            if (usageX > iconX + 14 + FontRenderUtility.getStringWidth("RAM") + 4) {
                FontRenderUtility.drawString(graphics, usageStr, usageX, by + 6, 0xFF8E99A8, false);
            }
        }

        int barX = bx + 8;
        int barW = pw - 16;
        int barY = by + 19;
        int barH = 5;
        Render2DUtility.drawRound(graphics, barX, barY, barW, barH, 2, 0x800E131E);
        Render2DUtility.drawRoundBorder(graphics, barX, barY, barW, barH, 2, 1, 0x18FFFFFF);

        int fillW = Math.max(3, (int) Math.round(barW * Math.min(1.0, Math.max(0.0, pct / 100.0))));
        fillW = Math.min(fillW, barW);
        Render2DUtility.drawRound(graphics, barX, barY, fillW, barH, 2, color);

        if (showGraph && graphSamples.length > 0) {
            int graphX = bx + 8;
            int graphY = by + 28;
            int graphW = pw - 16;
            int graphH = 10;
            Render2DUtility.drawRound(graphics, graphX, graphY, graphW, graphH, 2, 0x500E131E);

            int count = Math.min(graphIndex, graphSamples.length);
            if (count > 0) {
                float slotW = (float) graphW / (float) graphSamples.length;
                int barDrawW = Math.max(1, (int) Math.floor(slotW));
                for (int i = 0; i < count; i++) {
                    float val = graphSamples[(graphIndex - count + i) % graphSamples.length];
                    int gx = graphX + (int) (i * slotW);
                    int gh = Math.max(1, Math.round(val / 100f * graphH));
                    int gy = graphY + (graphH - gh);
                    int sColor = (val >= warnAt) ? 0xFFFF4545 : ((val >= 70) ? 0xFFFFB830 : 0xFF38BDF8);
                    graphics.fill(gx, gy, Math.min(graphX + graphW, gx + barDrawW), graphY + graphH, sColor);
                }
            }
        }
    }
}