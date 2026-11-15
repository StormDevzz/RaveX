package ravex.modules.hud;

import ravex.modules.annotations.HudModule;
import ravex.modules.annotations.Parameter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import ravex.utility.render.ColorUtility;
import ravex.modules.client.Hud;
import ravex.utility.render.HudRendererUtility;
import ravex.utility.render.TextureLoaderUtility;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;

@HudModule("FpsHud")
public class FpsHud extends ravex.modules.Module {
    @Parameter(name = "HighColor", color = true)
    public int highColor = 0xFF44FF88;
    @Parameter(name = "MidColor", color = true)
    public int midColor = 0xFFFFCC33;
    @Parameter(name = "LowColor", color = true)
    public int lowColor = 0xFFFF4455;
    @Parameter(name = "Shadow")
    public boolean shadow = true;
    @Parameter(name = "Instant")
    public boolean instant = false;

    private static final Identifier ICON = TextureLoaderUtility.HUD_FPS_WHITE;
    private static final int SAMPLE_COUNT = 12;

    private final long[] frameTimes = new long[SAMPLE_COUNT];
    private int frameIndex = 0;
    private int sampleCount = 0;
    private long lastFrameNs = 0L;

    public FpsHud() {
        super("FpsHud", 10, 30, 60, 14);
        setX(10);
        setY(30);
        setWidth(60);
        setHeight(14);
    }

    protected void onEnable() {
        resetFrameSamples();
    }

    protected void onDisable() {
        resetFrameSamples();
    }

    public void render(GuiGraphics graphics, float partialTicks) {
        if (!Modules.enabled(Hud.class)) {
            resetFrameSamples();
            return;
        }
        int fps = currentFps();
        int ac = ColorUtility.getActiveColor();
        boolean shadow = this.shadow;
        int highColor = this.highColor, midColor = this.midColor, lowColor = this.lowColor;
        int bx = getX(), by = getY();
        int fpsColor;
        if (fps >= 60) fpsColor = highColor;
        else if (fps >= 30) fpsColor = midColor;
        else fpsColor = lowColor;
        String fpsStr = String.valueOf(fps);
        String suffix = isVsync() ? " FPS (Vsync)" : " FPS";
        int tw = HudRendererUtility.textWidth(fpsStr) + HudRendererUtility.textWidth(suffix);
        int IS = HudRendererUtility.getIconSize();
        int pw = 4 + tw + 4 + IS + 4;
        int ph = 14;
        setWidth(pw);
        setHeight(ph);
        HudRendererUtility.drawBackground(graphics, bx, by, pw, ph);
        int ix = bx + 4;
        HudRendererUtility.drawText(graphics, fpsStr, ix, by + 2, fpsColor, shadow);
        ix += HudRendererUtility.textWidth(fpsStr);
        HudRendererUtility.drawText(graphics, suffix, ix, by + 2, 0xFF8080A0, false);
        HudRendererUtility.drawIcon(graphics, ICON, bx + pw - 4 - IS, by + (ph - IS) / 2, ac);
    }

    private int currentFps() {
        long now = System.nanoTime();
        if (!instant) {
            resetFrameSamples();
            lastFrameNs = now;
            return MinecraftWrapper.getWrapper().getFps();
        }
        if (lastFrameNs == 0L) {
            lastFrameNs = now;
            return MinecraftWrapper.getWrapper().getFps();
        }
        long delta = now - lastFrameNs;
        lastFrameNs = now;
        if (delta <= 0L || delta > 1_000_000_000L) {
            return MinecraftWrapper.getWrapper().getFps();
        }
        frameTimes[frameIndex] = delta;
        frameIndex = (frameIndex + 1) % SAMPLE_COUNT;
        if (sampleCount < SAMPLE_COUNT) sampleCount++;

        long total = 0L;
        for (int i = 0; i < sampleCount; i++) total += frameTimes[i];
        double average = total / (double) sampleCount;
        return average <= 0.0 ? 0 : (int) Math.round(1_000_000_000.0 / average);
    }

    private void resetFrameSamples() {
        lastFrameNs = 0L;
        frameIndex = 0;
        sampleCount = 0;
        java.util.Arrays.fill(frameTimes, 0L);
    }

    private boolean isVsync() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) return false;
            if (mc.options != null && mc.options.enableVsync() != null) {
                return mc.options.enableVsync().get();
            }
            try {
                java.lang.reflect.Field f = mc.options.getClass().getField("enableVsync");
                Object v = f.get(mc.options);
                if (v instanceof net.minecraft.client.OptionInstance<?> oi) {
                    return Boolean.TRUE.equals(oi.get());
                }
                if (v instanceof Boolean b) return b;
            } catch (Throwable ignored) {
            }
        } catch (Throwable ignored) {
        }
        return false;
    }
}
