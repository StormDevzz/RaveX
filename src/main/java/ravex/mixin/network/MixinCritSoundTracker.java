package ravex.mixin.network;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.sounds.SoundEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.combat.KillAura;

@Mixin(ClientPacketListener.class)
public class MixinCritSoundTracker {
    @Inject(method = "handleSoundEvent", at = @At("HEAD"))
    private void onSoundEvent(ClientboundSoundPacket packet, CallbackInfo ci) {
        if (packet.getSound().value() != SoundEvents.PLAYER_ATTACK_CRIT) return;
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null) return;
        double dx = packet.getX() - mc.getPlayer().getX();
        double dy = packet.getY() - mc.getPlayer().getY();
        double dz = packet.getZ() - mc.getPlayer().getZ();
        if (dx * dx + dy * dy + dz * dz > 36.0) return;
        KillAura.onCritSound(System.currentTimeMillis());
    }

    @Inject(method = "handleSoundEntityEvent", at = @At("HEAD"))
    private void onSoundEntityEvent(net.minecraft.network.protocol.game.ClientboundSoundEntityPacket packet, CallbackInfo ci) {
        if (packet.getSound().value() != SoundEvents.PLAYER_ATTACK_CRIT) return;
        KillAura.onCritSound(System.currentTimeMillis());
    }
}
