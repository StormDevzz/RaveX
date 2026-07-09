package ravex.mixin.player;

import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
<<<<<<< HEAD
import ravex.modules.misc.ChatHelper;
import ravex.modules.movement.HighJump;
=======
import ravex.modules.misc.ChatUtils;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3

@Mixin(ClientPacketListener.class)
public class MixinClientPacketListener {

    @ModifyVariable(method = "sendChat", at = @At("HEAD"), argsOnly = true)
    private String modifyChatMessage(String message) {
<<<<<<< HEAD
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
=======
        if (ChatUtils.INSTANCE.getEnabled() && ChatUtils.INSTANCE.zov.getValue()) {
            return message.replace('з', 'Z').replace('З', 'Z')
                         .replace('в', 'V').replace('В', 'V');
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        }
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

