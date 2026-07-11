package ravex.utility.render.animate;

import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import org.joml.Matrix3x2fc;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import ravex.utility.shaders.*;
import ravex.manager.HandShaderManager;

public class ShaderVertexConsumer implements VertexConsumer {
    private final VertexConsumer delegate;
    private final HandShaderManager.RenderInput renderInput;
    private float curX, curY, curZ;
    private float curU, curV;
    private float curNormX, curNormY, curNormZ;

    public ShaderVertexConsumer(VertexConsumer delegate, HandShaderManager.RenderInput renderInput) {
        this.delegate = delegate;
        this.renderInput = renderInput;
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        curX = x; curY = y; curZ = z;
        delegate.addVertex(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer setColor(int r, int g, int b, int a) {
        if (renderInput != null && renderInput.config.enabled) {
            ShaderUniforms uniforms = HandShaderManager.getPipeline().getUniforms();
            EffectInput ein = new EffectInput();
            ein.vertex.position = new Vec3(curX, curY, curZ);
            ein.vertex.normal = new Vec3(curNormX, curNormY, curNormZ);
            ein.vertex.uv = new Vec2(curU, curV);
            ein.localPos = ein.vertex.position;

            if (uniforms != null && uniforms.modelMatrix != null) {
                ein.uniforms = uniforms;
                Vec4 transformed = uniforms.modelMatrix.transform(new Vec4(curX, curY, curZ, 1f));
                ein.worldPos = new Vec3(
                    transformed.x + uniforms.cameraPos.x,
                    transformed.y + uniforms.cameraPos.y,
                    transformed.z + uniforms.cameraPos.z
                );
            } else {
                ein.worldPos = ein.vertex.position;
            }

            ein.intensity = renderInput.config.intensity;
            ein.normalizedTime = renderInput.time;
            ein.deltaTime = renderInput.deltaTime;

            EffectOutput eout = HandShaderManager.getPipeline().processVertex(ein);

            float blend = Math.min(eout.alpha, 1f);
            float nr = (r / 255f) * (1f - blend) + eout.color.r * blend;
            float ng = (g / 255f) * (1f - blend) + eout.color.g * blend;
            float nb = (b / 255f) * (1f - blend) + eout.color.b * blend;

            delegate.setColor((int)(nr * 255), (int)(ng * 255), (int)(nb * 255), 255);
        } else {
            delegate.setColor(r, g, b, a);
        }
        return this;
    }

    @Override
    public VertexConsumer setColor(int color) {
        float a = ((color >> 24) & 0xFF) / 255f;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        return setColor((int)(r*255), (int)(g*255), (int)(b*255), (int)(a*255));
    }

    @Override
    public VertexConsumer setColor(float r, float g, float b, float a) {
        return setColor((int)(r*255), (int)(g*255), (int)(b*255), (int)(a*255));
    }

    @Override
    public VertexConsumer setLineWidth(float width) { delegate.setLineWidth(width); return this; }

    @Override
    public VertexConsumer setUv(float u, float v) { curU = u; curV = v; delegate.setUv(u, v); return this; }

    @Override
    public VertexConsumer setUv1(int u, int v) { delegate.setUv1(u, v); return this; }

    @Override
    public VertexConsumer setUv2(int u, int v) { delegate.setUv2(u, v); return this; }

    @Override
    public VertexConsumer setNormal(float x, float y, float z) { curNormX = x; curNormY = y; curNormZ = z; delegate.setNormal(x, y, z); return this; }

    @Override
    public VertexConsumer setOverlay(int overlay) { delegate.setOverlay(overlay); return this; }

    @Override
    public VertexConsumer setLight(int light) { delegate.setLight(light); return this; }

    @Override
    public VertexConsumer addVertex(Matrix4fc matrix, float x, float y, float z) {
        curX = x; curY = y; curZ = z;
        delegate.addVertex(matrix, x, y, z);
        return this;
    }

    @Override
    public void addVertex(float x, float y, float z, int color, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ) {
        curX = x; curY = y; curZ = z;
        curU = u; curV = v;
        curNormX = normalX; curNormY = normalY; curNormZ = normalZ;
        if (renderInput != null && renderInput.config.enabled) {
            int ra = (color >> 24) & 0xFF;
            int rr = (color >> 16) & 0xFF;
            int rg = (color >> 8) & 0xFF;
            int rb = color & 0xFF;

            ShaderUniforms uniforms = HandShaderManager.getPipeline().getUniforms();
            EffectInput ein = new EffectInput();
            ein.vertex.position = new Vec3(x, y, z);
            ein.vertex.normal = new Vec3(normalX, normalY, normalZ);
            ein.vertex.uv = new Vec2(u, v);
            ein.localPos = ein.vertex.position;

            if (uniforms != null && uniforms.modelMatrix != null) {
                ein.uniforms = uniforms;
                Vec4 transformed = uniforms.modelMatrix.transform(new Vec4(x, y, z, 1f));
                ein.worldPos = new Vec3(
                    transformed.x + uniforms.cameraPos.x,
                    transformed.y + uniforms.cameraPos.y,
                    transformed.z + uniforms.cameraPos.z
                );
            } else {
                ein.worldPos = ein.vertex.position;
            }

            ein.intensity = renderInput.config.intensity;
            ein.normalizedTime = renderInput.time;
            ein.deltaTime = renderInput.deltaTime;

            EffectOutput eout = HandShaderManager.getPipeline().processVertex(ein);

            float blend = Math.min(eout.alpha, 1f);
            float rr_new = (rr / 255f) * (1f - blend) + eout.color.r * blend;
            float rg_new = (rg / 255f) * (1f - blend) + eout.color.g * blend;
            float rb_new = (rb / 255f) * (1f - blend) + eout.color.b * blend;

            int modifiedColor = (ra << 24) | ((int)(rr_new * 255) << 16) | ((int)(rg_new * 255) << 8) | (int)(rb_new * 255);
            delegate.addVertex(x, y, z, modifiedColor, u, v, overlay, light, normalX, normalY, normalZ);
        } else {
            delegate.addVertex(x, y, z, color, u, v, overlay, light, normalX, normalY, normalZ);
        }
    }

    @Override
    public VertexConsumer addVertex(Pose pose, float x, float y, float z) {
        curX = x; curY = y; curZ = z;
        delegate.addVertex(pose, x, y, z);
        return this;
    }

    @Override
    public VertexConsumer addVertex(Pose pose, Vector3f vec) {
        curX = vec.x(); curY = vec.y(); curZ = vec.z();
        delegate.addVertex(pose, vec);
        return this;
    }

    @Override
    public VertexConsumer addVertex(Vector3fc vec) {
        curX = vec.x(); curY = vec.y(); curZ = vec.z();
        delegate.addVertex(vec);
        return this;
    }

    @Override
    public VertexConsumer setNormal(Pose pose, float x, float y, float z) { curNormX = x; curNormY = y; curNormZ = z; delegate.setNormal(pose, x, y, z); return this; }

    @Override
    public VertexConsumer setNormal(Pose pose, Vector3f vec) { curNormX = vec.x(); curNormY = vec.y(); curNormZ = vec.z(); delegate.setNormal(pose, vec); return this; }

    @Override
    public void putBulkData(Pose pose, BakedQuad quad, float red, float green, float blue, float alpha, int combinedLight, int combinedOverlay) {
        delegate.putBulkData(pose, quad, red, green, blue, alpha, combinedLight, combinedOverlay);
    }

    @Override
    public void putBulkData(Pose pose, BakedQuad quad, float[] brightness, float red, float green, float blue, float alpha, int[] lightmap, int combinedOverlay) {
        delegate.putBulkData(pose, quad, brightness, red, green, blue, alpha, lightmap, combinedOverlay);
    }

    @Override
    public VertexConsumer addVertexWith2DPose(Matrix3x2fc matrix, float x, float y) {
        delegate.addVertexWith2DPose(matrix, x, y);
        return this;
    }
}
