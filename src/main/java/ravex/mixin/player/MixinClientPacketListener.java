package ravex.mixin.player;

import net.minecraft.client.multiplayer.ClientPacketListener;
import ravex.mcwrapper.MinecraftWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.misc.ChatHelper;
import ravex.modules.movement.HighJump;
import ravex.modules.Modules;

@Mixin(ClientPacketListener.class)
public class MixinClientPacketListener {

    @ModifyVariable(method = "sendChat", at = @At("HEAD"), argsOnly = true)
    private String modifyChatMessage(String message) {
        ChatHelper ch = Modules.get(ChatHelper.class);
        if (!Modules.enabled(ChatHelper.class) || !ch.zov) return message;
        return ch.applyZov(message);
    }

    @Inject(method = "handleOpenScreen", at = @At("HEAD"), cancellable = true)
    private void onHandleOpenScreen(net.minecraft.network.protocol.game.ClientboundOpenScreenPacket packet, CallbackInfo ci) {
        if (Modules.enabled(HighJump.class) && "GrimShulker".equals(Modules.get(HighJump.class).mode)) {
            if (packet.getType() == net.minecraft.world.inventory.MenuType.SHULKER_BOX) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "handleAnimate", at = @At("HEAD"), cancellable = true)
    private void onHandleAnimate(net.minecraft.network.protocol.game.ClientboundAnimatePacket packet, CallbackInfo ci) {
        if (packet.getAction() == net.minecraft.network.protocol.game.ClientboundAnimatePacket.CRITICAL_HIT
                || packet.getAction() == net.minecraft.network.protocol.game.ClientboundAnimatePacket.MAGIC_CRITICAL_HIT) {
            ravex.modules.combat.KillAura.onCritSound(System.currentTimeMillis());
        }
        net.minecraft.client.Minecraft mc = MinecraftWrapper.getInstance();
        if (mc.player != null && packet.getId() == mc.player.getId()) {
            if (packet.getAction() == 0 || packet.getAction() == 3) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "handleEntityEvent", at = @At("HEAD"))
    private void onTotemEvent(net.minecraft.network.protocol.game.ClientboundEntityEventPacket packet, CallbackInfo ci) {
        // event 35 is the totem pop
        if (packet.getEventId() != 35) return;
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getLevel() == null) return;
        if (packet.getEntity(mc.getLevel()) == mc.getPlayer()) {
            ravex.modules.render.Animations.totemPopNs = System.nanoTime();
        }
    }

    @Inject(method = "handleSetEntityData", at = @At("RETURN"))
    private void onSetEntityData(net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket packet, CallbackInfo ci) {
        // catch health 0 at apply time, the tick loop misses it
        var mc = MinecraftWrapper.getWrapper();
        var level = mc.getLevel();
        if (level == null) return;
        var entity = level.getEntity(packet.id());
        if (entity instanceof net.minecraft.world.entity.player.Player player && player.isDeadOrDying()) {
            ravex.modules.render.Animations.onVictimDied(player);
        }
    }

    @Inject(method = "handleBlockDestruction", at = @At("HEAD"))
    private void onBlockDestruction(net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket packet, CallbackInfo ci) {
        // server dig progress drives PacketMine timing
        ravex.modules.player.PacketMine.onServerDigProgress(packet.getId(), packet.getPos(), packet.getProgress());
    }
}

