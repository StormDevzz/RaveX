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
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.LevelRenderState;
import ravex.utility.render.BlockRenderer;
import ravex.utility.render.Render3DUtils;
import ravex.modules.player.Xray;
import ravex.modules.combat.Surround;
import ravex.modules.render.BlockOutline;
import ravex.modules.esp.Borders;
import ravex.modules.esp.Tunnels;
import ravex.modules.esp.HoleESP;
import ravex.modules.esp.VoidESP;

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
        method = "renderBlockOutline(Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;Lcom/mojang/blaze3d/vertex/PoseStack;ZLnet/minecraft/client/renderer/state/LevelRenderState;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onVanillaRenderBlockOutline(
        MultiBufferSource.BufferSource bufferSource,
        PoseStack poseStack,
        boolean isTranslucent,
        LevelRenderState levelRenderState,
        CallbackInfo ci
    ) {
        if (BlockOutline.INSTANCE.getEnabled()) {
            ci.cancel();
            if (!isTranslucent) {
                renderCustomBlockOutline(bufferSource, poseStack);
            }
        }
    }

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

        // --- Trap highlight ---
        ravex.modules.combat.Trap trap = ravex.modules.combat.Trap.INSTANCE;
        if (trap.getEnabled() && trap.render.getValue()) {
            synchronized (ravex.modules.combat.Trap.trapBlocks) {
                for (BlockPos pos : ravex.modules.combat.Trap.trapBlocks) {
                    if (pos == null) continue;
                    Vec3 blockPos = Vec3.atBottomCenterOf(pos);

                    try {
                        Matrix4f matrix = new Matrix4f(modelViewMatrix)
                            .translate(
                                (float)(blockPos.x - camPos.x),
                                (float)(blockPos.y - camPos.y),
                                (float)(blockPos.z - camPos.z)
                            );

                        int c = trap.color.getValue();
                        float r = ((c >> 16) & 0xFF) / 255.0f;
                        float g = ((c >> 8) & 0xFF) / 255.0f;
                        float b = (c & 0xFF) / 255.0f;
                        float a = 0.35f;

                        double size = 1.002;
                        Render3DUtils.renderFilledBox(matrix, size, r, g, b, a * 0.25f);
                        Render3DUtils.renderWireframe(matrix, size, r, g, b, a * 0.95f);
                        Render3DUtils.renderWireframe(matrix, size * 1.03, r, g, b, a * 0.2f);
                    } catch (Exception ignored) {}
                }
            }
        }

        // --- SelfTrap highlight ---
        ravex.modules.combat.SelfTrap selfTrap = ravex.modules.combat.SelfTrap.INSTANCE;
        if (selfTrap.getEnabled() && selfTrap.render.getValue()) {
            for (BlockPos pos : ravex.modules.combat.SelfTrap.getSelfTrapBlocks()) {
                if (pos == null) continue;
                Vec3 blockPos = Vec3.atBottomCenterOf(pos);

                try {
                    Matrix4f matrix = new Matrix4f(modelViewMatrix)
                        .translate(
                            (float)(blockPos.x - camPos.x),
                            (float)(blockPos.y - camPos.y),
                            (float)(blockPos.z - camPos.z)
                        );

                    int c = selfTrap.color.getValue();
                    float r = ((c >> 16) & 0xFF) / 255.0f;
                    float g = ((c >> 8) & 0xFF) / 255.0f;
                    float b = (c & 0xFF) / 255.0f;
                    float a = ((c >> 24) & 0xFF) / 255.0f;

                    double size = 1.002;
                    Render3DUtils.renderFilledBox(matrix, size, r, g, b, a * 0.25f);
                    Render3DUtils.renderWireframe(matrix, size, r, g, b, a * 0.95f);
                } catch (Exception ignored) {}
            }
        }

        // --- BasePlace highlight ---
        ravex.modules.combat.BasePlace basePlace = ravex.modules.combat.BasePlace.INSTANCE;
        if (basePlace.getEnabled() && basePlace.render.getValue() && ravex.modules.combat.BasePlace.getSimulatedPlacementBlock() != null) {
            BlockPos pos = ravex.modules.combat.BasePlace.getSimulatedPlacementBlock();
            try {
                Matrix4f matrix = new Matrix4f(modelViewMatrix)
                    .translate(
                        (float)(pos.getX() - camPos.x),
                        (float)(pos.getY() - camPos.y),
                        (float)(pos.getZ() - camPos.z)
                    );

                int c = basePlace.color.getValue();
                float r = ((c >> 16) & 0xFF) / 255.0f;
                float g = ((c >> 8) & 0xFF) / 255.0f;
                float b = (c & 0xFF) / 255.0f;
                float a = ((c >> 24) & 0xFF) / 255.0f;

                double size = 1.002;
                Render3DUtils.renderFilledBox(matrix, size, r, g, b, a * 0.25f);
                Render3DUtils.renderWireframe(matrix, size, r, g, b, a * 0.95f);
                Render3DUtils.renderWireframe(matrix, size * 1.03, r, g, b, a * 0.2f);
            } catch (Exception ignored) {}
        }

        // --- AnchorAura highlight ---
        ravex.modules.combat.AnchorAura anchorAura = ravex.modules.combat.AnchorAura.INSTANCE;
        if (anchorAura.getEnabled() && anchorAura.render.getValue() && ravex.modules.combat.AnchorAura.simulatedPlacementBlock != null) {
            BlockPos pos = ravex.modules.combat.AnchorAura.simulatedPlacementBlock;
            try {
                Matrix4f matrix = new Matrix4f(modelViewMatrix)
                    .translate(
                        (float)(pos.getX() - camPos.x),
                        (float)(pos.getY() - camPos.y),
                        (float)(pos.getZ() - camPos.z)
                    );

                int c = anchorAura.color.getValue();
                float r = ((c >> 16) & 0xFF) / 255.0f;
                float g = ((c >> 8) & 0xFF) / 255.0f;
                float b = (c & 0xFF) / 255.0f;
                float a = ((c >> 24) & 0xFF) / 255.0f;

                double size = 1.002;
                Render3DUtils.renderFilledBox(matrix, size, r, g, b, a * 0.25f);
                Render3DUtils.renderWireframe(matrix, size, r, g, b, a * 0.95f);
                Render3DUtils.renderWireframe(matrix, size * 1.03, r, g, b, a * 0.2f);
            } catch (Exception ignored) {}
        }

        // --- AutoCrystal highlight ---


        ravex.modules.combat.AutoCrystal ac = ravex.modules.combat.AutoCrystal.INSTANCE;
        if (ac.getEnabled() && ac.renderPlacement.getValue() && ravex.modules.combat.AutoCrystal.currentPlacementBlock != null) {
            BlockPos p = ravex.modules.combat.AutoCrystal.currentPlacementBlock;
            try {
                Matrix4f matrix = new Matrix4f(modelViewMatrix)
                    .translate(
                        (float)(p.getX() - camPos.x),
                        (float)(p.getY() - camPos.y),
                        (float)(p.getZ() - camPos.z)
                    );

                double size = 1.002;
                Render3DUtils.renderFilledBox(matrix, size, 0.2f, 0.8f, 1.0f, 0.22f);
                Render3DUtils.renderWireframe(matrix, size, 0.2f, 0.8f, 1.0f, 0.85f);
                Render3DUtils.renderWireframe(matrix, size * 1.03, 0.2f, 0.8f, 1.0f, 0.2f);
            } catch (Exception ignored) {}
        }

        // --- StashFinder overlay ---
        if (ravex.modules.misc.StashFinder.INSTANCE.getEnabled()) {
            double maxDist = ravex.modules.misc.StashFinder.INSTANCE.range.getValue();
            for (ravex.modules.misc.StashFinder.StashEntry stash : ravex.modules.misc.StashFinder.INSTANCE.getStashes()) {
                Vec3 stashPos = Vec3.atBottomCenterOf(stash.pos);
                double dist = stashPos.distanceTo(camPos);
                if (dist > maxDist) continue;

                try {
                    Matrix4f matrix = new Matrix4f(modelViewMatrix)
                        .translate(
                            (float)(stashPos.x - camPos.x),
                            (float)(stashPos.y - camPos.y + 0.5),
                            (float)(stashPos.z - camPos.z)
                        );

                    float scale = (float)Math.max(0.5, Math.min(3.0, 64.0 / dist));
                    matrix.scale(scale, scale, scale);

                    Render3DUtils.renderFilledBox(matrix, 1.0, 1.0f, 0.5f, 0.0f, 0.3f);
                    Render3DUtils.renderWireframe(matrix, 1.0, 1.0f, 0.5f, 0.0f, 0.8f);
                } catch (Exception ignored) {}
            }
        }

        // --- BreadCrumbs ---
        if (ravex.modules.render.BreadCrumbs.INSTANCE.getEnabled()) {
            ravex.modules.render.BreadCrumbs.renderTrails(modelViewMatrix, camPos);
        }

        // --- TreeCutter highlight ---
        ravex.modules.world.TreeCutter tc = ravex.modules.world.TreeCutter.INSTANCE;
        if (tc.getEnabled() && tc.render.getValue() && ravex.modules.world.TreeCutter.currentMiningBlock != null) {
            BlockPos p = ravex.modules.world.TreeCutter.currentMiningBlock;
            try {
                Matrix4f matrix = new Matrix4f(modelViewMatrix)
                    .translate(
                        (float)(p.getX() - camPos.x),
                        (float)(p.getY() - camPos.y),
                        (float)(p.getZ() - camPos.z)
                    );

                int c = tc.color.getValue();
                float r = ((c >> 16) & 0xFF) / 255.0f;
                float g = ((c >> 8) & 0xFF) / 255.0f;
                float b = (c & 0xFF) / 255.0f;
                float a = 0.35f;

                double size = 1.002;
                Render3DUtils.renderFilledBox(matrix, size, r, g, b, a * 0.25f);
                Render3DUtils.renderWireframe(matrix, size, r, g, b, a * 0.95f);
                Render3DUtils.renderWireframe(matrix, size * 1.03, r, g, b, a * 0.2f);
            } catch (Exception ignored) {}
        }

        // --- WebSelf highlight ---
        ravex.modules.combat.WebSelf ws = ravex.modules.combat.WebSelf.INSTANCE;
        if (ws.getEnabled() && ws.render.getValue() && ravex.modules.combat.WebSelf.targetPos != null) {
            BlockPos p = ravex.modules.combat.WebSelf.targetPos;
            try {
                Matrix4f matrix = new Matrix4f(modelViewMatrix)
                    .translate(
                        (float)(p.getX() - camPos.x),
                        (float)(p.getY() - camPos.y),
                        (float)(p.getZ() - camPos.z)
                    );
                float r = ravex.modules.combat.WebSelf.renderR;
                float g = ravex.modules.combat.WebSelf.renderG;
                float b = ravex.modules.combat.WebSelf.renderB;
                double size = 1.002;
                Render3DUtils.renderFilledBox(matrix, size, r, g, b, 0.20f);
                Render3DUtils.renderWireframe(matrix, size, r, g, b, 0.85f);
                Render3DUtils.renderWireframe(matrix, size * 1.03, r, g, b, 0.25f);
            } catch (Exception ignored) {}
        }

        // --- Breaker highlight ---
        ravex.modules.combat.Breaker br = ravex.modules.combat.Breaker.INSTANCE;
        if (br.getEnabled() && br.render.getValue() && ravex.modules.combat.Breaker.currentMiningBlock != null) {
            BlockPos p = ravex.modules.combat.Breaker.currentMiningBlock;
            try {
                Matrix4f matrix = new Matrix4f(modelViewMatrix)
                    .translate(
                        (float)(p.getX() - camPos.x),
                        (float)(p.getY() - camPos.y),
                        (float)(p.getZ() - camPos.z)
                    );

                int c = br.color.getValue();
                float r = ((c >> 16) & 0xFF) / 255.0f;
                float g = ((c >> 8) & 0xFF) / 255.0f;
                float b = (c & 0xFF) / 255.0f;
                float a = 0.35f;

                double size = 1.002;
                Render3DUtils.renderFilledBox(matrix, size, r, g, b, a * 0.25f);
                Render3DUtils.renderWireframe(matrix, size, r, g, b, a * 0.95f);
            } catch (Exception ignored) {}
        }

        // --- AutoTunnel highlight ---
        ravex.modules.world.AutoTunnel at = ravex.modules.world.AutoTunnel.INSTANCE;
        if (at.getEnabled() && at.render.getValue() && ravex.modules.world.AutoTunnel.currentTarget != null) {
            BlockPos p = ravex.modules.world.AutoTunnel.currentTarget;
            try {
                Matrix4f matrix = new Matrix4f(modelViewMatrix)
                    .translate(
                        (float)(p.getX() - camPos.x),
                        (float)(p.getY() - camPos.y),
                        (float)(p.getZ() - camPos.z)
                    );

                int c = at.color.getValue();
                float r = ((c >> 16) & 0xFF) / 255.0f;
                float g = ((c >> 8) & 0xFF) / 255.0f;
                float b = (c & 0xFF) / 255.0f;
                float a = ((c >> 24) & 0xFF) / 255.0f;

                double size = 1.002;
                Render3DUtils.renderFilledBox(matrix, size, r, g, b, a * 0.25f);
                Render3DUtils.renderWireframe(matrix, size, r, g, b, a * 0.95f);
                Render3DUtils.renderWireframe(matrix, size * 1.03, r, g, b, a * 0.2f);
            } catch (Exception ignored) {}
        }

        // --- Nuker highlight ---
        ravex.modules.world.Nuker nk = ravex.modules.world.Nuker.INSTANCE;
        if (nk.getEnabled() && nk.render.getValue() && ravex.modules.world.Nuker.currentTarget != null) {
            BlockPos p = ravex.modules.world.Nuker.currentTarget;
            try {
                Matrix4f matrix = new Matrix4f(modelViewMatrix)
                    .translate(
                        (float)(p.getX() - camPos.x),
                        (float)(p.getY() - camPos.y),
                        (float)(p.getZ() - camPos.z)
                    );

                int c = nk.color.getValue();
                float r = ((c >> 16) & 0xFF) / 255.0f;
                float g = ((c >> 8) & 0xFF) / 255.0f;
                float b = (c & 0xFF) / 255.0f;
                float a = ((c >> 24) & 0xFF) / 255.0f;

                double size = 1.002;
                Render3DUtils.renderFilledBox(matrix, size, r, g, b, a * 0.25f);
                Render3DUtils.renderWireframe(matrix, size, r, g, b, a * 0.95f);
                Render3DUtils.renderWireframe(matrix, size * 1.03, r, g, b, a * 0.2f);
            } catch (Exception ignored) {}
        }

        // --- AutoSmelt highlight ---
        ravex.modules.world.AutoSmelt sm = ravex.modules.world.AutoSmelt.INSTANCE;
        if (sm.getEnabled() && sm.render.getValue() && ravex.modules.world.AutoSmelt.currentTarget != null) {
            BlockPos p = ravex.modules.world.AutoSmelt.currentTarget;
            try {
                Matrix4f matrix = new Matrix4f(modelViewMatrix)
                    .translate((float)(p.getX() - camPos.x), (float)(p.getY() - camPos.y), (float)(p.getZ() - camPos.z));
                int c = sm.color.getValue();
                float r = ((c >> 16) & 0xFF) / 255.0f;
                float g = ((c >> 8) & 0xFF) / 255.0f;
                float b = (c & 0xFF) / 255.0f;
                float a = ((c >> 24) & 0xFF) / 255.0f;
                double size = 1.002;
                Render3DUtils.renderFilledBox(matrix, size, r, g, b, a * 0.25f);
                Render3DUtils.renderWireframe(matrix, size, r, g, b, a * 0.95f);
                Render3DUtils.renderWireframe(matrix, size * 1.03, r, g, b, a * 0.2f);
            } catch (Exception ignored) {}
        }

        // --- AutoBrew highlight ---
        ravex.modules.world.AutoBrew bw = ravex.modules.world.AutoBrew.INSTANCE;
        if (bw.getEnabled() && bw.render.getValue() && ravex.modules.world.AutoBrew.currentTarget != null) {
            BlockPos p = ravex.modules.world.AutoBrew.currentTarget;
            try {
                Matrix4f matrix = new Matrix4f(modelViewMatrix)
                    .translate((float)(p.getX() - camPos.x), (float)(p.getY() - camPos.y), (float)(p.getZ() - camPos.z));
                int c = bw.color.getValue();
                float r = ((c >> 16) & 0xFF) / 255.0f;
                float g = ((c >> 8) & 0xFF) / 255.0f;
                float b = (c & 0xFF) / 255.0f;
                float a = ((c >> 24) & 0xFF) / 255.0f;
                double size = 1.002;
                Render3DUtils.renderFilledBox(matrix, size, r, g, b, a * 0.25f);
                Render3DUtils.renderWireframe(matrix, size, r, g, b, a * 0.95f);
                Render3DUtils.renderWireframe(matrix, size * 1.03, r, g, b, a * 0.2f);
            } catch (Exception ignored) {}
        }

        // --- ECFarmer highlight ---
        ravex.modules.world.ECFarmer ec = ravex.modules.world.ECFarmer.INSTANCE;
        if (ec.getEnabled() && ec.render.getValue() && ravex.modules.world.ECFarmer.currentTarget != null) {
            BlockPos p = ravex.modules.world.ECFarmer.currentTarget;
            try {
                Matrix4f matrix = new Matrix4f(modelViewMatrix)
                    .translate((float)(p.getX() - camPos.x), (float)(p.getY() - camPos.y), (float)(p.getZ() - camPos.z));
                int c = ec.color.getValue();
                float r = ((c >> 16) & 0xFF) / 255.0f;
                float g = ((c >> 8) & 0xFF) / 255.0f;
                float b = (c & 0xFF) / 255.0f;
                float a = ((c >> 24) & 0xFF) / 255.0f;
                double size = 1.002;
                Render3DUtils.renderFilledBox(matrix, size, r, g, b, a * 0.25f);
                Render3DUtils.renderWireframe(matrix, size, r, g, b, a * 0.95f);
                Render3DUtils.renderWireframe(matrix, size * 1.03, r, g, b, a * 0.2f);
            } catch (Exception ignored) {}
        }

        // --- PortalBuild highlight ---
        ravex.modules.misc.PortalBuild pb = ravex.modules.misc.PortalBuild.INSTANCE;
        if (pb.getEnabled() && pb.render.getValue() && ravex.modules.misc.PortalBuild.currentTarget != null) {
            BlockPos p = ravex.modules.misc.PortalBuild.currentTarget;
            try {
                Matrix4f matrix = new Matrix4f(modelViewMatrix)
                    .translate((float)(p.getX() - camPos.x), (float)(p.getY() - camPos.y), (float)(p.getZ() - camPos.z));
                int c = pb.color.getValue();
                float r = ((c >> 16) & 0xFF) / 255.0f;
                float g = ((c >> 8) & 0xFF) / 255.0f;
                float b = (c & 0xFF) / 255.0f;
                float a = ((c >> 24) & 0xFF) / 255.0f;
                double size = 1.002;
                Render3DUtils.renderFilledBox(matrix, size, r, g, b, a * 0.25f);
                Render3DUtils.renderWireframe(matrix, size, r, g, b, a * 0.95f);
                Render3DUtils.renderWireframe(matrix, size * 1.03, r, g, b, a * 0.2f);
            } catch (Exception ignored) {}
        }

        // --- HoleFill highlight ---
        ravex.modules.combat.HoleFill hf = ravex.modules.combat.HoleFill.INSTANCE;
        if (hf.getEnabled() && hf.render.getValue()) {
            for (var hole : ravex.modules.combat.HoleFill.holePositions) {
                if (hole == null) continue;
                try {
                    Matrix4f matrix = new Matrix4f(modelViewMatrix)
                        .translate((float)(hole.getX() - camPos.x), (float)(hole.getY() - camPos.y), (float)(hole.getZ() - camPos.z));
                    int c = hf.color.getValue();
                    float r = ((c >> 16) & 0xFF) / 255.0f;
                    float g = ((c >> 8) & 0xFF) / 255.0f;
                    float b = (c & 0xFF) / 255.0f;
                    float a = ((c >> 24) & 0xFF) / 255.0f;
                    double size = 1.002;
                    Render3DUtils.renderFilledBox(matrix, size, r, g, b, a * 0.15f);
                    Render3DUtils.renderWireframe(matrix, size, r, g, b, a * 0.85f);
                } catch (Exception ignored) {}
            }
        }

        // --- Borders ---
        if (Borders.INSTANCE.getEnabled()) {
            int rd = Borders.INSTANCE.renderDistance.getValue().intValue();
            boolean showCurrent = Borders.INSTANCE.showCurrentChunk.getValue();
            boolean showAll = Borders.INSTANCE.showChunkBorders.getValue();
            int lc = Borders.INSTANCE.chunkColor.getValue();
            float lr = ((lc >> 16) & 0xFF) / 255.0f;
            float lg = ((lc >> 8) & 0xFF) / 255.0f;
            float lb = (lc & 0xFF) / 255.0f;
            float la = ((lc >> 24) & 0xFF) / 255.0f;
            float lw = Borders.INSTANCE.lineWidth.getValue().floatValue();

            if (showAll && mc.player != null) {
                int cx = mc.player.chunkPosition().x;
                int cz = mc.player.chunkPosition().z;
                for (int dx = -rd / 16; dx <= rd / 16; dx++) {
                    for (int dz = -rd / 16; dz <= rd / 16; dz++) {
                        if (showCurrent && dx == 0 && dz == 0) continue;
                        int bx = (cx + dx) << 4;
                        int bz = (cz + dz) << 4;
                        try {
                            renderChunkBorderLines(modelViewMatrix, bx, bz, lr, lg, lb, la, camPos);
                        } catch (Exception ignored) {}
                    }
                }
            }

            if (showCurrent) {
                int cc = Borders.INSTANCE.currentColor.getValue();
                float cr = ((cc >> 16) & 0xFF) / 255.0f;
                float cg = ((cc >> 8) & 0xFF) / 255.0f;
                float cb = (cc & 0xFF) / 255.0f;
                float ca = ((cc >> 24) & 0xFF) / 255.0f;
                if (mc.player != null) {
                    int cx = mc.player.chunkPosition().x;
                    int cz = mc.player.chunkPosition().z;
                    int bx = cx << 4;
                    int bz = cz << 4;
                    try {
                        renderChunkBorderLines(modelViewMatrix, bx, bz, cr, cg, cb, ca, camPos);
                    } catch (Exception ignored) {}
                }
            }
        }

        // --- Tunnels ---
        if (Tunnels.INSTANCE.getEnabled()) {
            int tunnelColorVal = Tunnels.INSTANCE.tunnelColor.getValue();
            float tr = ((tunnelColorVal >> 16) & 0xFF) / 255.0f;
            float tg = ((tunnelColorVal >> 8) & 0xFF) / 255.0f;
            float tb = (tunnelColorVal & 0xFF) / 255.0f;
            float ta = ((tunnelColorVal >> 24) & 0xFF) / 255.0f;
            boolean filled = Tunnels.INSTANCE.filled.getValue();
            boolean wire = Tunnels.INSTANCE.wireframe.getValue();
            for (BlockPos pos : Tunnels.INSTANCE.getTunnelBlocks()) {
                try {
                    Matrix4f matrix = new Matrix4f(modelViewMatrix)
                        .translate((float)(pos.getX() - camPos.x), (float)(pos.getY() - camPos.y), (float)(pos.getZ() - camPos.z));
                    if (filled) Render3DUtils.renderFilledBox(matrix, 1.002, tr, tg, tb, ta * 0.3f);
                    if (wire) Render3DUtils.renderWireframe(matrix, 1.002, tr, tg, tb, ta * 0.85f);
                } catch (Exception ignored) {}
            }
        }

        // --- HoleESP ---
        if (HoleESP.INSTANCE.getEnabled()) {
            int c = HoleESP.INSTANCE.safeColor.getValue();
            float hr = ((c >> 16) & 0xFF) / 255.0f;
            float hg = ((c >> 8) & 0xFF) / 255.0f;
            float hb = (c & 0xFF) / 255.0f;
            float ha = ((c >> 24) & 0xFF) / 255.0f;
            float hw = 0.04f;
            for (var pos : HoleESP.INSTANCE.getHoles()) {
                try {
                    float px = (float)(pos.getX() - camPos.x);
                    float py = (float)(pos.getY() - camPos.y);
                    float pz = (float)(pos.getZ() - camPos.z);
                    if (HoleESP.INSTANCE.filled.getValue()) {
                        Matrix4f mat = new Matrix4f(modelViewMatrix).translate(px, py, pz);
                        Render3DUtils.renderFilledBox(mat, 1.002, hr, hg, hb, ha * 0.3f, true);
                    }
                    if (HoleESP.INSTANCE.wireframe.getValue()) {
                        // 12 edges as thin axis-aligned boxes through the fixed fillNoDepth
                        Render3DUtils.renderAxisLine(modelViewMatrix, px, py, pz, px + 1, py, pz, hw, hr, hg, hb, ha, true);
                        Render3DUtils.renderAxisLine(modelViewMatrix, px + 1, py, pz, px + 1, py, pz + 1, hw, hr, hg, hb, ha, true);
                        Render3DUtils.renderAxisLine(modelViewMatrix, px + 1, py, pz + 1, px, py, pz + 1, hw, hr, hg, hb, ha, true);
                        Render3DUtils.renderAxisLine(modelViewMatrix, px, py, pz + 1, px, py, pz, hw, hr, hg, hb, ha, true);

                        Render3DUtils.renderAxisLine(modelViewMatrix, px, py + 1, pz, px + 1, py + 1, pz, hw, hr, hg, hb, ha, true);
                        Render3DUtils.renderAxisLine(modelViewMatrix, px + 1, py + 1, pz, px + 1, py + 1, pz + 1, hw, hr, hg, hb, ha, true);
                        Render3DUtils.renderAxisLine(modelViewMatrix, px + 1, py + 1, pz + 1, px, py + 1, pz + 1, hw, hr, hg, hb, ha, true);
                        Render3DUtils.renderAxisLine(modelViewMatrix, px, py + 1, pz + 1, px, py + 1, pz, hw, hr, hg, hb, ha, true);

                        Render3DUtils.renderAxisLine(modelViewMatrix, px, py, pz, px, py + 1, pz, hw, hr, hg, hb, ha, true);
                        Render3DUtils.renderAxisLine(modelViewMatrix, px + 1, py, pz, px + 1, py + 1, pz, hw, hr, hg, hb, ha, true);
                        Render3DUtils.renderAxisLine(modelViewMatrix, px + 1, py, pz + 1, px + 1, py + 1, pz + 1, hw, hr, hg, hb, ha, true);
                        Render3DUtils.renderAxisLine(modelViewMatrix, px, py, pz + 1, px, py + 1, pz + 1, hw, hr, hg, hb, ha, true);
                    }
                } catch (Exception ignored) {}
            }
        }

        // --- VoidESP ---
        if (VoidESP.INSTANCE.getEnabled()) {
            int vc = VoidESP.INSTANCE.voidColor.getValue();
            float vr = ((vc >> 16) & 0xFF) / 255.0f;
            float vg = ((vc >> 8) & 0xFF) / 255.0f;
            float vb = (vc & 0xFF) / 255.0f;
            float va = ((vc >> 24) & 0xFF) / 255.0f;
            boolean vf = VoidESP.INSTANCE.filled.getValue();
            boolean vw = VoidESP.INSTANCE.wireframe.getValue();
            for (BlockPos pos : VoidESP.INSTANCE.getVoidBlocks()) {
                try {
                    Matrix4f matrix = new Matrix4f(modelViewMatrix)
                        .translate((float)(pos.getX() - camPos.x), 0, (float)(pos.getZ() - camPos.z));
                    if (vf) Render3DUtils.renderFilledBox(matrix, 16.0, vr, vg, vb, va * 0.15f);
                    if (vw) Render3DUtils.renderWireframe(matrix, 16.0, vr, vg, vb, va * 0.4f);
                } catch (Exception ignored) {}
            }
        }

        // --- PacketMine animated highlight ---
        ravex.modules.exploit.PacketMine pm = ravex.modules.exploit.PacketMine.INSTANCE;
        if (pm.getEnabled() && pm.render.getValue()) {
            long globalTime = System.currentTimeMillis();
            for (var mb : ravex.modules.exploit.PacketMine.miningBlocks) {
                if (mb == null || mb.pos == null) continue;
                try {
                    Matrix4f matrix = new Matrix4f(modelViewMatrix)
                        .translate((float)(mb.pos.getX() - camPos.x), (float)(mb.pos.getY() - camPos.y), (float)(mb.pos.getZ() - camPos.z));

                    int c = pm.color.getValue();
                    float r = ((c >> 16) & 0xFF) / 255.0f;
                    float g = ((c >> 8) & 0xFF) / 255.0f;
                    float b = (c & 0xFF) / 255.0f;

                    long blockTime = globalTime - mb.startTime;
                    float progress = Math.min(1.0f, (float)blockTime / (float)Math.max(1, mb.breakAt));
                    float fadeOut = mb.done ? Math.max(0, 1.0f - (globalTime - mb.visibleUntil + 2500) / 2500.0f) : 1.0f;
                    if (fadeOut <= 0.01f) continue;

                    float pulse = 0.5f + 0.5f * (float)Math.sin(blockTime * 0.006 + progress * 3.14f);

                    float flashR = r + (1.0f - r) * progress * 0.8f;
                    float flashG = g + (1.0f - g) * progress * 0.8f;
                    float flashB = b + (1.0f - b) * progress * 0.8f;
                    double size = 1.002;
                    float fillAlpha = (0.3f - 0.2f * progress) * pulse * fadeOut;
                    Render3DUtils.renderFilledBox(matrix, size, flashR * pulse, flashG * pulse, flashB * pulse, fillAlpha);

                    float wireAlpha = (0.3f + 0.5f * (1.0f - progress)) * fadeOut;
                    Render3DUtils.renderWireframe(matrix, size * 1.005, r, g, b, wireAlpha);
                    Render3DUtils.renderWireframe(matrix, size * 1.015, r, g, b, wireAlpha * 0.5f);
                    Render3DUtils.renderWireframe(matrix, size * 1.025, r, g, b, wireAlpha * 0.2f);

                    for (int i = 0; i < 3; i++) {
                        float phase = (float)(blockTime * 0.003 + i * 2.09);
                        float beamX = (float)Math.cos(phase) * 0.15f + 0.5f;
                        float beamZ = (float)Math.sin(phase) * 0.15f + 0.5f;
                        float beamY = (float)((phase % 6.28) / 6.28) * 1.2f - 0.1f;
                        if (beamY < 0) beamY += 1.2f;
                        Matrix4f beamMat = new Matrix4f(modelViewMatrix)
                            .translate((float)(mb.pos.getX() + beamX - camPos.x),
                                       (float)(mb.pos.getY() + beamY - camPos.y),
                                       (float)(mb.pos.getZ() + beamZ - camPos.z));
                        Render3DUtils.renderFilledBox(beamMat, 0.04, flashR, flashG, flashB, 0.6f * (1.0f - progress) * fadeOut);
                    }
                } catch (Exception ignored) {}
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

    private void renderCustomBlockOutline(MultiBufferSource.BufferSource bufferSource, PoseStack poseStack) {
        Minecraft mc = Minecraft.getInstance();
        HitResult hit = mc.hitResult;
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) return;

        BlockHitResult blockHit = (BlockHitResult) hit;
        BlockPos pos = blockHit.getBlockPos();

        int color = BlockOutline.INSTANCE.color.getValue();
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        float a = ((color >> 24) & 0xFF) / 255.0f;
        boolean filled = BlockOutline.INSTANCE.filled.getValue();
        float lineWidth = BlockOutline.INSTANCE.width.getValue().floatValue();
        double outlineSize = 1.002;

        try {
            poseStack.pushPose();
            poseStack.translate((float)pos.getX(), (float)pos.getY(), (float)pos.getZ());

            Matrix4f matrix = new Matrix4f(poseStack.last().pose());

            if (filled) {
                VertexConsumer fillConsumer = bufferSource.getBuffer(RenderTypes.debugFilledBox());
                BlockRenderer.renderFilledBoxQuads(fillConsumer, matrix, outlineSize, r, g, b, a * 0.25f);
            }

            VertexConsumer lineConsumer = bufferSource.getBuffer(RenderTypes.lines());
            BlockRenderer.renderWireframe(lineConsumer, matrix, outlineSize, r, g, b, a, lineWidth);

            // glow
            VertexConsumer glowConsumer = bufferSource.getBuffer(RenderTypes.lines());
            BlockRenderer.renderWireframe(glowConsumer, matrix, outlineSize * 1.035, r, g, b, a * 0.35f, lineWidth * 0.6f);

            poseStack.popPose();
        } catch (Exception ignored) {}
    }

    private void renderChunkBorderLines(Matrix4f modelViewMatrix, int bx, int bz, float r, float g, float b, float a, Vec3 camPos) {
        float cx = (float)camPos.x;
        float cy = (float)camPos.y;
        float cz = (float)camPos.z;
        float th = 0.06f;

        // 4 vertical corner pillars (full world height)
        // Through-walls для видимости сквозь стены
        Render3DUtils.renderAxisLine(modelViewMatrix, bx - cx, -64 - cy, bz - cz, bx - cx, 320 - cy, bz - cz, th, r, g, b, a, true);
        Render3DUtils.renderAxisLine(modelViewMatrix, bx + 16 - cx, -64 - cy, bz - cz, bx + 16 - cx, 320 - cy, bz - cz, th, r, g, b, a, true);
        Render3DUtils.renderAxisLine(modelViewMatrix, bx - cx, -64 - cy, bz + 16 - cz, bx - cx, 320 - cy, bz + 16 - cz, th, r, g, b, a, true);
        Render3DUtils.renderAxisLine(modelViewMatrix, bx + 16 - cx, -64 - cy, bz + 16 - cz, bx + 16 - cx, 320 - cy, bz + 16 - cz, th, r, g, b, a, true);

        // 4 horizontal edges at the nearest Y levels around the player (every 32 blocks)
        int baseY = Math.round(cy / 32.0f) * 32;
        for (int dy = -32; dy <= 32; dy += 32) {
            int y = baseY + dy;
            if (y < -64 || y > 320) continue;
            float yOff = y - cy;
            float ha = a * (1.0f - Math.abs(dy) / 64.0f) * 0.5f;
            Render3DUtils.renderAxisLine(modelViewMatrix, bx - cx, yOff, bz - cz, bx + 16 - cx, yOff, bz - cz, th, r, g, b, ha, true);
            Render3DUtils.renderAxisLine(modelViewMatrix, bx + 16 - cx, yOff, bz - cz, bx + 16 - cx, yOff, bz + 16 - cz, th, r, g, b, ha, true);
            Render3DUtils.renderAxisLine(modelViewMatrix, bx + 16 - cx, yOff, bz + 16 - cz, bx - cx, yOff, bz + 16 - cz, th, r, g, b, ha, true);
            Render3DUtils.renderAxisLine(modelViewMatrix, bx - cx, yOff, bz + 16 - cz, bx - cx, yOff, bz - cz, th, r, g, b, ha, true);
        }
    }
}
