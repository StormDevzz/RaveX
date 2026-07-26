package ravex.modules.render;

import ravex.modules.annotations.ModuleInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.core.BlockPos;

import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.NumberParameter;
@ModuleInfo(name = "Borders", category = "Render")
public class Borders extends ravex.modules.Module {
public final BooleanParameter showChunkBorders = new BooleanParameter("ChunkBorders", true);
    public final BooleanParameter showCurrentChunk = new BooleanParameter("CurrentChunk", true);
    public final ColorParameter chunkColor = new ColorParameter("ChunkColor", 0x55FFFFFF);
    public final ColorParameter currentColor = new ColorParameter("CurrentColor", 0x55FF5500);
    public final NumberParameter lineWidth = new NumberParameter("LineWidth", 1.5, 0.5, 5.0, 0.5);
    public final NumberParameter renderDistance = new NumberParameter("RenderDist", 64, 16, 128, 16);
    private ChunkPos lastChunk;
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        lastChunk = mc.player.chunkPosition();
    }
    public ChunkPos getCurrentChunk() {
        return lastChunk;
    }
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("Borders").getEnabled();
    }

    public static Borders itz() {
        return ravex.manager.ModuleManager.delegate(Borders.class);
    }

    public java.util.List<ravex.parameter.Parameter<?>> getParameters() {
        java.util.List<ravex.parameter.Parameter<?>> list = new java.util.ArrayList<>();
        for (java.lang.reflect.Field field : getClass().getDeclaredFields()) {
            if (ravex.parameter.Parameter.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    list.add((ravex.parameter.Parameter<?>) field.get(this));
                } catch (Exception ignored) {}
            }
        }
        return list;
    }
}