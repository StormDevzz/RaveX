package ravex.mixin.render;
import ravex.manager.ModuleManager;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.LevelRenderer;
import ravex.mcwrapper.MinecraftWrapper;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import ravex.modules.combat.AnchorAura;
import ravex.modules.combat.AutoCrystal;
import ravex.modules.combat.BasePlace;
import ravex.modules.combat.Breaker;
import ravex.modules.combat.HoleFill;
import ravex.modules.combat.KillAura;
import ravex.modules.combat.SelfTrap;
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
import ravex.modules.render.BreadCrumbs;

import ravex.modules.render.CityESP;
import ravex.modules.render.Skeleton;
import ravex.modules.render.ESP;
import ravex.modules.render.Particles;
import ravex.modules.render.Search;
import ravex.modules.render.Trails;
import ravex.modules.render.Waypoint;
import ravex.modules.combat.PearlTarget;
import ravex.modules.world.ChestAura;
import ravex.modules.world.ECFarmer;
import ravex.modules.world.nuker.Nuker;
import ravex.modules.world.PVEUtils;
import ravex.modules.world.Scaffold;
import ravex.modules.world.AutoFarm;
import ravex.utility.misc.FarmUtility;
import ravex.modules.world.AutoTunnel;
import ravex.utility.render.Render3DUtility;
import ravex.modules.Modules;



@Mixin(LevelRenderer.class)
public class MixinLevelRenderer {
    @Shadow
    private RenderTarget entityOutlineTarget;

    private static final Matrix4f REUSABLE_MATRIX = new Matrix4f();
    private static long lastAnimTime = 0;

    @Inject(
        method = "entityOutlineTarget",
        at = @At("RETURN"),
        cancellable = true
    )
    private void onEntityOutlineTarget(CallbackInfoReturnable<RenderTarget> cir) {
        if (cir.getReturnValue() == null && entityOutlineTarget != null) {
            cir.setReturnValue(entityOutlineTarget);
        }
    }


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

        if (Modules.enabled(BlockOutline.class)) {
            return false;
        }
        return original;
    }

    @Inject(
        method = "renderLevel",
        at = @At("HEAD")
    )
    private void onRenderLevelHead(CallbackInfo ci) {
        if (Modules.enabled(Skeleton.class)) Skeleton.beginFrame();
    }

    @Inject(
        method = "extractVisibleEntities(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/culling/Frustum;Lnet/minecraft/client/DeltaTracker;Lnet/minecraft/client/renderer/state/LevelRenderState;)V",
        at = @At("TAIL")
    )
    private void onExtractVisibleEntities(
        net.minecraft.client.Camera camera,
        net.minecraft.client.renderer.culling.Frustum frustum,
        net.minecraft.client.DeltaTracker deltaTracker,
        net.minecraft.client.renderer.state.LevelRenderState levelRenderState,
        CallbackInfo ci
    ) {
        // logout ghosts are fake render states
        if (Modules.enabled(ravex.modules.render.LogoutSpots.class)) {
            try {
                ravex.modules.render.LogoutSpots.appendGhostStates(levelRenderState, deltaTracker.getGameTimeDeltaPartialTick(false));
            } catch (Exception ignored) {}
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
        renderHighlights(camera, modelViewMatrix, projectionMatrix, deltaTracker.getGameTimeDeltaPartialTick(false));
    }

    private void renderHighlights(net.minecraft.client.Camera camera, org.joml.Matrix4f modelViewMatrix, org.joml.Matrix4f projectionMatrix, float partialTick) {
        Vec3 camPos = camera.position();
        var mc = MinecraftWrapper.getInstance();
        if (mc.level == null || mc.player == null) return;

        Render3DUtility.beginFrame();
        long now = System.currentTimeMillis();
        if (lastAnimTime == 0) lastAnimTime = now;
        long deltaMs = now - lastAnimTime;
        lastAnimTime = now;
        if (deltaMs > 100) deltaMs = 16;

        float factor = Math.min(1.0f, (deltaMs / 50.0f) * 0.25f);
        double slideFactor = Math.min(1.0, (deltaMs / 50.0) * 0.35);


        if (Modules.enabled(BlockOutline.class)) {
              HitResult hit = mc.hitResult;
              if (Modules.enabled(ravex.modules.render.FreeCam.class) && mc.level != null && mc.player != null) {
                  ravex.modules.render.FreeCam fc = Modules.get(ravex.modules.render.FreeCam.class);
                  if (fc != null) {
                      try {
                          Vec3 eye = new Vec3(fc.x, fc.y, fc.z);
                          double reach = mc.player.blockInteractionRange();
                          Vec3 look = Vec3.directionFromRotation(fc.pitch, fc.yaw);
                          hit = mc.level.clip(new net.minecraft.world.level.ClipContext(
                              eye, eye.add(look.scale(reach)),
                              net.minecraft.world.level.ClipContext.Block.OUTLINE,
                              net.minecraft.world.level.ClipContext.Fluid.NONE, mc.player));
                      } catch (Exception ignored) {}
                  }
              }
              if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockHit = (BlockHitResult) hit;
                BlockPos pos = blockHit.getBlockPos();
                double tx = pos.getX();
                double ty = pos.getY();
                double tz = pos.getZ();

                if (Modules.get(BlockOutline.class).smooth) {
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
                    boX = tx; boY = ty; boZ = tz;
                    boAlpha = 1.0f;
                    boInitialized = true;
                }
            } else {
                if (Modules.get(BlockOutline.class).smooth) {
                    boAlpha += (0.0f - boAlpha) * factor;
                    if (boAlpha < 0.01f) {
                        boAlpha = 0.0f;
                        boInitialized = false;
                    }
                } else {
                    boAlpha = 0.0f;
                    boInitialized = false;
                }
            }

            if (boAlpha > 0.0f) {
                BlockOutline boMod = Modules.get(BlockOutline.class);
                int color = boMod.resolveColor();
                float r = ((color >> 16) & 0xFF) / 255.0f;
                float g = ((color >> 8) & 0xFF) / 255.0f;
                float b = (color & 0xFF) / 255.0f;
                float baseAlpha = ((color >> 24) & 0xFF) / 255.0f;
                float a = baseAlpha * boAlpha;
                boolean filled = boMod.filled;
                float thickness = (float) boMod.thickness;
                float rightBias = (float) boMod.rightBias;
                String outlineMode = boMod.mode;

                try {
                    float bx = (float)(boX - camPos.x);
                    float by = (float)(boY - camPos.y);
                    float bz = (float)(boZ - camPos.z);
                    float sx = bx + 1.0f;
                    float sy = by + 1.0f;
                    float sz = bz + 1.0f;

                    if (outlineMode.equals("Thin")) {
                        modelViewMatrix.translate(bx, by, bz, REUSABLE_MATRIX);
                        if (filled) {
                            Render3DUtility.batchFilledBox(REUSABLE_MATRIX, 1.002, r, g, b, a * 0.25f, true);
                        }
                        float lw = Math.max(0.5f, thickness * 0.7f);
                        Render3DUtility.batchWireframe(REUSABLE_MATRIX, 1.001, r, g, b, a, lw, true);
                    } else {
                        if (filled) {
                            modelViewMatrix.translate(bx, by, bz, REUSABLE_MATRIX);
                            Render3DUtility.batchFilledBox(REUSABLE_MATRIX, 1.002, r, g, b, a * 0.25f, true);
                        }
                        float base = thickness * 0.012f;
                        float leftW = Math.max(0.004f, base * (1.0f - rightBias * 0.35f));
                        float rightW = Math.max(0.004f, base * (1.0f + rightBias * 0.65f));
                        Render3DUtility.batchAxisLine(modelViewMatrix, bx, by, bz, bx, by, sz, leftW, r, g, b, a, true);
                        Render3DUtility.batchAxisLine(modelViewMatrix, bx, sy, bz, bx, sy, sz, leftW, r, g, b, a, true);
                        Render3DUtility.batchAxisLine(modelViewMatrix, bx, by, bz, bx, sy, bz, leftW, r, g, b, a, true);
                        Render3DUtility.batchAxisLine(modelViewMatrix, bx, by, sz, bx, sy, sz, leftW, r, g, b, a, true);
                        Render3DUtility.batchAxisLine(modelViewMatrix, sx, by, bz, sx, by, sz, rightW, r, g, b, a, true);
                        Render3DUtility.batchAxisLine(modelViewMatrix, sx, sy, bz, sx, sy, sz, rightW, r, g, b, a, true);
                        Render3DUtility.batchAxisLine(modelViewMatrix, sx, by, bz, sx, sy, bz, rightW, r, g, b, a, true);
                        Render3DUtility.batchAxisLine(modelViewMatrix, sx, by, sz, sx, sy, sz, rightW, r, g, b, a, true);
                        Render3DUtility.batchAxisLine(modelViewMatrix, bx, by, bz, sx, by, bz, base, r, g, b, a, true);
                        Render3DUtility.batchAxisLine(modelViewMatrix, bx, by, sz, sx, by, sz, base, r, g, b, a, true);
                        Render3DUtility.batchAxisLine(modelViewMatrix, bx, sy, bz, sx, sy, bz, base, r, g, b, a, true);
                        Render3DUtility.batchAxisLine(modelViewMatrix, bx, sy, sz, sx, sy, sz, base, r, g, b, a, true);
                    }
                } catch (Exception ignored) {}
            }
        } else {
            if (Modules.get(BlockOutline.class) != null && Modules.get(BlockOutline.class).smooth) {
                boAlpha += (0.0f - boAlpha) * factor;
                if (boAlpha < 0.01f) {
                    boAlpha = 0.0f;
                    boInitialized = false;
                }
            } else {
                boAlpha = 0.0f;
                boInitialized = false;
            }
        }


        AirPlace ap = Modules.get(AirPlace.class);
        if (Modules.enabled(AirPlace.class) && ap.render && ap.currentTarget != null) {
            double tx = ap.currentTarget.getX();
            double ty = ap.currentTarget.getY();
            double tz = ap.currentTarget.getZ();
            if (ap.animate) {
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

        if (Modules.enabled(AirPlace.class)) {
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


        Scaffold sc = Modules.get(Scaffold.class);
        boolean scActive = Modules.enabled(Scaffold.class) && sc.render;
        var scPos = scActive ? sc.getCurrentPos() : null;
        if (scPos != null) {
            double tx = scPos.getX();
            double ty = scPos.getY();
            double tz = scPos.getZ();
            if (sc.animate) {
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
            scAlpha += (0.0f - scAlpha) * factor * 0.5f;
            scSize += (0.0 - scSize) * factor * 0.5f;
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

        if (Modules.enabled(Scaffold.class)) {
            renderScaffoldHighlight(
                ravex.modules.world.Scaffold.highlightPos,
                ravex.modules.world.Scaffold.renderAlpha,
                ravex.modules.world.Scaffold.renderSize,
                ravex.modules.world.Scaffold.renderR,
                ravex.modules.world.Scaffold.renderG,
                ravex.modules.world.Scaffold.renderB,
                now, camPos, modelViewMatrix
            );
        }


        if (Modules.enabled(ChestAura.class) && Modules.get(ChestAura.class).render && !ChestAura.placedChests.isEmpty()) {
            long chestNow = System.currentTimeMillis();
            double durationMs = Modules.get(ChestAura.class).fadeSpeed * 1000.0;
            int color = Modules.get(ChestAura.class).highlightColor;
            float r = ((color >> 16) & 0xFF) / 255.0f;
            float g = ((color >> 8) & 0xFF) / 255.0f;
            float b = (color & 0xFF) / 255.0f;
            boolean filled = Modules.get(ChestAura.class).filled;

            Matrix4f mat = new Matrix4f();
            for (ravex.modules.world.ChestAura.PlacedChest chest : ravex.modules.world.ChestAura.placedChests) {
                long elapsed = chestNow - chest.placeTime;
                if (elapsed > durationMs) continue;

                float progress = (float)(elapsed / durationMs);
                float alpha = 1.0f - progress;

                try {
                    modelViewMatrix.translate(
                        (float)(ravex.utility.misc.block.BlockUtility.unpackX(chest.packedPos) - camPos.x),
                        (float)(ravex.utility.misc.block.BlockUtility.unpackY(chest.packedPos) - camPos.y),
                        (float)(ravex.utility.misc.block.BlockUtility.unpackZ(chest.packedPos) - camPos.z),
                        mat
                    );

                    double size = 1.002;
                    if (filled) {
                        Render3DUtility.batchFilledBox(mat, size, r, g, b, alpha * 0.25f);
                    }
                    Render3DUtility.batchWireframe(mat, size, r, g, b, alpha * 0.95f);
                    Render3DUtility.batchWireframe(mat, size * 1.03, r, g, b, alpha * 0.3f);
                } catch (Exception ignored) {}
            }
        }


        {
            ravex.modules.world.AutoWither aw = Modules.get(ravex.modules.world.AutoWither.class);
            if (aw != null && aw.render && aw.hasRenderBase()) {
                int awColor = aw.color;
                float awR = ((awColor >> 16) & 0xFF) / 255.0f;
                float awG = ((awColor >> 8) & 0xFF) / 255.0f;
                float awB = (awColor & 0xFF) / 255.0f;
                Matrix4f awMat = new Matrix4f();
                int awNext = aw.getBuildIndex();
                long awNow = now;
                int awCells = Math.min(ravex.modules.world.AutoWither.RENDER_OFFSETS.length, aw.getCellCount());
                for (int awI = 0; awI < awCells; awI++) {
                    int[] awOff = ravex.modules.world.AutoWither.RENDER_OFFSETS[awI];
                    try {
                        float fill = aw.getCellFillAlpha(awI, awNow);
                        float line = aw.getCellLineAlpha(awI, awNow);
                        // next cell grows while it breathes
                        float awScale = 1.002f + (awI == awNext ? 0.006f * fill / 0.22f : 0f);
                        modelViewMatrix.translate(
                            (float)(aw.getBaseX() + awOff[0] - camPos.x),
                            (float)(aw.getBaseY() + awOff[1] - camPos.y),
                            (float)(aw.getBaseZ() + awOff[2] - camPos.z),
                            awMat
                        );
                        Render3DUtility.batchFilledBox(awMat, awScale, awR, awG, awB, fill);
                        Render3DUtility.batchWireframe(awMat, awScale, awR, awG, awB, line);
                    } catch (Exception ignored) {}
                }
            }
        }

        if (Modules.enabled(Surround.class)) {
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

                        Render3DUtility.batchFilledBox(REUSABLE_MATRIX, s,
                            Surround.renderR, Surround.renderG, Surround.renderB, a * 0.25f);
                        Render3DUtility.batchWireframe(REUSABLE_MATRIX, s,
                            Surround.renderR, Surround.renderG, Surround.renderB, a * 0.95f);
                        Render3DUtility.batchWireframe(REUSABLE_MATRIX, s * 1.03,
                            Surround.renderR, Surround.renderG, Surround.renderB, a * 0.3f);
                    } catch (Exception ignored) {}
                }
            }
        }


        Trap trap = Modules.get(Trap.class);
        if (Modules.enabled(Trap.class) && trap.render) {
            synchronized (ravex.modules.combat.Trap.trapBlocks) {
                for (BlockPos pos : ravex.modules.combat.Trap.trapBlocks) {
                    if (pos == null) continue;
                    float tx = (float)(pos.getX() - camPos.x);
                    float ty = (float)(pos.getY() - camPos.y);
                    float tz = (float)(pos.getZ() - camPos.z);

                    try {
                        modelViewMatrix.translate(tx, ty, tz, REUSABLE_MATRIX);

                        int c = trap.color;
                        float r = ((c >> 16) & 0xFF) / 255.0f;
                        float g = ((c >> 8) & 0xFF) / 255.0f;
                        float b = (c & 0xFF) / 255.0f;
                        float a = 0.35f;

                        double size = 1.002;
                        Render3DUtility.batchFilledBox(REUSABLE_MATRIX, size, r, g, b, a * 0.25f);
                        Render3DUtility.batchWireframe(REUSABLE_MATRIX, size, r, g, b, a * 0.95f);
                        Render3DUtility.batchWireframe(REUSABLE_MATRIX, size * 1.03, r, g, b, a * 0.2f);
                    } catch (Exception ignored) {}
                }
            }
        }


        SelfTrap selfTrap = Modules.get(SelfTrap.class);
        if (Modules.enabled(SelfTrap.class) && selfTrap.render) {
            for (BlockPos pos : ravex.modules.combat.SelfTrap.getSelfTrapBlocks()) {
                if (pos == null) continue;
                float sx = (float)(pos.getX() - camPos.x);
                float sy = (float)(pos.getY() - camPos.y);
                float sz = (float)(pos.getZ() - camPos.z);

                try {
                    modelViewMatrix.translate(sx, sy, sz, REUSABLE_MATRIX);

                    int c = selfTrap.color;
                    float r = ((c >> 16) & 0xFF) / 255.0f;
                    float g = ((c >> 8) & 0xFF) / 255.0f;
                    float b = (c & 0xFF) / 255.0f;
                    float a = ((c >> 24) & 0xFF) / 255.0f;

                    double size = 1.002;
                    Render3DUtility.batchFilledBox(REUSABLE_MATRIX, size, r, g, b, a * 0.25f);
                    Render3DUtility.batchWireframe(REUSABLE_MATRIX, size, r, g, b, a * 0.95f);
                } catch (Exception ignored) {}
            }
        }


        BasePlace basePlace = Modules.get(BasePlace.class);
        if (Modules.enabled(BasePlace.class) && basePlace.render) {
            int c = basePlace.color;
            float r = ((c >> 16) & 0xFF) / 255.0f;
            float g = ((c >> 8) & 0xFF) / 255.0f;
            float b = (c & 0xFF) / 255.0f;
            float a = ((c >> 24) & 0xFF) / 255.0f;
            if (a <= 0.01f) a = 0.5f;

            BlockPos pos = ravex.modules.combat.BasePlace.getSimulatedPlacementBlock();
            if (pos != null) {
                try {
                    modelViewMatrix.translate(
                        (float)(pos.getX() - camPos.x),
                        (float)(pos.getY() - camPos.y),
                        (float)(pos.getZ() - camPos.z),
                        REUSABLE_MATRIX
                    );

                    double size = 1.002;
                    Render3DUtility.batchFilledBox(REUSABLE_MATRIX, size, r, g, b, a * 0.25f);
                    Render3DUtility.batchWireframe(REUSABLE_MATRIX, size, r, g, b, a * 0.95f);
                    Render3DUtility.batchWireframe(REUSABLE_MATRIX, size * 1.03, r, g, b, a * 0.2f);
                } catch (Exception ignored) {}
            }

            for (java.util.Map.Entry<BlockPos, Long> entry : ravex.modules.combat.BasePlace.getRecentPlacedBlocks().entrySet()) {
                BlockPos placedPos = entry.getKey();
                if (placedPos == null || placedPos.equals(pos)) continue;
                long elapsed = now - entry.getValue();
                if (elapsed > 1500) continue;
                float fade = 1.0f - (elapsed / 1500.0f);
                try {
                    modelViewMatrix.translate(
                        (float)(placedPos.getX() - camPos.x),
                        (float)(placedPos.getY() - camPos.y),
                        (float)(placedPos.getZ() - camPos.z),
                        REUSABLE_MATRIX
                    );

                    double size = 1.002;
                    Render3DUtility.batchFilledBox(REUSABLE_MATRIX, size, r, g, b, a * 0.25f * fade);
                    Render3DUtility.batchWireframe(REUSABLE_MATRIX, size, r, g, b, a * 0.95f * fade);
                } catch (Exception ignored) {}
            }
        }


        AnchorAura anchorAura = Modules.get(AnchorAura.class);
        if (Modules.enabled(AnchorAura.class) && anchorAura.render && ravex.modules.combat.AnchorAura.simulatedPlacementBlock != null) {
            BlockPos pos = ravex.modules.combat.AnchorAura.simulatedPlacementBlock;
            try {
                modelViewMatrix.translate(
                    (float)(pos.getX() - camPos.x),
                    (float)(pos.getY() - camPos.y),
                    (float)(pos.getZ() - camPos.z),
                    REUSABLE_MATRIX
                );

                int c = anchorAura.color;
                float r = ((c >> 16) & 0xFF) / 255.0f;
                float g = ((c >> 8) & 0xFF) / 255.0f;
                float b = (c & 0xFF) / 255.0f;
                float a = ((c >> 24) & 0xFF) / 255.0f;

                double size = 1.002;
                Render3DUtility.batchFilledBox(REUSABLE_MATRIX, size, r, g, b, a * 0.25f);
                Render3DUtility.batchWireframe(REUSABLE_MATRIX, size, r, g, b, a * 0.95f);
                Render3DUtility.batchWireframe(REUSABLE_MATRIX, size * 1.03, r, g, b, a * 0.2f);
            } catch (Exception ignored) {}
        }

        if (Modules.enabled(NewChunks.class)) {
            Modules.get(NewChunks.class).render(modelViewMatrix, camera);
        }

        if (Modules.enabled(ravex.modules.movement.NoFall.class)
                && Modules.get(ravex.modules.movement.NoFall.class).clutchHighlight
                && ravex.modules.movement.NoFall.clutchPlacedPos != null) {
            long clutchNow = System.currentTimeMillis();
            long clutchElapsed = clutchNow - ravex.modules.movement.NoFall.clutchPlacedMs;
            if (clutchElapsed < 5000L) {
                float clutchAlpha = 1f - clutchElapsed / 5000f;
                int clutchCol = Modules.get(ravex.modules.movement.NoFall.class).clutchColor;
                float cr = ((clutchCol >> 16) & 0xFF) / 255f;
                float cg = ((clutchCol >> 8) & 0xFF) / 255f;
                float cb = (clutchCol & 0xFF) / 255f;
                BlockPos cpos = ravex.modules.movement.NoFall.clutchPlacedPos;
                Matrix4f clutchMat = new Matrix4f();
                try {
                    modelViewMatrix.translate(
                        (float) (cpos.getX() - camPos.x),
                        (float) (cpos.getY() - camPos.y),
                        (float) (cpos.getZ() - camPos.z),
                        clutchMat
                    );
                    Render3DUtility.batchFilledBox(clutchMat, 1.0, cr, cg, cb, clutchAlpha * 0.35f);
                    Render3DUtility.batchWireframe(clutchMat, 1.002, cr, cg, cb, clutchAlpha * 0.95f);
                } catch (Exception ignored) {}
            } else {
                ravex.modules.movement.NoFall.clutchPlacedPos = null;
            }
        }

        // highlight for the shulker high jump
        if (Modules.enabled(ravex.modules.movement.HighJump.class)
                && Modules.get(ravex.modules.movement.HighJump.class).highlight
                && ravex.modules.movement.HighJump.shulkerHighlightPos != null) {
            long hjNow = System.currentTimeMillis();
            long hjElapsed = hjNow - ravex.modules.movement.HighJump.shulkerHighlightMs;
            if (hjElapsed < 3000L) {
                float hjAlpha = 1f - hjElapsed / 3000f;
                int hjCol = Modules.get(ravex.modules.movement.HighJump.class).highlightColor;
                float hjr = ((hjCol >> 16) & 0xFF) / 255f;
                float hjg = ((hjCol >> 8) & 0xFF) / 255f;
                float hjb = (hjCol & 0xFF) / 255f;
                BlockPos hpos = ravex.modules.movement.HighJump.shulkerHighlightPos;
                Matrix4f hjMat = new Matrix4f();
                try {
                    modelViewMatrix.translate(
                        (float) (hpos.getX() - camPos.x),
                        (float) (hpos.getY() - camPos.y),
                        (float) (hpos.getZ() - camPos.z),
                        hjMat
                    );
                    Render3DUtility.batchFilledBox(hjMat, 1.0, hjr, hjg, hjb, hjAlpha * 0.3f);
                    Render3DUtility.batchWireframe(hjMat, 1.004, hjr, hjg, hjb, hjAlpha * 0.95f);
                } catch (Exception ignored) {}
            } else {
                ravex.modules.movement.HighJump.shulkerHighlightPos = null;
            }
        }

        AutoCrystal ac = Modules.get(AutoCrystal.class);
        if (Modules.enabled(AutoCrystal.class) && ac.renderPlacement && ravex.modules.combat.AutoCrystal.currentPlacementBlock != null) {
            BlockPos p = ravex.modules.combat.AutoCrystal.currentPlacementBlock;
            try {
                modelViewMatrix.translate(
                    (float)(p.getX() - camPos.x),
                    (float)(p.getY() - camPos.y),
                    (float)(p.getZ() - camPos.z),
                    REUSABLE_MATRIX
                );

                double size = 1.002;
                Render3DUtility.batchFilledBox(REUSABLE_MATRIX, size, 0.2f, 0.8f, 1.0f, 0.22f);
                Render3DUtility.batchWireframe(REUSABLE_MATRIX, size, 0.2f, 0.8f, 1.0f, 0.85f);
                Render3DUtility.batchWireframe(REUSABLE_MATRIX, size * 1.03, 0.2f, 0.8f, 1.0f, 0.2f);
            } catch (Exception ignored) {}
        }


        if (Modules.enabled(StashFinder.class)) {
            double maxDist = Modules.get(StashFinder.class).range;
            for (StashFinder.StashEntry stash : Modules.get(StashFinder.class).getStashes()) {
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

                    Render3DUtility.batchFilledBox(REUSABLE_MATRIX, 1.0, 1.0f, 0.5f, 0.0f, 0.3f);
                    Render3DUtility.batchWireframe(REUSABLE_MATRIX, 1.0, 1.0f, 0.5f, 0.0f, 0.8f);
                } catch (Exception ignored) {}
            }
        }


        if (Modules.enabled(BreadCrumbs.class)) {
            ravex.modules.render.BreadCrumbs.renderTrails(modelViewMatrix, camPos);
        }


        if (ravex.modules.render.Trails.shouldRender()) {
            ravex.modules.render.Trails.renderTrails(modelViewMatrix, camPos);
        }


        if (Modules.enabled(Particles.class)) {
            ravex.modules.render.Particles.renderParticles(modelViewMatrix, camPos);
        }


        if (ravex.modules.render.JumpCircles.shouldRender()) {
            ravex.modules.render.JumpCircles.renderCircles(modelViewMatrix, camPos);
        }

        // storage esp
        if (Modules.enabled(ravex.modules.render.StorageESP.class)) {
            try {
                ravex.modules.render.StorageESP.renderStorage(modelViewMatrix, camPos);
            } catch (Exception ignored) {}
        }

        // projectile trajectories
        if (Modules.enabled(ravex.modules.render.Trajectories.class)) {
            try {
                ravex.modules.render.Trajectories.renderTrajectories(modelViewMatrix, camPos.x, camPos.y, camPos.z);
            } catch (Exception ignored) {}
        }

        if (Modules.enabled(ravex.modules.render.ChinaHat.class)) {
            try {
                ravex.modules.render.ChinaHat.render(modelViewMatrix, camPos, partialTick);
            } catch (Exception ignored) {}
        }


        WebSelf ws = Modules.get(WebSelf.class);
        if (Modules.enabled(WebSelf.class) && ws.render && ravex.modules.combat.WebSelf.targetPos != null) {
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
                Render3DUtility.batchFilledBox(REUSABLE_MATRIX, size, r, g, b, 0.20f);
                Render3DUtility.batchWireframe(REUSABLE_MATRIX, size, r, g, b, 0.85f);
                Render3DUtility.batchWireframe(REUSABLE_MATRIX, size * 1.03, r, g, b, 0.25f);
            } catch (Exception ignored) {}
        }


        Breaker br = Modules.get(Breaker.class);
        if (Modules.enabled(Breaker.class) && ravex.modules.combat.Breaker.currentMiningBlock != null) {
            BlockPos p = ravex.modules.combat.Breaker.currentMiningBlock;
            try {
                modelViewMatrix.translate(
                        (float)(p.getX() - camPos.x),
                        (float)(p.getY() - camPos.y),
                        (float)(p.getZ() - camPos.z),
                        REUSABLE_MATRIX
                    );

                int c = br.color;
                float r = ((c >> 16) & 0xFF) / 255.0f;
                float g = ((c >> 8) & 0xFF) / 255.0f;
                float b = (c & 0xFF) / 255.0f;
                float a = 0.35f;

                double size = 1.002;
                Render3DUtility.batchFilledBox(REUSABLE_MATRIX, size, r, g, b, a * 0.25f);
                Render3DUtility.batchWireframe(REUSABLE_MATRIX, size, r, g, b, a * 0.95f);
            } catch (Exception ignored) {}
        }


        AutoTunnel at = Modules.get(AutoTunnel.class);
        if (Modules.enabled(AutoTunnel.class) && at.render) {
            int c = at.color;
            float r = ((c >> 16) & 0xFF) / 255.0f;
            float g = ((c >> 8) & 0xFF) / 255.0f;
            float b = (c & 0xFF) / 255.0f;
            float a = ((c >> 24) & 0xFF) / 255.0f;
            // tunnel slice breathes, mined block is brighter
            float breathe = 0.5f + 0.5f * (float) Math.sin(now * 0.004);
            var volume = ravex.modules.world.AutoTunnel.getRenderVolume();
            for (BlockPos vp : volume) {
                try {
                    boolean active = ravex.modules.world.AutoTunnel.isTarget(vp);
                    modelViewMatrix.translate(
                            (float)(vp.getX() - camPos.x),
                            (float)(vp.getY() - camPos.y),
                            (float)(vp.getZ() - camPos.z),
                            REUSABLE_MATRIX
                        );
                    if (active) {
                        double size = 1.002 + 0.004 * breathe;
                        Render3DUtility.batchFilledBox(REUSABLE_MATRIX, size, r, g, b, a * (0.30f + 0.10f * breathe));
                        Render3DUtility.batchWireframe(REUSABLE_MATRIX, size, r, g, b, a * (0.85f + 0.15f * breathe));
                        Render3DUtility.batchWireframe(REUSABLE_MATRIX, size * 1.035, r, g, b, a * (0.18f + 0.08f * breathe));
                    } else {
                        Render3DUtility.batchFilledBox(REUSABLE_MATRIX, 1.002, r, g, b, a * (0.10f + 0.03f * breathe));
                        Render3DUtility.batchWireframe(REUSABLE_MATRIX, 1.002, r, g, b, a * (0.30f + 0.08f * breathe));
                    }
                } catch (Exception ignored) {}
            }
        }

        if (Modules.enabled(ravex.modules.world.AutoFarm.class)) {
            ravex.modules.world.AutoFarm af = Modules.get(ravex.modules.world.AutoFarm.class);
            BlockPos afPos = ravex.utility.misc.FarmUtility.getCurrentTarget();
            if (af != null && af.render && afPos != null) {
                try {
                    int afc = af.color;
                    float afr = ((afc >> 16) & 0xFF) / 255.0f;
                    float afg = ((afc >> 8) & 0xFF) / 255.0f;
                    float afb = (afc & 0xFF) / 255.0f;
                    float afa = ((afc >> 24) & 0xFF) / 255.0f;
                    float afp = 0.5f + 0.5f * (float) Math.sin(now * 0.005);
                    modelViewMatrix.translate(
                        (float)(afPos.getX() - camPos.x),
                        (float)(afPos.getY() - camPos.y),
                        (float)(afPos.getZ() - camPos.z),
                        REUSABLE_MATRIX);
                    Render3DUtility.batchFilledBox(REUSABLE_MATRIX, 1.002, afr, afg, afb, afa * (0.22f + 0.08f * afp));
                    Render3DUtility.batchWireframe(REUSABLE_MATRIX, 1.002, afr, afg, afb, afa * (0.75f + 0.25f * afp));
                    Render3DUtility.batchWireframe(REUSABLE_MATRIX, 1.036, afr, afg, afb, afa * (0.14f + 0.08f * afp));
                } catch (Exception ignored) {}
            }
        }


        Nuker nk = Modules.get(Nuker.class);
        if (ModuleManager.INSTANCE.getByName("Nuker").getEnabled() && nk.render && ravex.modules.world.nuker.Nuker.currentTarget != null) {
            BlockPos p = ravex.modules.world.nuker.Nuker.currentTarget;
            try {
                modelViewMatrix.translate(
                        (float)(p.getX() - camPos.x),
                        (float)(p.getY() - camPos.y),
                        (float)(p.getZ() - camPos.z),
                        REUSABLE_MATRIX
                    );

                int c = nk.color;
                float r = ((c >> 16) & 0xFF) / 255.0f;
                float g = ((c >> 8) & 0xFF) / 255.0f;
                float b = (c & 0xFF) / 255.0f;
                float a = ((c >> 24) & 0xFF) / 255.0f;

                double size = 1.002;
                Render3DUtility.batchFilledBox(REUSABLE_MATRIX, size, r, g, b, a * 0.25f);
                Render3DUtility.batchWireframe(REUSABLE_MATRIX, size, r, g, b, a * 0.95f);
                Render3DUtility.batchWireframe(REUSABLE_MATRIX, size * 1.03, r, g, b, a * 0.2f);
            } catch (Exception ignored) {}
        }


        PVEUtils sm = Modules.get(PVEUtils.class);
        BlockPos smeltPos = ravex.utility.player.ContainerUtility.getSmeltTarget();
        if (Modules.enabled(PVEUtils.class) && sm.mode.equals("AutoSmelt") && sm.smeltRender && smeltPos != null) {
            try {
                modelViewMatrix.translate((float)(smeltPos.getX() - camPos.x), (float)(smeltPos.getY() - camPos.y), (float)(smeltPos.getZ() - camPos.z), REUSABLE_MATRIX);
                int c = sm.smeltColor;
                float r = ((c >> 16) & 0xFF) / 255.0f;
                float g = ((c >> 8) & 0xFF) / 255.0f;
                float b = (c & 0xFF) / 255.0f;
                float a = ((c >> 24) & 0xFF) / 255.0f;
                double size = 1.002;
                Render3DUtility.batchFilledBox(REUSABLE_MATRIX, size, r, g, b, a * 0.25f);
                Render3DUtility.batchWireframe(REUSABLE_MATRIX, size, r, g, b, a * 0.95f);
                Render3DUtility.batchWireframe(REUSABLE_MATRIX, size * 1.03, r, g, b, a * 0.2f);
            } catch (Exception ignored) {}
        }


        PVEUtils bw = Modules.get(PVEUtils.class);
        if (Modules.enabled(PVEUtils.class) && bw.mode.equals("AutoBrew") && bw.brewRender) {
            BlockPos p = ravex.utility.player.ContainerUtility.getBrewTarget();
            if (p != null) try {
                modelViewMatrix.translate((float)(p.getX() - camPos.x), (float)(p.getY() - camPos.y), (float)(p.getZ() - camPos.z), REUSABLE_MATRIX);
                int c = bw.brewColor;
                float r = ((c >> 16) & 0xFF) / 255.0f;
                float g = ((c >> 8) & 0xFF) / 255.0f;
                float b = (c & 0xFF) / 255.0f;
                float a = ((c >> 24) & 0xFF) / 255.0f;
                double size = 1.002;
                Render3DUtility.batchFilledBox(REUSABLE_MATRIX, size, r, g, b, a * 0.25f);
                Render3DUtility.batchWireframe(REUSABLE_MATRIX, size, r, g, b, a * 0.95f);
                Render3DUtility.batchWireframe(REUSABLE_MATRIX, size * 1.03, r, g, b, a * 0.2f);
            } catch (Exception ignored) {}
        }


        ECFarmer ec = Modules.get(ECFarmer.class);
        if (Modules.enabled(ECFarmer.class) && ec.render) {
            int c = ec.color;
            float r = ((c >> 16) & 0xFF) / 255.0f;
            float g = ((c >> 8) & 0xFF) / 255.0f;
            float b = (c & 0xFF) / 255.0f;
            float a = ((c >> 24) & 0xFF) / 255.0f;
            float ecBreathe = 0.5f + 0.5f * (float) Math.sin(now * 0.0045);

            // previous block: short afterglow
            BlockPos ecLast = ravex.modules.world.ECFarmer.getLastTarget();
            BlockPos ecCurrent = ravex.modules.world.ECFarmer.getCurrentTarget();
            if (ecLast != null && !ecLast.equals(ecCurrent)) {
                float fade = ravex.modules.world.ECFarmer.getLastFade();
                try {
                    modelViewMatrix.translate(
                        (float)(ecLast.getX() - camPos.x),
                        (float)(ecLast.getY() - camPos.y),
                        (float)(ecLast.getZ() - camPos.z),
                        REUSABLE_MATRIX);
                    Render3DUtility.batchFilledBox(REUSABLE_MATRIX, 1.004, r, g, b, a * 0.16f * fade);
                    Render3DUtility.batchWireframe(REUSABLE_MATRIX, 1.004, r, g, b, a * 0.45f * fade);
                } catch (Exception ignored) {}
            }

            // current target: breathing highlight
            if (ecCurrent != null) {
                try {
                    double size = 1.002 + 0.004 * ecBreathe;
                    modelViewMatrix.translate(
                        (float)(ecCurrent.getX() - camPos.x),
                        (float)(ecCurrent.getY() - camPos.y),
                        (float)(ecCurrent.getZ() - camPos.z),
                        REUSABLE_MATRIX);
                    Render3DUtility.batchFilledBox(REUSABLE_MATRIX, size, r, g, b, a * (0.22f + 0.08f * ecBreathe));
                    Render3DUtility.batchWireframe(REUSABLE_MATRIX, size, r, g, b, a * (0.80f + 0.20f * ecBreathe));
                    Render3DUtility.batchWireframe(REUSABLE_MATRIX, size * 1.035, r, g, b, a * (0.15f + 0.08f * ecBreathe));
                } catch (Exception ignored) {}
            }
        }


        AutoPortal pb = Modules.get(AutoPortal.class);
        if (Modules.enabled(AutoPortal.class) && pb.render) {
            BlockPos p = ravex.modules.misc.AutoPortal.getCurrentTarget();
            if (p != null) try {
                modelViewMatrix.translate((float)(p.getX() - camPos.x), (float)(p.getY() - camPos.y), (float)(p.getZ() - camPos.z), REUSABLE_MATRIX);
                int c = pb.color;
                float r = ((c >> 16) & 0xFF) / 255.0f;
                float g = ((c >> 8) & 0xFF) / 255.0f;
                float b = (c & 0xFF) / 255.0f;
                float a = ((c >> 24) & 0xFF) / 255.0f;
                double size = 1.002;
                Render3DUtility.batchFilledBox(REUSABLE_MATRIX, size, r, g, b, a * 0.25f);
                Render3DUtility.batchWireframe(REUSABLE_MATRIX, size, r, g, b, a * 0.95f);
                Render3DUtility.batchWireframe(REUSABLE_MATRIX, size * 1.03, r, g, b, a * 0.2f);
            } catch (Exception ignored) {}
        }


        HoleFill hf = Modules.get(HoleFill.class);
        if (Modules.enabled(HoleFill.class) && hf.render) {
            for (var hole : ravex.modules.combat.HoleFill.holePositions) {
                if (hole == null) continue;
                try {
                    modelViewMatrix.translate(
                        (float)(ravex.utility.misc.block.BlockUtility.unpackX(hole) - camPos.x),
                        (float)(ravex.utility.misc.block.BlockUtility.unpackY(hole) - camPos.y),
                        (float)(ravex.utility.misc.block.BlockUtility.unpackZ(hole) - camPos.z),
                        REUSABLE_MATRIX);
                    int c = hf.color;
                    float r = ((c >> 16) & 0xFF) / 255.0f;
                    float g = ((c >> 8) & 0xFF) / 255.0f;
                    float b = (c & 0xFF) / 255.0f;
                    float a = ((c >> 24) & 0xFF) / 255.0f;
                    double size = 1.002;
                    Render3DUtility.batchFilledBox(REUSABLE_MATRIX, size, r, g, b, a * 0.15f);
                    Render3DUtility.batchWireframe(REUSABLE_MATRIX, size, r, g, b, a * 0.85f);
                } catch (Exception ignored) {}
            }
        }


        if (Modules.enabled(Borders.class)) {
            int rd = (int) Modules.get(Borders.class).renderDistance;
            boolean showCurrent = Modules.get(Borders.class).showCurrentChunk;
            boolean showAll = Modules.get(Borders.class).showChunkBorders;
            int lc = Modules.get(Borders.class).chunkColor;
            float lr = ((lc >> 16) & 0xFF) / 255.0f;
            float lg = ((lc >> 8) & 0xFF) / 255.0f;
            float lb = (lc & 0xFF) / 255.0f;
            float la = ((lc >> 24) & 0xFF) / 255.0f;
            float lw = (float) Modules.get(Borders.class).lineWidth;

            if (showAll && mc.player != null) {
                int cx = mc.player.chunkPosition().x;
                int cz = mc.player.chunkPosition().z;
                for (int dx = -rd / 16; dx <= rd / 16; dx++) {
                    for (int dz = -rd / 16; dz <= rd / 16; dz++) {
                        if (showCurrent && dx == 0 && dz == 0) continue;
                        int bx = (cx + dx) << 4;
                        int bz = (cz + dz) << 4;
                        try {
                            renderChunkBorderLines(modelViewMatrix, bx, bz, lr, lg, lb, la, camPos, lw * 0.04f);
                        } catch (Exception ignored) {}
                    }
                }
            }

            if (showCurrent) {
                int cc = Modules.get(Borders.class).currentColor;
                float cr = ((cc >> 16) & 0xFF) / 255.0f;
                float cg = ((cc >> 8) & 0xFF) / 255.0f;
                float cb = (cc & 0xFF) / 255.0f;
                float ca = ((cc >> 24) & 0xFF) / 255.0f;
                if (mc.player != null) {
                    int cx = mc.player.chunkPosition().x;
                    int cz = mc.player.chunkPosition().z;
                    int bx = cx << 4;
                    int bz = cz << 4;
                    float feetY = (float) mc.player.position().y;
                    try {
                        renderCurrentChunkHighlight(modelViewMatrix, bx, bz, feetY, cr, cg, cb, ca, camPos, lw * 0.04f);
                    } catch (Exception ignored) {}
                }
            }
        }


        if (Modules.enabled(ravex.modules.render.TunnelESP.class)) {
            var tunnelEsp = Modules.get(ravex.modules.render.TunnelESP.class);
            int tunnelColorVal = tunnelEsp.tunnelColor;
            float tr = ((tunnelColorVal >> 16) & 0xFF) / 255.0f;
            float tg = ((tunnelColorVal >> 8) & 0xFF) / 255.0f;
            float tb = (tunnelColorVal & 0xFF) / 255.0f;
            float ta = ((tunnelColorVal >> 24) & 0xFF) / 255.0f;
            boolean filled = tunnelEsp.tunnelFilled;
            boolean wire = tunnelEsp.tunnelWireframe;
            for (BlockPos pos : tunnelEsp.getTunnelBlocks()) {
                try {
                    modelViewMatrix.translate((float)(pos.getX() - camPos.x), (float)(pos.getY() - camPos.y), (float)(pos.getZ() - camPos.z), REUSABLE_MATRIX);
                    if (filled) Render3DUtility.batchFilledBox(REUSABLE_MATRIX, 1.002, tr, tg, tb, ta * 0.3f);
                    if (wire) Render3DUtility.batchWireframe(REUSABLE_MATRIX, 1.002, tr, tg, tb, ta * 0.85f);
                } catch (Exception ignored) {}
            }
        }


        if (Modules.enabled(ravex.modules.render.HoleESP.class)) {
            var holeEsp = Modules.get(ravex.modules.render.HoleESP.class);
            int c = holeEsp.safeColor;
            float hr = ((c >> 16) & 0xFF) / 255.0f;
            float hg = ((c >> 8) & 0xFF) / 255.0f;
            float hb = (c & 0xFF) / 255.0f;
            float ha = ((c >> 24) & 0xFF) / 255.0f;
            float hw = 0.04f;
            for (var pos : holeEsp.getHoles()) {
                try {
                    float px = (float)(pos.getX() - camPos.x);
                    float py = (float)(pos.getY() - camPos.y);
                    float pz = (float)(pos.getZ() - camPos.z);
                    if (holeEsp.holeFilled) {
                        modelViewMatrix.translate(px, py, pz, REUSABLE_MATRIX);
                        Render3DUtility.batchFilledBox(REUSABLE_MATRIX, 1.002, hr, hg, hb, ha * 0.3f, true);
                    }
                    if (holeEsp.holeWireframe) {

                        Render3DUtility.batchAxisLine(modelViewMatrix, px, py, pz, px + 1, py, pz, hw, hr, hg, hb, ha, true);
                        Render3DUtility.batchAxisLine(modelViewMatrix, px + 1, py, pz, px + 1, py, pz + 1, hw, hr, hg, hb, ha, true);
                        Render3DUtility.batchAxisLine(modelViewMatrix, px + 1, py, pz + 1, px, py, pz + 1, hw, hr, hg, hb, ha, true);
                        Render3DUtility.batchAxisLine(modelViewMatrix, px, py, pz + 1, px, py, pz, hw, hr, hg, hb, ha, true);

                        Render3DUtility.batchAxisLine(modelViewMatrix, px, py + 1, pz, px + 1, py + 1, pz, hw, hr, hg, hb, ha, true);
                        Render3DUtility.batchAxisLine(modelViewMatrix, px + 1, py + 1, pz, px + 1, py + 1, pz + 1, hw, hr, hg, hb, ha, true);
                        Render3DUtility.batchAxisLine(modelViewMatrix, px + 1, py + 1, pz + 1, px, py + 1, pz + 1, hw, hr, hg, hb, ha, true);
                        Render3DUtility.batchAxisLine(modelViewMatrix, px, py + 1, pz + 1, px, py + 1, pz, hw, hr, hg, hb, ha, true);

                        Render3DUtility.batchAxisLine(modelViewMatrix, px, py, pz, px, py + 1, pz, hw, hr, hg, hb, ha, true);
                        Render3DUtility.batchAxisLine(modelViewMatrix, px + 1, py, pz, px + 1, py + 1, pz, hw, hr, hg, hb, ha, true);
                        Render3DUtility.batchAxisLine(modelViewMatrix, px + 1, py, pz + 1, px + 1, py + 1, pz + 1, hw, hr, hg, hb, ha, true);
                        Render3DUtility.batchAxisLine(modelViewMatrix, px, py, pz + 1, px, py + 1, pz + 1, hw, hr, hg, hb, ha, true);
                    }
                } catch (Exception ignored) {}
            }
        }


        if (Modules.enabled(ravex.modules.render.VoidESP.class)) {
            var voidEsp = Modules.get(ravex.modules.render.VoidESP.class);
            int vc = voidEsp.voidColor;
            float vr = ((vc >> 16) & 0xFF) / 255.0f;
            float vg = ((vc >> 8) & 0xFF) / 255.0f;
            float vb = (vc & 0xFF) / 255.0f;
            float va = ((vc >> 24) & 0xFF) / 255.0f;
            boolean vf = voidEsp.voidFilled;
            boolean vw = voidEsp.voidWireframe;
            for (BlockPos pos : voidEsp.getVoidBlocks()) {
                try {
                    modelViewMatrix.translate((float) (pos.getX() - camPos.x), (float) (pos.getY() - camPos.y), (float) (pos.getZ() - camPos.z), REUSABLE_MATRIX);
                    if (vf) Render3DUtility.batchFilledBox(REUSABLE_MATRIX, 1.002, vr, vg, vb, va * 0.35f, true);
                    if (vw) Render3DUtility.batchWireframe(REUSABLE_MATRIX, 1.002, vr, vg, vb, va * 0.8f, 1.5f, true);
                } catch (Exception ignored) {}
            }
        }


        if (Modules.enabled(CityESP.class)) {
            BlockPos cp = Modules.get(CityESP.class).getCityBlock();
            if (cp != null) {
                double dist = Math.sqrt(mc.player.distanceToSqr(cp.getX() + 0.5, cp.getY() + 0.5, cp.getZ() + 0.5));
                if (dist <= Modules.get(CityESP.class).renderRange) {
                    try {
                        modelViewMatrix.translate((float)(cp.getX() - camPos.x), (float)(cp.getY() - camPos.y), (float)(cp.getZ() - camPos.z), REUSABLE_MATRIX);
                        int fc = Modules.get(CityESP.class).fillColor;
                        float fr = ((fc >> 16) & 0xFF) / 255.0f;
                        float fg = ((fc >> 8) & 0xFF) / 255.0f;
                        float fb = (fc & 0xFF) / 255.0f;
                        float fa = ((fc >> 24) & 0xFF) / 255.0f;
                        int lc = Modules.get(CityESP.class).lineColor;
                        float lr = ((lc >> 16) & 0xFF) / 255.0f;
                        float lg = ((lc >> 8) & 0xFF) / 255.0f;
                        float lb = (lc & 0xFF) / 255.0f;
                        float la = ((lc >> 24) & 0xFF) / 255.0f;
                        if (Modules.get(CityESP.class).filled) Render3DUtility.batchFilledBox(REUSABLE_MATRIX, 1.002, fr, fg, fb, fa);
                        if (Modules.get(CityESP.class).wireframe) Render3DUtility.batchWireframe(REUSABLE_MATRIX, 1.002, lr, lg, lb, la);
                    } catch (Exception ignored) {}
                }
            }
        }


        PacketMine pm = Modules.get(PacketMine.class);
        if (Modules.enabled(PacketMine.class) && pm.render) {
            long globalTime = System.currentTimeMillis();
            for (var mb : ravex.modules.player.PacketMine.miningBlocks) {
                if (mb == null || mb.pos == null) continue;
                try {
                    modelViewMatrix.translate((float)(mb.pos.getX() - camPos.x), (float)(mb.pos.getY() - camPos.y), (float)(mb.pos.getZ() - camPos.z), REUSABLE_MATRIX);

                    int c = pm.color;
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
                    Render3DUtility.batchFilledBox(REUSABLE_MATRIX, size, flashR * pulse, flashG * pulse, flashB * pulse, fillAlpha);

                    float wireAlpha = (0.3f + 0.5f * (1.0f - progress)) * fadeOut;
                    Render3DUtility.batchWireframe(REUSABLE_MATRIX, size * 1.005, r, g, b, wireAlpha, 1.5f, true);
                    Render3DUtility.batchWireframe(REUSABLE_MATRIX, size * 1.015, r, g, b, wireAlpha * 0.5f, 1.5f, true);
                    Render3DUtility.batchWireframe(REUSABLE_MATRIX, size * 1.025, r, g, b, wireAlpha * 0.2f, 1.5f, true);

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
                        Render3DUtility.batchFilledBox(REUSABLE_MATRIX, 0.04, flashR, flashG, flashB, 0.6f * (1.0f - progress) * fadeOut);
                    }
                } catch (Exception ignored) {}
            }
        }


        if (Modules.enabled(Waypoint.class)) {
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
                        Render3DUtility.batchAxisLine(modelViewMatrix,
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
                    Render3DUtility.batchFilledBox(REUSABLE_MATRIX, size, wr, wg, wb, 0.6f, true);
                    Render3DUtility.batchWireframe(REUSABLE_MATRIX, size * 1.5, wr, wg, wb, 0.9f, 2.0f, true);
                    Render3DUtility.batchWireframe(REUSABLE_MATRIX, size * 2.0, wr, wg, wb, 0.3f, 2.0f, true);
                } catch (Exception ignored) {}
            }
        }

        // AutoFarm highlight blocks in 3D world
        AutoFarm af = ModuleManager.get(AutoFarm.class);
        if (Modules.enabled(AutoFarm.class) && af != null && af.highlight) {
            long afNow = System.currentTimeMillis();
            int afDuration = af.highlightDuration;
            int afColor = af.highlightColor;
            float afR = ((afColor >> 16) & 0xFF) / 255.0f;
            float afG = ((afColor >> 8) & 0xFF) / 255.0f;
            float afB = (afColor & 0xFF) / 255.0f;
            float afBaseAlpha = ((afColor >> 24) & 0xFF) / 255.0f;
            boolean afFilled = af.highlightFilled;
            boolean afWireframe = af.highlightWireframe;

            var highlightTimes = FarmUtility.getHighlightTimes();
            for (var entry : highlightTimes.entrySet()) {
                long posLong = entry.getKey();
                long hlTime = entry.getValue();
                long elapsed = afNow - hlTime;
                if (elapsed >= afDuration) continue;

                float progress = (float) elapsed / afDuration;

                // ease-out cubic для плавного затухания
                float fade = 1f - progress;
                fade = 1f - (1f - fade) * (1f - fade) * (1f - fade);

                float r = afR, g = afG, b = afB, a = afBaseAlpha * fade;

                BlockPos pos = BlockPos.of(posLong);
                Matrix4f mat = new Matrix4f();
                modelViewMatrix.translate(
                    (float) (pos.getX() - camPos.x),
                    (float) (pos.getY() - camPos.y),
                    (float) (pos.getZ() - camPos.z),
                    mat
                );

                double s = 1.002;
                if (afFilled) {
                    Render3DUtility.batchFilledBox(mat, s, r, g, b, fade * 0.25f * afBaseAlpha, true);
                }
                if (afWireframe) {
                    Render3DUtility.batchWireframe(mat, s, r, g, b, fade * 0.95f * afBaseAlpha);
                    Render3DUtility.batchWireframe(mat, s * 1.03, r, g, b, fade * 0.3f * afBaseAlpha);
                }
            }
        }

        if (Modules.enabled(PearlTarget.class)) {
            try {
                Modules.get(PearlTarget.class).render(modelViewMatrix, camera);
            } catch (Exception ignored) {}
        }

        Search search = Modules.get(Search.class);
        if (Modules.enabled(Search.class) && search.esp) {
            Vec3 sp = camera.position();
            int sbc = search.blockColor;
            float sbr = ((sbc >> 16) & 0xFF) / 255.0f;
            float sbg = ((sbc >> 8) & 0xFF) / 255.0f;
            float sbb = (sbc & 0xFF) / 255.0f;
            float sba = ((sbc >> 24) & 0xFF) / 255.0f;
            for (BlockPos p : search.getFoundBlocks()) {
                try {
                    modelViewMatrix.translate((float)(p.getX() - sp.x), (float)(p.getY() - sp.y), (float)(p.getZ() - sp.z), REUSABLE_MATRIX);
                    Render3DUtility.batchFilledBox(REUSABLE_MATRIX, 1.002, sbr, sbg, sbb, sba * 0.25f);
                    Render3DUtility.batchWireframe(REUSABLE_MATRIX, 1.002, sbr, sbg, sbb, sba * 0.85f);
                } catch (Exception ignored) {}
            }
            int sec = search.entityColor;
            float ser = ((sec >> 16) & 0xFF) / 255.0f;
            float seg = ((sec >> 8) & 0xFF) / 255.0f;
            float seb = (sec & 0xFF) / 255.0f;
            float sea = ((sec >> 24) & 0xFF) / 255.0f;
            if (mc.level != null) {
                for (Entity entity : mc.level.entitiesForRendering()) {
                    if (entity == null || entity.isRemoved()) continue;
                    Identifier id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
                    if (!search.isEntitySelected(id)) continue;
                    try {
                        modelViewMatrix.translate((float)(entity.getX() - sp.x), (float)(entity.getY() - sp.y), (float)(entity.getZ() - sp.z), REUSABLE_MATRIX);
                        double s = entity.getBbWidth() + 0.1;
                        double sh = entity.getBbHeight();
                        float anim = (float)(System.currentTimeMillis() * 0.003 + entity.getId() * 1.7);
                        float pulse = 0.7f + 0.3f * (float)Math.sin(anim);
                        Render3DUtility.batchFilledBox(REUSABLE_MATRIX, s, ser, seg, seb, sea * 0.15f * pulse);
                        Render3DUtility.batchWireframe(REUSABLE_MATRIX, s, ser, seg, seb, sea * 0.6f * pulse);
                        Render3DUtility.batchWireframe(REUSABLE_MATRIX, s * 1.1, ser, seg, seb, sea * 0.15f * pulse);
                        modelViewMatrix.translate(0, (float)sh, 0, REUSABLE_MATRIX);
                        Render3DUtility.batchFilledBox(REUSABLE_MATRIX, s, ser, seg, seb, sea * 0.1f * pulse);
                        Render3DUtility.batchWireframe(REUSABLE_MATRIX, s, ser, seg, seb, sea * 0.4f * pulse);
                    } catch (Exception ignored) {}
                }
            }
        }

        if (Modules.enabled(KillAura.class) && Modules.get(KillAura.class).targetEsp) {
            try {
                Modules.get(KillAura.class).render(modelViewMatrix, camera, mc.getDeltaTracker().getGameTimeDeltaTicks());
            } catch (Exception ignored) {}
        }

        if (Modules.enabled(AutoCrystal.class) && Modules.get(AutoCrystal.class).targetEsp) {
            try {
                Modules.get(AutoCrystal.class).render(modelViewMatrix, camera, mc.getDeltaTracker().getGameTimeDeltaTicks());
            } catch (Exception ignored) {}
        }

        if (Modules.enabled(Skeleton.class)) {
            try {
                Modules.get(Skeleton.class).renderWorld(modelViewMatrix);
            } catch (Exception ignored) {}
        }

        if (Modules.enabled(ravex.modules.render.LogoutSpots.class)) {
            try {
                ravex.modules.render.LogoutSpots.renderWorld(camera, modelViewMatrix, projectionMatrix);
            } catch (Exception ignored) {}
        }

        Render3DUtility.endFrame();
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

            Render3DUtility.batchFilledBox(REUSABLE_MATRIX, size, r, g, b, filledAlpha);
            Render3DUtility.batchWireframe(REUSABLE_MATRIX, size, r, g, b, lineAlpha);
            Render3DUtility.batchWireframe(REUSABLE_MATRIX, size * 1.02, r, g, b, lineAlpha * 0.4f);
        } catch (Exception ignored) {}
    }

    private void renderScaffoldHighlight(Vec3 highlightPos, float alpha, double size, float r, float g, float b, long nowMs, Vec3 camPos, Matrix4f modelViewMatrix) {
        if (highlightPos == null || alpha <= 0.01f) return;

        try {
            modelViewMatrix.translate(
                    (float)(highlightPos.x - camPos.x),
                    (float)(highlightPos.y - camPos.y),
                    (float)(highlightPos.z - camPos.z),
                    REUSABLE_MATRIX
                );

            double s = Math.max(0.2, size);
            float pulse = 0.7f + 0.3f * (float) Math.sin(nowMs * 0.0035);

            Render3DUtility.batchFilledBox(REUSABLE_MATRIX, s, r, g, b, alpha * 0.14f, true);
            Render3DUtility.batchWireframe(REUSABLE_MATRIX, s * 0.985, r, g, b, alpha * 0.28f, 1.0f, true);
            Render3DUtility.batchWireframe(REUSABLE_MATRIX, s * 1.004, r, g, b, alpha * 0.95f, 1.6f, true);
            Render3DUtility.batchWireframe(REUSABLE_MATRIX, s * 1.05, r, g, b, alpha * 0.4f * pulse, 1.0f, true);
        } catch (Exception ignored) {}
    }

    private void renderChunkBorderLines(Matrix4f modelViewMatrix, int bx, int bz, float r, float g, float b, float a, Vec3 camPos, float th) {
        float cx = (float)camPos.x;
        float cy = (float)camPos.y;
        float cz = (float)camPos.z;



        Render3DUtility.batchAxisLine(modelViewMatrix, bx - cx, -64 - cy, bz - cz, bx - cx, 320 - cy, bz - cz, th, r, g, b, a, true);
        Render3DUtility.batchAxisLine(modelViewMatrix, bx + 16 - cx, -64 - cy, bz - cz, bx + 16 - cx, 320 - cy, bz - cz, th, r, g, b, a, true);
        Render3DUtility.batchAxisLine(modelViewMatrix, bx - cx, -64 - cy, bz + 16 - cz, bx - cx, 320 - cy, bz + 16 - cz, th, r, g, b, a, true);
        Render3DUtility.batchAxisLine(modelViewMatrix, bx + 16 - cx, -64 - cy, bz + 16 - cz, bx + 16 - cx, 320 - cy, bz + 16 - cz, th, r, g, b, a, true);


        int baseY = Math.round(cy / 32.0f) * 32;
        for (int dy = -32; dy <= 32; dy += 32) {
            int y = baseY + dy;
            if (y < -64 || y > 320) continue;
            float yOff = y - cy;
            float ha = a * (1.0f - Math.abs(dy) / 64.0f) * 0.5f;
            Render3DUtility.batchAxisLine(modelViewMatrix, bx - cx, yOff, bz - cz, bx + 16 - cx, yOff, bz - cz, th, r, g, b, ha, true);
            Render3DUtility.batchAxisLine(modelViewMatrix, bx + 16 - cx, yOff, bz - cz, bx + 16 - cx, yOff, bz + 16 - cz, th, r, g, b, ha, true);
            Render3DUtility.batchAxisLine(modelViewMatrix, bx + 16 - cx, yOff, bz + 16 - cz, bx - cx, yOff, bz + 16 - cz, th, r, g, b, ha, true);
            Render3DUtility.batchAxisLine(modelViewMatrix, bx - cx, yOff, bz + 16 - cz, bx - cx, yOff, bz - cz, th, r, g, b, ha, true);
        }
    }

    private void renderCurrentChunkHighlight(Matrix4f modelViewMatrix, int bx, int bz, float feetY, float r, float g, float b, float a, Vec3 camPos, float th) {
        float cx = (float)camPos.x;
        float cy = (float)camPos.y;
        float cz = (float)camPos.z;
        float x0 = bx - cx;
        float x1 = bx + 16 - cx;
        float z0 = bz - cz;
        float z1 = bz + 16 - cz;
        float yb = feetY - cy + 0.02f;
        float yt = yb + 6f;
        float vertA = Math.min(1f, a * 1.6f);
        float topA = a * 0.7f;

        Render3DUtility.batchAxisLine(modelViewMatrix, x0, yb, z0, x0, yt, z0, th, r, g, b, vertA, true);
        Render3DUtility.batchAxisLine(modelViewMatrix, x1, yb, z0, x1, yt, z0, th, r, g, b, vertA, true);
        Render3DUtility.batchAxisLine(modelViewMatrix, x0, yb, z1, x0, yt, z1, th, r, g, b, vertA, true);
        Render3DUtility.batchAxisLine(modelViewMatrix, x1, yb, z1, x1, yt, z1, th, r, g, b, vertA, true);

        Render3DUtility.batchAxisLine(modelViewMatrix, x0, yb, z0, x1, yb, z0, th, r, g, b, a, true);
        Render3DUtility.batchAxisLine(modelViewMatrix, x1, yb, z0, x1, yb, z1, th, r, g, b, a, true);
        Render3DUtility.batchAxisLine(modelViewMatrix, x1, yb, z1, x0, yb, z1, th, r, g, b, a, true);
        Render3DUtility.batchAxisLine(modelViewMatrix, x0, yb, z1, x0, yb, z0, th, r, g, b, a, true);

        Render3DUtility.batchAxisLine(modelViewMatrix, x0, yt, z0, x1, yt, z0, th, r, g, b, topA, true);
        Render3DUtility.batchAxisLine(modelViewMatrix, x1, yt, z0, x1, yt, z1, th, r, g, b, topA, true);
        Render3DUtility.batchAxisLine(modelViewMatrix, x1, yt, z1, x0, yt, z1, th, r, g, b, topA, true);
        Render3DUtility.batchAxisLine(modelViewMatrix, x0, yt, z1, x0, yt, z0, th, r, g, b, topA, true);
    }
}
