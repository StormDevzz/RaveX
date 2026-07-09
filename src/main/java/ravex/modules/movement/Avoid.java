package ravex.modules.movement;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
public class Avoid extends Module {
    public final BooleanParameter cactus = new BooleanParameter("Cactus", true);
    public final BooleanParameter berryBush = new BooleanParameter("BerryBush", true);
    public final BooleanParameter witherRose = new BooleanParameter("WitherRose", true);
    public final BooleanParameter fire = new BooleanParameter("Fire", true);
    public final BooleanParameter magma = new BooleanParameter("Magma", true);

    public boolean shouldAvoid(Block block) {
        if (!getEnabled()) return false;
        if (block == Blocks.CACTUS) return cactus.getValue();
        if (block == Blocks.SWEET_BERRY_BUSH) return berryBush.getValue();
        if (block == Blocks.WITHER_ROSE) return witherRose.getValue();
        if (block == Blocks.FIRE || block == Blocks.SOUL_FIRE) return fire.getValue();
        if (block == Blocks.MAGMA_BLOCK) return magma.getValue();
        return false;
    }
    public static boolean maybeEnabled() {
        return maybeEnabled(Avoid.class);
    }
    public static Avoid itz() {
        return ModuleManager.get(Avoid.class);
    }
}
