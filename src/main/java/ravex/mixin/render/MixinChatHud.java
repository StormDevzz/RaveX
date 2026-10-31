package ravex.mixin.render;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.hud.ChatHud;
import ravex.modules.render.Animations;
import ravex.modules.Modules;

@Mixin(ChatComponent.class)
public abstract class MixinChatHud {
    @Unique
    private static final ThreadLocal<Boolean> ravex$transformed = ThreadLocal.withInitial(() -> false);

    @Inject(method = "render", at = @At("HEAD"))
    private void onChatRenderPre(GuiGraphics graphics, Font font, int mouseX, int mouseY, int something, boolean focused, boolean canFocus, CallbackInfo ci) {
        // sync line spacing with the chat font
        ravex.modules.client.Fonts.applyChatLineSpacing();
        if (ravex$transformed.get()) return;
        var pose = graphics.pose();
        boolean pushed = false;

        if (Modules.enabled(ChatHud.class)) {
            ChatHud chatHud = Modules.get(ChatHud.class);
            int defaultBaseY = graphics.guiHeight() - 40 - chatHud.getHeight();
            float dx = (float) chatHud.getX();
            float dy = (float) (chatHud.getY() - defaultBaseY);
            pose.pushMatrix();
            pushed = true;
            pose.translate(dx, dy);
            float s = chatHud.getUserScale();
            pose.scale(s, s);
        }

        // newest message reveal (Animations)
        float e = Animations.chatAnimEase();
        if (e < 1f) {
            Animations m = Modules.get(Animations.class);
            String mode = m == null ? "Slide" : m.chatMode;
            if (!pushed) {
                pose.pushMatrix();
                pushed = true;
            }
            float inv = 1f - e;
            switch (mode) {
                case "Drop" -> {
                    float y = -30f * inv * inv;
                    if (e > 0.6f) y += (float) Math.sin((e - 0.6f) / 0.4f * Math.PI) * 4f;
                    pose.translate(0f, y);
                }
                case "Pop" -> {
                    float ax = graphics.guiWidth() / 2f;
                    float ay = graphics.guiHeight() - 40f;
                    float u = e - 1f;
                    float back = 1f + 2.70158f * u * u * u + 1.70158f * u * u;
                    float s2 = 0.7f + 0.3f * back;
                    pose.translate(ax, ay + 8f * inv);
                    pose.scale(s2, s2);
                    pose.translate(-ax, -ay);
                }
                default -> pose.translate(0f, 30f * inv * inv);
            }
        }

        if (pushed) ravex$transformed.set(true);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void onChatRenderPost(GuiGraphics graphics, Font font, int mouseX, int mouseY, int something, boolean focused, boolean canFocus, CallbackInfo ci) {
        if (!ravex$transformed.get()) return;
        ravex$transformed.set(false);
        graphics.pose().popMatrix();
    }
}
