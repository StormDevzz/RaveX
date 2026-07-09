package ravex.mixin.player;

import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.player.ExtraChat;

@Mixin(ClientPacketListener.class)
public class MixinClientPacketListener {

    @ModifyVariable(method = "sendChat", at = @At("HEAD"), argsOnly = true)
    private String modifyChatMessage(String message) {
        if (ExtraChat.INSTANCE.getEnabled() && ExtraChat.INSTANCE.zov.getValue()) {
            return message.replace('з', 'Z').replace('З', 'Z')
                         .replace('в', 'V').replace('В', 'V');
        }
        return message;
    }

    @Inject(method = "handleOpenScreen", at = @At("HEAD"), cancellable = true)
    private void onHandleOpenScreen(net.minecraft.network.protocol.game.ClientboundOpenScreenPacket packet, CallbackInfo ci) {
        if (ravex.modules.movement.HighJump.INSTANCE.getEnabled() && "GrimShulker".equals(ravex.modules.movement.HighJump.INSTANCE.mode.getValue())) {
            if (packet.getType() == net.minecraft.world.inventory.MenuType.SHULKER_BOX) {
                ci.cancel();
            }
        }
    }
}

