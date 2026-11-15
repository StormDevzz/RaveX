package ravex.mixin.movement;

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
import ravex.modules.Modules;

@Mixin(ClientPacketListener.class)
public class MixinExplosionVelocity {

    private Vec3 preExplosionMotion = null;

    @Inject(method = "handleExplosion", at = @At("HEAD"))
    private void onHandleExplosion(ClientboundExplodePacket packet, CallbackInfo ci) {
        if (!Modules.enabled(Velocity.class) || !Modules.get(Velocity.class).explosion) return;

        var player = ravex.mcwrapper.MinecraftWrapper.getInstance().player;
        if (player != null) {
            preExplosionMotion = player.getDeltaMovement();
        }

        Optional<Vec3> kb = packet.playerKnockback();
        if (kb.isEmpty()) return;

        String mode = Modules.get(Velocity.class).mode;
        double h = Modules.get(Velocity.class).horizontal;
        double v = Modules.get(Velocity.class).vertical;

        if ("Cancel".equals(mode) || (h == 0.0 && v == 0.0)) {
            ((AccessorExplodePacket) (Object) packet).setPlayerKnockback(Optional.empty());
            return;
        }

        Vec3 original = kb.get();
        Vec3 modified;

        switch (mode) {
            case "Cancel" -> {
                ((AccessorExplodePacket) (Object) packet).setPlayerKnockback(Optional.empty());
                return;
            }
            case "Matrix" -> {
                double noise = (Math.random() - 0.5) * 0.015;
                modified = new Vec3(original.x * h + noise, original.y * v, original.z * h + noise);
            }
            case "NCP" -> modified = new Vec3(original.x * h, original.y, original.z * h);
            case "Grim" -> modified = new Vec3(original.x * 0.1, original.y * 0.0, original.z * 0.1);
            case "GrimStrict" -> {
                double grimH = Modules.get(Velocity.class).grimHorizontal / 100.0;
                double grimV = Modules.get(Velocity.class).grimVertical / 100.0;
                modified = new Vec3(original.x * (1.0 - grimH), original.y * (1.0 - grimV), original.z * (1.0 - grimH));
            }
            default -> { return; }
        }

        ((AccessorExplodePacket) (Object) packet).setPlayerKnockback(Optional.of(modified));
    }

    @Inject(method = "handleExplosion", at = @At("TAIL"))
    private void onHandleExplosionTail(ClientboundExplodePacket packet, CallbackInfo ci) {
        if (!Modules.enabled(Velocity.class) || !Modules.get(Velocity.class).explosion) return;
        String mode = Modules.get(Velocity.class).mode;
        double h = Modules.get(Velocity.class).horizontal;
        double v = Modules.get(Velocity.class).vertical;
        if ("Cancel".equals(mode) || (h == 0.0 && v == 0.0)) {
            var player = ravex.mcwrapper.MinecraftWrapper.getInstance().player;
            if (player != null && preExplosionMotion != null) {
                // Completely restore player's movement so zero explosion knockback affects jumping or running
                player.setDeltaMovement(preExplosionMotion);
            }
        }
        preExplosionMotion = null;
    }
}
