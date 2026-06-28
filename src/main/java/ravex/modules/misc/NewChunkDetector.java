package ravex.modules.misc;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.world.level.ChunkPos;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NewChunkDetector extends Module {
    public static final NewChunkDetector INSTANCE = new NewChunkDetector();

    public final BooleanParameter notify = new BooleanParameter("Notify Chat", true);
    public final BooleanParameter render = new BooleanParameter("Render", true);
    public final ModeParameter method = new ModeParameter("Method", "Normal", 
            List.of("Normal", "Strict"));

    private final Set<ChunkPos> newChunks = new HashSet<>();
    private final Set<ChunkPos> oldChunks = new HashSet<>();

    private NewChunkDetector() {
        super("NewChunkDetector", Category.MISC);
        addParameter(notify);
        addParameter(render);
        addParameter(method);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        
        if (newChunks.size() > 2000) newChunks.clear();
        if (oldChunks.size() > 2000) oldChunks.clear();
    }

    public void onPacketReceive(Object packet) {
        if (!this.getEnabled()) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        if (packet instanceof ClientboundLevelChunkWithLightPacket chunkPacket) {
            ChunkPos pos = new ChunkPos(chunkPacket.getX(), chunkPacket.getZ());

            if (newChunks.contains(pos) || oldChunks.contains(pos)) return;

            boolean isNewChunk = false;

            if ("Normal".equals(method.getValue())) {
                if (chunkPacket.getLightData() != null) {
                    isNewChunk = !mc.level.getChunkSource().hasChunk(pos.x, pos.z);
                }
            } else if ("Strict".equals(method.getValue())) {
                isNewChunk = !mc.level.getChunkSource().hasChunk(pos.x, pos.z);
            }

            if (isNewChunk) {
                newChunks.add(pos);
                if (notify.getValue()) {
                    // Перенаправляем отправку сообщения в главный поток игры, чтобы не было краша
                    mc.execute(() -> {
                        if (mc.player != null) {
                            mc.player.displayClientMessage(
                                Component.literal("§7[§cChunkDetector§7] §aNew chunk found at: §e" + pos.getMinBlockX() + ", " + pos.getMinBlockZ()), 
                                false
                            );
                        }
                    });
                }
            } else {
                oldChunks.add(pos);
            }
        }
    }

    @Override
    protected void onDisable() {
        newChunks.clear();
        oldChunks.clear();
    }

    public Set<ChunkPos> getNewChunks() {
        return newChunks;
    }

    public Set<ChunkPos> getOldChunks() {
        return oldChunks;
    }
}
