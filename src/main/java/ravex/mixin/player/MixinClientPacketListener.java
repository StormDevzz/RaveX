package ravex.mixin.player;

import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.misc.ChatHelper;
import ravex.modules.movement.HighJump;

@Mixin(ClientPacketListener.class)
public class MixinClientPacketListener {

    @ModifyVariable(method = "sendChat", at = @At("HEAD"), argsOnly = true)
    private String modifyChatMessage(String message) {
        ChatHelper ch = ChatHelper.itz();
        if (!ch.getEnabled() || !ch.zov.getValue()) return message;
        return ch.applyZov(message);
    }

    @Inject(method = "handleOpenScreen", at = @At("HEAD"), cancellable = true)
    private void onHandleOpenScreen(net.minecraft.network.protocol.game.ClientboundOpenScreenPacket packet, CallbackInfo ci) {
        if (HighJump.maybeEnabled() && "GrimShulker".equals(HighJump.itz().mode.getValue())) {
            if (packet.getType() == net.minecraft.world.inventory.MenuType.SHULKER_BOX) {
                ci.cancel();
            }
        }
    }
}

