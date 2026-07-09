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
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

<<<<<<< HEAD
import ravex.modules.combat.AnchorAura;
import ravex.modules.combat.AutoCrystal;
import ravex.modules.combat.BasePlace;
import ravex.modules.combat.Breaker;
import ravex.modules.combat.HoleFill;
import ravex.modules.combat.SelfTrap;
=======
import ravex.utility.render.Render3DUtils;
import ravex.modules.player.Xray;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import ravex.modules.combat.Surround;
import ravex.modules.combat.Trap;
import ravex.modules.combat.WebSelf;
import ravex.modules.misc.AutoPortal;
import ravex.modules.misc.NewChunks;
import ravex.modules.misc.StashFinder;
import ravex.modules.player.AirPlace;
import ravex.modules.player.PacketMine;
import ravex.modules.render.BlockOutline;
import ravex.modules.render.Borders;
<<<<<<< HEAD
import ravex.modules.render.BreadCrumbs;
import ravex.modules.render.ESP;
import ravex.modules.render.Particles;
import ravex.modules.render.Trails;
import ravex.modules.render.Waypoint;
import ravex.modules.combat.PearlTarget;
import ravex.modules.world.ChestAura;
import ravex.modules.world.ECFarmer;
import ravex.modules.world.nuker.Nuker;
import ravex.modules.world.PVEUtils;
import ravex.modules.world.Scaffold;
import ravex.modules.world.TreeCutter;
import ravex.modules.world.AutoTunnel;
import ravex.utility.render.Render3DUtils;


=======
import ravex.modules.render.ESP;
import ravex.modules.render.HoleESP;
import ravex.modules.render.VoidESP;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3

@Mixin(LevelRenderer.class)
public class MixinLevelRenderer {
    private static final Matrix4f REUSABLE_MATRIX = new Matrix4f();
    private static long lastAnimTime = 0;

    
    private static double apX = 0, apY = 0, apZ = 0;
    private static float apAlpha = 0.0f;
    private static double apSize = 0.0;
    private static boolean apInitialized = false;

    
    private static double scX = 0, scY = 0, scZ = 0;
    private static float scAlpha = 0.0f;
    private static double scSize = 0.0;
    private static boolean scInitialized = false;

    
    private static double boX = 0, boY = 0, boZ = 0;
    private static float boAlpha = 0.0f;
    private static boolean boInitialized = false;

    @ModifyVariable(
        method = "renderLevel",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0
    )
    private boolean disableVanillaBlockOutline(boolean original) {
        
<<<<<<< HEAD
        if (BlockOutline.maybeEnabled()) {
=======
        if (BlockOutline.INSTANCE.getEnabled()) {
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            return false;
        }
        return original;
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

        Render3DUtils.beginFrame();
        long now = System.currentTimeMillis();
        if (lastAnimTime == 0) lastAnimTime = now;
        long deltaMs = now - lastAnimTime;
        lastAnimTime = now;
        if (deltaMs > 100) deltaMs = 16;

        float factor = Math.min(1.0f, (deltaMs / 50.0f) * 0.25f);
        double slideFactor = Math.min(1.0, (deltaMs / 50.0) * 0.35);

        
<<<<<<< HEAD
        if (BlockOutline.maybeEnabled()) {
=======
        if (BlockOutline.INSTANCE.getEnabled()) {
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            HitResult hit = mc.hitResult;
            if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockHit = (BlockHitResult) hit;
                BlockPos pos = blockHit.getBlockPos();
                double tx = pos.getX();
                double ty = pos.getY();
                double tz = pos.getZ();

                if (!boInitialized) {
                    boX = tx; boY = ty; boZ = tz;
                    boInitialized = true;
                } else {
                    boX += (tx - boX) * slideFactor;
                    boY += (ty - boY) * slideFactor;
                    boZ += (tz - boZ) * slideFactor;
                }
                boAlpha += (1.0f - boAlpha) * factor;
            } else {
                boAlpha += (0.0f - boAlpha) * factor;
                if (boAlpha < 0.01f) {
                    boAlpha = 0.0f;
                    boInitialized = false;
                }
            }

            if (boAlpha > 0.0f) {
<<<<<<< HEAD
                int color = BlockOutline.itz().color.getValue();
=======
                int color = BlockOutline.INSTANCE.color.getValue();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
                float r = ((color >> 16) & 0xFF) / 255.0f;
                float g = ((color >> 8) & 0xFF) / 255.0f;
                float b = (color & 0xFF) / 255.0f;
                float baseAlpha = ((color >> 24) & 0xFF) / 255.0f;
                float a = baseAlpha * boAlpha;
<<<<<<< HEAD
                boolean filled = BlockOutline.itz().filled.getValue();
                float lineWidth = BlockOutline.itz().width.getValue().floatValue();
=======
                boolean filled = BlockOutline.INSTANCE.filled.getValue();
                float lineWidth = BlockOutline.INSTANCE.width.getValue().floatValue();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3

                try {
                    float bx = (float)(boX - camPos.x);
                    float by = (float)(boY - camPos.y);
                    float bz = (float)(boZ - camPos.z);
                    float sx = bx + 1.0f;
                    float sy = by + 1.0f;
                    float sz = bz + 1.0f;
                    float edgeWidth = lineWidth * 0.02f;
                    if (filled) {
                        modelViewMatrix.translate(bx, by, bz, REUSABLE_MATRIX);
                        Render3DUtils.batchFilledBox(REUSABLE_MATRIX, 1.002, r, g, b, a * 0.25f, true);
                    }
                    Render3DUtils.batchAxisLine(modelViewMatrix, bx, by, bz, sx, by, bz, edgeWidth, r, g, b, a, true);
                    Render3DUtils.batchAxisLine(modelViewMatrix, sx, by, bz, sx, by, sz, edgeWidth, r, g, b, a, true);
                    Render3DUtils.batchAxisLine(modelViewMatrix, sx, by, sz, bx, by, sz, edgeWidth, r, g, b, a, true);
                    Render3DUtils.batchAxisLine(modelViewMatrix, bx, by, sz, bx, by, bz, edgeWidth, r, g, b, a, true);
                    Render3DUtils.batchAxisLine(modelViewMatrix, bx, sy, bz, sx, sy, bz, edgeWidth, r, g, b, a, true);
                    Render3DUtils.batchAxisLine(modelViewMatrix, sx, sy, bz, sx, sy, sz, edgeWidth, r, g, b, a, true);
                    Render3DUtils.batchAxisLine(modelViewMatrix, sx, sy, sz, bx, sy, sz, edgeWidth, r, g, b, a, true);
                    Render3DUtils.batchAxisLine(modelViewMatrix, bx, sy, sz, bx, sy, bz, edgeWidth, r, g, b, a, true);
                    Render3DUtils.batchAxisLine(modelViewMatrix, bx, by, bz, bx, sy, bz, edgeWidth, r, g, b, a, true);
                    Render3DUtils.batchAxisLine(modelViewMatrix, sx, by, bz, sx, sy, bz, edgeWidth, r, g, b, a, true);
                    Render3DUtils.batchAxisLine(modelViewMatrix, sx, by, sz, sx, sy, sz, edgeWidth, r, g, b, a, true);
                    Render3DUtils.batchAxisLine(modelViewMatrix, bx, by, sz, bx, sy, sz, edgeWidth, r, g, b, a, true);
                } catch (Exception ignored) {}
            }
        } else {
            boAlpha += (0.0f - boAlpha) * factor;
            if (boAlpha < 0.01f) {
                boAlpha = 0.0f;
                boInitialized = false;
            }
        }

        
<<<<<<< HEAD
        AirPlace ap = AirPlace.itz();
=======
        ravex.modules.player.AirPlace ap = ravex.modules.player.AirPlace.INSTANCE;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
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

        if (AirPlace.maybeEnabled()) {
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

        
<<<<<<< HEAD
        Scaffold sc = Scaffold.itz();
        if (sc.getEnabled() && sc.render.getValue()) {
            var currPos = sc.getCurrentPos();
            if (currPos == null) {} else {
            double tx = currPos.getX();
            double ty = currPos.getY();
            double tz = currPos.getZ();
=======
        ravex.modules.world.Scaffold sc = ravex.modules.world.Scaffold.INSTANCE;
        if (sc.getEnabled() && sc.render.getValue() && sc.currentTarget != null) {
            double tx = sc.currentTarget.getX();
            double ty = sc.currentTarget.getY();
            double tz = sc.currentTarget.getZ();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
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
            } } else {
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

        if (Scaffold.maybeEnabled()) {
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

        
<<<<<<< HEAD
        if (ChestAura.maybeEnabled() && ChestAura.itz().render.getValue() && !ChestAura.placedChests.isEmpty()) {
=======
        if (ravex.modules.world.ChestAura.INSTANCE.getEnabled() && ravex.modules.world.ChestAura.INSTANCE.render.getValue() && !ravex.modules.world.ChestAura.placedChests.isEmpty()) {
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            long chestNow = System.currentTimeMillis();
            double durationMs = ChestAura.itz().fadeSpeed.getValue() * 1000.0;
            int color = ChestAura.itz().highlightColor.getValue();
            float r = ((color >> 16) & 0xFF) / 255.0f;
            float g = ((color >> 8) & 0xFF) / 255.0f;
            float b = (color & 0xFF) / 255.0f;
            boolean filled = ChestAura.itz().filled.getValue();

            Matrix4f mat = new Matrix4f();
            for (ravex.modules.world.ChestAura.PlacedChest chest : ravex.modules.world.ChestAura.placedChests) {
                long elapsed = chestNow - chest.placeTime;
                if (elapsed > durationMs) continue;

                float progress = (float)(elapsed / durationMs);
                float alpha = 1.0f - progress;

                try {
                    modelViewMatrix.translate(
<<<<<<< HEAD
                        (float)(ravex.utility.misc.block.BlockUtility.unpackX(chest.packedPos) - camPos.x),
                        (float)(ravex.utility.misc.block.BlockUtility.unpackY(chest.packedPos) - camPos.y),
                        (float)(ravex.utility.misc.block.BlockUtility.unpackZ(chest.packedPos) - camPos.z),
=======
                        (float)(chest.pos.getX() - camPos.x),
                        (float)(chest.pos.getY() - camPos.y),
                        (float)(chest.pos.getZ() - camPos.z),
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
                        mat
                    );

                    double size = 1.002;
                    if (filled) {
                        Render3DUtils.batchFilledBox(mat, size, r, g, b, alpha * 0.25f);
                    }
                    Render3DUtils.batchWireframe(mat, size, r, g, b, alpha * 0.95f);
                    Render3DUtils.batchWireframe(mat, size * 1.03, r, g, b, alpha * 0.3f);
                } catch (Exception ignored) {}
            }
        }

        
<<<<<<< HEAD
        if (Surround.maybeEnabled()) {
=======
        if (Surround.INSTANCE.getEnabled()) {
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            synchronized (Surround.surroundBlocks) {
                for (BlockPos pos : Surround.surroundBlocks) {
                    if (pos == null) continue;
                    Vec3 blockPos = Vec3.atBottomCenterOf(pos);

                    float dx = (float)(pos.getX() - camPos.x);
                    float dy = (float)(pos.getY() - camPos.y);
                    float dz = (float)(pos.getZ() - camPos.z);
                    try {
                        modelViewMatrix.translate(dx, dy, dz, REUSABLE_MATRIX);

                        float a = Surround.renderAlpha * 0.85f;
                        float s = (float)Surround.renderSize;

                        Render3DUtils.batchFilledBox(REUSABLE_MATRIX, s,
                            Surround.renderR, Surround.renderG, Surround.renderB, a * 0.25f);
                        Render3DUtils.batchWireframe(REUSABLE_MATRIX, s,
                            Surround.renderR, Surround.renderG, Surround.renderB, a * 0.95f);
                        Render3DUtils.batchWireframe(REUSABLE_MATRIX, s * 1.03,
                            Surround.renderR, Surround.renderG, Surround.renderB, a * 0.3f);
                    } catch (Exception ignored) {}
                }
            }
        }

        
<<<<<<< HEAD
        Trap trap = Trap.itz();
=======
        ravex.modules.combat.Trap trap = ravex.modules.combat.Trap.INSTANCE;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        if (trap.getEnabled() && trap.render.getValue()) {
            synchronized (ravex.modules.combat.Trap.trapBlocks) {
                for (BlockPos pos : ravex.modules.combat.Trap.trapBlocks) {
                    if (pos == null) continue;
                    float tx = (float)(pos.getX() - camPos.x);
                    float ty = (float)(pos.getY() - camPos.y);
                    float tz = (float)(pos.getZ() - camPos.z);

                    try {
                        modelViewMatrix.translate(tx, ty, tz, REUSABLE_MATRIX);

                        int c = trap.color.getValue();
                        float r = ((c >> 16) & 0xFF) / 255.0f;
                        float g = ((c >> 8) & 0xFF) / 255.0f;
                        float b = (c & 0xFF) / 255.0f;
                        float a = 0.35f;

                        double size = 1.002;
                        Render3DUtils.batchFilledBox(REUSABLE_MATRIX, size, r, g, b, a * 0.25f);
                        Render3DUtils.batchWireframe(REUSABLE_MATRIX, size, r, g, b, a * 0.95f);
                        Render3DUtils.batchWireframe(REUSABLE_MATRIX, size * 1.03, r, g, b, a * 0.2f);
                    } catch (Exception ignored) {}
                }
            }
        }

        
<<<<<<< HEAD
        SelfTrap selfTrap = SelfTrap.itz();
=======
        ravex.modules.combat.SelfTrap selfTrap = ravex.modules.combat.SelfTrap.INSTANCE;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        if (selfTrap.getEnabled() && selfTrap.render.getValue()) {
            for (BlockPos pos : ravex.modules.combat.SelfTrap.getSelfTrapBlocks()) {
                if (pos == null) continue;
                float sx = (float)(pos.getX() - camPos.x);
                float sy = (float)(pos.getY() - camPos.y);
                float sz = (float)(pos.getZ() - camPos.z);

                try {
                    modelViewMatrix.translate(sx, sy, sz, REUSABLE_MATRIX);

                    int c = selfTrap.color.getValue();
                    float r = ((c >> 16) & 0xFF) / 255.0f;
                    float g = ((c >> 8) & 0xFF) / 255.0f;
                    float b = (c & 0xFF) / 255.0f;
                    float a = ((c >> 24) & 0xFF) / 255.0f;

                    double size = 1.002;
                    Render3DUtils.batchFilledBox(REUSABLE_MATRIX, size, r, g, b, a * 0.25f);
                    Render3DUtils.batchWireframe(REUSABLE_MATRIX, size, r, g, b, a * 0.95f);
                } catch (Exception ignored) {}
            }
        }

        
<<<<<<< HEAD
        BasePlace basePlace = BasePlace.itz();
=======
        ravex.modules.combat.BasePlace basePlace = ravex.modules.combat.BasePlace.INSTANCE;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        if (basePlace.getEnabled() && basePlace.render.getValue() && ravex.modules.combat.BasePlace.getSimulatedPlacementBlock() != null) {
            BlockPos pos = ravex.modules.combat.BasePlace.getSimulatedPlacementBlock();
            try {
                modelViewMatrix.translate(
                    (float)(pos.getX() - camPos.x),
                    (float)(pos.getY() - camPos.y),
                    (float)(pos.getZ() - camPos.z),
                    REUSABLE_MATRIX
                );

                int c = basePlace.color.getValue();
                float r = ((c >> 16) & 0xFF) / 255.0f;
                float g = ((c >> 8) & 0xFF) / 255.0f;
                float b = (c & 0xFF) / 255.0f;
                float a = ((c >> 24) & 0xFF) / 255.0f;

                double size = 1.002;
                Render3DUtils.batchFilledBox(REUSABLE_MATRIX, size, r, g, b, a * 0.25f);
                Render3DUtils.batchWireframe(REUSABLE_MATRIX, size, r, g, b, a * 0.95f);
                Render3DUtils.batchWireframe(REUSABLE_MATRIX, size * 1.03, r, g, b, a * 0.2f);
            } catch (Exception ignored) {}
        }

        
<<<<<<< HEAD
        AnchorAura anchorAura = AnchorAura.itz();
=======
        ravex.modules.combat.AnchorAura anchorAura = ravex.modules.combat.AnchorAura.INSTANCE;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        if (anchorAura.getEnabled() && anchorAura.render.getValue() && ravex.modules.combat.AnchorAura.simulatedPlacementBlock != null) {
            BlockPos pos = ravex.modules.combat.AnchorAura.simulatedPlacementBlock;
            try {
                modelViewMatrix.translate(
                    (float)(pos.getX() - camPos.x),
                    (float)(pos.getY() - camPos.y),
                    (float)(pos.getZ() - camPos.z),
                    REUSABLE_MATRIX
                );

                int c = anchorAura.color.getValue();
                float r = ((c >> 16) & 0xFF) / 255.0f;
                float g = ((c >> 8) & 0xFF) / 255.0f;
                float b = (c & 0xFF) / 255.0f;
                float a = ((c >> 24) & 0xFF) / 255.0f;

                double size = 1.002;
                Render3DUtils.batchFilledBox(REUSABLE_MATRIX, size, r, g, b, a * 0.25f);
                Render3DUtils.batchWireframe(REUSABLE_MATRIX, size, r, g, b, a * 0.95f);
                Render3DUtils.batchWireframe(REUSABLE_MATRIX, size * 1.03, r, g, b, a * 0.2f);
            } catch (Exception ignored) {}
        }

<<<<<<< HEAD
        if (NewChunks.maybeEnabled()) {
            NewChunks.itz().render(modelViewMatrix, camera);
        }

        AutoCrystal ac = AutoCrystal.itz();
=======
        if (ravex.modules.misc.NewChunks.INSTANCE.getEnabled()) {
            ravex.modules.misc.NewChunks.INSTANCE.render(modelViewMatrix, camera);
        }

        ravex.modules.combat.AutoCrystal ac = ravex.modules.combat.AutoCrystal.INSTANCE;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        if (ac.getEnabled() && ac.renderPlacement.getValue() && ravex.modules.combat.AutoCrystal.currentPlacementBlock != null) {
            BlockPos p = ravex.modules.combat.AutoCrystal.currentPlacementBlock;
            try {
                modelViewMatrix.translate(
                    (float)(p.getX() - camPos.x),
                    (float)(p.getY() - camPos.y),
                    (float)(p.getZ() - camPos.z),
                    REUSABLE_MATRIX
                );

                double size = 1.002;
                Render3DUtils.batchFilledBox(REUSABLE_MATRIX, size, 0.2f, 0.8f, 1.0f, 0.22f);
                Render3DUtils.batchWireframe(REUSABLE_MATRIX, size, 0.2f, 0.8f, 1.0f, 0.85f);
                Render3DUtils.batchWireframe(REUSABLE_MATRIX, size * 1.03, 0.2f, 0.8f, 1.0f, 0.2f);
            } catch (Exception ignored) {}
        }

        
<<<<<<< HEAD
        if (StashFinder.maybeEnabled()) {
            double maxDist = StashFinder.itz().range.getValue();
            for (StashFinder.StashEntry stash : StashFinder.itz().getStashes()) {
=======
        if (ravex.modules.misc.StashFinder.INSTANCE.getEnabled()) {
            double maxDist = ravex.modules.misc.StashFinder.INSTANCE.range.getValue();
            for (ravex.modules.misc.StashFinder.StashEntry stash : ravex.modules.misc.StashFinder.INSTANCE.getStashes()) {
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
                Vec3 stashPos = Vec3.atBottomCenterOf(stash.pos);
                double dist = stashPos.distanceTo(camPos);
                if (dist > maxDist) continue;

                try {
                    modelViewMatrix.translate(
                        (float)(stashPos.x - camPos.x),
                        (float)(stashPos.y - camPos.y + 0.5),
                        (float)(stashPos.z - camPos.z),
                        REUSABLE_MATRIX
                    );

                    float s = (float)Math.max(0.5, Math.min(3.0, 64.0 / dist));
                    REUSABLE_MATRIX.scale(s, s, s);

                    Render3DUtils.batchFilledBox(REUSABLE_MATRIX, 1.0, 1.0f, 0.5f, 0.0f, 0.3f);
                    Render3DUtils.batchWireframe(REUSABLE_MATRIX, 1.0, 1.0f, 0.5f, 0.0f, 0.8f);
                } catch (Exception ignored) {}
            }
        }

        
<<<<<<< HEAD
        if (BreadCrumbs.maybeEnabled()) {
=======
        if (ravex.modules.render.BreadCrumbs.INSTANCE.getEnabled()) {
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            ravex.modules.render.BreadCrumbs.renderTrails(modelViewMatrix, camPos);
        }

        
<<<<<<< HEAD
        if (Trails.maybeEnabled()) {
=======
        if (ravex.modules.render.Trails.INSTANCE.getEnabled()) {
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            ravex.modules.render.Trails.renderTrails(modelViewMatrix, camPos);
        }

        
<<<<<<< HEAD
        if (Particles.maybeEnabled()) {
=======
        if (ravex.modules.render.Particles.INSTANCE.getEnabled()) {
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            ravex.modules.render.Particles.renderParticles(modelViewMatrix, camPos);
        }

        
<<<<<<< HEAD
        TreeCutter tc = TreeCutter.itz();
        BlockPos mp = ravex.modules.world.TreeCutter.getMiningPos();
        if (tc.getEnabled() && tc.render.getValue() && mp != null) {
            BlockPos p = mp;
=======
        ravex.modules.world.TreeCutter tc = ravex.modules.world.TreeCutter.INSTANCE;
        if (tc.getEnabled() && tc.render.getValue() && ravex.modules.world.TreeCutter.currentMiningBlock != null) {
            BlockPos p = ravex.modules.world.TreeCutter.currentMiningBlock;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            try {
                modelViewMatrix.translate(
                        (float)(p.getX() - camPos.x),
                        (float)(p.getY() - camPos.y),
                        (float)(p.getZ() - camPos.z),
                        REUSABLE_MATRIX
                    );

                int c = tc.color.getValue();
                float r = ((c >> 16) & 0xFF) / 255.0f;
                float g = ((c >> 8) & 0xFF) / 255.0f;
                float b = (c & 0xFF) / 255.0f;
                float a = 0.35f;

                double size = 1.002;
                Render3DUtils.batchFilledBox(REUSABLE_MATRIX, size, r, g, b, a * 0.25f);
                Render3DUtils.batchWireframe(REUSABLE_MATRIX, size, r, g, b, a * 0.95f);
                Render3DUtils.batchWireframe(REUSABLE_MATRIX, size * 1.03, r, g, b, a * 0.2f);
            } catch (Exception ignored) {}
        }

        
<<<<<<< HEAD
        WebSelf ws = WebSelf.itz();
=======
        ravex.modules.combat.WebSelf ws = ravex.modules.combat.WebSelf.INSTANCE;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        if (ws.getEnabled() && ws.render.getValue() && ravex.modules.combat.WebSelf.targetPos != null) {
            BlockPos p = ravex.modules.combat.WebSelf.targetPos;
            try {
                modelViewMatrix.translate(
                        (float)(p.getX() - camPos.x),
                        (float)(p.getY() - camPos.y),
                        (float)(p.getZ() - camPos.z),
                        REUSABLE_MATRIX
                    );
                float r = ravex.modules.combat.WebSelf.renderR;
                float g = ravex.modules.combat.WebSelf.renderG;
                float b = ravex.modules.combat.WebSelf.renderB;
                double size = 1.002;
                Render3DUtils.batchFilledBox(REUSABLE_MATRIX, size, r, g, b, 0.20f);
                Render3DUtils.batchWireframe(REUSABLE_MATRIX, size, r, g, b, 0.85f);
                Render3DUtils.batchWireframe(REUSABLE_MATRIX, size * 1.03, r, g, b, 0.25f);
            } catch (Exception ignored) {}
        }

        
<<<<<<< HEAD
        Breaker br = Breaker.itz();
=======
        ravex.modules.combat.Breaker br = ravex.modules.combat.Breaker.INSTANCE;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        if (br.getEnabled() && br.render.getValue() && ravex.modules.combat.Breaker.currentMiningBlock != null) {
            BlockPos p = ravex.modules.combat.Breaker.currentMiningBlock;
            try {
                modelViewMatrix.translate(
                        (float)(p.getX() - camPos.x),
                        (float)(p.getY() - camPos.y),
                        (float)(p.getZ() - camPos.z),
                        REUSABLE_MATRIX
                    );

                int c = br.color.getValue();
                float r = ((c >> 16) & 0xFF) / 255.0f;
                float g = ((c >> 8) & 0xFF) / 255.0f;
                float b = (c & 0xFF) / 255.0f;
                float a = 0.35f;

                double size = 1.002;
                Render3DUtils.batchFilledBox(REUSABLE_MATRIX, size, r, g, b, a * 0.25f);
                Render3DUtils.batchWireframe(REUSABLE_MATRIX, size, r, g, b, a * 0.95f);
            } catch (Exception ignored) {}
        }

        
<<<<<<< HEAD
        AutoTunnel at = AutoTunnel.itz();
        if (at.getEnabled() && at.render.getValue()) {
            BlockPos p = ravex.modules.world.AutoTunnel.getCurrentTarget();
            if (p != null) try {
=======
        ravex.modules.world.AutoTunnel at = ravex.modules.world.AutoTunnel.INSTANCE;
        if (at.getEnabled() && at.render.getValue() && ravex.modules.world.AutoTunnel.currentTarget != null) {
            BlockPos p = ravex.modules.world.AutoTunnel.currentTarget;
            try {
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
                modelViewMatrix.translate(
                        (float)(p.getX() - camPos.x),
                        (float)(p.getY() - camPos.y),
                        (float)(p.getZ() - camPos.z),
                        REUSABLE_MATRIX
                    );

                int c = at.color.getValue();
                float r = ((c >> 16) & 0xFF) / 255.0f;
                float g = ((c >> 8) & 0xFF) / 255.0f;
                float b = (c & 0xFF) / 255.0f;
                float a = ((c >> 24) & 0xFF) / 255.0f;

                double size = 1.002;
                Render3DUtils.batchFilledBox(REUSABLE_MATRIX, size, r, g, b, a * 0.25f);
                Render3DUtils.batchWireframe(REUSABLE_MATRIX, size, r, g, b, a * 0.95f);
                Render3DUtils.batchWireframe(REUSABLE_MATRIX, size * 1.03, r, g, b, a * 0.2f);
            } catch (Exception ignored) {}
        }

        
<<<<<<< HEAD
        Nuker nk = Nuker.itz();
=======
        ravex.modules.world.nuker.Nuker nk = ravex.modules.world.nuker.Nuker.INSTANCE;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        if (nk.getEnabled() && nk.render.getValue() && ravex.modules.world.nuker.Nuker.currentTarget != null) {
            BlockPos p = ravex.modules.world.nuker.Nuker.currentTarget;
            try {
                modelViewMatrix.translate(
                        (float)(p.getX() - camPos.x),
                        (float)(p.getY() - camPos.y),
                        (float)(p.getZ() - camPos.z),
                        REUSABLE_MATRIX
                    );

                int c = nk.color.getValue();
                float r = ((c >> 16) & 0xFF) / 255.0f;
                float g = ((c >> 8) & 0xFF) / 255.0f;
                float b = (c & 0xFF) / 255.0f;
                float a = ((c >> 24) & 0xFF) / 255.0f;

                double size = 1.002;
                Render3DUtils.batchFilledBox(REUSABLE_MATRIX, size, r, g, b, a * 0.25f);
                Render3DUtils.batchWireframe(REUSABLE_MATRIX, size, r, g, b, a * 0.95f);
                Render3DUtils.batchWireframe(REUSABLE_MATRIX, size * 1.03, r, g, b, a * 0.2f);
            } catch (Exception ignored) {}
        }

        
<<<<<<< HEAD
        PVEUtils sm = PVEUtils.itz();
        if (sm.getEnabled() && sm.mode.getValue().equals("AutoSmelt") && sm.smeltRender.getValue() && ravex.modules.world.PVEUtils.smeltTarget != null) {
            BlockPos p = ravex.modules.world.PVEUtils.smeltTarget;
            try {
                modelViewMatrix.translate((float)(p.getX() - camPos.x), (float)(p.getY() - camPos.y), (float)(p.getZ() - camPos.z), REUSABLE_MATRIX);
                int c = sm.smeltColor.getValue();
=======
        ravex.modules.world.AutoSmelt sm = ravex.modules.world.AutoSmelt.INSTANCE;
        if (sm.getEnabled() && sm.render.getValue() && ravex.modules.world.AutoSmelt.currentTarget != null) {
            BlockPos p = ravex.modules.world.AutoSmelt.currentTarget;
            try {
                modelViewMatrix.translate((float)(p.getX() - camPos.x), (float)(p.getY() - camPos.y), (float)(p.getZ() - camPos.z), REUSABLE_MATRIX);
                int c = sm.color.getValue();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
                float r = ((c >> 16) & 0xFF) / 255.0f;
                float g = ((c >> 8) & 0xFF) / 255.0f;
                float b = (c & 0xFF) / 255.0f;
                float a = ((c >> 24) & 0xFF) / 255.0f;
                double size = 1.002;
                Render3DUtils.batchFilledBox(REUSABLE_MATRIX, size, r, g, b, a * 0.25f);
                Render3DUtils.batchWireframe(REUSABLE_MATRIX, size, r, g, b, a * 0.95f);
                Render3DUtils.batchWireframe(REUSABLE_MATRIX, size * 1.03, r, g, b, a * 0.2f);
            } catch (Exception ignored) {}
        }

        
<<<<<<< HEAD
        PVEUtils bw = PVEUtils.itz();
        if (bw.getEnabled() && bw.mode.getValue().equals("AutoBrew") && bw.brewRender.getValue()) {
            BlockPos p = ravex.modules.world.PVEUtils.getBrewTarget();
            if (p != null) try {
                modelViewMatrix.translate((float)(p.getX() - camPos.x), (float)(p.getY() - camPos.y), (float)(p.getZ() - camPos.z), REUSABLE_MATRIX);
                int c = bw.brewColor.getValue();
=======
        ravex.modules.world.AutoBrew bw = ravex.modules.world.AutoBrew.INSTANCE;
        if (bw.getEnabled() && bw.render.getValue() && ravex.modules.world.AutoBrew.currentTarget != null) {
            BlockPos p = ravex.modules.world.AutoBrew.currentTarget;
            try {
                modelViewMatrix.translate((float)(p.getX() - camPos.x), (float)(p.getY() - camPos.y), (float)(p.getZ() - camPos.z), REUSABLE_MATRIX);
                int c = bw.color.getValue();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
                float r = ((c >> 16) & 0xFF) / 255.0f;
                float g = ((c >> 8) & 0xFF) / 255.0f;
                float b = (c & 0xFF) / 255.0f;
                float a = ((c >> 24) & 0xFF) / 255.0f;
                double size = 1.002;
                Render3DUtils.batchFilledBox(REUSABLE_MATRIX, size, r, g, b, a * 0.25f);
                Render3DUtils.batchWireframe(REUSABLE_MATRIX, size, r, g, b, a * 0.95f);
                Render3DUtils.batchWireframe(REUSABLE_MATRIX, size * 1.03, r, g, b, a * 0.2f);
            } catch (Exception ignored) {}
        }

        
<<<<<<< HEAD
        ECFarmer ec = ECFarmer.itz();
        if (ec.getEnabled() && ec.render.getValue()) {
            BlockPos p = ravex.modules.world.ECFarmer.getCurrentTarget();
            if (p != null) try {
=======
        ravex.modules.world.ECFarmer ec = ravex.modules.world.ECFarmer.INSTANCE;
        if (ec.getEnabled() && ec.render.getValue() && ravex.modules.world.ECFarmer.currentTarget != null) {
            BlockPos p = ravex.modules.world.ECFarmer.currentTarget;
            try {
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
                modelViewMatrix.translate((float)(p.getX() - camPos.x), (float)(p.getY() - camPos.y), (float)(p.getZ() - camPos.z), REUSABLE_MATRIX);
                int c = ec.color.getValue();
                float r = ((c >> 16) & 0xFF) / 255.0f;
                float g = ((c >> 8) & 0xFF) / 255.0f;
                float b = (c & 0xFF) / 255.0f;
                float a = ((c >> 24) & 0xFF) / 255.0f;
                double size = 1.002;
                Render3DUtils.batchFilledBox(REUSABLE_MATRIX, size, r, g, b, a * 0.25f);
                Render3DUtils.batchWireframe(REUSABLE_MATRIX, size, r, g, b, a * 0.95f);
                Render3DUtils.batchWireframe(REUSABLE_MATRIX, size * 1.03, r, g, b, a * 0.2f);
            } catch (Exception ignored) {}
        }

        
<<<<<<< HEAD
        AutoPortal pb = AutoPortal.itz();
        if (pb.getEnabled() && pb.render.getValue()) {
            BlockPos p = ravex.modules.misc.AutoPortal.getCurrentTarget();
            if (p != null) try {
=======
        ravex.modules.misc.PortalBuild pb = ravex.modules.misc.PortalBuild.INSTANCE;
        if (pb.getEnabled() && pb.render.getValue() && ravex.modules.misc.PortalBuild.currentTarget != null) {
            BlockPos p = ravex.modules.misc.PortalBuild.currentTarget;
            try {
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
                modelViewMatrix.translate((float)(p.getX() - camPos.x), (float)(p.getY() - camPos.y), (float)(p.getZ() - camPos.z), REUSABLE_MATRIX);
                int c = pb.color.getValue();
                float r = ((c >> 16) & 0xFF) / 255.0f;
                float g = ((c >> 8) & 0xFF) / 255.0f;
                float b = (c & 0xFF) / 255.0f;
                float a = ((c >> 24) & 0xFF) / 255.0f;
                double size = 1.002;
                Render3DUtils.batchFilledBox(REUSABLE_MATRIX, size, r, g, b, a * 0.25f);
                Render3DUtils.batchWireframe(REUSABLE_MATRIX, size, r, g, b, a * 0.95f);
                Render3DUtils.batchWireframe(REUSABLE_MATRIX, size * 1.03, r, g, b, a * 0.2f);
            } catch (Exception ignored) {}
        }

        
<<<<<<< HEAD
        HoleFill hf = HoleFill.itz();
=======
        ravex.modules.combat.HoleFill hf = ravex.modules.combat.HoleFill.INSTANCE;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        if (hf.getEnabled() && hf.render.getValue()) {
            for (var hole : ravex.modules.combat.HoleFill.holePositions) {
                if (hole == null) continue;
                try {
<<<<<<< HEAD
                    modelViewMatrix.translate(
                        (float)(ravex.utility.misc.block.BlockUtility.unpackX(hole) - camPos.x),
                        (float)(ravex.utility.misc.block.BlockUtility.unpackY(hole) - camPos.y),
                        (float)(ravex.utility.misc.block.BlockUtility.unpackZ(hole) - camPos.z),
                        REUSABLE_MATRIX);
=======
                    modelViewMatrix.translate((float)(hole.getX() - camPos.x), (float)(hole.getY() - camPos.y), (float)(hole.getZ() - camPos.z), REUSABLE_MATRIX);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
                    int c = hf.color.getValue();
                    float r = ((c >> 16) & 0xFF) / 255.0f;
                    float g = ((c >> 8) & 0xFF) / 255.0f;
                    float b = (c & 0xFF) / 255.0f;
                    float a = ((c >> 24) & 0xFF) / 255.0f;
                    double size = 1.002;
                    Render3DUtils.batchFilledBox(REUSABLE_MATRIX, size, r, g, b, a * 0.15f);
                    Render3DUtils.batchWireframe(REUSABLE_MATRIX, size, r, g, b, a * 0.85f);
                } catch (Exception ignored) {}
            }
        }

        
<<<<<<< HEAD
        if (Borders.maybeEnabled()) {
            int rd = Borders.itz().renderDistance.getValue().intValue();
            boolean showCurrent = Borders.itz().showCurrentChunk.getValue();
            boolean showAll = Borders.itz().showChunkBorders.getValue();
            int lc = Borders.itz().chunkColor.getValue();
=======
        if (Borders.INSTANCE.getEnabled()) {
            int rd = Borders.INSTANCE.renderDistance.getValue().intValue();
            boolean showCurrent = Borders.INSTANCE.showCurrentChunk.getValue();
            boolean showAll = Borders.INSTANCE.showChunkBorders.getValue();
            int lc = Borders.INSTANCE.chunkColor.getValue();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            float lr = ((lc >> 16) & 0xFF) / 255.0f;
            float lg = ((lc >> 8) & 0xFF) / 255.0f;
            float lb = (lc & 0xFF) / 255.0f;
            float la = ((lc >> 24) & 0xFF) / 255.0f;
<<<<<<< HEAD
            float lw = Borders.itz().lineWidth.getValue().floatValue();
=======
            float lw = Borders.INSTANCE.lineWidth.getValue().floatValue();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3

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
<<<<<<< HEAD
                int cc = Borders.itz().currentColor.getValue();
=======
                int cc = Borders.INSTANCE.currentColor.getValue();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
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

        
<<<<<<< HEAD
        if (ESP.maybeEnabled() && ESP.itz().mode.getValue().equals("Tunnels")) {
            int tunnelColorVal = ESP.itz().tunnelColor.getValue();
=======
        if (ESP.INSTANCE.getEnabled() && ESP.INSTANCE.mode.getValue().equals("Tunnels")) {
            int tunnelColorVal = ESP.INSTANCE.tunnelColor.getValue();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            float tr = ((tunnelColorVal >> 16) & 0xFF) / 255.0f;
            float tg = ((tunnelColorVal >> 8) & 0xFF) / 255.0f;
            float tb = (tunnelColorVal & 0xFF) / 255.0f;
            float ta = ((tunnelColorVal >> 24) & 0xFF) / 255.0f;
<<<<<<< HEAD
            boolean filled = ESP.itz().tunnelFilled.getValue();
            boolean wire = ESP.itz().tunnelWireframe.getValue();
            for (BlockPos pos : ESP.itz().getTunnelBlocks()) {
=======
            boolean filled = ESP.INSTANCE.tunnelFilled.getValue();
            boolean wire = ESP.INSTANCE.tunnelWireframe.getValue();
            for (BlockPos pos : ESP.INSTANCE.getTunnelBlocks()) {
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
                try {
                    modelViewMatrix.translate((float)(pos.getX() - camPos.x), (float)(pos.getY() - camPos.y), (float)(pos.getZ() - camPos.z), REUSABLE_MATRIX);
                    if (filled) Render3DUtils.batchFilledBox(REUSABLE_MATRIX, 1.002, tr, tg, tb, ta * 0.3f);
                    if (wire) Render3DUtils.batchWireframe(REUSABLE_MATRIX, 1.002, tr, tg, tb, ta * 0.85f);
                } catch (Exception ignored) {}
            }
        }

        
<<<<<<< HEAD
        if (ESP.maybeEnabled() && ESP.itz().mode.getValue().equals("Holes")) {
            int c = ESP.itz().safeColor.getValue();
=======
        if (HoleESP.INSTANCE.getEnabled()) {
            int c = HoleESP.INSTANCE.safeColor.getValue();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            float hr = ((c >> 16) & 0xFF) / 255.0f;
            float hg = ((c >> 8) & 0xFF) / 255.0f;
            float hb = (c & 0xFF) / 255.0f;
            float ha = ((c >> 24) & 0xFF) / 255.0f;
            float hw = 0.04f;
<<<<<<< HEAD
            for (var pos : ESP.itz().getHoles()) {
=======
            for (var pos : HoleESP.INSTANCE.getHoles()) {
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
                try {
                    float px = (float)(pos.getX() - camPos.x);
                    float py = (float)(pos.getY() - camPos.y);
                    float pz = (float)(pos.getZ() - camPos.z);
<<<<<<< HEAD
                    if (ESP.itz().holeFilled.getValue()) {
                        modelViewMatrix.translate(px, py, pz, REUSABLE_MATRIX);
                        Render3DUtils.batchFilledBox(REUSABLE_MATRIX, 1.002, hr, hg, hb, ha * 0.3f, true);
                    }
                    if (ESP.itz().holeWireframe.getValue()) {
=======
                    if (HoleESP.INSTANCE.filled.getValue()) {
                        modelViewMatrix.translate(px, py, pz, REUSABLE_MATRIX);
                        Render3DUtils.batchFilledBox(REUSABLE_MATRIX, 1.002, hr, hg, hb, ha * 0.3f, true);
                    }
                    if (HoleESP.INSTANCE.wireframe.getValue()) {
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
                        
                        Render3DUtils.batchAxisLine(modelViewMatrix, px, py, pz, px + 1, py, pz, hw, hr, hg, hb, ha, true);
                        Render3DUtils.batchAxisLine(modelViewMatrix, px + 1, py, pz, px + 1, py, pz + 1, hw, hr, hg, hb, ha, true);
                        Render3DUtils.batchAxisLine(modelViewMatrix, px + 1, py, pz + 1, px, py, pz + 1, hw, hr, hg, hb, ha, true);
                        Render3DUtils.batchAxisLine(modelViewMatrix, px, py, pz + 1, px, py, pz, hw, hr, hg, hb, ha, true);

                        Render3DUtils.batchAxisLine(modelViewMatrix, px, py + 1, pz, px + 1, py + 1, pz, hw, hr, hg, hb, ha, true);
                        Render3DUtils.batchAxisLine(modelViewMatrix, px + 1, py + 1, pz, px + 1, py + 1, pz + 1, hw, hr, hg, hb, ha, true);
                        Render3DUtils.batchAxisLine(modelViewMatrix, px + 1, py + 1, pz + 1, px, py + 1, pz + 1, hw, hr, hg, hb, ha, true);
                        Render3DUtils.batchAxisLine(modelViewMatrix, px, py + 1, pz + 1, px, py + 1, pz, hw, hr, hg, hb, ha, true);

                        Render3DUtils.batchAxisLine(modelViewMatrix, px, py, pz, px, py + 1, pz, hw, hr, hg, hb, ha, true);
                        Render3DUtils.batchAxisLine(modelViewMatrix, px + 1, py, pz, px + 1, py + 1, pz, hw, hr, hg, hb, ha, true);
                        Render3DUtils.batchAxisLine(modelViewMatrix, px + 1, py, pz + 1, px + 1, py + 1, pz + 1, hw, hr, hg, hb, ha, true);
                        Render3DUtils.batchAxisLine(modelViewMatrix, px, py, pz + 1, px, py + 1, pz + 1, hw, hr, hg, hb, ha, true);
                    }
                } catch (Exception ignored) {}
            }
        }

        
<<<<<<< HEAD
        if (ESP.maybeEnabled() && ESP.itz().mode.getValue().equals("Void")) {
            int vc = ESP.itz().voidColor.getValue();
=======
        if (VoidESP.INSTANCE.getEnabled()) {
            int vc = VoidESP.INSTANCE.voidColor.getValue();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            float vr = ((vc >> 16) & 0xFF) / 255.0f;
            float vg = ((vc >> 8) & 0xFF) / 255.0f;
            float vb = (vc & 0xFF) / 255.0f;
            float va = ((vc >> 24) & 0xFF) / 255.0f;
<<<<<<< HEAD
            boolean vf = ESP.itz().voidFilled.getValue();
            boolean vw = ESP.itz().voidWireframe.getValue();
            for (BlockPos pos : ESP.itz().getVoidBlocks()) {
=======
            boolean vf = VoidESP.INSTANCE.filled.getValue();
            boolean vw = VoidESP.INSTANCE.wireframe.getValue();
            for (BlockPos pos : VoidESP.INSTANCE.getVoidBlocks()) {
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
                try {
                    modelViewMatrix.translate((float)(pos.getX() - camPos.x), 0, (float)(pos.getZ() - camPos.z), REUSABLE_MATRIX);
                    if (vf) Render3DUtils.batchFilledBox(REUSABLE_MATRIX, 16.0, vr, vg, vb, va * 0.15f);
                    if (vw) Render3DUtils.batchWireframe(REUSABLE_MATRIX, 16.0, vr, vg, vb, va * 0.4f);
                } catch (Exception ignored) {}
            }
        }

        
<<<<<<< HEAD
        PacketMine pm = PacketMine.itz();
=======
        ravex.modules.player.PacketMine pm = ravex.modules.player.PacketMine.INSTANCE;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        if (pm.getEnabled() && pm.render.getValue()) {
            long globalTime = System.currentTimeMillis();
            for (var mb : ravex.modules.player.PacketMine.miningBlocks) {
                if (mb == null || mb.pos == null) continue;
                try {
                    modelViewMatrix.translate((float)(mb.pos.getX() - camPos.x), (float)(mb.pos.getY() - camPos.y), (float)(mb.pos.getZ() - camPos.z), REUSABLE_MATRIX);

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
                    Render3DUtils.batchFilledBox(REUSABLE_MATRIX, size, flashR * pulse, flashG * pulse, flashB * pulse, fillAlpha);

                    float wireAlpha = (0.3f + 0.5f * (1.0f - progress)) * fadeOut;
                    Render3DUtils.batchWireframe(REUSABLE_MATRIX, size * 1.005, r, g, b, wireAlpha, 1.5f, true);
                    Render3DUtils.batchWireframe(REUSABLE_MATRIX, size * 1.015, r, g, b, wireAlpha * 0.5f, 1.5f, true);
                    Render3DUtils.batchWireframe(REUSABLE_MATRIX, size * 1.025, r, g, b, wireAlpha * 0.2f, 1.5f, true);

                    for (int i = 0; i < 3; i++) {
                        float phase = (float)(blockTime * 0.003 + i * 2.09);
                        float beamX = (float)Math.cos(phase) * 0.15f + 0.5f;
                        float beamZ = (float)Math.sin(phase) * 0.15f + 0.5f;
                        float beamY = (float)((phase % 6.28) / 6.28) * 1.2f - 0.1f;
                        if (beamY < 0) beamY += 1.2f;
                        modelViewMatrix.translate(
                                       (float)(mb.pos.getX() + beamX - 0.5f - camPos.x),
                                       (float)(mb.pos.getY() + beamY - 0.5f - camPos.y),
                                       (float)(mb.pos.getZ() + beamZ - 0.5f - camPos.z),
                                       REUSABLE_MATRIX);
                        Render3DUtils.batchFilledBox(REUSABLE_MATRIX, 0.04, flashR, flashG, flashB, 0.6f * (1.0f - progress) * fadeOut);
                    }
                } catch (Exception ignored) {}
            }
        }

        
<<<<<<< HEAD
        if (Waypoint.maybeEnabled()) {
=======
        if (ravex.modules.render.Waypoint.INSTANCE.getEnabled()) {
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            int wpColor = ravex.modules.render.Waypoint.getColor();
            float wr = ((wpColor >> 16) & 0xFF) / 255.0f;
            float wg = ((wpColor >> 8) & 0xFF) / 255.0f;
            float wb = (wpColor & 0xFF) / 255.0f;
            double wpSize = ravex.modules.render.Waypoint.getMarkerSize();
            double maxDist = ravex.modules.render.Waypoint.getRange();
            boolean showBeam = ravex.modules.render.Waypoint.isShowBeam();
            String currentDim = mc.level != null ? mc.level.dimension().identifier().toString() : null;

            for (var wp : ravex.modules.render.Waypoint.getWaypoints()) {
                if (currentDim != null && !wp.dimension().equals(currentDim)) continue;

                double dx = wp.x() + 0.5 - camPos.x;
                double dy = wp.y() + 0.5 - camPos.y;
                double dz = wp.z() + 0.5 - camPos.z;
                double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (dist > maxDist) continue;

                try {
                    if (showBeam) {
                        Render3DUtils.batchAxisLine(modelViewMatrix,
                            (float)(wp.x() + 0.5 - camPos.x),
                            (float)(wp.y() - camPos.y),
                            (float)(wp.z() + 0.5 - camPos.z),
                            (float)(wp.x() + 0.5 - camPos.x),
                            (float)(wp.y() + 0.5 - camPos.y),
                            (float)(wp.z() + 0.5 - camPos.z),
                            0.06f, wr, wg, wb, 0.4f, true);
                    }

                    modelViewMatrix.translate(
                        (float)(wp.x() + 0.5 - camPos.x),
                        (float)(wp.y() + 0.5 - camPos.y),
                        (float)(wp.z() + 0.5 - camPos.z),
                        REUSABLE_MATRIX
                    );

                    double size = 0.15 * (wpSize / 2.0);
                    Render3DUtils.batchFilledBox(REUSABLE_MATRIX, size, wr, wg, wb, 0.6f, true);
                    Render3DUtils.batchWireframe(REUSABLE_MATRIX, size * 1.5, wr, wg, wb, 0.9f, 2.0f, true);
                    Render3DUtils.batchWireframe(REUSABLE_MATRIX, size * 2.0, wr, wg, wb, 0.3f, 2.0f, true);
                } catch (Exception ignored) {}
            }
        }

        
<<<<<<< HEAD
        if (PearlTarget.maybeEnabled()) {
            try {
                PearlTarget.itz().render(modelViewMatrix, camera);
=======
        if (ravex.modules.combat.PearlTarget.INSTANCE.getEnabled()) {
            try {
                ravex.modules.combat.PearlTarget.INSTANCE.render(modelViewMatrix, camera);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            } catch (Exception ignored) {}
        }

        Render3DUtils.endFrame();
    }

    private void renderBlockHighlight(Vec3 highlightPos, float alpha, double size, float r, float g, float b, Vec3 camPos, Matrix4f modelViewMatrix) {
        if (highlightPos == null || alpha <= 0.01f) return;

        try {
            modelViewMatrix.translate(
                    (float)(highlightPos.x - camPos.x),
                    (float)(highlightPos.y - camPos.y),
                    (float)(highlightPos.z - camPos.z),
                    REUSABLE_MATRIX
                );

            float filledAlpha = alpha * 0.3f;
            float lineAlpha = alpha * 0.95f;

            Render3DUtils.batchFilledBox(REUSABLE_MATRIX, size, r, g, b, filledAlpha);
            Render3DUtils.batchWireframe(REUSABLE_MATRIX, size, r, g, b, lineAlpha);
            Render3DUtils.batchWireframe(REUSABLE_MATRIX, size * 1.02, r, g, b, lineAlpha * 0.4f);
        } catch (Exception ignored) {}
    }

    private void renderChunkBorderLines(Matrix4f modelViewMatrix, int bx, int bz, float r, float g, float b, float a, Vec3 camPos) {
        float cx = (float)camPos.x;
        float cy = (float)camPos.y;
        float cz = (float)camPos.z;
        float th = 0.06f;

        
        
        Render3DUtils.batchAxisLine(modelViewMatrix, bx - cx, -64 - cy, bz - cz, bx - cx, 320 - cy, bz - cz, th, r, g, b, a, true);
        Render3DUtils.batchAxisLine(modelViewMatrix, bx + 16 - cx, -64 - cy, bz - cz, bx + 16 - cx, 320 - cy, bz - cz, th, r, g, b, a, true);
        Render3DUtils.batchAxisLine(modelViewMatrix, bx - cx, -64 - cy, bz + 16 - cz, bx - cx, 320 - cy, bz + 16 - cz, th, r, g, b, a, true);
        Render3DUtils.batchAxisLine(modelViewMatrix, bx + 16 - cx, -64 - cy, bz + 16 - cz, bx + 16 - cx, 320 - cy, bz + 16 - cz, th, r, g, b, a, true);

        
        int baseY = Math.round(cy / 32.0f) * 32;
        for (int dy = -32; dy <= 32; dy += 32) {
            int y = baseY + dy;
            if (y < -64 || y > 320) continue;
            float yOff = y - cy;
            float ha = a * (1.0f - Math.abs(dy) / 64.0f) * 0.5f;
            Render3DUtils.batchAxisLine(modelViewMatrix, bx - cx, yOff, bz - cz, bx + 16 - cx, yOff, bz - cz, th, r, g, b, ha, true);
            Render3DUtils.batchAxisLine(modelViewMatrix, bx + 16 - cx, yOff, bz - cz, bx + 16 - cx, yOff, bz + 16 - cz, th, r, g, b, ha, true);
            Render3DUtils.batchAxisLine(modelViewMatrix, bx + 16 - cx, yOff, bz + 16 - cz, bx - cx, yOff, bz + 16 - cz, th, r, g, b, ha, true);
            Render3DUtils.batchAxisLine(modelViewMatrix, bx - cx, yOff, bz + 16 - cz, bx - cx, yOff, bz - cz, th, r, g, b, ha, true);
        }
    }
}
