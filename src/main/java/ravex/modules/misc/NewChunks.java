package ravex.modules.misc;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.event.Subscribe;
import ravex.event.network.PacketEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.world.level.ChunkPos;

import ravex.utility.render.Render3DUtility;
import org.joml.Matrix4f;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;
import ravex.modules.client.Notifications;

@Module(name = "NewChunks", category = "Misc")
public class NewChunks {
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
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getLevel() == null) return;
        if (loadedChunks.size() > 3000) loadedChunks.clear();
        if (visitedChunks.size() > 3000) visitedChunks.clear();
        if (old112Chunks.size() > 3000) old112Chunks.clear();
        if (analyzedChunks.size() > 3000) analyzedChunks.clear();
        int renderDist = mc.getOptions().renderDistance().get();
        int pChunkX = mc.getPlayer().chunkPosition().x;
        int pChunkZ = mc.getPlayer().chunkPosition().z;
        for (int dx = -renderDist; dx <= renderDist; dx++) {
            for (int dz = -renderDist; dz <= renderDist; dz++) {
                int cx = pChunkX + dx;
                int cz = pChunkZ + dz;
                ChunkPos pos = new ChunkPos(cx, cz);
                if (mc.getLevel().getChunkSource().hasChunk(cx, cz) && !analyzedChunks.contains(pos)) {
                    var chunk = mc.getLevel().getChunkSource().getChunk(cx, cz, false);
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
        int result = analyzeChunk(sampledNames, sampledYs);
        if (result == 2) {
            old112Chunks.add(pos);
            if (notify) {
                MinecraftWrapper.getWrapper().execute(() -> {
                    var mc = MinecraftWrapper.getWrapper();
                    if (mc.getPlayer() != null) {
                        int color = Modules.get(Notifications.class).messageColor;
                        Component message = Component.literal("[")
                            .withStyle(style -> style.withColor(0x7F7F7F))
                            .append(Component.literal("NewChunks").withStyle(style -> style.withColor(color)))
                            .append(Component.literal("] ").withStyle(style -> style.withColor(0x7F7F7F)))
                            .append(Component.literal("Old 1.12.2 chunk found at: ").withStyle(style -> style.withColor(0x7F7F7F)))
                            .append(Component.literal(pos.getMinBlockX() + ", " + pos.getMinBlockZ()).withStyle(style -> style.withColor(color)));
                        mc.getPlayer().displayClientMessage(message, false);
                    }
                });
            }
        } else if (hasFlowingFluid(chunk)) {
            // flowing fluids mean this chunk was simulated before
            visitedChunks.add(pos);
        } else if (hasDeepslateBelowZero(chunk)) {
            loadedChunks.add(pos);
        } else {
            visitedChunks.add(pos);
        }
    }

    // 1.18+ chunks have deepslate below y=0, old ones do not
    private boolean hasDeepslateBelowZero(net.minecraft.world.level.chunk.LevelChunk chunk) {
        ChunkPos pos = chunk.getPos();
        for (int y = chunk.getMinY(); y < -2; y += 4) {
            for (int x = 0; x < 16; x += 4) {
                for (int z = 0; z < 16; z += 4) {
                    net.minecraft.core.BlockPos bp = new net.minecraft.core.BlockPos(pos.getMinBlockX() + x, y, pos.getMinBlockZ() + z);
                    String name = BuiltInRegistries.BLOCK.getKey(chunk.getBlockState(bp).getBlock()).getPath();
                    if (name.contains("deepslate")) return true;
                }
            }
        }
        return false;
    }

    private boolean hasFlowingFluid(net.minecraft.world.level.chunk.LevelChunk chunk) {
        ChunkPos pos = chunk.getPos();
        int minY = Math.max(chunk.getMinY(), -64);
        int maxY = Math.min(chunk.getMaxY(), 320);
        for (int y = minY; y < maxY; y += 4) {
            for (int x = 0; x < 16; x += 2) {
                for (int z = 0; z < 16; z += 2) {
                    net.minecraft.core.BlockPos bp = new net.minecraft.core.BlockPos(pos.getMinBlockX() + x, y, pos.getMinBlockZ() + z);
                    var fluid = chunk.getFluidState(bp);
                    if (!fluid.isEmpty() && !fluid.isSource()) return true;
                }
            }
        }
        return false;
    }
    @Subscribe
    public void onPacketEvent(PacketEvent event) {
        if (!Modules.enabled(NewChunks.class) || !event.isReceive()) return;
        onPacketReceive(event.getPacket());
    }

    public void onPacketReceive(Object packet) {
        if (!Modules.enabled(NewChunks.class)) return;
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getLevel() == null) return;
        if (packet instanceof ClientboundLevelChunkWithLightPacket chunkPacket) {
        } else if (packet instanceof ClientboundBlockUpdatePacket blockPacket) {
            net.minecraft.core.BlockPos bp = blockPacket.getPos();
            ChunkPos cp = new ChunkPos(bp);
            if (mc.getPlayer().chunkPosition().x != cp.x || mc.getPlayer().chunkPosition().z != cp.z) {
                visitedChunks.add(cp);
            }
        } else if (packet instanceof ClientboundSectionBlocksUpdatePacket sectionPacket) {
            sectionPacket.runUpdates((bp, state) -> {
                ChunkPos cp = new ChunkPos(bp);
                if (mc.getPlayer().chunkPosition().x != cp.x || mc.getPlayer().chunkPosition().z != cp.z) {
                    visitedChunks.add(cp);
                }
            });
        }
    }
    public void render(Matrix4f modelViewMatrix, net.minecraft.client.Camera camera) {
        if (!render) return;
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getLevel() == null) return;
        net.minecraft.world.phys.Vec3 camPos = camera.position();
        float y = (float) (mc.getLevel().getMinY() - camPos.y + 0.01f);
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
    private static int analyzeChunk(List<String> names, List<Integer> ys) {
        for (String name : names) {
            if (name.contains("deepslate") || name.contains("copper") || name.contains("tuff")
                || name.contains("raw_") || name.contains("glow_") || name.contains("kelp")
                || name.contains("seagrass") || name.contains("sea_pickle") || name.contains("bamboo")
                || name.contains("sweet_berry") || name.contains("campfire") || name.contains("barrel")
                || name.contains("smoker") || name.contains("blast_") || name.contains("lantern")
                || name.contains("bee_") || name.contains("crying_") || name.contains("respawn_")
                || name.contains("amethyst") || name.contains("calcite") || name.contains("dripleaf")
                || name.contains("spore_") || name.contains("mangrove") || name.contains("cherry")
                || name.contains("pale_") || name.contains("trial_") || name.contains("vault")
                || name.contains("crafter")) {
                return 0;
            }
        }
        boolean bedrockAtZero = false;
        boolean belowZero = false;
        for (int i = 0; i < names.size(); i++) {
            int y = ys.get(i);
            if (y == 0 && "bedrock".equals(names.get(i))) bedrockAtZero = true;
            if (y < 0) belowZero = true;
        }
        return bedrockAtZero && !belowZero ? 2 : 0;
    }






}