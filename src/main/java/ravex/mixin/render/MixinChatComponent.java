package ravex.mixin.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import ravex.modules.misc.NameProtect;

@Mixin(ChatComponent.class)
public abstract class MixinChatComponent {

    @ModifyVariable(method = "addMessage(Lnet/minecraft/network/chat/Component;)V",
                    at = @At("HEAD"), argsOnly = true)
    private Component protectChatMessage(Component message) {
        if (!NameProtect.INSTANCE.getEnabled()) return message;
        if (Minecraft.getInstance().player == null) return message;
        return NameProtect.INSTANCE.protectComponent(message);
    }
}
