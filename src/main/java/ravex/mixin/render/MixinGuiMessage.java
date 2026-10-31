package ravex.mixin.render;

import net.minecraft.client.GuiMessage;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.ComponentRenderUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ravex.modules.Modules;

import java.util.List;

@Mixin(GuiMessage.class)
public class MixinGuiMessage {
    @Unique
    private static boolean ravex$chatFontLogged = false;

    @Redirect(
        method = "splitLines",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ComponentRenderUtils;wrapComponents(Lnet/minecraft/network/chat/FormattedText;ILnet/minecraft/client/gui/Font;)Ljava/util/List;")
    )
    private List<FormattedCharSequence> ravex$chatFont(FormattedText text, int width, Font font) {
        ravex.modules.client.Fonts fonts = Modules.get(ravex.modules.client.Fonts.class);
        boolean applied = false;
        if (fonts != null && Modules.enabled(ravex.modules.client.Fonts.class) && fonts.chatFont && text instanceof Component component) {
            // keep the client font through the wrap step
            text = ravex.utility.render.FontRenderUtility.applyGlobalFont(component);
            applied = true;
        }
        List<FormattedCharSequence> out = ComponentRenderUtils.wrapComponents(text, width, font);
        if (applied && !ravex$chatFontLogged && !out.isEmpty()) {
            ravex$chatFontLogged = true;
            try {
                out.get(0).accept((index, style, codepoint) -> {
                    ravex.RaveX.LOGGER.info("[RaveX/font] chat line style: {}", style == null ? null : style.getFont());
                    return false;
                });
            } catch (Throwable ignored) {
            }
        }
        return out;
    }
}
