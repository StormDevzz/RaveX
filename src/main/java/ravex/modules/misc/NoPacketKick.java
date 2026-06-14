package ravex.modules.misc;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.NumberParameter;
import ravex.parameter.BooleanParameter;
import ravex.utility.misc.NativeLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Queue;

public class NoPacketKick extends Module {
    public static final NoPacketKick INSTANCE = new NoPacketKick();

    public final NumberParameter packetsPerSec = new NumberParameter("Packets/s", 80, 10, 500, 5);
    public final NumberParameter burst = new NumberParameter("Burst", 15, 5, 50, 1);
    public final BooleanParameter filterMove = new BooleanParameter("Filter Move", true);
    public final BooleanParameter filterInteract = new BooleanParameter("Filter Interact", false);
    public final BooleanParameter filterChat = new BooleanParameter("Filter Chat", false);

    private final Queue<Packet<?>> pendingPackets = new ConcurrentLinkedQueue<>();

    private NoPacketKick() {
        super("NoPacketKick", Category.MISC);
        addParameter(packetsPerSec);
        addParameter(burst);
        addParameter(filterMove);
        addParameter(filterInteract);
        addParameter(filterChat);
    }

    static {
        NativeLoader.load();
    }

    @Override
    protected void onEnable() {
        try {
            nativeInit(packetsPerSec.getValue().intValue(), burst.getValue().intValue());
        } catch (UnsatisfiedLinkError ignored) {}
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

        try {
            return nativeShouldAllow();
        } catch (UnsatisfiedLinkError e) {
            return true;
        }
    }

    private native void nativeInit(int packetsPerSec, int burst);
    private native boolean nativeShouldAllow();
    private native int nativeGetRate();
    private native void nativeReset();
}
