package ravex.mixin.menu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.gui.proxy.ProxyConfigScreen;

import java.util.Random;
import ravex.modules.client.Settings;
import ravex.utility.render.FontRenderUtility;

@Mixin(TitleScreen.class)
public abstract class MixinTitleScreen extends Screen {

    @Shadow
    private SplashRenderer splash;

    private String ravexSplashText = "RaveX on top!";

    protected MixinTitleScreen(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
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
                .exceptionally(ex -> null);

        this.splash = null;

        this.addRenderableWidget(Button.builder(
            Component.literal("Proxy Config"),
            btn -> Minecraft.getInstance().setScreen(new ProxyConfigScreen((TitleScreen)(Object)this))
        ).bounds(this.width / 2 + 104, this.height / 4 + 72, 100, 20).build());
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        var mc = Minecraft.getInstance();
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

        graphics.drawString(font, "RaveX Client", 8, 8, Settings.itz().menuColor.getValue(), true);
        graphics.drawString(font, "Logged in as: §f" + mc.getUser().getName(), 8, 20, 0xFF888888, true);
        graphics.drawString(font, "Build: §7" + ravex.RaveX.version, 8, 32, 0xFF888888, true);

        String quoteLine1 = "§7\"The ultimate utility client\"";
        String quoteLine2 = "§fRaveX Client | Premium Edition";

        int w1 = font.width(quoteLine1);
        int w2 = font.width(quoteLine2);

        graphics.drawString(font, quoteLine1, width - w1 - 8, height - 24, 0xFFFFFF, true);
        graphics.drawString(font, quoteLine2, width - w2 - 8, height - 12, 0xFFFFFF, true);
    }
}
