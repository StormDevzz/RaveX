package ravex.modules.misc;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import ravex.event.Subscribe;
import ravex.event.network.PacketEvent;
import ravex.utility.nativelib.NativeLibraryUtility;
import ravex.utility.misc.block.BlockUtility;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.world.level.ChunkPos;
import ravex.utility.misc.PhysicUtility;

import ravex.utility.render.Render3DUtility;
import org.joml.Matrix4f;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;
import ravex.mcwrapper.MinecraftWrapper;

@ModuleInfo(name = "NewChunks", category = "Misc")
public class NewChunks implements ModuleAccess {
    @Parameter(name = "Notify")
    public boolean notify = true;
    @Parameter(name = "Render")
    public boolean render = true;
    @Parameter(name = "RenderLoaded")
    public boolean renderLoaded = true;
    @Parameter(name = "RenderVisited")
    public boolean renderVisited = true;
    @Parameter(name = "Render1.12.2")
    public boolean render112 = true;
    @Parameter(name = "LoadedColor", color = true)
    public int loadedColor = 0xFFFFFFFF;
    @Parameter(name = "VisitedColor", color = true)
    public int visitedColor = 0xFFFF3333;
    @Parameter(name = "1.12.2Color", color = true)
    public int old112Color = 0xFF33FF33;
    private final Set<ChunkPos> loadedChunks = new HashSet<>();
    private final Set<ChunkPos> visitedChunks = new HashSet<>();
    private final Set<ChunkPos> old112Chunks = new HashSet<>();
    private final Set<ChunkPos> analyzedChunks = new HashSet<>();
    private static final NativeLibraryUtility NATIVE = NativeLibraryUtility.of("ravex_chunkexploit");
    static {
        NATIVE.load();
    }
    private static native int nativeAnalyzeChunk(String[] blockNames, int[] blockYs);
    public void onTick() {
        var mc = MinecraftWrapper.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (loadedChunks.size() > 3000) loadedChunks.clear();
        if (visitedChunks.size() > 3000) visitedChunks.clear();
        if (old112Chunks.size() > 3000) old112Chunks.clear();
        if (analyzedChunks.size() > 3000) analyzedChunks.clear();
        int renderDist = mc.options.renderDistance().get();
        int pChunkX = mc.player.chunkPosition().x;
        int pChunkZ = mc.player.chunkPosition().z;
        for (int dx = -renderDist; dx <= renderDist; dx++) {
            for (int dz = -renderDist; dz <= renderDist; dz++) {
                int cx = pChunkX + dx;
                int cz = pChunkZ + dz;
                ChunkPos pos = new ChunkPos(cx, cz);
                if (mc.level.getChunkSource().hasChunk(cx, cz) && !analyzedChunks.contains(pos)) {
                    var chunk = mc.level.getChunkSource().getChunk(cx, cz, false);
                    if (chunk != null) {
                        analyzeChunk(chunk);
                    }
                }
            }
        }
    }
    private void analyzeChunk(net.minecraft.world.level.chunk.LevelChunk chunk) {
        ChunkPos pos = chunk.getPos();
        analyzedChunks.add(pos);
        List<String> sampledNames = new ArrayList<>();
        List<Integer> sampledYs = new ArrayList<>();
        int[] sampleYs = { -10, 0, 10, 64 };
        int[] sampleXZs = { 0, 4, 8, 12 };
        for (int y : sampleYs) {
            if (y < chunk.getMinY() || y >= chunk.getMaxY()) continue;
            for (int x : sampleXZs) {
                for (int z : sampleXZs) {
                    net.minecraft.core.BlockPos bp = new net.minecraft.core.BlockPos(pos.getMinBlockX() + x, y, pos.getMinBlockZ() + z);
                    var state = chunk.getBlockState(bp);
                    if (state != null && !state.isAir()) {
                        String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
                        sampledNames.add(path);
                        sampledYs.add(y);
                    }
                }
            }
        }
        int result = 0;
        if (NATIVE.isLoaded()) {
            try {
                String[] names = sampledNames.toArray(new String[0]);
                int[] ys = new int[sampledYs.size()];
                for (int i = 0; i < ys.length; i++) {
                    ys[i] = sampledYs.get(i);
                }
                result = nativeAnalyzeChunk(names, ys);
            } catch (Throwable t) {
                result = 0;
            }
        }
        if (result == 2) {
            old112Chunks.add(pos);
            if (notify) {
                MinecraftWrapper.getInstance().execute(() -> {
                    var mc = MinecraftWrapper.getInstance();
                    if (mc.player != null) {
                        int color = ravex.manager.ModuleManager.delegate(ravex.modules.client.Notifications.class).messageColor;
                        Component message = Component.literal("[")
                            .withStyle(style -> style.withColor(0x7F7F7F))
                            .append(Component.literal("NewChunks").withStyle(style -> style.withColor(color)))
                            .append(Component.literal("] ").withStyle(style -> style.withColor(0x7F7F7F)))
                            .append(Component.literal("Old 1.12.2 chunk found at: ").withStyle(style -> style.withColor(0x7F7F7F)))
                            .append(Component.literal(pos.getMinBlockX() + ", " + pos.getMinBlockZ()).withStyle(style -> style.withColor(color)));
                        mc.player.displayClientMessage(message, false);
                    }
                });
            }
        } else {
            loadedChunks.add(pos);
        }
    }
    @Subscribe
    public void onPacketEvent(PacketEvent event) {
        if (!ravex.manager.ModuleManager.INSTANCE.getByName("NewChunks").getEnabled() || !event.isReceive()) return;
        onPacketReceive(event.getPacket());
    }

    public void onPacketReceive(Object packet) {
        if (!ravex.manager.ModuleManager.INSTANCE.getByName("NewChunks").getEnabled()) return;
        var mc = MinecraftWrapper.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (packet instanceof ClientboundLevelChunkWithLightPacket chunkPacket) {
        } else if (packet instanceof ClientboundBlockUpdatePacket blockPacket) {
            net.minecraft.core.BlockPos bp = blockPacket.getPos();
            ChunkPos cp = new ChunkPos(bp);
            if (mc.player.chunkPosition().x != cp.x || mc.player.chunkPosition().z != cp.z) {
                visitedChunks.add(cp);
            }
        } else if (packet instanceof ClientboundSectionBlocksUpdatePacket sectionPacket) {
            sectionPacket.runUpdates((bp, state) -> {
                ChunkPos cp = new ChunkPos(bp);
                if (mc.player.chunkPosition().x != cp.x || mc.player.chunkPosition().z != cp.z) {
                    visitedChunks.add(cp);
                }
            });
        }
    }
    public void render(Matrix4f modelViewMatrix, net.minecraft.client.Camera camera) {
        if (!render) return;
        var mc = MinecraftWrapper.getInstance();
        if (mc.player == null || mc.level == null) return;
        net.minecraft.world.phys.Vec3 camPos = camera.position();
        float y = (float) (mc.level.getMinY() - camPos.y + 0.01f);
        Matrix4f reusable = new Matrix4f();
        if (renderLoaded) {
            int loadedVal = loadedColor;
            float lr = ((loadedVal >> 16) & 0xFF) / 255.0f;
            float lg = ((loadedVal >> 8) & 0xFF) / 255.0f;
            float lb = (loadedVal & 0xFF) / 255.0f;
            float la = ((loadedVal >> 24) & 0xFF) / 255.0f;
            for (ChunkPos pos : loadedChunks) {
                drawChunkOutline(modelViewMatrix, reusable, pos, camPos, y, lr, lg, lb, la);
            }
        }
        if (renderVisited) {
            int visitedVal = visitedColor;
            float vr = ((visitedVal >> 16) & 0xFF) / 255.0f;
            float vg = ((visitedVal >> 8) & 0xFF) / 255.0f;
            float vb = (visitedVal & 0xFF) / 255.0f;
            float va = ((visitedVal >> 24) & 0xFF) / 255.0f;
            for (ChunkPos pos : visitedChunks) {
                drawChunkOutline(modelViewMatrix, reusable, pos, camPos, y, vr, vg, vb, va);
            }
        }
        if (render112) {
            int oldVal = old112Color;
            float or = ((oldVal >> 16) & 0xFF) / 255.0f;
            float og = ((oldVal >> 8) & 0xFF) / 255.0f;
            float ob = (oldVal & 0xFF) / 255.0f;
            float oa = ((oldVal >> 24) & 0xFF) / 255.0f;
            for (ChunkPos pos : old112Chunks) {
                drawChunkOutline(modelViewMatrix, reusable, pos, camPos, y, or, og, ob, oa);
            }
        }
    }
    private void drawChunkOutline(Matrix4f modelViewMatrix, Matrix4f reusable, ChunkPos pos, net.minecraft.world.phys.Vec3 camPos, float y, float r, float g, float b, float a) {
        float x1 = (float) (pos.getMinBlockX() - camPos.x);
        float z1 = (float) (pos.getMinBlockZ() - camPos.z);
        float x2 = x1 + 16.0f;
        float z2 = z1 + 16.0f;
        Render3DUtility.batchAxisLine(modelViewMatrix, x1, y, z1, x2, y, z1, 0.03f, r, g, b, a, true);
        Render3DUtility.batchAxisLine(modelViewMatrix, x2, y, z1, x2, y, z2, 0.03f, r, g, b, a, true);
        Render3DUtility.batchAxisLine(modelViewMatrix, x2, y, z2, x1, y, z2, 0.03f, r, g, b, a, true);
        Render3DUtility.batchAxisLine(modelViewMatrix, x1, y, z2, x1, y, z1, 0.03f, r, g, b, a, true);
        try {
            modelViewMatrix.translate(x1, y, z1, reusable);
            reusable.scale(16.0f, 0.01f, 16.0f);
            Render3DUtility.batchFilledBox(reusable, 1.0, r, g, b, a * 0.15f, true);
        } catch (Exception ignored) {}
    }
    public void onDisable() {
        loadedChunks.clear();
        visitedChunks.clear();
        old112Chunks.clear();
        analyzedChunks.clear();
    }

    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("NewChunks").getEnabled();
    }

    public static NewChunks itz() {
        return ravex.manager.ModuleManager.delegate(NewChunks.class);
    }


}