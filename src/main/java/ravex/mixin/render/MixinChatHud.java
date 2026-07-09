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

@Mixin(ChatComponent.class)
public abstract class MixinChatHud {
    @Unique
    private static final ThreadLocal<Boolean> ravex$transformed = ThreadLocal.withInitial(() -> false);

    @Inject(method = "render", at = @At("HEAD"))
    private void onChatRenderPre(GuiGraphics graphics, Font font, int mouseX, int mouseY, int something, boolean focused, boolean canFocus, CallbackInfo ci) {
<<<<<<< HEAD
        if (!ChatHud.maybeEnabled()) return;
=======
        if (!ChatHud.INSTANCE.getEnabled()) return;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        if (ravex$transformed.get()) return;
        ravex$transformed.set(true);
        var pose = graphics.pose();
        pose.pushMatrix();
<<<<<<< HEAD
        pose.translate(ChatHud.itz().getX(), ChatHud.itz().getY());
        float s = ChatHud.itz().scale.getValue().floatValue();
=======
        pose.translate(ChatHud.INSTANCE.getX(), ChatHud.INSTANCE.getY());
        float s = ChatHud.INSTANCE.scale.getValue().floatValue();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        pose.scale(s, s);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void onChatRenderPost(GuiGraphics graphics, Font font, int mouseX, int mouseY, int something, boolean focused, boolean canFocus, CallbackInfo ci) {
<<<<<<< HEAD
        if (!ChatHud.maybeEnabled()) return;
=======
        if (!ChatHud.INSTANCE.getEnabled()) return;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        if (!ravex$transformed.get()) return;
        ravex$transformed.set(false);
        graphics.pose().popMatrix();
    }
}
