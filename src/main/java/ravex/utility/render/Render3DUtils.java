package ravex.utility.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import com.mojang.blaze3d.vertex.VertexConsumer;

public class Render3DUtils {
    public static double[] getCameraPos(net.minecraft.client.Camera camera) {
        try {
            java.lang.reflect.Field positionField = camera.getClass().getDeclaredField("position");
            positionField.setAccessible(true);
            net.minecraft.world.phys.Vec3 posVec = (net.minecraft.world.phys.Vec3) positionField.get(camera);
            return new double[]{posVec.x, posVec.y, posVec.z};
        } catch (Exception e) {
            return new double[]{0.0, 0.0, 0.0};
        }
    }

    public static float getPitch(net.minecraft.client.Camera camera) {
        try {
            java.lang.reflect.Field f = camera.getClass().getDeclaredField("xRot");
            f.setAccessible(true);
            return ((Number) f.get(camera)).floatValue();
        } catch (Exception e) {
            try {
                java.lang.reflect.Field f = camera.getClass().getDeclaredField("pitch");
                f.setAccessible(true);
                return ((Number) f.get(camera)).floatValue();
            } catch (Exception ex) {
                return 0.0f;
            }
        }
    }

    public static float getYaw(net.minecraft.client.Camera camera) {
        try {
            java.lang.reflect.Field f = camera.getClass().getDeclaredField("yRot");
            f.setAccessible(true);
            return ((Number) f.get(camera)).floatValue();
        } catch (Exception e) {
            try {
                java.lang.reflect.Field f = camera.getClass().getDeclaredField("yaw");
                f.setAccessible(true);
                return ((Number) f.get(camera)).floatValue();
            } catch (Exception ex) {
                return 0.0f;
            }
        }
    }

    public static VertexConsumer getLinesConsumer(MultiBufferSource.BufferSource bufferSource) {
        try {
            Class<?> renderTypeClass = Class.forName("net.minecraft.client.renderer.RenderType");
            java.lang.reflect.Method linesMethod = renderTypeClass.getDeclaredMethod("lines");
            Object linesRenderType = linesMethod.invoke(null);
            
            java.lang.reflect.Method getBufferMethod = bufferSource.getClass().getMethod("getBuffer", renderTypeClass);
            return (VertexConsumer) getBufferMethod.invoke(bufferSource, linesRenderType);
        } catch (Exception e) {
            return null;
        }
    }

    public static void endLinesBatch(MultiBufferSource.BufferSource bufferSource) {
        try {
            Class<?> renderTypeClass = Class.forName("net.minecraft.client.renderer.RenderType");
            java.lang.reflect.Method linesMethod = renderTypeClass.getDeclaredMethod("lines");
            Object linesRenderType = linesMethod.invoke(null);
            
            java.lang.reflect.Method endBatchMethod = bufferSource.getClass().getMethod("endBatch", renderTypeClass);
            endBatchMethod.invoke(bufferSource, linesRenderType);
        } catch (Exception ignored) {}
    }
}
