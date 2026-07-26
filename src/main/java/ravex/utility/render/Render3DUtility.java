package ravex.utility.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

import ravex.mixin.render.AccessorRenderType;
import ravex.utility.player.rotation.RotationUtility;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.List;
import java.util.Optional;

public class Render3DUtility {
    private static final ByteBufferBuilder ALLOCATOR = new ByteBufferBuilder(512 * 1024);
    private static final ByteBufferBuilder FILL_ALLOCATOR = new ByteBufferBuilder(256 * 1024);
    private static final ByteBufferBuilder FILL_NODEPTH_ALLOCATOR = new ByteBufferBuilder(256 * 1024);
    private static final ByteBufferBuilder LINE_ALLOCATOR = new ByteBufferBuilder(256 * 1024);
    private static final ByteBufferBuilder LINE_NODEPTH_ALLOCATOR = new ByteBufferBuilder(256 * 1024);
    private static final ByteBufferBuilder LINE_ADDITIVE_ALLOCATOR = new ByteBufferBuilder(512 * 1024);
    private static final ByteBufferBuilder LINE_ADDITIVE_NODEPTH_ALLOCATOR = new ByteBufferBuilder(512 * 1024);

    private static final RenderType FILL_TYPE = RenderTypes.debugFilledBox();
    private static final RenderType LINE_TYPE = RenderTypes.lines();

    private static final RenderType FILL_NO_DEPTH;
    private static final RenderType LINE_NO_DEPTH;
    private static final RenderType LINE_ADDITIVE;
    private static final RenderType LINE_ADDITIVE_NO_DEPTH;
    private static RenderType SOULS_ADDITIVE;

    private static final ByteBufferBuilder ESP_ALLOCATOR = new ByteBufferBuilder(256 * 1024);

    static {
        RenderType f = null, l = null, la = null, lan = null;
        try {
            f = buildNoDepthType(RenderPipelines.DEBUG_FILLED_BOX, "ravex_fill_nodepth");
            l = buildNoDepthType(RenderPipelines.LINES, "ravex_line_nodepth");
            la = buildAdditiveType(RenderPipelines.LINES, "ravex_line_additive");
            lan = buildAdditiveNoDepthType(RenderPipelines.LINES, "ravex_line_additive_nodepth");
        } catch (Exception e) {

        }
        FILL_NO_DEPTH = f;
        LINE_NO_DEPTH = l;
        LINE_ADDITIVE = la;
        LINE_ADDITIVE_NO_DEPTH = lan;
    }

    private static RenderType buildAdditiveNoDepthType(RenderPipeline source, String name) {
        return cloneWithOverride(source, name,
            b -> b.withBlend(BlendFunction.ADDITIVE)
                  .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                  .withDepthWrite(false));
    }

    private static boolean hasNoDepth() {
        return FILL_NO_DEPTH != null && LINE_NO_DEPTH != null;
    }

    private static RenderType buildNoDepthType(RenderPipeline source, String name) {
        return cloneWithOverride(source, name,
            b -> b.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withDepthWrite(false));
    }

    private static RenderType buildAdditiveType(RenderPipeline source, String name) {
        return cloneWithOverride(source, name,
            b -> b.withBlend(BlendFunction.ADDITIVE));
    }

    private static RenderType cloneWithOverride(RenderPipeline source, String name,
                                                  java.util.function.Consumer<RenderPipeline.Builder> overrider) {
        Identifier id = Identifier.withDefaultNamespace(name);
        RenderPipeline.Builder builder = RenderPipeline.builder()
            .withLocation(id)
            .withVertexShader(source.getVertexShader())
            .withFragmentShader(source.getFragmentShader())
            .withVertexFormat(source.getVertexFormat(), source.getVertexFormatMode())
            .withDepthTestFunction(source.getDepthTestFunction())
            .withDepthWrite(source.isWriteDepth())
            .withCull(source.isCull())
            .withDepthBias(source.getDepthBiasScaleFactor(), source.getDepthBiasConstant())
            .withPolygonMode(source.getPolygonMode())
            .withColorLogic(source.getColorLogic())
            .withColorWrite(source.isWriteColor(), source.isWriteAlpha());

        source.getBlendFunction().ifPresentOrElse(
            b -> builder.withBlend(b),
            () -> builder.withoutBlend()
        );

        for (String s : source.getSamplers()) {
            builder.withSampler(s);
        }
        for (RenderPipeline.UniformDescription u : source.getUniforms()) {
            builder.withUniform(u.name(), u.type());
        }

        overrider.accept(builder);

        RenderPipeline newPipe = builder.build();
        RenderSetup setup = RenderSetup.builder(newPipe).createRenderSetup();
        return AccessorRenderType.invokeCreate(name, setup);
    }





    private static BufferBuilder fillBuilder;
    private static BufferBuilder fillNoDepthBuilder;
    private static BufferBuilder lineBuilder;
    private static BufferBuilder lineNoDepthBuilder;
    private static BufferBuilder lineAdditiveBuilder;
    private static BufferBuilder lineAdditiveNoDepthBuilder;

    private static boolean fillUsed = false;
    private static boolean fillNoDepthUsed = false;
    private static boolean lineUsed = false;
    private static boolean lineNoDepthUsed = false;
    private static boolean lineAdditiveUsed = false;
    private static boolean lineAdditiveNoDepthUsed = false;

    public static void beginFrame() {
        FILL_ALLOCATOR.clear();
        fillBuilder = new BufferBuilder(FILL_ALLOCATOR, FILL_TYPE.mode(), FILL_TYPE.format());
        if (FILL_NO_DEPTH != null) {
            FILL_NODEPTH_ALLOCATOR.clear();
            fillNoDepthBuilder = new BufferBuilder(FILL_NODEPTH_ALLOCATOR, FILL_NO_DEPTH.mode(), FILL_NO_DEPTH.format());
        }
        LINE_ALLOCATOR.clear();
        lineBuilder = new BufferBuilder(LINE_ALLOCATOR, LINE_TYPE.mode(), LINE_TYPE.format());
        if (LINE_NO_DEPTH != null) {
            LINE_NODEPTH_ALLOCATOR.clear();
            lineNoDepthBuilder = new BufferBuilder(LINE_NODEPTH_ALLOCATOR, LINE_NO_DEPTH.mode(), LINE_NO_DEPTH.format());
        }
        if (LINE_ADDITIVE != null) {
            LINE_ADDITIVE_ALLOCATOR.clear();
            lineAdditiveBuilder = new BufferBuilder(LINE_ADDITIVE_ALLOCATOR, LINE_ADDITIVE.mode(), LINE_ADDITIVE.format());
        }
        if (LINE_ADDITIVE_NO_DEPTH != null) {
            LINE_ADDITIVE_NODEPTH_ALLOCATOR.clear();
            lineAdditiveNoDepthBuilder = new BufferBuilder(LINE_ADDITIVE_NODEPTH_ALLOCATOR, LINE_ADDITIVE_NO_DEPTH.mode(), LINE_ADDITIVE_NO_DEPTH.format());
        }
        fillUsed = fillNoDepthUsed = lineUsed = lineNoDepthUsed = lineAdditiveUsed = lineAdditiveNoDepthUsed = false;
    }

    public static void endFrame() {
        if (fillBuilder != null && fillUsed) {
            MeshData mesh = fillBuilder.buildOrThrow();
            FILL_TYPE.draw(mesh);
        }
        fillBuilder = null;
        if (fillNoDepthBuilder != null && fillNoDepthUsed) {
            MeshData mesh = fillNoDepthBuilder.buildOrThrow();
            FILL_NO_DEPTH.draw(mesh);
        }
        fillNoDepthBuilder = null;
        if (lineBuilder != null && lineUsed) {
            MeshData mesh = lineBuilder.buildOrThrow();
            LINE_TYPE.draw(mesh);
        }
        lineBuilder = null;
        if (lineNoDepthBuilder != null && lineNoDepthUsed) {
            MeshData mesh = lineNoDepthBuilder.buildOrThrow();
            LINE_NO_DEPTH.draw(mesh);
        }
        lineNoDepthBuilder = null;
        if (lineAdditiveBuilder != null && lineAdditiveUsed) {
            MeshData mesh = lineAdditiveBuilder.buildOrThrow();
            LINE_ADDITIVE.draw(mesh);
        }
        lineAdditiveBuilder = null;
        if (lineAdditiveNoDepthBuilder != null && lineAdditiveNoDepthUsed) {
            MeshData mesh = lineAdditiveNoDepthBuilder.buildOrThrow();
            LINE_ADDITIVE_NO_DEPTH.draw(mesh);
        }
        lineAdditiveNoDepthBuilder = null;
    }




    public static void batchFilledBox(Matrix4f matrix, double size, float r, float g, float b, float a) {
        batchFilledBox(matrix, size, r, g, b, a, false);
    }

    public static void batchFilledBox(Matrix4f matrix, double size, float r, float g, float b, float a, boolean throughWalls) {
        BufferBuilder buf;
        if (throughWalls && fillNoDepthBuilder != null) {
            buf = fillNoDepthBuilder;
            fillNoDepthUsed = true;
        } else {
            buf = fillBuilder;
            fillUsed = true;
        }
        BlockRendererUtility.renderFilledBoxQuads(buf, matrix, size, r, g, b, a);
    }

    public static void batchWireframe(Matrix4f matrix, double size, float r, float g, float b, float a) {
        batchWireframe(matrix, size, r, g, b, a, 1.0f, false);
    }

    public static void batchWireframe(Matrix4f matrix, double size, float r, float g, float b, float a, float lineWidth) {
        batchWireframe(matrix, size, r, g, b, a, lineWidth, false);
    }

    public static void batchWireframe(Matrix4f matrix, double size, float r, float g, float b, float a, float lineWidth, boolean throughWalls) {
        if (lineWidth > 1.0f) {
            BufferBuilder buf;
            if (throughWalls && fillNoDepthBuilder != null) {
                buf = fillNoDepthBuilder;
                fillNoDepthUsed = true;
            } else {
                buf = fillBuilder;
                fillUsed = true;
            }
            BlockRendererUtility.renderThickWireframe(buf, matrix, size, r, g, b, a, lineWidth);
        } else {
            BufferBuilder buf;
            if (throughWalls && lineNoDepthBuilder != null) {
                buf = lineNoDepthBuilder;
                lineNoDepthUsed = true;
            } else {
                buf = lineBuilder;
                lineUsed = true;
            }
            BlockRendererUtility.renderWireframe(buf, matrix, size, r, g, b, a, lineWidth);
        }
    }

    public static void batchAxisLine(Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, float thickness, float r, float g, float b, float a) {
        batchAxisLine(matrix, x1, y1, z1, x2, y2, z2, thickness, r, g, b, a, false);
    }

    public static void batchAxisLine(Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, float thickness, float r, float g, float b, float a, boolean throughWalls) {
        float h = thickness * 0.5f;
        float minX, maxX, minY, maxY, minZ, maxZ;
        float dx = Math.abs(x2 - x1), dy = Math.abs(y2 - y1), dz = Math.abs(z2 - z1);
        if (dx >= dy && dx >= dz) {
            minX = Math.min(x1, x2);
            maxX = Math.max(x1, x2);
            minY = Math.min(y1, y2) - h;  maxY = Math.max(y1, y2) + h;
            minZ = Math.min(z1, z2) - h;  maxZ = Math.max(z1, z2) + h;
        } else if (dy >= dz) {
            minX = Math.min(x1, x2) - h;  maxX = Math.max(x1, x2) + h;
            minY = Math.min(y1, y2);
            maxY = Math.max(y1, y2);
            minZ = Math.min(z1, z2) - h;  maxZ = Math.max(z1, z2) + h;
        } else {
            minX = Math.min(x1, x2) - h;  maxX = Math.max(x1, x2) + h;
            minY = Math.min(y1, y2) - h;  maxY = Math.max(y1, y2) + h;
            minZ = Math.min(z1, z2);
            maxZ = Math.max(z1, z2);
        }
        int ir = (int)(r * 255);
        int ig = (int)(g * 255);
        int ib = (int)(b * 255);
        int ia = (int)(a * 255);
        BufferBuilder buf;
        if (throughWalls && fillNoDepthBuilder != null) {
            buf = fillNoDepthBuilder;
            fillNoDepthUsed = true;
        } else {
            buf = fillBuilder;
            fillUsed = true;
        }
        BlockRendererUtility.renderSolidBox(buf, matrix, minX, minY, minZ, maxX, maxY, maxZ, ir, ig, ib, ia);
    }

    public static void batchLineStrip(Matrix4f matrix, List<org.joml.Vector3f> points, float r, float g, float b, float a, float lineWidth) {
        batchLineStrip(matrix, points, r, g, b, a, lineWidth, false);
    }

    public static void batchLineStrip(Matrix4f matrix, List<org.joml.Vector3f> points, float r, float g, float b, float a, float lineWidth, boolean throughWalls) {
        if (points.size() < 2) return;
        BufferBuilder buf;
        if (throughWalls && lineNoDepthBuilder != null) {
            buf = lineNoDepthBuilder;
            lineNoDepthUsed = true;
        } else {
            buf = lineBuilder;
            lineUsed = true;
        }
        int ir = (int)(r * 255);
        int ig = (int)(g * 255);
        int ib = (int)(b * 255);
        int ia = (int)(a * 255);
        for (int i = 1; i < points.size(); i++) {
            org.joml.Vector3f p1 = points.get(i - 1);
            org.joml.Vector3f p2 = points.get(i);
            BlockRendererUtility.renderLine3D(buf, matrix,
                p1.x, p1.y, p1.z,
                p2.x, p2.y, p2.z,
                ir, ig, ib, ia, lineWidth);
        }
    }

    public static void batchLineAdditive(Matrix4f matrix, List<org.joml.Vector3f> points, float r, float g, float b, float a, float lineWidth) {
        batchLineAdditive(matrix, points, r, g, b, a, lineWidth, false);
    }

    public static void batchLineAdditive(Matrix4f matrix, List<org.joml.Vector3f> points, float r, float g, float b, float a, float lineWidth, boolean throughWalls) {
        if (points.size() < 2) return;
        BufferBuilder buf;
        if (throughWalls && lineAdditiveNoDepthBuilder != null) {
            buf = lineAdditiveNoDepthBuilder;
            lineAdditiveNoDepthUsed = true;
        } else if (lineAdditiveBuilder != null) {
            buf = lineAdditiveBuilder;
            lineAdditiveUsed = true;
        } else {
            return;
        }
        int ir = (int)(r * 255);
        int ig = (int)(g * 255);
        int ib = (int)(b * 255);
        int ia = (int)(a * 255);
        for (int i = 1; i < points.size(); i++) {
            org.joml.Vector3f p1 = points.get(i - 1);
            org.joml.Vector3f p2 = points.get(i);
            BlockRendererUtility.renderLine3D(buf, matrix,
                p1.x, p1.y, p1.z,
                p2.x, p2.y, p2.z,
                ir, ig, ib, ia, lineWidth);
        }
    }




    public static void renderFilledBox(Matrix4f matrix, double size, float r, float g, float b, float a) {
        renderFilledBox(matrix, size, r, g, b, a, false);
    }

    public static void renderFilledBox(Matrix4f matrix, double size, float r, float g, float b, float a, boolean throughWalls) {
        RenderType type = (throughWalls && hasNoDepth()) ? FILL_NO_DEPTH : FILL_TYPE;
        BufferBuilder builder = new BufferBuilder(ALLOCATOR, type.mode(), type.format());
        BlockRendererUtility.renderFilledBoxQuads(builder, matrix, size, r, g, b, a);
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
        RenderType type = (throughWalls && hasNoDepth()) ? LINE_NO_DEPTH : LINE_TYPE;
        if (lineWidth > 1.0f) {
            BufferBuilder builder = new BufferBuilder(ALLOCATOR, type.mode(), type.format());
            BlockRendererUtility.renderThickWireframe(builder, matrix, size, r, g, b, a, lineWidth);
            MeshData mesh = builder.buildOrThrow();
            type.draw(mesh);
        } else {
            BufferBuilder builder = new BufferBuilder(ALLOCATOR, type.mode(), type.format());
            BlockRendererUtility.renderWireframe(builder, matrix, size, r, g, b, a, lineWidth);
            MeshData mesh = builder.buildOrThrow();
            type.draw(mesh);
        }
    }

    public static void renderAxisLine(Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, float thickness, float r, float g, float b, float a) {
        renderAxisLine(matrix, x1, y1, z1, x2, y2, z2, thickness, r, g, b, a, false);
    }

    public static void renderAxisLine(Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, float thickness, float r, float g, float b, float a, boolean throughWalls) {
        float h = thickness * 0.5f;
        float minX, maxX, minY, maxY, minZ, maxZ;
        float dx = Math.abs(x2 - x1), dy = Math.abs(y2 - y1), dz = Math.abs(z2 - z1);
        if (dx >= dy && dx >= dz) {
            minX = Math.min(x1, x2);
            maxX = Math.max(x1, x2);
            minY = Math.min(y1, y2) - h;  maxY = Math.max(y1, y2) + h;
            minZ = Math.min(z1, z2) - h;  maxZ = Math.max(z1, z2) + h;
        } else if (dy >= dz) {
            minX = Math.min(x1, x2) - h;  maxX = Math.max(x1, x2) + h;
            minY = Math.min(y1, y2);
            maxY = Math.max(y1, y2);
            minZ = Math.min(z1, z2) - h;  maxZ = Math.max(z1, z2) + h;
        } else {
            minX = Math.min(x1, x2) - h;  maxX = Math.max(x1, x2) + h;
            minY = Math.min(y1, y2) - h;  maxY = Math.max(y1, y2) + h;
            minZ = Math.min(z1, z2);
            maxZ = Math.max(z1, z2);
        }
        int ir = (int)(r * 255);
        int ig = (int)(g * 255);
        int ib = (int)(b * 255);
        int ia = (int)(a * 255);
        RenderType type = (throughWalls && hasNoDepth()) ? FILL_NO_DEPTH : FILL_TYPE;
        BufferBuilder builder = new BufferBuilder(ALLOCATOR, type.mode(), type.format());
        BlockRendererUtility.renderSolidBox(builder, matrix, minX, minY, minZ, maxX, maxY, maxZ, ir, ig, ib, ia);
        MeshData mesh = builder.buildOrThrow();
        type.draw(mesh);
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
            BlockRendererUtility.renderLine3D(builder, matrix,
                p1.x, p1.y, p1.z,
                p2.x, p2.y, p2.z,
                ir, ig, ib, ia, lineWidth);
        }
        MeshData mesh = builder.buildOrThrow();
        LINE_TYPE.draw(mesh);
    }
    public static void renderRaveXESP(Matrix4f modelViewMatrix, Camera camera, Entity target, int color, float scanProgress, float rotation, float tickDelta) {
        double tPosX = target.xo + (target.getX() - target.xo) * tickDelta - camera.position().x;
        double tPosY = target.yo + (target.getY() - target.yo) * tickDelta - camera.position().y;
        double tPosZ = target.zo + (target.getZ() - target.zo) * tickDelta - camera.position().z;

        float height = target.getBbHeight();
        float width = target.getBbWidth();

        RenderType renderType = RenderTypes.entityTranslucent(TextureLoaderUtility.getCircleWhiteTexture());
        RenderType lineType = RenderTypes.lines();

        int overlay = OverlayTexture.NO_OVERLAY;
        int light = 0xF000F0;

        double progress = scanProgress % 2.0;
        if (progress > 1.0) {
            progress = 2.0 - progress;
        }

        double ringY = tPosY + progress * height;
        float alphaFade = 0.3f + 0.7f * (float) Math.sin(progress * Math.PI);
        if (alphaFade > 0.01f) {
            Matrix4f matrix = new Matrix4f(modelViewMatrix);
            matrix.translate((float) tPosX, (float) ringY, (float) tPosZ);

            RotationUtility.rotateY(matrix, rotation);

            float radius = width * (1.1f + 0.12f * (float) Math.sin(rotation * 0.05));

            ESP_ALLOCATOR.clear();
            BufferBuilder builder = new BufferBuilder(ESP_ALLOCATOR, renderType.mode(), renderType.format());
            int colDisk = ColorUtility.applyAlpha(color, alphaFade * 0.22f);
            builder.addVertex(matrix, -radius, 0.0f, -radius).setColor(colDisk).setUv(0.0f, 0.0f).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
            builder.addVertex(matrix, -radius, 0.0f, radius).setColor(colDisk).setUv(0.0f, 1.0f).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
            builder.addVertex(matrix, radius, 0.0f, radius).setColor(colDisk).setUv(1.0f, 1.0f).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
            builder.addVertex(matrix, radius, 0.0f, -radius).setColor(colDisk).setUv(1.0f, 0.0f).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
            renderType.draw(builder.buildOrThrow());

            ESP_ALLOCATOR.clear();
            BufferBuilder lineBuilder = new BufferBuilder(ESP_ALLOCATOR, lineType.mode(), lineType.format());
            int colLine = ColorUtility.applyAlpha(color, alphaFade * 0.85f);
            int ir = ColorUtility.getRed(colLine);
            int ig = ColorUtility.getGreen(colLine);
            int ib = ColorUtility.getBlue(colLine);
            int ia = ColorUtility.getAlpha(colLine);

            int segments = 24;
            float step = (float) (2 * Math.PI / segments);
            for (int s = 0; s < segments; s++) {
                float theta1 = s * step;
                float theta2 = (s + 1) * step;

                float x1 = (float) Math.cos(theta1) * radius;
                float z1 = (float) Math.sin(theta1) * radius;
                float x2 = (float) Math.cos(theta2) * radius;
                float z2 = (float) Math.sin(theta2) * radius;

                lineBuilder.addVertex(matrix, x1, 0.0f, z1).setColor(ir, ig, ib, ia).setNormal(x2 - x1, 0, z2 - z1).setLineWidth(3.5f);
                lineBuilder.addVertex(matrix, x2, 0.0f, z2).setColor(ir, ig, ib, ia).setNormal(x2 - x1, 0, z2 - z1).setLineWidth(3.5f);
            }
            lineType.draw(lineBuilder.buildOrThrow());
        }
    }

    public static void renderSoulsESP(Matrix4f modelViewMatrix, Camera camera, Entity target, int color, int length, int factor, float shaking, float amplitude, float tickDelta) {
        double tPosX = target.xo + (target.getX() - target.xo) * tickDelta - camera.position().x;
        double tPosY = target.yo + (target.getY() - target.yo) * tickDelta - camera.position().y;
        double tPosZ = target.zo + (target.getZ() - target.zo) * tickDelta - camera.position().z;

        float iAge = target.tickCount + tickDelta;

        RenderType renderType = SOULS_ADDITIVE;
        if (renderType == null) return;

        ESP_ALLOCATOR.clear();
        BufferBuilder builder = new BufferBuilder(ESP_ALLOCATOR, renderType.mode(), renderType.format());

        int irBase = ColorUtility.getRed(color);
        int igBase = ColorUtility.getGreen(color);
        int ibBase = ColorUtility.getBlue(color);

        for (int j = 0; j < 3; j++) {
            for (int i = 0; i <= length; i++) {
                double radians = Math.toRadians((((float) i / 1.5f + iAge) * factor + (j * 120)) % (factor * 360));
                double sinQuad = Math.sin(Math.toRadians(iAge * 2.5f + i * (j + 1)) * amplitude) / shaking;

                float offset = ((float) i / length);

                Matrix4f matrix = new Matrix4f(modelViewMatrix);

                double ox = Math.cos(radians) * target.getBbWidth();
                double oy = 1.0 + sinQuad;
                double oz = Math.sin(radians) * target.getBbWidth();
                matrix.translate((float)(tPosX + ox), (float)(tPosY + oy), (float)(tPosZ + oz));

                float scale = Math.max(0.35f * offset, 0.28f);
                matrix.m00(scale);
                matrix.m01(0.0f);
                matrix.m02(0.0f);
                matrix.m10(0.0f);
                matrix.m11(scale);
                matrix.m12(0.0f);
                matrix.m20(0.0f);
                matrix.m21(0.0f);
                matrix.m22(scale);

                int ia = Math.round(offset * 220.0f);

                builder.addVertex(matrix, -1.0f, 1.0f, 0.0f).setColor(irBase, igBase, ibBase, ia).setUv(0.0f, 1.0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(0, 1, 0);
                builder.addVertex(matrix, 1.0f, 1.0f, 0.0f).setColor(irBase, igBase, ibBase, ia).setUv(1.0f, 1.0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(0, 1, 0);
                builder.addVertex(matrix, 1.0f, -1.0f, 0.0f).setColor(irBase, igBase, ibBase, ia).setUv(1.0f, 0.0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(0, 1, 0);
                builder.addVertex(matrix, -1.0f, -1.0f, 0.0f).setColor(irBase, igBase, ibBase, ia).setUv(0.0f, 0.0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(0, 1, 0);
            }
        }

        renderType.draw(builder.buildOrThrow());
    }

    public static void renderCircleESP(Matrix4f modelViewMatrix, Camera camera, Entity target, int color, float circleStep, float prevCircleStep, float tickDelta) {
        double cs = prevCircleStep + (circleStep - prevCircleStep) * tickDelta;
        double prevSinAnim = Math.abs(1.0 + Math.sin(cs - 0.45)) / 2.0;
        double sinAnim = Math.abs(1.0 + Math.sin(cs)) / 2.0;

        double tPosX = target.xo + (target.getX() - target.xo) * tickDelta - camera.position().x;
        double tPosY = target.yo + (target.getY() - target.yo) * tickDelta - camera.position().y;
        double tPosZ = target.zo + (target.getZ() - target.zo) * tickDelta - camera.position().z;

        double height = target.getBbHeight();
        double rWidth = target.getBbWidth() * 0.8;

        double yBottom = tPosY + prevSinAnim * height;
        double yTop = tPosY + sinAnim * height;

        int ir = ColorUtility.getRed(color);
        int ig = ColorUtility.getGreen(color);
        int ib = ColorUtility.getBlue(color);

        Matrix4f matrix = new Matrix4f(modelViewMatrix);

        RenderType renderType = FILL_NO_DEPTH != null ? FILL_NO_DEPTH : RenderTypes.debugFilledBox();
        ESP_ALLOCATOR.clear();
        BufferBuilder builder = new BufferBuilder(ESP_ALLOCATOR, renderType.mode(), renderType.format());

        int segments = 30;
        for (int i = 0; i < segments; i++) {
            double angle1 = (i * 2.0 * Math.PI) / segments;
            double angle2 = ((i + 1) * 2.0 * Math.PI) / segments;

            float cos1 = (float) (Math.cos(angle1) * rWidth);
            float sin1 = (float) (Math.sin(angle1) * rWidth);
            float cos2 = (float) (Math.cos(angle2) * rWidth);
            float sin2 = (float) (Math.sin(angle2) * rWidth);

            builder.addVertex(matrix, (float)(tPosX + cos1), (float)yBottom, (float)(tPosZ + sin1)).setColor(ir, ig, ib, 0);
            builder.addVertex(matrix, (float)(tPosX + cos2), (float)yBottom, (float)(tPosZ + sin2)).setColor(ir, ig, ib, 0);
            builder.addVertex(matrix, (float)(tPosX + cos2), (float)yTop, (float)(tPosZ + sin2)).setColor(ir, ig, ib, 170);
            builder.addVertex(matrix, (float)(tPosX + cos1), (float)yTop, (float)(tPosZ + sin1)).setColor(ir, ig, ib, 170);
        }
        renderType.draw(builder.buildOrThrow());

        RenderType lineType = LINE_NO_DEPTH != null ? LINE_NO_DEPTH : RenderTypes.lines();
        ESP_ALLOCATOR.clear();
        BufferBuilder lineBuilder = new BufferBuilder(ESP_ALLOCATOR, lineType.mode(), lineType.format());

        for (int i = 0; i < segments; i++) {
            double angle1 = (i * 2.0 * Math.PI) / segments;
            double angle2 = ((i + 1) * 2.0 * Math.PI) / segments;

            float cos1 = (float) (Math.cos(angle1) * rWidth);
            float sin1 = (float) (Math.sin(angle1) * rWidth);
            float cos2 = (float) (Math.cos(angle2) * rWidth);
            float sin2 = (float) (Math.sin(angle2) * rWidth);

            float nx = cos2 - cos1;
            float nz = sin2 - sin1;

            lineBuilder.addVertex(matrix, (float)(tPosX + cos1), (float)yTop, (float)(tPosZ + sin1)).setColor(ir, ig, ib, 230).setNormal(nx, 0, nz).setLineWidth(4.0f);
            lineBuilder.addVertex(matrix, (float)(tPosX + cos2), (float)yTop, (float)(tPosZ + sin2)).setColor(ir, ig, ib, 230).setNormal(nx, 0, nz).setLineWidth(4.0f);
        }
        lineType.draw(lineBuilder.buildOrThrow());
    }
}