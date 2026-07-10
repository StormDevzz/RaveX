package ravex.mixin.network;

import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.modules.exploit.AntiHunger;
import ravex.modules.exploit.PacketCanceller;
import ravex.modules.misc.PacketLogger;
import ravex.modules.misc.NoPacketKick;
import ravex.modules.exploit.HandshakeSpoof;
import io.netty.channel.ChannelHandlerContext;

@Mixin(Connection.class)
public class MixinConnection {
    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void onSendPacket(Packet<?> packet, CallbackInfo ci) {
        if (packet instanceof ServerboundInteractPacket interactPacket) {
            int entityId = ((AccessorServerboundInteractPacket) interactPacket).getEntityId();
            if (entityId == -9999) {
                ci.cancel();
                return;
            }
        }

        if (packet instanceof ClientIntentionPacket handshakePacket) {
            if (HandshakeSpoof.INSTANCE.getEnabled()) {
                AccessorClientIntentionPacket accessor = (AccessorClientIntentionPacket) (Object) handshakePacket;
                String originalHost = handshakePacket.hostName();
                int originalProtocol = handshakePacket.protocolVersion();

                String spoofedHost = HandshakeSpoof.INSTANCE.getSpoofedHost(originalHost);
                int spoofedProtocol = HandshakeSpoof.INSTANCE.getSpoofedProtocol(originalProtocol);

                accessor.setHostName(spoofedHost);
                accessor.setProtocolVersion(spoofedProtocol);
            }
        }

        if (NoPacketKick.INSTANCE.getEnabled() && !NoPacketKick.INSTANCE.shouldAllow(packet)) {
            ci.cancel();
            return;
        }

        if (PacketLogger.INSTANCE.getEnabled() && PacketLogger.INSTANCE.outgoing.getValue()) {
            PacketLogger.INSTANCE.logPacket("C2S ->", packet);
        }

        if (packet instanceof ServerboundUseItemPacket usePacket) {
            if (ravex.modules.exploit.FakePearl.INSTANCE.getEnabled()) {
                String trg = ravex.modules.exploit.FakePearl.INSTANCE.trigger.getValue();
                if ("Right Click".equals(trg) || "Both".equals(trg)) {
                    net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                    if (mc.player != null && mc.player.getItemInHand(usePacket.getHand()).is(net.minecraft.world.item.Items.ENDER_PEARL)) {
                        ci.cancel();
                        ravex.modules.exploit.FakePearl.INSTANCE.throwFakePearl();
                        mc.player.swing(usePacket.getHand());
                        return;
                    }
                }
            }
        }

        if (packet instanceof ServerboundUseItemPacket usePacket) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player != null && mc.player.getItemInHand(usePacket.getHand()).is(net.minecraft.world.item.Items.ENDER_PEARL)) {
                if (ravex.modules.exploit.Phase.INSTANCE.getEnabled()) {
                    ravex.modules.exploit.Phase.INSTANCE.clip();
                }
            }
        }

        if (packet instanceof ServerboundSwingPacket) {
            if (ravex.modules.exploit.NoMineAnimation.INSTANCE.getEnabled() && ravex.modules.exploit.NoMineAnimation.INSTANCE.hideSwing.getValue()) {
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                if (mc.gameMode != null && mc.gameMode.isDestroying()) {
                    ci.cancel();
                    return;
                }
            }
        }

        if (ravex.modules.exploit.RaytraceBypass.INSTANCE.getEnabled()) {
            net.minecraft.core.BlockPos pos = null;
            if (packet instanceof ServerboundPlayerActionPacket actionPacket && actionPacket.getAction() == ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK) {
                pos = actionPacket.getPos();
            } else if (packet instanceof ServerboundUseItemOnPacket useOnPacket) {
                pos = useOnPacket.getHitResult().getBlockPos();
            }

            if (pos != null) {
                float[] rot = ravex.modules.exploit.RaytraceBypass.INSTANCE.getBypassRotations(pos.getX(), pos.getY(), pos.getZ());
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                if (mc.player != null && mc.getConnection() != null) {
                    if (ravex.modules.exploit.RaytraceBypass.INSTANCE.silent.getValue()) {
                        mc.getConnection().send(new ServerboundMovePlayerPacket.Rot(rot[0], rot[1], mc.player.onGround(), mc.player.horizontalCollision));
                    } else {
                        mc.player.setYRot(rot[0]);
                        mc.player.setXRot(rot[1]);
                    }
                }
            }
        }

        if (ravex.modules.exploit.PortalGodMode.INSTANCE.getEnabled()) {
            if (packet instanceof ServerboundAcceptTeleportationPacket) {
                ci.cancel();
                return;
            }
        }

        if (PacketCanceller.INSTANCE.getEnabled()) {
            boolean cancel = false;
            if (packet instanceof ServerboundMovePlayerPacket && PacketCanceller.INSTANCE.move.getValue()) cancel = true;
            if (packet instanceof ServerboundPlayerInputPacket && PacketCanceller.INSTANCE.input.getValue()) cancel = true;
            if (packet instanceof ServerboundInteractPacket && PacketCanceller.INSTANCE.interact.getValue()) cancel = true;
            if (packet instanceof ServerboundSwingPacket && PacketCanceller.INSTANCE.swing.getValue()) cancel = true;
            if (packet instanceof ServerboundUseItemPacket && PacketCanceller.INSTANCE.use.getValue()) cancel = true;
            if (packet instanceof ServerboundUseItemOnPacket && PacketCanceller.INSTANCE.use.getValue()) cancel = true;

            if (cancel) {
                ci.cancel();
                return;
            }
        }

        if (ravex.modules.movement.NoFall.INSTANCE.getEnabled() && packet instanceof ServerboundMovePlayerPacket movePacket) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player != null) {
                double[] outData = new double[2];
                boolean changed = ravex.modules.movement.NoFall.handleNoFall(
                    ravex.modules.movement.NoFall.INSTANCE.mode.getValue(),
                    mc.player.fallDistance,
                    movePacket.getY(mc.player.getY()),
                    movePacket.isOnGround(),
                    outData
                );
                if (changed) {
                    AccessorServerboundMovePlayerPacket accessor = (AccessorServerboundMovePlayerPacket) movePacket;
                    accessor.setOnGround(outData[0] > 0.5);
                    if (movePacket.hasPosition()) {
                        accessor.setY(outData[1]);
                    }
                }
            }
        }

        if (AntiHunger.INSTANCE.getEnabled()) {
            String currentMode = AntiHunger.INSTANCE.mode.getValue();

            if (packet instanceof ServerboundMovePlayerPacket) {
                if ("Full".equals(currentMode) || "OnGround".equals(currentMode)) {
                    ((AccessorServerboundMovePlayerPacket) packet).setOnGround(false);
                }
            }

            if (packet instanceof ServerboundPlayerCommandPacket commandPacket) {
                var action = commandPacket.getAction();
                if (action == ServerboundPlayerCommandPacket.Action.START_SPRINTING ||
                    action == ServerboundPlayerCommandPacket.Action.STOP_SPRINTING) {
                    if ("Full".equals(currentMode) || "Sprint".equals(currentMode)) {
                        ci.cancel();
                    }
                }
            }
        }
    }

    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"))
    private void onChannelRead(ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
        ravex.modules.exploit.NewChunks.INSTANCE.onPacketReceive(packet);

        if (PacketLogger.INSTANCE.getEnabled() && PacketLogger.INSTANCE.incoming.getValue()) {
            PacketLogger.INSTANCE.logPacket("S2C <-", packet);
        }

        if (ravex.modules.world.NoGhostBlocks.INSTANCE.getEnabled()) {
            if (packet instanceof ClientboundBlockUpdatePacket blockUpdate) {
                net.minecraft.core.BlockPos pos = blockUpdate.getPos();
                String blockId = ravex.modules.world.NoGhostBlocks.getBlockId(blockUpdate.getBlockState());
                ravex.modules.world.NoGhostBlocks.onServerBlockUpdate(pos.getX(), pos.getY(), pos.getZ(), blockId);
            } else if (packet instanceof ClientboundSectionBlocksUpdatePacket sectionUpdate) {
                sectionUpdate.runUpdates((pos, state) -> {
                    String blockId = ravex.modules.world.NoGhostBlocks.getBlockId(state);
                    ravex.modules.world.NoGhostBlocks.onServerBlockUpdate(pos.getX(), pos.getY(), pos.getZ(), blockId);
                });
            }
        }
    }
}
