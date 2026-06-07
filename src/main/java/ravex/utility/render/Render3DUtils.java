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
        renderWireframe(matrix, size, r, g, b, a, 1.0f);
    }

    public static void renderWireframe(Matrix4f matrix, double size, float r, float g, float b, float a, float lineWidth) {
        if (lineWidth > 1.0f) {
            BufferBuilder builder = new BufferBuilder(ALLOCATOR, FILL_TYPE.mode(), FILL_TYPE.format());
            BlockRenderer.renderThickWireframe(builder, matrix, size, r, g, b, a, lineWidth);
            MeshData mesh = builder.buildOrThrow();
            FILL_TYPE.draw(mesh);
        } else {
            BufferBuilder builder = new BufferBuilder(ALLOCATOR, LINE_TYPE.mode(), LINE_TYPE.format());
            BlockRenderer.renderWireframe(builder, matrix, size, r, g, b, a, lineWidth);
            MeshData mesh = builder.buildOrThrow();
            LINE_TYPE.draw(mesh);
        }
    }

    public static void renderLineStrip(Matrix4f matrix, java.util.List<org.joml.Vector3f> points, float r, float g, float b, float a, float lineWidth) {
        if (points.size() < 2) return;
        BufferBuilder builder = new BufferBuilder(ALLOCATOR, LINE_TYPE.mode(), LINE_TYPE.format());
        int ir = (int)(r * 255);
        int ig = (int)(g * 255);
        int ib = (int)(b * 255);
        int ia = (int)(a * 255);
        for (int i = 1; i < points.size(); i++) {
            org.joml.Vector3f p1 = points.get(i - 1);
            org.joml.Vector3f p2 = points.get(i);
            BlockRenderer.renderLine3D(builder, matrix,
                p1.x, p1.y, p1.z,
                p2.x, p2.y, p2.z,
                ir, ig, ib, ia, lineWidth);
        }
        MeshData mesh = builder.buildOrThrow();
        LINE_TYPE.draw(mesh);
    }
}
