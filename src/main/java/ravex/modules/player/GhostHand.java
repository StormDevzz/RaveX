package ravex.modules.player;

import ravex.modules.annotations.ModuleInfo;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;
@ModuleInfo(name = "GhostHand", category = "Player")
public class GhostHand extends ravex.modules.Module {
public final NumberParameter range = new NumberParameter("Range", 6.0, 3.0, 12.0, 0.5);
    public final BooleanParameter chests = new BooleanParameter("Chests", true);
    public final BooleanParameter enderChests = new BooleanParameter("EnderChests", true);
    public final BooleanParameter furnaces = new BooleanParameter("Furnaces", true);
    public final BooleanParameter craftingTables = new BooleanParameter("Crafting", true);
    public final BooleanParameter enchantTables = new BooleanParameter("Enchanting", true);
    public final BooleanParameter allBlocks = new BooleanParameter("AllBlocks", false);
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("GhostHand").getEnabled();
    }
    public static GhostHand itz() {
        return ravex.manager.ModuleManager.delegate(GhostHand.class);
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