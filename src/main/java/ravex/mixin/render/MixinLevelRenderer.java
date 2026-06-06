package ravex.mixin.render;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ravex.utility.render.Render3DUtils;
import ravex.modules.player.Xray;
import ravex.modules.combat.Surround;
import ravex.modules.render.BlockOutline;

@Mixin(LevelRenderer.class)
public class MixinLevelRenderer {
    private static long lastAnimTime = 0;

    // Smooth state variables for AirPlace
    private static double apX = 0, apY = 0, apZ = 0;
    private static float apAlpha = 0.0f;
    private static double apSize = 0.0;
    private static boolean apInitialized = false;

    // Smooth state variables for Scaffold
    private static double scX = 0, scY = 0, scZ = 0;
    private static float scAlpha = 0.0f;
    private static double scSize = 0.0;
    private static boolean scInitialized = false;
    @Inject(
        method = "renderLevel",
        at = @At("TAIL")
    )
    private void onRenderLevel(
        com.mojang.blaze3d.resource.GraphicsResourceAllocator graphicsResourceAllocator,
        net.minecraft.client.DeltaTracker deltaTracker,
        boolean renderBlockOutline,
        net.minecraft.client.Camera camera,
        org.joml.Matrix4f modelViewMatrix,
        org.joml.Matrix4f projectionMatrix,
        org.joml.Matrix4f matrix3,
        com.mojang.blaze3d.buffers.GpuBufferSlice gpuBufferSlice,
        org.joml.Vector4f vector4f,
        boolean bool2,
        CallbackInfo ci
    ) {
        renderHighlights(camera, modelViewMatrix);
    }

    private void renderHighlights(net.minecraft.client.Camera camera, org.joml.Matrix4f modelViewMatrix) {
        Vec3 camPos = camera.position();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        long now = System.currentTimeMillis();
        if (lastAnimTime == 0) lastAnimTime = now;
        long deltaMs = now - lastAnimTime;
        lastAnimTime = now;
        if (deltaMs > 100) deltaMs = 16;

        float factor = Math.min(1.0f, (deltaMs / 50.0f) * 0.25f);
        double slideFactor = Math.min(1.0, (deltaMs / 50.0) * 0.35);

        // --- AirPlace highlight ---
        ravex.modules.player.AirPlace ap = ravex.modules.player.AirPlace.INSTANCE;
        if (ap.getEnabled() && ap.render.getValue() && ap.currentTarget != null) {
            double tx = ap.currentTarget.getX();
            double ty = ap.currentTarget.getY();
            double tz = ap.currentTarget.getZ();
            if (ap.animate.getValue()) {
                if (!apInitialized) {
                    apX = tx; apY = ty; apZ = tz;
                    apInitialized = true;
                } else {
                    apX += (tx - apX) * slideFactor;
                    apY += (ty - apY) * slideFactor;
                    apZ += (tz - apZ) * slideFactor;
                }
                apAlpha += (1.0f - apAlpha) * factor;
                apSize += (1.0 - apSize) * factor;
            } else {
                apX = tx; apY = ty; apZ = tz;
                apAlpha = 1.0f;
                apSize = 1.0;
            }
            ravex.modules.player.AirPlace.highlightPos = new Vec3(apX, apY, apZ);
            ravex.modules.player.AirPlace.renderAlpha = apAlpha;
            ravex.modules.player.AirPlace.renderSize = apSize;
        } else {
            apAlpha += (0.0f - apAlpha) * factor;
            apSize += (0.0 - apSize) * factor;
            if (apAlpha < 0.01f) {
                apAlpha = 0.0f;
                apSize = 0.0;
                apInitialized = false;
                ravex.modules.player.AirPlace.highlightPos = null;
            } else {
                ravex.modules.player.AirPlace.highlightPos = new Vec3(apX, apY, apZ);
            }
            ravex.modules.player.AirPlace.renderAlpha = apAlpha;
            ravex.modules.player.AirPlace.renderSize = apSize;
        }

        if (ravex.modules.player.AirPlace.INSTANCE.getEnabled()) {
            renderBlockHighlight(
                ravex.modules.player.AirPlace.highlightPos,
                ravex.modules.player.AirPlace.renderAlpha,
                ravex.modules.player.AirPlace.renderSize,
                ravex.modules.player.AirPlace.renderR,
                ravex.modules.player.AirPlace.renderG,
                ravex.modules.player.AirPlace.renderB,
                camPos, modelViewMatrix
            );
        }

        // --- Scaffold highlight ---
        ravex.modules.world.Scaffold sc = ravex.modules.world.Scaffold.INSTANCE;
        if (sc.getEnabled() && sc.render.getValue() && sc.currentTarget != null) {
            double tx = sc.currentTarget.getX();
            double ty = sc.currentTarget.getY();
            double tz = sc.currentTarget.getZ();
            if (sc.animate.getValue()) {
                if (!scInitialized) {
                    scX = tx; scY = ty; scZ = tz;
                    scInitialized = true;
                } else {
                    scX += (tx - scX) * slideFactor;
                    scY += (ty - scY) * slideFactor;
                    scZ += (tz - scZ) * slideFactor;
                }
                scAlpha += (1.0f - scAlpha) * factor;
                scSize += (1.0 - scSize) * factor;
            } else {
                scX = tx; scY = ty; scZ = tz;
                scAlpha = 1.0f;
                scSize = 1.0;
            }
            ravex.modules.world.Scaffold.highlightPos = new Vec3(scX, scY, scZ);
            ravex.modules.world.Scaffold.renderAlpha = scAlpha;
            ravex.modules.world.Scaffold.renderSize = scSize;
        } else {
            scAlpha += (0.0f - scAlpha) * factor;
            scSize += (0.0 - scSize) * factor;
            if (scAlpha < 0.01f) {
                scAlpha = 0.0f;
                scSize = 0.0;
                scInitialized = false;
                ravex.modules.world.Scaffold.highlightPos = null;
            } else {
                ravex.modules.world.Scaffold.highlightPos = new Vec3(scX, scY, scZ);
            }
            ravex.modules.world.Scaffold.renderAlpha = scAlpha;
            ravex.modules.world.Scaffold.renderSize = scSize;
        }

        if (ravex.modules.world.Scaffold.INSTANCE.getEnabled()) {
            renderBlockHighlight(
                ravex.modules.world.Scaffold.highlightPos,
                ravex.modules.world.Scaffold.renderAlpha,
                ravex.modules.world.Scaffold.renderSize,
                ravex.modules.world.Scaffold.renderR,
                ravex.modules.world.Scaffold.renderG,
                ravex.modules.world.Scaffold.renderB,
                camPos, modelViewMatrix
            );
        }

        // --- BlockOutline ---
        if (BlockOutline.INSTANCE.getEnabled()) {
            renderBlockOutline(camPos, modelViewMatrix);
        }

        // --- ChestAura highlights ---
        if (ravex.modules.world.ChestAura.INSTANCE.getEnabled() && ravex.modules.world.ChestAura.INSTANCE.render.getValue() && !ravex.modules.world.ChestAura.placedChests.isEmpty()) {
            long chestNow = System.currentTimeMillis();
            double durationMs = ravex.modules.world.ChestAura.INSTANCE.fadeSpeed.getValue() * 1000.0;
            int color = ravex.modules.world.ChestAura.INSTANCE.highlightColor.getValue();
            float r = ((color >> 16) & 0xFF) / 255.0f;
            float g = ((color >> 8) & 0xFF) / 255.0f;
            float b = (color & 0xFF) / 255.0f;
            boolean filled = ravex.modules.world.ChestAura.INSTANCE.filled.getValue();

            for (ravex.modules.world.ChestAura.PlacedChest chest : ravex.modules.world.ChestAura.placedChests) {
                long elapsed = chestNow - chest.placeTime;
                if (elapsed > durationMs) continue;

                float progress = (float)(elapsed / durationMs);
                float alpha = 1.0f - progress;

                try {
                    Matrix4f matrix = new Matrix4f(modelViewMatrix)
                        .translate(
                            (float)(chest.pos.getX() - camPos.x),
                            (float)(chest.pos.getY() - camPos.y),
                            (float)(chest.pos.getZ() - camPos.z)
                        );

                    double size = 1.002;
                    if (filled) {
                        Render3DUtils.renderFilledBox(matrix, size, r, g, b, alpha * 0.25f);
                    }
                    Render3DUtils.renderWireframe(matrix, size, r, g, b, alpha * 0.95f);
                    Render3DUtils.renderWireframe(matrix, size * 1.03, r, g, b, alpha * 0.3f);
                } catch (Exception ignored) {}
            }
        }

        // --- Surround ---
        if (Surround.INSTANCE.getEnabled()) {
            synchronized (Surround.surroundBlocks) {
                for (BlockPos pos : Surround.surroundBlocks) {
                    if (pos == null) continue;
                    Vec3 blockPos = Vec3.atBottomCenterOf(pos);

                    try {
                        Matrix4f matrix = new Matrix4f(modelViewMatrix)
                            .translate(
                                (float)(blockPos.x - camPos.x),
                                (float)(blockPos.y - camPos.y),
                                (float)(blockPos.z - camPos.z)
                            );

                        float a = Surround.renderAlpha * 0.85f;
                        float s = (float)Surround.renderSize;

                        Render3DUtils.renderFilledBox(matrix, s,
                            Surround.renderR, Surround.renderG, Surround.renderB, a * 0.25f);
                        Render3DUtils.renderWireframe(matrix, s,
                            Surround.renderR, Surround.renderG, Surround.renderB, a * 0.95f);
                        Render3DUtils.renderWireframe(matrix, s * 1.03,
                            Surround.renderR, Surround.renderG, Surround.renderB, a * 0.3f);
                    } catch (Exception ignored) {}
                }
            }
        }
    }

    private void renderBlockHighlight(Vec3 highlightPos, float alpha, double size, float r, float g, float b, Vec3 camPos, Matrix4f modelViewMatrix) {
        if (highlightPos == null || alpha <= 0.01f) return;

        try {
            Matrix4f matrix = new Matrix4f(modelViewMatrix)
                .translate(
                    (float)(highlightPos.x - camPos.x),
                    (float)(highlightPos.y - camPos.y),
                    (float)(highlightPos.z - camPos.z)
                );

            float filledAlpha = alpha * 0.3f;
            float lineAlpha = alpha * 0.95f;

            Render3DUtils.renderFilledBox(matrix, size, r, g, b, filledAlpha);
            Render3DUtils.renderWireframe(matrix, size, r, g, b, lineAlpha);
            Render3DUtils.renderWireframe(matrix, size * 1.02, r, g, b, lineAlpha * 0.4f);
        } catch (Exception ignored) {}
    }

    private void renderBlockOutline(Vec3 camPos, Matrix4f modelViewMatrix) {
        Minecraft mc = Minecraft.getInstance();
        HitResult hit = mc.hitResult;
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) return;

        BlockHitResult blockHit = (BlockHitResult) hit;
        BlockPos pos = blockHit.getBlockPos();

        int color = BlockOutline.INSTANCE.color.getValue();
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        boolean filled = BlockOutline.INSTANCE.filled.getValue();

        try {
            Matrix4f matrix = new Matrix4f(modelViewMatrix)
                .translate(
                    (float)(pos.getX() - camPos.x),
                    (float)(pos.getY() - camPos.y),
                    (float)(pos.getZ() - camPos.z)
                );

            double outlineSize = 1.002;
            float lineWidth = BlockOutline.INSTANCE.width.getValue().floatValue();
            if (filled) {
                Render3DUtils.renderFilledBox(matrix, outlineSize, r, g, b, 0.2f);
            }
            Render3DUtils.renderWireframe(matrix, outlineSize, r, g, b, 0.95f, lineWidth);
            Render3DUtils.renderWireframe(matrix, outlineSize * 1.03, r, g, b, 0.3f, lineWidth);
        } catch (Exception ignored) {}
    }
}
