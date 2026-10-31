package ravex.mixin.movement;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.Modules;
import ravex.utility.player.PlayerUtility;

@Mixin(ClientPacketListener.class)
public class MixinNoRotate {
    private float ravexSavedRotYaw;
    private float ravexSavedRotPitch;
    private boolean ravexSavingRot;

    @Inject(method = "handleMovePlayer", at = @At("HEAD"))
    private void onHandleMovePlayerHead(ClientboundPlayerPositionPacket packet, CallbackInfo ci) {
        // silent mode: ignore server rotations
        if (!Modules.enabled(ravex.modules.world.Scaffold.class)) return;
        if (!ravex.modules.world.Scaffold.noRotateView()) return;
        var p = PlayerUtility.getPlayer();
        if (p == null) return;
        ravexSavedRotYaw = p.getYRot();
        ravexSavedRotPitch = p.getXRot();
        ravexSavingRot = true;
    }

    @Inject(method = "handleMovePlayer", at = @At("TAIL"))
    private void onHandleMovePlayerTail(ClientboundPlayerPositionPacket packet, CallbackInfo ci) {
        if (!ravexSavingRot) return;
        ravexSavingRot = false;
        var p = PlayerUtility.getPlayer();
        if (p == null) return;
        p.setYRot(ravexSavedRotYaw);
        p.setXRot(ravexSavedRotPitch);
    }
}
