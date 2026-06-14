package ravex.utility.render;

import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

import java.util.Optional;
import org.joml.Matrix4f;
import ravex.mixin.render.AccessorRenderType;

import java.util.List;

public class Render3DUtils {
    private static final ByteBufferBuilder ALLOCATOR = new ByteBufferBuilder(RenderType.SMALL_BUFFER_SIZE);

    private static final RenderType FILL_TYPE = RenderTypes.debugFilledBox();
    private static final RenderType LINE_TYPE = RenderTypes.lines();

    private static RenderType fillNoDepth;
    private static RenderType lineNoDepth;

    private static void ensureNoDepthTypes() {
        if (fillNoDepth != null) return;

        RenderPipeline linesPipe = RenderPipelines.LINES;
        RenderPipeline fillPipe = RenderPipelines.DEBUG_FILLED_BOX;

        fillNoDepth = buildNoDepthType(fillPipe, "ravex_fill_nodepth");
        lineNoDepth = buildNoDepthType(linesPipe, "ravex_line_nodepth");
    }

    private static RenderType buildNoDepthType(RenderPipeline source, String name) {
        Identifier id = Identifier.withDefaultNamespace(name);
        RenderPipeline.Builder builder = RenderPipeline.builder()
            .withLocation(id)
            .withVertexShader(source.getVertexShader())
            .withFragmentShader(source.getFragmentShader())
            .withVertexFormat(source.getVertexFormat(), source.getVertexFormatMode())
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withCull(source.isCull())
            .withDepthBias(source.getDepthBiasScaleFactor(), source.getDepthBiasConstant());

        Optional<com.mojang.blaze3d.pipeline.BlendFunction> blend = source.getBlendFunction();
        if (blend.isPresent()) {
            builder = builder.withBlend(blend.get());
        } else {
            builder = builder.withoutBlend();
        }

        RenderPipeline newPipe = builder.build();

        RenderSetup setup = RenderSetup.builder(newPipe).createRenderSetup();
        return AccessorRenderType.invokeCreate(name, setup);
    }

    public static void renderFilledBox(Matrix4f matrix, double size, float r, float g, float b, float a) {
        renderFilledBox(matrix, size, r, g, b, a, false);
    }

    public static void renderFilledBox(Matrix4f matrix, double size, float r, float g, float b, float a, boolean throughWalls) {
        ensureNoDepthTypes();
        RenderType type = throughWalls ? fillNoDepth : FILL_TYPE;
        BufferBuilder builder = new BufferBuilder(ALLOCATOR, type.mode(), type.format());
        BlockRenderer.renderFilledBoxQuads(builder, matrix, size, r, g, b, a);
        MeshData mesh = builder.buildOrThrow();
        type.draw(mesh);
    }

    public static void renderWireframe(Matrix4f matrix, double size, float r, float g, float b, float a) {
        renderWireframe(matrix, size, r, g, b, a, 1.0f, false);
    }

    public static void renderWireframe(Matrix4f matrix, double size, float r, float g, float b, float a, float lineWidth) {
        renderWireframe(matrix, size, r, g, b, a, lineWidth, false);
    }

    public static void renderWireframe(Matrix4f matrix, double size, float r, float g, float b, float a, float lineWidth, boolean throughWalls) {
        ensureNoDepthTypes();
        RenderType type = throughWalls ? lineNoDepth : LINE_TYPE;
        if (lineWidth > 1.0f) {
            BufferBuilder builder = new BufferBuilder(ALLOCATOR, type.mode(), type.format());
            BlockRenderer.renderThickWireframe(builder, matrix, size, r, g, b, a, lineWidth);
            MeshData mesh = builder.buildOrThrow();
            type.draw(mesh);
        } else {
            BufferBuilder builder = new BufferBuilder(ALLOCATOR, type.mode(), type.format());
            BlockRenderer.renderWireframe(builder, matrix, size, r, g, b, a, lineWidth);
            MeshData mesh = builder.buildOrThrow();
            type.draw(mesh);
        }
    }

    public static void renderLineStrip(Matrix4f matrix, List<org.joml.Vector3f> points, float r, float g, float b, float a, float lineWidth) {
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
