package ravex.modules.movement;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class Avoid extends Module {
    public static final Avoid INSTANCE = new Avoid();

    public final BooleanParameter cactus = new BooleanParameter("Cactus", true);
    public final BooleanParameter berryBush = new BooleanParameter("Berry Bush", true);
    public final BooleanParameter witherRose = new BooleanParameter("Wither Rose", true);
    public final BooleanParameter fire = new BooleanParameter("Fire", true);
    public final BooleanParameter magma = new BooleanParameter("Magma", true);

    private Avoid() {
        super("Avoid", Category.MOVEMENT);
        addParameter(cactus);
        addParameter(berryBush);
        addParameter(witherRose);
        addParameter(fire);
        addParameter(magma);
    }

    public boolean shouldAvoid(Block block) {
        if (!getEnabled()) return false;
        if (block == Blocks.CACTUS) return cactus.getValue();
        if (block == Blocks.SWEET_BERRY_BUSH) return berryBush.getValue();
        if (block == Blocks.WITHER_ROSE) return witherRose.getValue();
        if (block == Blocks.FIRE || block == Blocks.SOUL_FIRE) return fire.getValue();
        if (block == Blocks.MAGMA_BLOCK) return magma.getValue();
        return false;
    }
}
