package ravex.modules.render;
<<<<<<< HEAD
import ravex.manager.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.core.BlockPos;
=======
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.core.BlockPos;
import ravex.modules.Category;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.NumberParameter;
public class Borders extends Module {
<<<<<<< HEAD
=======
    public static final Borders INSTANCE = new Borders();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    public final BooleanParameter showChunkBorders = new BooleanParameter("ChunkBorders", true);
    public final BooleanParameter showCurrentChunk = new BooleanParameter("CurrentChunk", true);
    public final ColorParameter chunkColor = new ColorParameter("ChunkColor", 0x55FFFFFF);
    public final ColorParameter currentColor = new ColorParameter("CurrentColor", 0x55FF5500);
    public final NumberParameter lineWidth = new NumberParameter("LineWidth", 1.5, 0.5, 5.0, 0.5);
    public final NumberParameter renderDistance = new NumberParameter("RenderDist", 64, 16, 128, 16);
    private ChunkPos lastChunk;

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        lastChunk = mc.player.chunkPosition();
    }
    public ChunkPos getCurrentChunk() {
        return lastChunk;
    }
<<<<<<< HEAD
    public static boolean maybeEnabled() {
        return maybeEnabled(Borders.class);
    }

    public static Borders itz() {
        return ModuleManager.get(Borders.class);
    }
=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
}
