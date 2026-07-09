package ravex.mixin.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.mixin.network.AccessorExplodePacket;
import ravex.modules.movement.Velocity;
import java.util.Optional;

@Mixin(ClientPacketListener.class)
public class MixinExplosionVelocity {

    @Inject(method = "handleExplosion", at = @At("HEAD"))
    private void onHandleExplosion(ClientboundExplodePacket packet, CallbackInfo ci) {
        if (!Velocity.maybeEnabled() || !Velocity.itz().explosion.getValue()) return;

        Optional<Vec3> kb = packet.playerKnockback();
        if (kb.isEmpty()) return;

        String mode = Velocity.itz().mode.getValue();
        double h = Velocity.itz().horizontal.getValue();
        double v = Velocity.itz().vertical.getValue();

        Vec3 original = kb.get();
        Vec3 modified;

        switch (mode) {
            case "Cancel" -> modified = Vec3.ZERO;
            case "Matrix" -> {
                double noise = (Math.random() - 0.5) * 0.015;
                modified = new Vec3(original.x * h + noise, original.y * v, original.z * h + noise);
            }
            case "NCP" -> modified = new Vec3(original.x * h, original.y, original.z * h);
            case "Grim" -> modified = new Vec3(original.x * 0.1, original.y * 0.0, original.z * 0.1);
            case "GrimStrict" -> {
                double grimH = Velocity.itz().grimHorizontal.getValue() / 100.0;
                double grimV = Velocity.itz().grimVertical.getValue() / 100.0;
                modified = new Vec3(original.x * (1.0 - grimH), original.y * (1.0 - grimV), original.z * (1.0 - grimH));
            }
            default -> { return; }
        }

        ((AccessorExplodePacket) (Object) packet).setPlayerKnockback(Optional.of(modified));
    }
}
