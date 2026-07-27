package ravex.modules.movement;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import net.minecraft.world.level.block.Block;
import ravex.utility.misc.block.BlockUtility;
import ravex.modules.Modules;
@Module(name = "Avoid", category = "Movement")
public class Avoid {
    @Parameter(name = "Cactus")
    public boolean cactus = true;
    @Parameter(name = "BerryBush")
    public boolean berryBush = true;
    @Parameter(name = "WitherRose")
    public boolean witherRose = true;
    @Parameter(name = "Fire")
    public boolean fire = true;
    @Parameter(name = "Magma")
    public boolean magma = true;

    public boolean shouldAvoid(Block block) {
        if (!Modules.enabled(Avoid.class)) return false;
        if (block == net.minecraft.world.level.block.Blocks.CACTUS) return cactus;
        if (block == net.minecraft.world.level.block.Blocks.SWEET_BERRY_BUSH) return berryBush;
        if (block == net.minecraft.world.level.block.Blocks.WITHER_ROSE) return witherRose;
        if (block == net.minecraft.world.level.block.Blocks.FIRE || block == net.minecraft.world.level.block.Blocks.SOUL_FIRE) return fire;
        if (block == net.minecraft.world.level.block.Blocks.MAGMA_BLOCK) return magma;
        return false;
    }




}