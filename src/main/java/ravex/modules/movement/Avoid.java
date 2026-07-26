package ravex.modules.movement;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import net.minecraft.world.level.block.Block;
import ravex.utility.misc.block.BlockUtility;
@ModuleInfo(name = "Avoid", category = "Movement")
public class Avoid implements ModuleAccess {
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
        if (!ravex.manager.ModuleManager.INSTANCE.getByName("Avoid").getEnabled()) return false;
        if (block == net.minecraft.world.level.block.Blocks.CACTUS) return cactus;
        if (block == net.minecraft.world.level.block.Blocks.SWEET_BERRY_BUSH) return berryBush;
        if (block == net.minecraft.world.level.block.Blocks.WITHER_ROSE) return witherRose;
        if (block == net.minecraft.world.level.block.Blocks.FIRE || block == net.minecraft.world.level.block.Blocks.SOUL_FIRE) return fire;
        if (block == net.minecraft.world.level.block.Blocks.MAGMA_BLOCK) return magma;
        return false;
    }
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("Avoid").getEnabled();
    }
    public static Avoid itz() {
        return ravex.manager.ModuleManager.delegate(Avoid.class);
    }


}