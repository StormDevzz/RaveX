package ravex.mixin.menu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(TitleScreen.class)
public class MixinTitleScreen {

    @Shadow
    private SplashRenderer splash;

    private String ravexSplashText = "RaveX on top!";

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        // =========================================================================
        // RaveX Custom Splashes Pool
        // Add or modify any text strings below! One will be chosen randomly on load.
        // =========================================================================
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
                "Now with premium LUA scripts!",
                "Buttery smooth rendering!",
                "Unmatched design aesthetics!",
                "Akrien 1.12.2?",
                "Xiaomi 13t Pro",
                "RaveX - Open Source client :3",
                "RaveX on top",
                "Buy minecraft license",
                "1000 IQ play",
                "CelkaPasta"
        };

        // Immediately choose a local fallback splash
        try {
            Random random = new Random();
            ravexSplashText = ravexSplashes[random.nextInt(ravexSplashes.length)];
        } catch (Exception e) {
            ravexSplashText = "RaveX Client!";
        }

        // Try to fetch dynamic online splashes from GitHub using our network utility
        ravex.utility.network.Github.fetchRawContent("StormDevzz", "RaveX", "main", "splashes.txt")
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
                .exceptionally(ex -> {
                    // Silently fail if offline or unavailable, fallback remains active
                    return null;
                });

        // Hide vanilla splash to let us draw our custom glowing red splash
        this.splash = null;
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        var mc = Minecraft.getInstance();
        var font = mc.font;
        if (font == null)
            return;

        // Screen dimensions
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();

        // ─────────────────────────────────────────────────────────────────────────
        // Premium Custom Red Bouncing Splash Text with Dynamic Sway & Color Wave
        // ─────────────────────────────────────────────────────────────────────────
        if (ravexSplashText != null && !ravexSplashText.isEmpty()) {
            long millis = System.currentTimeMillis();

            // 1. Smooth, premium breathing scale pulse (no high frequency jitter!)
            float basePulse = (float) Math.sin((double) (millis % 1500L) / 1500.0 * Math.PI * 2.0); // elegant 1.5s wave
            float scale = 1.8F - Math.abs(basePulse * 0.15F); // subtle scale pulse
            scale = scale * 100.0F / (float) (font.width(ravexSplashText) + 32);

            // 2. Slow, breathing crimson-to-rose color shift (3.5 second cycle)
            double wave = Math.sin((double) (millis % 3500L) / 3500.0 * Math.PI * 2.0);
            int r = (int) (225 + wave * 30); // 195 to 255
            int g = (int) (40 + wave * 25);   // 15 to 65 (soft crimson-rose warmth)
            int b = (int) (60 + wave * 30);   // 30 to 90 (elegant soft rose tone)
            int activeRedColor = 0xFF000000 | (r << 16) | (g << 8) | b;
            int shadowColor = 0xAA200505; // Dark red shadow for neon depth

            // 3. Pose Transformations using 2D Matrix3x2fStack
            var pose = graphics.pose();
            pose.pushMatrix();

            // Translate to splash position (Vanilla-aligned, no frantic jitter shaking)
            pose.translate((float) (width / 2 + 90), 70.0F);

            // Gentle, slow sway/rotation over time (4.0 second cycle)
            float rotationAngle = -20.0F + (float) Math.sin((double) (millis % 4000L) / 4000.0 * Math.PI * 2.0) * 2.0F;
            pose.rotate((float) Math.toRadians(rotationAngle));
            pose.scale(scale, scale);

            // Render drop shadow & glowing text
            graphics.drawCenteredString(font, ravexSplashText, 1, -7, shadowColor); // 3D Shadow
            graphics.drawCenteredString(font, ravexSplashText, 0, -8, activeRedColor); // Silky Smooth Red Splash

            pose.popMatrix();
        }

        // Top Left Info Panel
        graphics.drawString(font, "RaveX Client", 8, 8, 0xFFE53935, true); // Red Brand Color
        graphics.drawString(font, "Logged in as: §f" + mc.getUser().getName(), 8, 20, 0xFF888888, true);
        graphics.drawString(font, "Build: §7" + ravex.RaveX.version, 8, 32, 0xFF888888, true);

        // Bottom Right Custom Splash / Info
        String quoteLine1 = "§7\"The ultimate utility client\"";
        String quoteLine2 = "§fRaveX Client | Premium Edition";

        int w1 = font.width(quoteLine1);
        int w2 = font.width(quoteLine2);

        graphics.drawString(font, quoteLine1, width - w1 - 8, height - 24, 0xFFFFFF, true);
        graphics.drawString(font, quoteLine2, width - w2 - 8, height - 12, 0xFFFFFF, true);
    }
}
