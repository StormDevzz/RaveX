package ravex.mixin.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.misc.ChatHelper;
import ravex.modules.misc.NameProtect;

@Mixin(ChatComponent.class)
public abstract class MixinChatComponent {

    @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;)V",
            at = @At("HEAD"), cancellable = true)
    private void filterChatMessage(Component message, CallbackInfo ci) {
        if (ChatHelper.itz().shouldFilterMessage(message.getString())) {
            ci.cancel();
        }
    }

    @ModifyVariable(method = "addMessage(Lnet/minecraft/network/chat/Component;)V",
                    at = @At("HEAD"), argsOnly = true)
    private Component modifyChatMessage(Component message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return message;
        Component m = message;
        if (NameProtect.maybeEnabled()) {
            m = NameProtect.itz().protectComponent(m);
        }
        ChatHelper ch = ChatHelper.itz();
        if (ch.getEnabled() && ch.timestamp.getValue()) {
            String ts = ch.applyTimestamp("");
            if (!ts.isEmpty()) {
                m = Component.literal(ts).copy().append(m);
            }
        }
        return m;
    }
}
