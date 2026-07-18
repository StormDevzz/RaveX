package ravex.modules.world;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import ravex.event.Subscribe;
import ravex.event.network.PacketEvent;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.NumberParameter;
import ravex.utility.render.Render3DUtils;
import org.joml.Matrix4f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChunkAnalyzer extends Module {
    public final BooleanParameter notify = new BooleanParameter("Notify", true);
    public final BooleanParameter render = new BooleanParameter("Render", true);
    public final NumberParameter confidenceThreshold = new NumberParameter("Threshold", 0.7, 0.0, 1.0, 0.05);
    public final ColorParameter lowColor = new ColorParameter("LowColor", 0x88FFAA00);
    public final ColorParameter mediumColor = new ColorParameter("MediumColor", 0x88FF6600);
    public final ColorParameter highColor = new ColorParameter("HighColor", 0x88FF0000);

    private static final double LAVA_DENSITY_EXPECTED_NETHER = 0.035;
    private static final double BEDROCK_DENSITY_EXPECTED = 0.12;
    private static final int ANALYSIS_INTERVAL_TICKS = 40;
    private int tickCounter = 0;

    private final Map<ChunkPos, ChunkAnalysis> analyzedChunks = new ConcurrentHashMap<>();
    private final Set<ChunkPos> pendingChunks = ConcurrentHashMap.newKeySet();

    private static class ChunkAnalysis {
        final double confidence;
        final String reason;
        final long timestamp;

        ChunkAnalysis(double confidence, String reason) {
            this.confidence = Math.min(1.0, Math.max(0.0, confidence));
            this.reason = reason;
            this.timestamp = System.currentTimeMillis();
        }
    }

    @Subscribe
    public void onPacketEvent(PacketEvent event) {
        if (!getEnabled() || !event.isReceive()) return;
        if (event.getPacket() instanceof ClientboundLevelChunkWithLightPacket packet) {
            ChunkPos pos = new ChunkPos(packet.getX(), packet.getZ());
            pendingChunks.add(pos);
        }
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        tickCounter++;
        if (tickCounter % ANALYSIS_INTERVAL_TICKS != 0) return;

        if (pendingChunks.size() > 200) pendingChunks.clear();
        if (analyzedChunks.size() > 500) analyzedChunks.clear();

        Level.ExplorationState explorationState = null;
        try {
            explorationState = mc.level.getExplorationState();
        } catch (Exception ignored) {}

        Set<ChunkPos> toAnalyze = new HashSet<>(pendingChunks);
        pendingChunks.clear();

        for (ChunkPos pos : toAnalyze) {
            if (analyzedChunks.containsKey(pos)) continue;
            LevelChunk chunk = mc.level.getChunkSource().getChunk(pos.x, pos.z, false);
            if (chunk == null) continue;
            analyzeChunk(mc, chunk, explorationState);
        }
    }

    private void analyzeChunk(Minecraft mc, LevelChunk chunk, Level.ExplorationState explorationState) {
        ChunkPos pos = chunk.getPos();
        Level level = mc.level;
        if (level == null) return;

        boolean isNether = level.dimension() == Level.NETHER;
        boolean isOverworld = level.dimension() == Level.OVERWORLD;
        List<String> anomalies = new ArrayList<>();
        double totalConfidence = 0.0;

        if (isNether) {
            double lavaScore = analyzeNetherLavaAnomaly(chunk);
            if (lavaScore > 0.0) {
                anomalies.add("lava");
                totalConfidence += lavaScore * 0.5;
            }
        }

        if (isOverworld || isNether) {
            double bedrockScore = analyzeBedrockDensityAnomaly(chunk);
            if (bedrockScore > 0.0) {
                anomalies.add("bedrock");
                totalConfidence += bedrockScore * 0.35;
            }
        }

        if (isOverworld) {
            double growthScore = analyzeGrowthAnomaly(chunk);
            if (growthScore > 0.0) {
                anomalies.add("growth");
                totalConfidence += growthScore * 0.3;
            }
        }

        if (explorationState != null) {
            long lastVisit = getLastVisitTime(pos, explorationState);
            if (lastVisit > 0) {
                double timeConfidence = Math.min(1.0, lastVisit / 7.776e12);
                totalConfidence += timeConfidence * 0.2;
                if (lastVisit > 2.592e12) {
                    anomalies.add("old_visit");
                }
            }
        }

        if (!anomalies.isEmpty()) {
            double confidence = Math.min(1.0, totalConfidence);
            String reason = String.join(", ", anomalies);
            analyzedChunks.put(pos, new ChunkAnalysis(confidence, reason));

            if (notify.getValue() && confidence >= confidenceThreshold.getValue()) {
                int pct = (int) (confidence * 100);
                int color = ModuleManager.get(ravex.modules.client.Notifications.class).messageColor.getValue();
                Component msg = Component.literal("[")
                    .withStyle(s -> s.withColor(0x7F7F7F))
                    .append(Component.literal("ChunkAnalyzer").withStyle(s -> s.withColor(color)))
                    .append(Component.literal("] ").withStyle(s -> s.withColor(0x7F7F7F)))
                    .append(Component.literal("Activity detected at ")
                        .withStyle(s -> s.withColor(0x7F7F7F)))
                    .append(Component.literal(pos.x + " " + pos.z)
                        .withStyle(s -> s.withColor(color)))
                    .append(Component.literal(" (" + pct + "%)")
                        .withStyle(s -> s.withColor(0x7F7F7F)));
                mc.execute(() -> {
                    if (mc.player != null) mc.player.displayClientMessage(msg, false);
                });
            }
        }
    }

    private double analyzeNetherLavaAnomaly(LevelChunk chunk) {
        ChunkPos pos = chunk.getPos();
        int samples = 0;
        int lavaCount = 0;
        int[] verticalLavaLayers = new int[128];
        int minY = chunk.getMinY();
        int maxY = chunk.getMaxY();

        for (int x = 0; x < 16; x += 4) {
            for (int z = 0; z < 16; z += 4) {
                for (int y = minY + 32; y < maxY && y < minY + 80; y++) {
                    BlockState state = chunk.getBlockState(new BlockPos(pos.getMinBlockX() + x, y, pos.getMinBlockZ() + z));
                    if (state.getBlock() == Blocks.LAVA) {
                        lavaCount++;
                        int idx = y - minY;
                        if (idx >= 0 && idx < verticalLavaLayers.length) {
                            verticalLavaLayers[idx]++;
                        }
                    }
                    samples++;
                }
            }
        }

        if (samples == 0) return 0.0;
        double density = (double) lavaCount / samples;
        double densityDiff = Math.abs(density - LAVA_DENSITY_EXPECTED_NETHER);

        int flatLayers = 0;
        for (int i = 1; i < verticalLavaLayers.length; i++) {
            if (verticalLavaLayers[i] > 0 && verticalLavaLayers[i - 1] > 0) {
                flatLayers++;
            }
        }
        double flatScore = flatLayers > 5 ? Math.min(1.0, flatLayers / 20.0) : 0.0;

        if (densityDiff > 0.02 || flatScore > 0.3) {
            return Math.max(densityDiff * 10, flatScore);
        }
        return 0.0;
    }

    private double analyzeBedrockDensityAnomaly(LevelChunk chunk) {
        ChunkPos pos = chunk.getPos();
        int bedrockCount = 0;
        int totalCount = 0;
        int minY = chunk.getMinY();

        for (int x = 0; x < 16; x += 2) {
            for (int z = 0; z < 16; z += 2) {
                for (int y = minY; y <= minY + 5; y++) {
                    BlockState state = chunk.getBlockState(new BlockPos(pos.getMinBlockX() + x, y, pos.getMinBlockZ() + z));
                    if (state.getBlock() == Blocks.BEDROCK) {
                        bedrockCount++;
                    }
                    totalCount++;
                }
            }
        }

        if (totalCount == 0) return 0.0;
        double density = (double) bedrockCount / totalCount;
        double diff = BEDROCK_DENSITY_EXPECTED - density;
        if (diff > 0.04) {
            return Math.min(1.0, diff * 8);
        }
        return 0.0;
    }

    private double analyzeGrowthAnomaly(LevelChunk chunk) {
        ChunkPos pos = chunk.getPos();
        int minY = chunk.getMinY();
        int maxY = chunk.getMaxY();
        int treeBlocks = 0;
        int bambooBlocks = 0;
        int totalSolid = 0;

        for (int x = 0; x < 16; x += 4) {
            for (int z = 0; z < 16; z += 4) {
                for (int y = minY; y < maxY; y++) {
                    BlockState state = chunk.getBlockState(new BlockPos(pos.getMinBlockX() + x, y, pos.getMinBlockZ() + z));
                    if (state.isAir()) continue;
                    totalSolid++;
                    String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
                    if (path.contains("log") || path.contains("leaves")) {
                        treeBlocks++;
                    } else if (path.contains("bamboo")) {
                        bambooBlocks++;
                    }
                }
            }
        }

        if (totalSolid < 50) return 0.0;
        double treeRatio = (double) treeBlocks / totalSolid;
        double bambooRatio = (double) bambooBlocks / totalSolid;

        double treeAnomaly = 0.0;
        double bambooAnomaly = 0.0;

        if (treeRatio > 0.0 && treeRatio < 0.01) {
            treeAnomaly = 0.3;
        }
        if (bambooRatio > 0.0 && bambooRatio < 0.005) {
            bambooAnomaly = 0.3;
        }

        return Math.max(treeAnomaly, bambooAnomaly);
    }

    private long getLastVisitTime(ChunkPos pos, Level.ExplorationState explorationState) {
        try {
            var explorationMap = explorationState.exploredChunks();
            if (explorationMap != null) {
                var entry = explorationMap.get(pos.toLong());
                if (entry instanceof Long lastVisit) {
                    return lastVisit;
                }
            }
        } catch (Exception ignored) {}
        return 0;
    }

    public void render(Matrix4f modelViewMatrix, net.minecraft.client.Camera camera) {
        if (!render.getValue()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        double threshold = confidenceThreshold.getValue();
        Vec3 camPos = camera.position();
        float baseY = (float) (mc.level.getMinY() - camPos.y + 0.01f);
        Matrix4f reusable = new Matrix4f();

        for (Map.Entry<ChunkPos, ChunkAnalysis> entry : analyzedChunks.entrySet()) {
            ChunkAnalysis analysis = entry.getValue();
            if (analysis.confidence < threshold) continue;

            ChunkPos pos = entry.getKey();
            float x1 = (float) (pos.getMinBlockX() - camPos.x);
            float z1 = (float) (pos.getMinBlockZ() - camPos.z);
            float x2 = x1 + 16.0f;
            float z2 = z1 + 16.0f;

            int colorVal;
            if (analysis.confidence >= 0.85) {
                colorVal = highColor.getValue();
            } else if (analysis.confidence >= 0.7) {
                colorVal = mediumColor.getValue();
            } else {
                colorVal = lowColor.getValue();
            }

            float r = ((colorVal >> 16) & 0xFF) / 255.0f;
            float g = ((colorVal >> 8) & 0xFF) / 255.0f;
            float b = (colorVal & 0xFF) / 255.0f;
            float a = ((colorVal >> 24) & 0xFF) / 255.0f;

            drawChunkOutline(modelViewMatrix, reusable, x1, z1, x2, z2, baseY, r, g, b, a);
        }
    }

    private void drawChunkOutline(Matrix4f modelViewMatrix, Matrix4f reusable, float x1, float z1, float x2, float z2, float y, float r, float g, float b, float a) {
        Render3DUtils.batchAxisLine(modelViewMatrix, x1, y, z1, x2, y, z1, 0.03f, r, g, b, a, true);
        Render3DUtils.batchAxisLine(modelViewMatrix, x2, y, z1, x2, y, z2, 0.03f, r, g, b, a, true);
        Render3DUtils.batchAxisLine(modelViewMatrix, x2, y, z2, x1, y, z2, 0.03f, r, g, b, a, true);
        Render3DUtils.batchAxisLine(modelViewMatrix, x1, y, z2, x1, y, z1, 0.03f, r, g, b, a, true);
    }

    @Override
    protected void onDisable() {
        analyzedChunks.clear();
        pendingChunks.clear();
    }

    public static boolean maybeEnabled() {
        return maybeEnabled(ChunkAnalyzer.class);
    }

    public static ChunkAnalyzer itz() {
        return ModuleManager.get(ChunkAnalyzer.class);
    }
}
