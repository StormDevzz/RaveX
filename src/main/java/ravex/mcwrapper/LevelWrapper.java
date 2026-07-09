package ravex.mcwrapper;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import java.util.List;

public class LevelWrapper {
    private final Level level;

    public LevelWrapper(Level level) {
        this.level = level;
    }

    public Level getRaw() { return level; }

    public BlockState getBlockState(BlockPos pos) {
        return level.getBlockState(pos);
    }

    public Block getBlock(BlockPos pos) {
        return level.getBlockState(pos).getBlock();
    }

    public boolean isAir(BlockPos pos) {
        return level.getBlockState(pos).isAir();
    }

    public boolean isSolid(BlockPos pos) {
        return level.getBlockState(pos).isSolid();
    }

    @SuppressWarnings("unchecked")
    public <T extends Entity> List<T> getEntitiesOfClass(Class<T> clazz, AABB bounds) {
        return level.getEntitiesOfClass(clazz, bounds);
    }

    @SuppressWarnings("unchecked")
    public List<Player> getPlayers() {
        return (List<Player>) level.players();
    }

    public List<Entity> getEntitiesInAABB(AABB bounds) {
        return level.getEntities((Entity) null, bounds);
    }

    public BlockHitResult rayTrace(Vec3 from, Vec3 to, ClipContext.Block blockMode, ClipContext.Fluid fluidMode) {
        return level.clip(new ClipContext(from, to, blockMode, fluidMode, (Entity) null));
    }

    public boolean isLoaded(BlockPos pos) {
        return level.isLoaded(pos);
    }

    public int getMinBuildHeight() { return level.getMinY(); }
    public int getMaxBuildHeight() { return level.getMaxY(); }
    public int getSeaLevel() { return level.getSeaLevel(); }

    public long getTime() { return level.getGameTime(); }
    public long getDayTime() { return level.getDayTime(); }

    public boolean isDay() {
        return level.getDayTime() % 24000L < 13000L;
    }

    public boolean isRaining() { return level.isRaining(); }
    public boolean isThundering() { return level.isThundering(); }

    public float getLightLevel(BlockPos pos) {
        return level.getBrightness(LightLayer.SKY, pos);
    }

    public String getDimension() {
        return level.dimension().identifier().toString();
    }

    public boolean isOverworld() {
        return level.dimension() == net.minecraft.world.level.Level.OVERWORLD;
    }

    public boolean isNether() {
        return level.dimension() == net.minecraft.world.level.Level.NETHER;
    }

    public boolean isEnd() {
        return level.dimension() == net.minecraft.world.level.Level.END;
    }
}
