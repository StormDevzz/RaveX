package ravex.mixin.network;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.world.FakePlayer;

@Mixin(ClientPacketListener.class)
public class MixinFakePlayerExplosion {
    @Inject(method = "handleExplosion", at = @At("HEAD"))
    private void onHandleExplosion(ClientboundExplodePacket packet, CallbackInfo ci) {
        var mc = MinecraftWrapper.getWrapper();
        mc.execute(() -> FakePlayer.onExplosion(packet.center()));
    }
}
