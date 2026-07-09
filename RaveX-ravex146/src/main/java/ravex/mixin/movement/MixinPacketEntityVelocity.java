package ravex.mixin.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.movement.Velocity;

@Mixin(ClientPacketListener.class)
public class MixinPacketEntityVelocity {

    @Inject(method = "handleSetEntityMotion", at = @At("HEAD"), cancellable = true)
    private void onHandleSetEntityMotion(ClientboundSetEntityMotionPacket packet, CallbackInfo ci) {
        if (!Velocity.INSTANCE.getEnabled()) return;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || packet.getId() != player.getId()) return;

        String mode = Velocity.INSTANCE.mode.getValue();
        double h = Velocity.INSTANCE.horizontal.getValue();
        double v = Velocity.INSTANCE.vertical.getValue();

        Vec3 velocity = new Vec3(packet.getXa(), packet.getYa(), packet.getZa());

        switch (mode) {
            case "Cancel" -> {
                player.setDeltaMovement(Vec3.ZERO);
                ci.cancel();
            }
            case "Matrix" -> {
                double noise = (Math.random() - 0.5) * 0.015;
                Vec3 modified = new Vec3(velocity.x * h + noise, velocity.y * v, velocity.z * h + noise);
                player.setDeltaMovement(modified);
                ci.cancel();
            }
            case "NCP" -> {
                Vec3 modified = new Vec3(velocity.x * h, velocity.y, velocity.z * h);
                player.setDeltaMovement(modified);
                ci.cancel();
            }
        }
    }
}
