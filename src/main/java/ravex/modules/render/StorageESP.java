package ravex.modules.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BrewingStandBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.DropperBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.render.Render3DUtility;

// highlights storage blocks, the boxes show through walls
@Module(name = "StorageESP", category = "Render")
public class StorageESP {
    @Parameter(name = "Range", min = 8, max = 96, step = 8)
    public double range = 48;

    @Parameter(name = "Chests")
    public boolean chests = true;
    @Parameter(name = "EnderChests")
    public boolean enderChests = true;
    @Parameter(name = "Shulkers")
    public boolean shulkers = true;
    @Parameter(name = "Barrels")
    public boolean barrels = true;
    @Parameter(name = "Hoppers")
    public boolean hoppers = true;
    @Parameter(name = "Droppers")
    public boolean droppers = true;
    @Parameter(name = "Dispensers")
    public boolean dispensers = true;
    @Parameter(name = "Furnaces")
    public boolean furnaces = true;
    @Parameter(name = "BrewingStands")
    public boolean brewingStands = true;

    @Parameter(name = "ChestColor", color = true)
    public int chestColor = 0xFFFFFF00;
    @Parameter(name = "EnderChestColor", color = true)
    public int enderChestColor = 0xFFAA00FF;
    @Parameter(name = "ShulkerColor", color = true)
    public int shulkerColor = 0xFFFF55FF;
    @Parameter(name = "BarrelColor", color = true)
    public int barrelColor = 0xFFFFAA00;
    @Parameter(name = "HopperColor", color = true)
    public int hopperColor = 0xFFAAAAAA;
    @Parameter(name = "DropperColor", color = true)
    public int dropperColor = 0xFF888888;
    @Parameter(name = "DispenserColor", color = true)
    public int dispenserColor = 0xFF888888;
    @Parameter(name = "FurnaceColor", color = true)
    public int furnaceColor = 0xFFFF8800;
    @Parameter(name = "BrewingColor", color = true)
    public int brewingColor = 0xFF55FFFF;

    @Parameter(name = "Filled")
    public boolean filled = false;
    @Parameter(name = "Wireframe")
    public boolean wireframe = true;
    @Parameter(name = "ThroughWalls")
    public boolean throughWalls = true;

        // the box of one container, it follows the real block shape
    static final class StorageBox {
        final BlockPos pos;
        final double minX;
        final double minY;
        final double minZ;
        final double maxX;
        final double maxY;
        final double maxZ;
        final int color;

        StorageBox(BlockPos pos, AABB bounds, int color) {
            this.pos = pos;
            this.minX = bounds.minX;
            this.minY = bounds.minY;
            this.minZ = bounds.minZ;
            this.maxX = bounds.maxX;
            this.maxY = bounds.maxY;
            this.maxZ = bounds.maxZ;
            this.color = color;
        }
    }

    private final List<StorageBox> storages = new ArrayList<>();
    private int tickCounter = 0;

    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getLevel() == null) {
            storages.clear();
            return;
        }
        // the block entity list rarely changes, one scan per tick is enough
        if (++tickCounter % 5 != 0) return;
        tickCounter = 0;

        storages.clear();
        var level = mc.getLevel();
        var player = mc.getPlayer();
        double maxDistSq = range * range;
        int playerChunkX = player.getBlockX() >> 4;
        int playerChunkZ = player.getBlockZ() >> 4;
        int chunkRadius = (int) Math.ceil(range / 16.0) + 1;

        for (int cx = playerChunkX - chunkRadius; cx <= playerChunkX + chunkRadius; cx++) {
            for (int cz = playerChunkZ - chunkRadius; cz <= playerChunkZ + chunkRadius; cz++) {
                if (!level.getChunkSource().hasChunk(cx, cz)) continue;
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) continue;
                for (var entry : chunk.getBlockEntities().entrySet()) {
                    BlockPos pos = entry.getKey();
                    if (pos.distSqr(player.blockPosition()) > maxDistSq) continue;
                    BlockState state = level.getBlockState(pos);
                    if (!matches(state.getBlock())) continue;
                    StorageBox box = buildBox(level, pos, state);
                    if (box != null) storages.add(box);
                }
            }
        }
    }

        // the box hugs the block, a double chest becomes one wide box
    private StorageBox buildBox(net.minecraft.world.level.Level level, BlockPos pos, BlockState state) {
        AABB bounds = shapeBounds(level, pos, state);
        if (bounds == null) return null;

        if (state.getBlock() instanceof ChestBlock) {
            ChestType type = state.getValue(ChestBlock.TYPE);
            if (type != ChestType.SINGLE) {
                BlockPos partnerPos = pos.relative(ChestBlock.getConnectedDirection(state));
                BlockState partner = level.getBlockState(partnerPos);
                boolean paired = partner.getBlock() instanceof ChestBlock;
                if (type == ChestType.RIGHT) {
                    // the left half owns the merged box
                    if (paired) return null;
                } else if (paired) {
                    AABB other = shapeBounds(level, partnerPos, partner);
                    if (other != null) {
                        bounds = new AABB(
                            Math.min(bounds.minX, other.minX),
                            Math.min(bounds.minY, other.minY),
                            Math.min(bounds.minZ, other.minZ),
                            Math.max(bounds.maxX, other.maxX),
                            Math.max(bounds.maxY, other.maxY),
                            Math.max(bounds.maxZ, other.maxZ));
                    }
                }
            }
        }
        return new StorageBox(pos, bounds, getColor(state.getBlock()));
    }

    private int getColor(Block block) {
        if (block instanceof ChestBlock) return chestColor;
        if (block instanceof EnderChestBlock) return enderChestColor;
        if (block instanceof ShulkerBoxBlock) return shulkerColor;
        if (block instanceof BarrelBlock) return barrelColor;
        if (block instanceof HopperBlock) return hopperColor;
        if (block instanceof DropperBlock) return dropperColor;
        if (block instanceof DispenserBlock) return dispenserColor;
        if (block instanceof AbstractFurnaceBlock) return furnaceColor;
        if (block instanceof BrewingStandBlock) return brewingColor;
        return 0xFF40A9F8;
    }

    // the shape of the block in world coordinates, a full block as fallback
    private AABB shapeBounds(net.minecraft.world.level.Level level, BlockPos pos, BlockState state) {
        try {
            AABB local = state.getShape(level, pos).bounds();
            if (local == null) return null;
            double w = local.maxX - local.minX;
            double h = local.maxY - local.minY;
            double d = local.maxZ - local.minZ;
            if (w <= 0.0 || h <= 0.0 || d <= 0.0) return null;
            return new AABB(
                pos.getX() + local.minX,
                pos.getY() + local.minY,
                pos.getZ() + local.minZ,
                pos.getX() + local.maxX,
                pos.getY() + local.maxY,
                pos.getZ() + local.maxZ);
        } catch (Throwable ignored) {
            return new AABB(pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1.0, pos.getY() + 1.0, pos.getZ() + 1.0);
        }
    }

    private boolean matches(Block block) {
        if (block instanceof ChestBlock) return chests;
        if (block instanceof EnderChestBlock) return enderChests;
        if (block instanceof ShulkerBoxBlock) return shulkers;
        if (block instanceof BarrelBlock) return barrels;
        if (block instanceof HopperBlock) return hoppers;
        if (block instanceof DropperBlock) return droppers;
        if (block instanceof DispenserBlock) return dispensers;
        if (block instanceof AbstractFurnaceBlock) return furnaces;
        if (block instanceof BrewingStandBlock) return brewingStands;
        return false;
    }

    public void onDisable() {
        storages.clear();
    }

    public List<StorageBox> getBoxes() {
        return storages;
    }

    private static final Matrix4f REUSABLE_MATRIX = new Matrix4f();

    public static void renderStorage(Matrix4f matrix, Vec3 camPos) {
        try {
            if (!Modules.enabled(StorageESP.class)) return;
            StorageESP m = Modules.get(StorageESP.class);
            if (m == null || m.getBoxes().isEmpty()) return;
            if (!m.filled && !m.wireframe) return;

            float lw = 1.5f;

            for (StorageBox box : m.getBoxes()) {
                int lc = box.color;
                float lr = ((lc >> 16) & 0xFF) / 255.0f;
                float lg = ((lc >> 8) & 0xFF) / 255.0f;
                float lb = (lc & 0xFF) / 255.0f;
                float la = ((lc >> 24) & 0xFF) / 255.0f;
                if ((lc >> 24) == 0) la = 1.0f;
                double dx = box.maxX - box.minX;
                double dy = box.maxY - box.minY;
                double dz = box.maxZ - box.minZ;
                double bdx = box.maxX - box.minX;
                double bdy = box.maxY - box.minY;
                double bdz = box.maxZ - box.minZ;

                REUSABLE_MATRIX.set(matrix);
                REUSABLE_MATRIX.translate(
                        (float) (box.minX - camPos.x),
                        (float) (box.minY - camPos.y),
                        (float) (box.minZ - camPos.z));
                REUSABLE_MATRIX.scale((float) bdx, (float) bdy, (float) bdz);

                if (m.filled) {
                    Render3DUtility.batchFilledBox(REUSABLE_MATRIX, 1.0, lr, lg, lb, la * 0.4f, m.throughWalls);
                }
                if (m.wireframe) {
                    Render3DUtility.batchWireframe(REUSABLE_MATRIX, 1.0, lr, lg, lb, la, lw, m.throughWalls);
                }
            }
        } catch (Throwable t) {
            System.err.println("[RaveX] StorageESP render error: " + t.getMessage());
        }
    }
}
