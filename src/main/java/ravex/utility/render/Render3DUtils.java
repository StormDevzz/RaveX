package ravex.utility.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import org.joml.Matrix4f;

public class Render3DUtils {
    private static final ByteBufferBuilder ALLOCATOR = new ByteBufferBuilder(RenderType.SMALL_BUFFER_SIZE);

    private static final RenderType FILL_TYPE = RenderTypes.debugFilledBox();
    private static final RenderType LINE_TYPE = RenderTypes.lines();

    public static void renderFilledBox(Matrix4f matrix, double size, float r, float g, float b, float a) {
        BufferBuilder builder = new BufferBuilder(ALLOCATOR, FILL_TYPE.mode(), FILL_TYPE.format());
        BlockRenderer.renderFilledBoxQuads(builder, matrix, size, r, g, b, a);
        MeshData mesh = builder.buildOrThrow();
        FILL_TYPE.draw(mesh);
    }

    public static void renderWireframe(Matrix4f matrix, double size, float r, float g, float b, float a) {
        BufferBuilder builder = new BufferBuilder(ALLOCATOR, LINE_TYPE.mode(), LINE_TYPE.format());
        BlockRenderer.renderWireframe(builder, matrix, size, r, g, b, a);
        MeshData mesh = builder.buildOrThrow();
        LINE_TYPE.draw(mesh);
    }
}
