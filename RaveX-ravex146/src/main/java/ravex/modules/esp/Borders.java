package ravex.modules.esp;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.core.BlockPos;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.NumberParameter;

public class Borders extends Module {
    public static final Borders INSTANCE = new Borders();

    public final BooleanParameter showChunkBorders = new BooleanParameter("ChunkBorders", true);
    public final BooleanParameter showCurrentChunk = new BooleanParameter("CurrentChunk", true);
    public final ColorParameter chunkColor = new ColorParameter("ChunkColor", 0x55FFFFFF);
    public final ColorParameter currentColor = new ColorParameter("CurrentColor", 0x55FF5500);
    public final NumberParameter lineWidth = new NumberParameter("LineWidth", 1.5, 0.5, 5.0, 0.5);
    public final NumberParameter renderDistance = new NumberParameter("RenderDist", 64, 16, 128, 16);
    private ChunkPos lastChunk;

    private Borders() {
        super("Borders", Category.RENDER);
        addParameter(showChunkBorders);
        addParameter(showCurrentChunk);
        addParameter(chunkColor);
        addParameter(currentColor);
        addParameter(lineWidth);
        addParameter(renderDistance);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        lastChunk = mc.player.chunkPosition();
    }

    public ChunkPos getCurrentChunk() {
        return lastChunk;
    }

}
