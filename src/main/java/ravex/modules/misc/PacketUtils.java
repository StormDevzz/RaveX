package ravex.modules.misc;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
public class PacketUtils extends Module {
    public static final PacketUtils INSTANCE = new PacketUtils();
    public final BooleanParameter logOutgoing = new BooleanParameter("Log Outgoing", true);
    public final BooleanParameter logIncoming = new BooleanParameter("Log Incoming", false);
    public final BooleanParameter logToChat = new BooleanParameter("Log To Chat", true);
    public final NumberParameter rateLimit = new NumberParameter("Rate Limit", 80, 10, 500, 5);
    public final NumberParameter burst = new NumberParameter("Burst", 15, 5, 50, 1);
    public final BooleanParameter filterMove = new BooleanParameter("Filter Move", true);
    public final BooleanParameter filterInteract = new BooleanParameter("Filter Interact", false);
    public final BooleanParameter filterChat = new BooleanParameter("Filter Chat", false);
    public final BooleanParameter cancelMove = new BooleanParameter("Cancel Move (C2S)", false);
    public final BooleanParameter cancelInput = new BooleanParameter("Cancel Input (C2S)", false);
    public final BooleanParameter cancelInteract = new BooleanParameter("Cancel Interact (C2S)", false);
    public final BooleanParameter cancelSwing = new BooleanParameter("Cancel Swing (C2S)", false);
    public final BooleanParameter cancelUse = new BooleanParameter("Cancel Use (C2S)", false);

    public void logPacket(String direction, Object packet) {
        if (!getEnabled()) return;
        String name = packet.getClass().getSimpleName();
        String message = "§7[§6Packet§7] §d" + direction + " §e" + name;
        Minecraft mc = Minecraft.getInstance();
        if (logToChat.getValue() && mc.player != null) {
            mc.player.displayClientMessage(Component.literal(message), false);
        } else {
            System.out.println("[PacketUtils] " + direction + " " + packet.getClass().getName());
        }
    }
    public boolean shouldAllow(Packet<?> packet) {
        if (!getEnabled()) return true;
        if (packet instanceof ServerboundMovePlayerPacket && !filterMove.getValue()) return true;
        if (packet instanceof ServerboundInteractPacket && !filterInteract.getValue()) return true;
        if (packet instanceof ServerboundSwingPacket && !filterInteract.getValue()) return true;
        if (packet instanceof ServerboundUseItemPacket && !filterInteract.getValue()) return true;
        if (packet instanceof ServerboundUseItemOnPacket && !filterInteract.getValue()) return true;
        if (packet instanceof ServerboundChatPacket && !filterChat.getValue()) return true;
        if (packet instanceof ServerboundChatCommandPacket && !filterChat.getValue()) return true;
        return true;
    }
    public boolean shouldCancel(Packet<?> packet) {
        if (!getEnabled()) return false;
        if (packet instanceof ServerboundMovePlayerPacket && cancelMove.getValue()) return true;
        if (packet instanceof ServerboundPlayerInputPacket && cancelInput.getValue()) return true;
        if (packet instanceof ServerboundInteractPacket && cancelInteract.getValue()) return true;
        if (packet instanceof ServerboundSwingPacket && cancelSwing.getValue()) return true;
        if (packet instanceof ServerboundUseItemPacket && cancelUse.getValue()) return true;
        if (packet instanceof ServerboundUseItemOnPacket && cancelUse.getValue()) return true;
        return false;
    }
}
