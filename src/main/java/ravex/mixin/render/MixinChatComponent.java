package ravex.mixin.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.misc.ChatHelper;
import ravex.modules.misc.NameProtect;
import ravex.modules.Modules;

@Mixin(ChatComponent.class)
public abstract class MixinChatComponent {

    @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;)V",
            at = @At("HEAD"), cancellable = true)
    private void filterChatMessage(Component message, CallbackInfo ci) {
        if (Modules.get(ChatHelper.class).shouldFilterMessage(message.getString())) {
            ci.cancel();
        }
    }

    @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
            at = @At("HEAD"), cancellable = true)
    private void filterChatMessageFull(Component message, MessageSignature signature, GuiMessageTag tag, CallbackInfo ci) {
        if (Modules.get(ChatHelper.class).shouldFilterMessage(message.getString())) {
            ci.cancel();
        }
    }

    @ModifyVariable(method = "addMessage(Lnet/minecraft/network/chat/Component;)V",
                    at = @At("HEAD"), argsOnly = true)
    private Component modifyChatMessage(Component message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return message;
        Component m = message;
        if (Modules.enabled(NameProtect.class)) {
            m = Modules.get(NameProtect.class).protectComponent(m);
        }
        ChatHelper ch = Modules.get(ChatHelper.class);
        if (Modules.enabled(ChatHelper.class) && ch.timestamp) {
            String ts = ch.applyTimestamp("");
            if (!ts.isEmpty()) {
                m = Component.literal(ts).copy().append(m);
            }
        }
        return m;
    }
}
