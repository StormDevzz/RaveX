package ravex.mixin.player;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.player.ChorusExploit;

@Mixin(ClientPacketListener.class)
public class MixinChorusPositionHandler {

    @Inject(method = "handleMovePlayer", at = @At("HEAD"), cancellable = true)
    private void onHandleMovePlayer(ClientboundPlayerPositionPacket packet, CallbackInfo ci) {
        if (!ChorusExploit.itz().shouldCapturePosition(System.currentTimeMillis())) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (mc.player.getCooldowns().isOnCooldown(new ItemStack(Items.CHORUS_FRUIT))) {
            net.minecraft.world.phys.Vec3 pos = packet.change().position();
            ChorusExploit.itz().storeTarget(
                    pos.x, pos.y, pos.z, packet.id());
            ci.cancel();
        }
    }
}
