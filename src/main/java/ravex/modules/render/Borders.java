package ravex.modules.render;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import net.minecraft.world.level.ChunkPos;
import ravex.utility.misc.block.BlockUtility;
import ravex.mcwrapper.MinecraftWrapper;

@Module(name = "Borders", category = "Render")
public class Borders {
    @Parameter(name = "ChunkBorders")
    public boolean showChunkBorders = true;
    @Parameter(name = "CurrentChunk")
    public boolean showCurrentChunk = true;
    @Parameter(name = "ChunkColor", color = true, visible = "showChunkBorders")
    public int chunkColor = 0x55FFFFFF;
    @Parameter(name = "CurrentColor", color = true, visible = "showCurrentChunk")
    public int currentColor = 0x55FF5500;
    @Parameter(name = "LineWidth", min = 0.5, max = 5.0, step = 0.5)
    public double lineWidth = 1.5;
    @Parameter(name = "RenderDist", min = 16, max = 128, step = 16)
    public double renderDistance = 64;
    private ChunkPos lastChunk;
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player == null || mc.getLevel() == null) return;
        lastChunk = player.chunkPosition();
    }
    public ChunkPos getCurrentChunk() {
        return lastChunk;
    }
}
