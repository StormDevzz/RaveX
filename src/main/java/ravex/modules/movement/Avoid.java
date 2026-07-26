package ravex.modules.movement;

import ravex.modules.annotations.ModuleInfo;
import ravex.parameter.BooleanParameter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
@ModuleInfo(name = "Avoid", category = "Movement")
public class Avoid extends ravex.modules.Module {
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
        return ravex.manager.ModuleManager.INSTANCE.getByName("Avoid").getEnabled();
    }
    public static Avoid itz() {
        return ravex.manager.ModuleManager.delegate(Avoid.class);
    }

    public java.util.List<ravex.parameter.Parameter<?>> getParameters() {
        java.util.List<ravex.parameter.Parameter<?>> list = new java.util.ArrayList<>();
        for (java.lang.reflect.Field field : getClass().getDeclaredFields()) {
            if (ravex.parameter.Parameter.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    list.add((ravex.parameter.Parameter<?>) field.get(this));
                } catch (Exception ignored) {}
            }
        }
        return list;
    }
}