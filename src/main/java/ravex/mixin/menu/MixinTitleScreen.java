package ravex.mixin.menu;

import net.minecraft.client.gui.GuiGraphics;
import ravex.mcwrapper.MinecraftWrapper;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.RenderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Redirect;
import ravex.gui.proxy.ProxyConfigScreen;
import ravex.utility.render.Render2DUtility;

import java.util.Random;
import ravex.modules.client.Settings;
import ravex.modules.client.MainMenu;
import ravex.utility.render.FontRenderUtility;
import ravex.modules.Modules;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;

@Mixin(TitleScreen.class)
public abstract class MixinTitleScreen extends Screen {

    private static final Identifier RAVEX_LOGO = Identifier.fromNamespaceAndPath("ravex", "textures/ravexv2");
    private static final int LOGO_SRC_W = 1220;
    private static final int LOGO_SRC_H = 1396;
    private static boolean ravexLogoLoaded = false;

    private static final Identifier VERSION_3D_ID = Identifier.fromNamespaceAndPath("ravex", "version_3d");
    private static String version3DText;
    private static int version3DTexW;
    private static int version3DTexH;
    private static float version3DContentX;
    private static float version3DContentY;
    private static float version3DContentW;
    private static float version3DContentH;
    private static boolean version3DLoaded = false;

    @Shadow
    private SplashRenderer splash;

    private String ravexSplashText = "RaveX on top!";

    protected MixinTitleScreen(Component title) {
        super(title);
    }

    private static boolean ravex$mainMenuEnabled() {
        return Modules.enabled(MainMenu.class);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        if (!ravex$mainMenuEnabled()) return;
        String[] ravexSplashes = {
                "Малая Токмачка бухает",
                "Зелебобашаламетбимба",
                "Александр Лобанов",
                "Этот клиент сделан с нуля!",
                "А вот дага фурэ фэмбой!",
                "джээной мост буст",
                "Подожди 2 секунды",
                "Олденбург бухает",
                "рваный хуй",
                "попбоб забустил",
                "бублик бимп",
                "сэр",
                "HvH 1.12.2?",
                "RaveX on top!",
                "VoltHack?",
                "хачапури",
                "Buttery smooth rendering!",
                "Unmatched design aesthetics!",
                "Akrien 1.12.2?",
                "Xiaomi 13t Pro",
                "RaveX - Open Source client :3",
                "RaveX on top",
                "Buy minecraft license",
                "1000 IQ play",
                "CelkaPasta",
                "1 IQ play",
                "larper",
                "mister larper",
                "doxxxxxxx",
                "ur sigma"
        };

        try {
            Random random = new Random();
            ravexSplashText = ravexSplashes[random.nextInt(ravexSplashes.length)];
        } catch (Exception e) {
            ravexSplashText = "RaveX Client!";
        }

        ravex.utility.network.GithubUtility.fetchRawContent("StormDevzz", "RaveX", "main", "splashes.txt")
                .thenAccept(content -> {
                    if (content != null && !content.isBlank()) {
                        String[] onlineSplashes = content.split("\\r?\\n");
                        java.util.List<String> validSplashes = java.util.Arrays.stream(onlineSplashes)
                                .map(String::trim)
                                .filter(s -> !s.isEmpty() && !s.startsWith("#"))
                                .toList();
                        if (!validSplashes.isEmpty()) {
                            Random random = new Random();
                            ravexSplashText = validSplashes.get(random.nextInt(validSplashes.size()));
                        }
                    }
                })
                .exceptionally(ex -> null);

        this.splash = null;

        this.addRenderableWidget(Button.builder(
            Component.literal("Proxy Config"),
            btn -> MinecraftWrapper.getInstance().setScreen(new ProxyConfigScreen((TitleScreen)(Object)this))
        ).bounds(this.width / 2 + 104, this.height / 4 + 72, 100, 20).build());

        for (GuiEventListener child : java.util.List.copyOf(this.children())) {
            if (child instanceof Button button) {
                String message = button.getMessage().getString();
                if (message.contains("Copyright") || message.contains("Mojang")) {
                    this.removeWidget(button);
                }
            }
        }
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V"))
    private void ravex$cancelVanillaVersionLine(GuiGraphics graphics, Font font, String text, int x, int y, int color) {
        if (ravex$mainMenuEnabled() && text != null && text.startsWith("Minecraft ")) {
            return;
        }
        graphics.drawString(font, text, x, y, color);
    }

    private void ravex$ensureLogo() {
        if (ravexLogoLoaded) return;
        String path = "/assets/ravex/textures/ravexv2.png";
        try (InputStream stream = MixinTitleScreen.class.getResourceAsStream(path)) {
            if (stream == null) return;
            NativeImage image = NativeImage.read(stream);
            DynamicTexture tex = new DynamicTexture(() -> "ravexv2_menu", image);
            Render2DUtility.setLinearSampler(tex);
            MinecraftWrapper.getWrapper().getTextureManager().register(RAVEX_LOGO, tex);
            ravexLogoLoaded = true;
        } catch (Exception ignored) {
        }
    }

    private static java.awt.Font ravex$loadAwtFont(float size) {
        String path = "/assets/ravex/font/space_grotesk_bold.ttf";
        try (InputStream stream = MixinTitleScreen.class.getResourceAsStream(path)) {
            if (stream != null) {
                return java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, stream).deriveFont(size);
            }
        } catch (Exception ignored) {
        }
        return new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.BOLD, (int) size);
    }

    private void ravex$ensureVersion3D(String text) {
        if (version3DLoaded && text.equals(version3DText)) return;

        int fontSize = 72;
        int depth = 14;
        int outline = 5;
        int shadow = 10;
        int pad = depth + outline + shadow + 12;

        java.awt.Font awtFont = ravex$loadAwtFont(fontSize);
        BufferedImage measureImg = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D mg = measureImg.createGraphics();
        mg.setFont(awtFont);
        FontRenderContext frc = mg.getFontRenderContext();
        GlyphVector gv = awtFont.createGlyphVector(frc, text);
        Rectangle2D bounds = gv.getVisualBounds();
        mg.dispose();

        int textW = (int) Math.ceil(bounds.getWidth()) + 4;
        int textH = (int) Math.ceil(bounds.getHeight()) + 4;
        int imgW = textW + pad * 2;
        int imgH = textH + pad * 2;

        BufferedImage img = new BufferedImage(imgW, imgH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setFont(awtFont);

        float baseX = pad - (float) bounds.getX();
        float baseY = pad - (float) bounds.getY();

        GlyphVector shapeGv = awtFont.createGlyphVector(g.getFontRenderContext(), text);
        java.awt.geom.Path2D facePath = new java.awt.geom.Path2D.Float(shapeGv.getOutline(baseX, baseY));
        Rectangle2D faceBounds = facePath.getBounds2D();

        g.setColor(new Color(0, 0, 0, 90));
        for (int i = shadow; i >= 1; i--) {
            java.awt.geom.AffineTransform at = java.awt.geom.AffineTransform.getTranslateInstance(i * 0.9, i * 0.9);
            g.fill(at.createTransformedShape(facePath));
        }

        for (int d = depth; d >= 1; d--) {
            float t = 1.0f - (d - 1) / (float) depth;
            int r = (int) (0x0C + t * 0x4A);
            int gr = (int) (0x12 + t * 0x5C);
            int b = (int) (0x22 + t * 0x82);
            g.setColor(new Color(r, gr, b));
            java.awt.geom.AffineTransform at = java.awt.geom.AffineTransform.getTranslateInstance(d, d);
            g.fill(at.createTransformedShape(facePath));
        }

        g.setStroke(new BasicStroke(outline, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(4, 6, 12));
        g.draw(facePath);

        java.awt.geom.AffineTransform bevelLight = java.awt.geom.AffineTransform.getTranslateInstance(-2.5, -2.5);
        g.setColor(new Color(255, 255, 255, 240));
        g.fill(bevelLight.createTransformedShape(facePath));

        java.awt.geom.AffineTransform bevelDark = java.awt.geom.AffineTransform.getTranslateInstance(2.5, 2.5);
        g.setColor(new Color(18, 28, 52));
        g.fill(bevelDark.createTransformedShape(facePath));

        GradientPaint faceGrad = new GradientPaint(
                baseX, baseY - fontSize * 0.35f, new Color(0xF2, 0xF6, 0xFC),
                baseX, baseY + fontSize * 0.55f, new Color(0xA0, 0xAE, 0xC8)
        );
        g.setPaint(faceGrad);
        g.fill(facePath);

        g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(255, 255, 255, 70));
        java.awt.geom.AffineTransform topHi = java.awt.geom.AffineTransform.getTranslateInstance(-1, -1);
        g.draw(topHi.createTransformedShape(facePath));

        g.dispose();

        NativeImage nativeImg = new NativeImage(imgW, imgH, false);
        for (int y = 0; y < imgH; y++) {
            for (int x = 0; x < imgW; x++) {
                nativeImg.setPixel(x, y, img.getRGB(x, y));
            }
        }

        DynamicTexture tex = new DynamicTexture(() -> "ravex_version_3d", nativeImg);
        Render2DUtility.setLinearSampler(tex);
        MinecraftWrapper.getWrapper().getTextureManager().register(VERSION_3D_ID, tex);

        version3DText = text;
        version3DTexW = imgW;
        version3DTexH = imgH;
        version3DContentX = (float) faceBounds.getX();
        version3DContentY = (float) faceBounds.getY();
        version3DContentW = (float) faceBounds.getWidth();
        version3DContentH = (float) faceBounds.getHeight();
        version3DLoaded = true;
    }

    private void ravex$draw3DText(GuiGraphics graphics, String text, int contentLeft, int contentCenterY, int targetContentH) {
        ravex$ensureVersion3D(text);
        if (!version3DLoaded || version3DContentH <= 0f) return;

        float scale = targetContentH / version3DContentH;
        int drawW = Math.round(version3DTexW * scale);
        int drawH = Math.round(version3DTexH * scale);
        int drawX = Math.round(contentLeft - version3DContentX * scale);
        int drawY = Math.round(contentCenterY - (version3DContentY + version3DContentH * 0.5f) * scale);
        graphics.blit(RenderPipelines.GUI_TEXTURED, VERSION_3D_ID,
                drawX, drawY, 0f, 0f, drawW, drawH, version3DTexW, version3DTexH, version3DTexW, version3DTexH);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!ravex$mainMenuEnabled()) return;
        var mc = MinecraftWrapper.getInstance();
        var font = mc.font;
        if (font == null)
            return;

        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();

        if (ravexSplashText != null && !ravexSplashText.isEmpty()) {
            long millis = System.currentTimeMillis();
            Component splashComponent = FontRenderUtility.getTextComponent(ravexSplashText);

            float basePulse = (float) Math.sin((double) (millis % 1500L) / 1500.0 * Math.PI * 2.0);
            float scale = 1.8F - Math.abs(basePulse * 0.15F);
            scale = scale * 100.0F / (float) (font.width(splashComponent) + 32);

            double wave = Math.sin((double) (millis % 3500L) / 3500.0 * Math.PI * 2.0);
            int r = (int) (40 + wave * 20);
            int g = (int) (100 + wave * 30);
            int b = (int) (225 + wave * 30);
            int activeBlueColor = 0xFF000000 | (r << 16) | (g << 8) | b;
            int shadowColor = 0xAA050520;

            var pose = graphics.pose();
            pose.pushMatrix();

            pose.translate((float) (width / 2 + 90), 70.0F);

            float rotationAngle = -20.0F + (float) Math.sin((double) (millis % 4000L) / 4000.0 * Math.PI * 2.0) * 2.0F;
            pose.rotate((float) Math.toRadians(rotationAngle));
            pose.scale(scale, scale);

            graphics.drawCenteredString(font, splashComponent, 1, -7, shadowColor);
            graphics.drawCenteredString(font, splashComponent, 0, -8, activeBlueColor);

            pose.popMatrix();
        }

        ravex$ensureLogo();

        int logoH = 40;
        int logoW = (int) (logoH * (float) LOGO_SRC_W / LOGO_SRC_H);
        int textX = 8;
        if (ravexLogoLoaded) {
            graphics.blit(RAVEX_LOGO, 8, 6, 8 + logoW, 6 + logoH, 0.0f, 1.0f, 0.0f, 1.0f);
            textX = 8 + logoW + 8;
        }

        FontRenderUtility.drawString(graphics, "Rave", textX, 8, 0xFFFFFFFF, true);
        int raveW = FontRenderUtility.getStringWidth("Rave");
        FontRenderUtility.drawString(graphics, "X", textX + raveW, 8, Modules.get(Settings.class).menuColor, true);
        int xW = FontRenderUtility.getStringWidth("X");
        FontRenderUtility.drawString(graphics, " Client", textX + raveW + xW, 8, 0xFFFFFFFF, true);
        int buildY = 24;
        FontRenderUtility.drawString(graphics, "Build:", textX, buildY, 0xFF888888, true);
        int buildLabelW = FontRenderUtility.getStringWidth("Build:");
        int buildCenterY = buildY + FontRenderUtility.getFontHeight() / 2;
        ravex$draw3DText(graphics, ravex.RaveX.version, textX + buildLabelW + 3, buildCenterY, 12);

        String quoteLine1 = "§7\"The ultimate utility client\"";
        String quoteLine2 = "§fRaveX Client | Premium Edition";

        int w1 = font.width(quoteLine1);
        int w2 = font.width(quoteLine2);

        FontRenderUtility.drawString(graphics, quoteLine1, width - w1 - 8, height - 24, 0xFFFFFF, true);
        FontRenderUtility.drawString(graphics, quoteLine2, width - w2 - 8, height - 12, 0xFFFFFF, true);
    }
}
