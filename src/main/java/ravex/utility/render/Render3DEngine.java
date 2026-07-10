package ravex.utility.render;

import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
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
import org.joml.Vector3f;
import org.joml.Quaternionf;
import org.joml.Vector4f;
import ravex.mixin.render.AccessorRenderType;
import ravex.utility.player.rotation.RotationUtility;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Render3DEngine {

    private static final ByteBufferBuilder ALLOCATOR = new ByteBufferBuilder(512 * 1024);
    private static final ByteBufferBuilder FILL_ALLOCATOR = new ByteBufferBuilder(256 * 1024);
    private static final ByteBufferBuilder FILL_NODEPTH_ALLOCATOR = new ByteBufferBuilder(256 * 1024);
    private static final ByteBufferBuilder LINE_ALLOCATOR = new ByteBufferBuilder(256 * 1024);
    private static final ByteBufferBuilder LINE_NODEPTH_ALLOCATOR = new ByteBufferBuilder(256 * 1024);
    private static final ByteBufferBuilder LINE_ADDITIVE_ALLOCATOR = new ByteBufferBuilder(512 * 1024);
    private static final ByteBufferBuilder LINE_ADDITIVE_NODEPTH_ALLOCATOR = new ByteBufferBuilder(512 * 1024);

    private static final RenderType FILL_TYPE = RenderTypes.debugFilledBox();
    private static final RenderType LINE_TYPE = RenderTypes.lines();

    private static RenderType FILL_NO_DEPTH;
    private static RenderType LINE_NO_DEPTH;
    private static RenderType LINE_ADDITIVE;
    private static RenderType LINE_ADDITIVE_NO_DEPTH;

    private static BufferBuilder fillBuilder;
    private static BufferBuilder fillNoDepthBuilder;
    private static BufferBuilder lineBuilder;
    private static BufferBuilder lineNoDepthBuilder;
    private static BufferBuilder lineAdditiveBuilder;
    private static BufferBuilder lineAdditiveNoDepthBuilder;

    private static boolean fillUsed, fillNdUsed, lineUsed, lineNdUsed, lineAddUsed, lineAddNdUsed;

    private static final Matrix4f REUSABLE = new Matrix4f();
    private static Camera lastCamera;

    static {
        try {
            FILL_NO_DEPTH = cloneRenderType(RenderPipelines.DEBUG_FILLED_BOX, "ravex3d_fill_nd",
                b -> b.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withDepthWrite(false));
            LINE_NO_DEPTH = cloneRenderType(RenderPipelines.LINES, "ravex3d_line_nd",
                b -> b.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withDepthWrite(false));
            LINE_ADDITIVE = cloneRenderType(RenderPipelines.LINES, "ravex3d_line_add",
                b -> b.withBlend(BlendFunction.ADDITIVE));
            LINE_ADDITIVE_NO_DEPTH = cloneRenderType(RenderPipelines.LINES, "ravex3d_line_add_nd",
                b -> b.withBlend(BlendFunction.ADDITIVE).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withDepthWrite(false));
        } catch (Exception e) {
            ravex.RaveX.LOGGER.warn("[R3D] Failed to create custom render types: {}", e.getMessage());
        }
    }

    private static RenderType cloneRenderType(RenderPipeline source, String name, Consumer<RenderPipeline.Builder> overrider) {
        Identifier id = Identifier.withDefaultNamespace(name);
        RenderPipeline.Builder rb = RenderPipeline.builder()
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

        source.getBlendFunction().ifPresentOrElse(b -> rb.withBlend(b), () -> rb.withoutBlend());
        for (String s : source.getSamplers()) rb.withSampler(s);
        for (RenderPipeline.UniformDescription u : source.getUniforms()) rb.withUniform(u.name(), u.type());
        overrider.accept(rb);
        RenderSetup setup = RenderSetup.builder(rb.build()).createRenderSetup();
        return AccessorRenderType.invokeCreate(name, setup);
    }

    private static boolean hasCustom() {
        return FILL_NO_DEPTH != null;
    }

    public static Camera getLastCamera() {
        return lastCamera;
    }

    public static void beginFrame() {
        lastCamera = Minecraft.getInstance().gameRenderer.getMainCamera();
        fillUsed = fillNdUsed = lineUsed = lineNdUsed = lineAddUsed = lineAddNdUsed = false;

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
    }

    public static void endFrame() {
        if (fillBuilder != null && fillUsed) FILL_TYPE.draw(fillBuilder.buildOrThrow());
        if (fillNoDepthBuilder != null && fillNdUsed) FILL_NO_DEPTH.draw(fillNoDepthBuilder.buildOrThrow());
        if (lineBuilder != null && lineUsed) LINE_TYPE.draw(lineBuilder.buildOrThrow());
        if (lineNoDepthBuilder != null && lineNdUsed) LINE_NO_DEPTH.draw(lineNoDepthBuilder.buildOrThrow());
        if (lineAdditiveBuilder != null && lineAddUsed) LINE_ADDITIVE.draw(lineAdditiveBuilder.buildOrThrow());
        if (lineAdditiveNoDepthBuilder != null && lineAddNdUsed) LINE_ADDITIVE_NO_DEPTH.draw(lineAdditiveNoDepthBuilder.buildOrThrow());

        fillBuilder = fillNoDepthBuilder = lineBuilder = lineNoDepthBuilder = lineAdditiveBuilder = lineAdditiveNoDepthBuilder = null;
    }

    public static Vec3 cameraPos() {
        Camera cam = lastCamera;
        return cam != null ? cam.position() : Vec3.ZERO;
    }

    public static Matrix4f translate(Matrix4f source, Vec3 pos) {
        return source.translate((float) pos.x, (float) pos.y, (float) pos.z, REUSABLE);
    }

    public static Matrix4f translate(Matrix4f source, double x, double y, double z) {
        return source.translate((float) x, (float) y, (float) z, REUSABLE);
    }

    public static void fillBox(Matrix4f matrix, AABB box, int color) {
        fillBox(matrix, box, color, false);
    }

    public static void fillBox(Matrix4f matrix, AABB box, int color, boolean throughWalls) {
        BufferBuilder buf = throughWalls && fillNoDepthBuilder != null ? fillNoDepthBuilder : fillBuilder;
        if (buf == fillNoDepthBuilder) fillNdUsed = true;
        else fillUsed = true;
        renderBoxQuads(buf, matrix, box, color);
    }

    public static void fillBoxGradient(Matrix4f matrix, AABB box, int bottomColor, int topColor) {
        fillBoxGradient(matrix, box, bottomColor, topColor, false);
    }

    public static void fillBoxGradient(Matrix4f matrix, AABB box, int bottomColor, int topColor, boolean throughWalls) {
        BufferBuilder buf = throughWalls && fillNoDepthBuilder != null ? fillNoDepthBuilder : fillBuilder;
        if (buf == fillNoDepthBuilder) fillNdUsed = true;
        else fillUsed = true;
        renderBoxQuadsGradient(buf, matrix, box, bottomColor, topColor);
    }

    public static void wireframeBox(Matrix4f matrix, AABB box, int color, float lineWidth) {
        wireframeBox(matrix, box, color, lineWidth, false);
    }

    public static void wireframeBox(Matrix4f matrix, AABB box, int color, float lineWidth, boolean throughWalls) {
        float r = ColorUtility.getRed(color) / 255f;
        float g = ColorUtility.getGreen(color) / 255f;
        float b = ColorUtility.getBlue(color) / 255f;
        float a = ColorUtility.getAlpha(color) / 255f;

        if (lineWidth > 1f) {
            BufferBuilder buf = throughWalls && fillNoDepthBuilder != null ? fillNoDepthBuilder : fillBuilder;
            if (buf == fillNoDepthBuilder) fillNdUsed = true;
            else fillUsed = true;
            renderThickEdges(buf, matrix, box, r, g, b, a, lineWidth);
        } else {
            BufferBuilder buf = throughWalls && lineNoDepthBuilder != null ? lineNoDepthBuilder : lineBuilder;
            if (buf == lineNoDepthBuilder) lineNdUsed = true;
            else lineUsed = true;
            renderEdges(buf, matrix, box, r, g, b, a);
        }
    }

    public static void glowBox(Matrix4f matrix, AABB box, int color, float lineWidth, float glowSize) {
        glowBox(matrix, box, color, lineWidth, glowSize, false);
    }

    public static void glowBox(Matrix4f matrix, AABB box, int color, float lineWidth, float glowSize, boolean throughWalls) {
        wireframeBox(matrix, box, ColorUtility.applyAlpha(color, 0.3f), lineWidth + glowSize, throughWalls);
        wireframeBox(matrix, box, ColorUtility.applyAlpha(color, 0.15f), lineWidth + glowSize * 2, throughWalls);
        wireframeBox(matrix, box, color, lineWidth, throughWalls);
    }

    public static void line(Matrix4f matrix, Vec3 from, Vec3 to, int color, float lineWidth) {
        line(matrix, from, to, color, lineWidth, false);
    }

    public static void line(Matrix4f matrix, Vec3 from, Vec3 to, int color, float lineWidth, boolean throughWalls) {
        float r = ColorUtility.getRed(color) / 255f;
        float g = ColorUtility.getGreen(color) / 255f;
        float b = ColorUtility.getBlue(color) / 255f;
        float a = ColorUtility.getAlpha(color) / 255f;
        int ir = ColorUtility.getRed(color), ig = ColorUtility.getGreen(color), ib = ColorUtility.getBlue(color), ia = ColorUtility.getAlpha(color);

        if (lineWidth > 1f) {
            BufferBuilder buf = throughWalls && fillNoDepthBuilder != null ? fillNoDepthBuilder : fillBuilder;
            if (buf == fillNoDepthBuilder) fillNdUsed = true;
            else fillUsed = true;
            renderThickLine(buf, matrix, from, to, ir, ig, ib, ia, lineWidth);
        } else {
            BufferBuilder buf = throughWalls && lineNoDepthBuilder != null ? lineNoDepthBuilder : lineBuilder;
            if (buf == lineNoDepthBuilder) lineNdUsed = true;
            else lineUsed = true;
            buf.addVertex(matrix, (float) from.x, (float) from.y, (float) from.z).setColor(ir, ig, ib, ia).setNormal((float)(to.x - from.x), (float)(to.y - from.y), (float)(to.z - from.z)).setLineWidth(lineWidth);
            buf.addVertex(matrix, (float) to.x, (float) to.y, (float) to.z).setColor(ir, ig, ib, ia).setNormal((float)(to.x - from.x), (float)(to.y - from.y), (float)(to.z - from.z)).setLineWidth(lineWidth);
        }
    }

    public static void lineStrip(Matrix4f matrix, List<Vec3> points, int color, float lineWidth) {
        lineStrip(matrix, points, color, lineWidth, false);
    }

    public static void lineStrip(Matrix4f matrix, List<Vec3> points, int color, float lineWidth, boolean throughWalls) {
        if (points.size() < 2) return;
        for (int i = 1; i < points.size(); i++) {
            line(matrix, points.get(i - 1), points.get(i), color, lineWidth, throughWalls);
        }
    }

    public static void lineAdditive(Matrix4f matrix, Vec3 from, Vec3 to, int color, float lineWidth) {
        lineAdditive(matrix, from, to, color, lineWidth, false);
    }

    public static void lineAdditive(Matrix4f matrix, Vec3 from, Vec3 to, int color, float lineWidth, boolean throughWalls) {
        int ir = ColorUtility.getRed(color), ig = ColorUtility.getGreen(color), ib = ColorUtility.getBlue(color), ia = ColorUtility.getAlpha(color);
        BufferBuilder buf;
        if (throughWalls && lineAdditiveNoDepthBuilder != null) {
            buf = lineAdditiveNoDepthBuilder; lineAddNdUsed = true;
        } else if (lineAdditiveBuilder != null) {
            buf = lineAdditiveBuilder; lineAddUsed = true;
        } else return;
        buf.addVertex(matrix, (float) from.x, (float) from.y, (float) from.z).setColor(ir, ig, ib, ia).setNormal((float)(to.x - from.x), (float)(to.y - from.y), (float)(to.z - from.z)).setLineWidth(lineWidth);
        buf.addVertex(matrix, (float) to.x, (float) to.y, (float) to.z).setColor(ir, ig, ib, ia).setNormal((float)(to.x - from.x), (float)(to.y - from.y), (float)(to.z - from.z)).setLineWidth(lineWidth);
    }

    public static void drawTargetESP(Matrix4f matrix, AABB box, int color, float time, float speed) {
        float r = ColorUtility.getRed(color) / 255f;
        float g = ColorUtility.getGreen(color) / 255f;
        float b = ColorUtility.getBlue(color) / 255f;
        float a = ColorUtility.getAlpha(color) / 255f;

        float pulse = 0.5f + 0.5f * (float) Math.sin(time * speed);
        int faded = ColorUtility.toRGBA((int)(r * 255), (int)(g * 255), (int)(b * 255), (int)(a * pulse * 255));
        int bright = ColorUtility.toRGBA((int)(r * 255), (int)(g * 255), (int)(b * 255), (int)(a * 255));

        wireframeBox(matrix, box, faded, 1.5f);
        double expand = 0.02 * pulse;
        AABB glowBox = box.inflate(expand);
        wireframeBox(matrix, glowBox, ColorUtility.applyAlpha(faded, 0.3f), 2.5f);

        float dx = (float)(box.getXsize() * 0.5);
        float dz = (float)(box.getZsize() * 0.5);
        float baseY = (float) box.minY;
        float topY = (float) box.maxY;
        float midX = (float)(box.minX + box.maxX) / 2;
        float midZ = (float)(box.minZ + box.maxZ) / 2;

        List<Vec3> helix = new ArrayList<>();
        int segments = 32;
        float heightStep = (topY - baseY) / segments;
        for (int i = 0; i <= segments; i++) {
            float t = i / (float) segments;
            float angle = t * (float) Math.PI * 6 + time * speed * 2;
            float radius = 0.1f + 0.3f * (1 - t);
            float hx = midX + (float) Math.cos(angle) * radius;
            float hz = midZ + (float) Math.sin(angle) * radius;
            float hy = baseY + t * (topY - baseY);
            helix.add(new Vec3(hx, hy, hz));
        }
        lineStrip(matrix, helix, ColorUtility.applyAlpha(bright, pulse * 0.7f), 1.5f);
    }

    public static AABB blockAABB(double x, double y, double z) {
        return new AABB(x, y, z, x + 1, y + 1, z + 1);
    }

    public static Vec3 worldToScreen(Vec3 worldPos) {
        Camera cam = lastCamera;
        if (cam == null) return null;
        Minecraft mc = Minecraft.getInstance();
        if (mc.getWindow() == null) return null;

        Matrix4f proj = ravex.manager.ShaderManager.INSTANCE.getProjectionMatrix();
        Quaternionf camRot = new Quaternionf(cam.rotation()).conjugate();
        Matrix4f view = new Matrix4f().rotation(camRot);
        Vector4f pos = new Vector4f((float)(worldPos.x - cam.position().x), (float)(worldPos.y - cam.position().y), (float)(worldPos.z - cam.position().z), 1);
        pos.mul(view);
        pos.mul(proj);

        if (pos.w <= 0) return null;

        float invW = 1f / pos.w;
        float sx = (pos.x * invW + 1) / 2 * mc.getWindow().getWidth();
        float sy = (1 - pos.y * invW) / 2 * mc.getWindow().getHeight();

        return new Vec3(sx, sy, pos.z * invW);
    }

    public static AABB boxAt(Vec3 pos, double size) {
        double h = size / 2;
        return new AABB(pos.x - h, pos.y - h, pos.z - h, pos.x + h, pos.y + h, pos.z + h);
    }

    public static AABB boxAt(double x, double y, double z, double size) {
        double h = size / 2;
        return new AABB(x - h, y - h, z - h, x + h, y + h, z + h);
    }

    private static void renderBoxQuads(BufferBuilder buf, Matrix4f matrix, AABB box, int color) {
        int ir = ColorUtility.getRed(color), ig = ColorUtility.getGreen(color), ib = ColorUtility.getBlue(color), ia = ColorUtility.getAlpha(color);
        float minX = (float) box.minX, minY = (float) box.minY, minZ = (float) box.minZ;
        float maxX = (float) box.maxX, maxY = (float) box.maxY, maxZ = (float) box.maxZ;

        buf.addVertex(matrix, minX, minY, minZ).setColor(ir, ig, ib, ia);
        buf.addVertex(matrix, maxX, minY, minZ).setColor(ir, ig, ib, ia);
        buf.addVertex(matrix, maxX, minY, maxZ).setColor(ir, ig, ib, ia);
        buf.addVertex(matrix, minX, minY, maxZ).setColor(ir, ig, ib, ia);

        buf.addVertex(matrix, minX, maxY, minZ).setColor(ir, ig, ib, ia);
        buf.addVertex(matrix, minX, maxY, maxZ).setColor(ir, ig, ib, ia);
        buf.addVertex(matrix, maxX, maxY, maxZ).setColor(ir, ig, ib, ia);
        buf.addVertex(matrix, maxX, maxY, minZ).setColor(ir, ig, ib, ia);

        buf.addVertex(matrix, minX, minY, minZ).setColor(ir, ig, ib, ia);
        buf.addVertex(matrix, minX, maxY, minZ).setColor(ir, ig, ib, ia);
        buf.addVertex(matrix, maxX, maxY, minZ).setColor(ir, ig, ib, ia);
        buf.addVertex(matrix, maxX, minY, minZ).setColor(ir, ig, ib, ia);

        buf.addVertex(matrix, minX, minY, maxZ).setColor(ir, ig, ib, ia);
        buf.addVertex(matrix, maxX, minY, maxZ).setColor(ir, ig, ib, ia);
        buf.addVertex(matrix, maxX, maxY, maxZ).setColor(ir, ig, ib, ia);
        buf.addVertex(matrix, minX, maxY, maxZ).setColor(ir, ig, ib, ia);

        buf.addVertex(matrix, minX, minY, minZ).setColor(ir, ig, ib, ia);
        buf.addVertex(matrix, minX, minY, maxZ).setColor(ir, ig, ib, ia);
        buf.addVertex(matrix, minX, maxY, maxZ).setColor(ir, ig, ib, ia);
        buf.addVertex(matrix, minX, maxY, minZ).setColor(ir, ig, ib, ia);

        buf.addVertex(matrix, maxX, minY, minZ).setColor(ir, ig, ib, ia);
        buf.addVertex(matrix, maxX, maxY, minZ).setColor(ir, ig, ib, ia);
        buf.addVertex(matrix, maxX, maxY, maxZ).setColor(ir, ig, ib, ia);
        buf.addVertex(matrix, maxX, minY, maxZ).setColor(ir, ig, ib, ia);
    }

    private static void renderBoxQuadsGradient(BufferBuilder buf, Matrix4f matrix, AABB box, int bottomColor, int topColor) {
        int br = ColorUtility.getRed(bottomColor), bg = ColorUtility.getGreen(bottomColor), bb = ColorUtility.getBlue(bottomColor), ba = ColorUtility.getAlpha(bottomColor);
        int tr = ColorUtility.getRed(topColor), tg = ColorUtility.getGreen(topColor), tb = ColorUtility.getBlue(topColor), ta = ColorUtility.getAlpha(topColor);
        float minX = (float) box.minX, minY = (float) box.minY, minZ = (float) box.minZ;
        float maxX = (float) box.maxX, maxY = (float) box.maxY, maxZ = (float) box.maxZ;

        buf.addVertex(matrix, minX, minY, minZ).setColor(br, bg, bb, ba);
        buf.addVertex(matrix, maxX, minY, minZ).setColor(br, bg, bb, ba);
        buf.addVertex(matrix, maxX, minY, maxZ).setColor(br, bg, bb, ba);
        buf.addVertex(matrix, minX, minY, maxZ).setColor(br, bg, bb, ba);

        buf.addVertex(matrix, minX, maxY, minZ).setColor(tr, tg, tb, ta);
        buf.addVertex(matrix, minX, maxY, maxZ).setColor(tr, tg, tb, ta);
        buf.addVertex(matrix, maxX, maxY, maxZ).setColor(tr, tg, tb, ta);
        buf.addVertex(matrix, maxX, maxY, minZ).setColor(tr, tg, tb, ta);

        int mr = (br + tr) / 2, mg = (bg + tg) / 2, mb = (bb + tb) / 2, ma = (ba + ta) / 2;

        buf.addVertex(matrix, minX, minY, minZ).setColor(br, bg, bb, ba);
        buf.addVertex(matrix, minX, maxY, minZ).setColor(tr, tg, tb, ta);
        buf.addVertex(matrix, maxX, maxY, minZ).setColor(tr, tg, tb, ta);
        buf.addVertex(matrix, maxX, minY, minZ).setColor(br, bg, bb, ba);

        buf.addVertex(matrix, minX, minY, maxZ).setColor(br, bg, bb, ba);
        buf.addVertex(matrix, maxX, minY, maxZ).setColor(br, bg, bb, ba);
        buf.addVertex(matrix, maxX, maxY, maxZ).setColor(tr, tg, tb, ta);
        buf.addVertex(matrix, minX, maxY, maxZ).setColor(tr, tg, tb, ta);

        buf.addVertex(matrix, minX, minY, minZ).setColor(br, bg, bb, ba);
        buf.addVertex(matrix, minX, minY, maxZ).setColor(br, bg, bb, ba);
        buf.addVertex(matrix, minX, maxY, maxZ).setColor(tr, tg, tb, ta);
        buf.addVertex(matrix, minX, maxY, minZ).setColor(tr, tg, tb, ta);

        buf.addVertex(matrix, maxX, minY, minZ).setColor(br, bg, bb, ba);
        buf.addVertex(matrix, maxX, maxY, minZ).setColor(tr, tg, tb, ta);
        buf.addVertex(matrix, maxX, maxY, maxZ).setColor(tr, tg, tb, ta);
        buf.addVertex(matrix, maxX, minY, maxZ).setColor(br, bg, bb, ba);
    }

    private static void renderEdges(BufferBuilder buf, Matrix4f matrix, AABB box, float r, float g, float b, float a) {
        int ir = (int)(r * 255), ig = (int)(g * 255), ib = (int)(b * 255), ia = (int)(a * 255);
        float minX = (float) box.minX, minY = (float) box.minY, minZ = (float) box.minZ;
        float maxX = (float) box.maxX, maxY = (float) box.maxY, maxZ = (float) box.maxZ;

        buf.addVertex(matrix, minX, minY, minZ).setColor(ir, ig, ib, ia);
        buf.addVertex(matrix, maxX, minY, minZ).setColor(ir, ig, ib, ia);
        buf.addVertex(matrix, maxX, minY, minZ).setColor(ir, ig, ib, ia);
        buf.addVertex(matrix, maxX, minY, maxZ).setColor(ir, ig, ib, ia);
        buf.addVertex(matrix, maxX, minY, maxZ).setColor(ir, ig, ib, ia);
        buf.addVertex(matrix, minX, minY, maxZ).setColor(ir, ig, ib, ia);
        buf.addVertex(matrix, minX, minY, maxZ).setColor(ir, ig, ib, ia);
        buf.addVertex(matrix, minX, minY, minZ).setColor(ir, ig, ib, ia);

        buf.addVertex(matrix, minX, maxY, minZ).setColor(ir, ig, ib, ia);
        buf.addVertex(matrix, maxX, maxY, minZ).setColor(ir, ig, ib, ia);
        buf.addVertex(matrix, maxX, maxY, minZ).setColor(ir, ig, ib, ia);
        buf.addVertex(matrix, maxX, maxY, maxZ).setColor(ir, ig, ib, ia);
        buf.addVertex(matrix, maxX, maxY, maxZ).setColor(ir, ig, ib, ia);
        buf.addVertex(matrix, minX, maxY, maxZ).setColor(ir, ig, ib, ia);
        buf.addVertex(matrix, minX, maxY, maxZ).setColor(ir, ig, ib, ia);
        buf.addVertex(matrix, minX, maxY, minZ).setColor(ir, ig, ib, ia);

        buf.addVertex(matrix, minX, minY, minZ).setColor(ir, ig, ib, ia);
        buf.addVertex(matrix, minX, maxY, minZ).setColor(ir, ig, ib, ia);
        buf.addVertex(matrix, maxX, minY, minZ).setColor(ir, ig, ib, ia);
        buf.addVertex(matrix, maxX, maxY, minZ).setColor(ir, ig, ib, ia);
        buf.addVertex(matrix, maxX, minY, maxZ).setColor(ir, ig, ib, ia);
        buf.addVertex(matrix, maxX, maxY, maxZ).setColor(ir, ig, ib, ia);
        buf.addVertex(matrix, minX, minY, maxZ).setColor(ir, ig, ib, ia);
        buf.addVertex(matrix, minX, maxY, maxZ).setColor(ir, ig, ib, ia);
    }

    private static void renderThickEdges(BufferBuilder buf, Matrix4f matrix, AABB box, float r, float g, float b, float a, float lineWidth) {
        float t = 0.005f * lineWidth;
        float minX = (float) box.minX, minY = (float) box.minY, minZ = (float) box.minZ;
        float maxX = (float) box.maxX, maxY = (float) box.maxY, maxZ = (float) box.maxZ;
        int ir = (int)(r * 255), ig = (int)(g * 255), ib = (int)(b * 255), ia = (int)(a * 255);

        renderSolidBoxRaw(buf, matrix, minX, minY - t, minZ - t, maxX, minY + t, minZ + t, ir, ig, ib, ia);
        renderSolidBoxRaw(buf, matrix, minX, maxY - t, minZ - t, maxX, maxY + t, minZ + t, ir, ig, ib, ia);
        renderSolidBoxRaw(buf, matrix, minX, minY - t, maxZ - t, maxX, minY + t, maxZ + t, ir, ig, ib, ia);
        renderSolidBoxRaw(buf, matrix, minX, maxY - t, maxZ - t, maxX, maxY + t, maxZ + t, ir, ig, ib, ia);

        renderSolidBoxRaw(buf, matrix, minX - t, minY, minZ - t, minX + t, maxY, minZ + t, ir, ig, ib, ia);
        renderSolidBoxRaw(buf, matrix, maxX - t, minY, minZ - t, maxX + t, maxY, minZ + t, ir, ig, ib, ia);
        renderSolidBoxRaw(buf, matrix, minX - t, minY, maxZ - t, minX + t, maxY, maxZ + t, ir, ig, ib, ia);
        renderSolidBoxRaw(buf, matrix, maxX - t, minY, maxZ - t, maxX + t, maxY, maxZ + t, ir, ig, ib, ia);

        renderSolidBoxRaw(buf, matrix, minX - t, minY - t, minZ, minX + t, minY + t, maxZ, ir, ig, ib, ia);
        renderSolidBoxRaw(buf, matrix, maxX - t, minY - t, minZ, maxX + t, minY + t, maxZ, ir, ig, ib, ia);
        renderSolidBoxRaw(buf, matrix, minX - t, maxY - t, minZ, minX + t, maxY + t, maxZ, ir, ig, ib, ia);
        renderSolidBoxRaw(buf, matrix, maxX - t, maxY - t, minZ, maxX + t, maxY + t, maxZ, ir, ig, ib, ia);
    }

    private static void renderSolidBoxRaw(BufferBuilder buf, Matrix4f matrix, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, int ir, int ig, int ib, int ia) {
        buf.addVertex(matrix, minX, minY, minZ).setColor(ir, ig, ib, ia);
        buf.addVertex(matrix, maxX, minY, minZ).setColor(ir, ig, ib, ia);
        buf.addVertex(matrix, maxX, minY, maxZ).setColor(ir, ig, ib, ia);
        buf.addVertex(matrix, minX, minY, maxZ).setColor(ir, ig, ib, ia);
        buf.addVertex(matrix, minX, maxY, minZ).setColor(ir, ig, ib, ia);
        buf.addVertex(matrix, minX, maxY, maxZ).setColor(ir, ig, ib, ia);
        buf.addVertex(matrix, maxX, maxY, maxZ).setColor(ir, ig, ib, ia);
        buf.addVertex(matrix, maxX, maxY, minZ).setColor(ir, ig, ib, ia);
        buf.addVertex(matrix, minX, minY, minZ).setColor(ir, ig, ib, ia);
        buf.addVertex(matrix, minX, maxY, minZ).setColor(ir, ig, ib, ia);
        buf.addVertex(matrix, maxX, maxY, minZ).setColor(ir, ig, ib, ia);
        buf.addVertex(matrix, maxX, minY, minZ).setColor(ir, ig, ib, ia);
        buf.addVertex(matrix, minX, minY, maxZ).setColor(ir, ig, ib, ia);
        buf.addVertex(matrix, maxX, minY, maxZ).setColor(ir, ig, ib, ia);
        buf.addVertex(matrix, maxX, maxY, maxZ).setColor(ir, ig, ib, ia);
        buf.addVertex(matrix, minX, maxY, maxZ).setColor(ir, ig, ib, ia);
        buf.addVertex(matrix, minX, minY, minZ).setColor(ir, ig, ib, ia);
        buf.addVertex(matrix, minX, minY, maxZ).setColor(ir, ig, ib, ia);
        buf.addVertex(matrix, minX, maxY, maxZ).setColor(ir, ig, ib, ia);
        buf.addVertex(matrix, minX, maxY, minZ).setColor(ir, ig, ib, ia);
        buf.addVertex(matrix, maxX, minY, minZ).setColor(ir, ig, ib, ia);
        buf.addVertex(matrix, maxX, maxY, minZ).setColor(ir, ig, ib, ia);
        buf.addVertex(matrix, maxX, maxY, maxZ).setColor(ir, ig, ib, ia);
        buf.addVertex(matrix, maxX, minY, maxZ).setColor(ir, ig, ib, ia);
    }

    private static void renderThickLine(BufferBuilder buf, Matrix4f matrix, Vec3 from, Vec3 to, int ir, int ig, int ib, int ia, float lineWidth) {
        float dx = (float)(to.x - from.x), dy = (float)(to.y - from.y), dz = (float)(to.z - from.z);
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.001f) return;
        dx /= len; dy /= len; dz /= len;

        float upX, upY, upZ;
        if (Math.abs(dy) < 0.99f) { upX = 0; upY = 1; upZ = 0; }
        else { upX = 1; upY = 0; upZ = 0; }

        float rx = dy * upZ - dz * upY;
        float ry = dz * upX - dx * upZ;
        float rz = dx * upY - dy * upX;
        float rl = (float) Math.sqrt(rx * rx + ry * ry + rz * rz);
        if (rl < 0.001f) return;
        rx /= rl; ry /= rl; rz /= rl;

        float t = lineWidth * 0.02f;
        float ax = rx * t, ay = ry * t, az = rz * t;

        float fx = (float) from.x, fy = (float) from.y, fz = (float) from.z;
        float tx = (float) to.x, ty = (float) to.y, tz = (float) to.z;

        buf.addVertex(matrix, fx - ax, fy - ay, fz - az).setColor(ir, ig, ib, ia);
        buf.addVertex(matrix, fx + ax, fy + ay, fz + az).setColor(ir, ig, ib, ia);
        buf.addVertex(matrix, tx + ax, ty + ay, tz + az).setColor(ir, ig, ib, ia);
        buf.addVertex(matrix, tx - ax, ty - ay, tz - az).setColor(ir, ig, ib, ia);
    }

    private static final ByteBufferBuilder ESP_ALLOCATOR = new ByteBufferBuilder(256 * 1024);

    public static void renderRaveXESP(Matrix4f modelViewMatrix, Camera camera, Entity target, int color, float scanProgress, float rotation, float tickDelta) {
        double tPosX = target.xo + (target.getX() - target.xo) * tickDelta - camera.position().x;
        double tPosY = target.yo + (target.getY() - target.yo) * tickDelta - camera.position().y;
        double tPosZ = target.zo + (target.getZ() - target.zo) * tickDelta - camera.position().z;

        float height = target.getBbHeight();
        float width = target.getBbWidth();

        RenderType renderType = RenderTypes.entityTranslucent(TextureLoader.getCircleWhiteTexture());
        RenderType lineType = RenderTypes.lines();

        int overlay = OverlayTexture.NO_OVERLAY;
        int light = 0xF000F0;

        // Draw exactly one scanning ring (as requested: "нужно чтобы было лишь одно")
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

            // 1. Translucent scan disk
            ESP_ALLOCATOR.clear();
            BufferBuilder builder = new BufferBuilder(ESP_ALLOCATOR, renderType.mode(), renderType.format());
            int colDisk = ColorUtility.applyAlpha(color, alphaFade * 0.22f);
            builder.addVertex(matrix, -radius, 0.0f, -radius).setColor(colDisk).setUv(0.0f, 0.0f).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
            builder.addVertex(matrix, -radius, 0.0f, radius).setColor(colDisk).setUv(0.0f, 1.0f).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
            builder.addVertex(matrix, radius, 0.0f, radius).setColor(colDisk).setUv(1.0f, 1.0f).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
            builder.addVertex(matrix, radius, 0.0f, -radius).setColor(colDisk).setUv(1.0f, 0.0f).setOverlay(overlay).setLight(light).setNormal(0, 1, 0);
            renderType.draw(builder.buildOrThrow());

            // 2. Translucent outer line outline ring (lineWidth set to 3.5f for a thicker line)
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

        // debugFilledBox has POSITION_COLOR format and expects groups of 4 vertices (QUADS emulation)
        RenderType renderType = RenderTypes.debugFilledBox();

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

                // Perfect screen-aligned billboard: reset rotation columns to identity scaled by scale factor
                float sizePulse = 0.8f + 0.35f * (float) Math.sin(iAge * 0.15f + i * 0.5f);
                float scale = Math.max(0.18f * offset, 0.12f) * sizePulse * 0.85f;
                matrix.m00(scale);
                matrix.m01(0.0f);
                matrix.m02(0.0f);
                matrix.m10(0.0f);
                matrix.m11(scale);
                matrix.m12(0.0f);
                matrix.m20(0.0f);
                matrix.m21(0.0f);
                matrix.m22(scale);

                int ia = Math.round(offset * 0.95f * 255.0f);

                // Draw a 100% solid, perfectly sharp 12-sided dodecagon circle using 3 overlapping quads
                for (int stepIdx = 0; stepIdx < 3; stepIdx++) {
                    float angle = (float) (stepIdx * Math.PI / 6.0); // 0, 30, 60 degrees
                    float cos = (float) Math.cos(angle);
                    float sin = (float) Math.sin(angle);
                    
                    builder.addVertex(matrix, -cos - sin, -sin + cos, 0.0f).setColor(irBase, igBase, ibBase, ia);
                    builder.addVertex(matrix, cos - sin, sin + cos, 0.0f).setColor(irBase, igBase, ibBase, ia);
                    builder.addVertex(matrix, cos + sin, sin - cos, 0.0f).setColor(irBase, igBase, ibBase, ia);
                    builder.addVertex(matrix, -cos + sin, -sin - cos, 0.0f).setColor(irBase, igBase, ibBase, ia);
                }
            }
        }

        renderType.draw(builder.buildOrThrow());
    }
}
