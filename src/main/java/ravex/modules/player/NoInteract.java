package ravex.modules.player;

import ravex.modules.annotations.ModuleInfo;
import ravex.parameter.BooleanParameter;
@ModuleInfo(name = "NoInteract", category = "net.minecraft.world.entity.player.Player")
public class NoInteract extends ravex.modules.Module {
public final BooleanParameter allBlocks = new BooleanParameter("AllBlocks", false);
    public final BooleanParameter chests = new BooleanParameter("Chests", true);
    public final BooleanParameter enderChests = new BooleanParameter("EnderChests", true);
    public final BooleanParameter furnaces = new BooleanParameter("Furnaces", true);
    public final BooleanParameter crafting = new BooleanParameter("Crafting", false);
    public final BooleanParameter enchanting = new BooleanParameter("Enchanting", false);

    public NoInteract() {
        chests.setVisible(() -> !allBlocks.getValue());
        enderChests.setVisible(() -> !allBlocks.getValue());
        furnaces.setVisible(() -> !allBlocks.getValue());
        crafting.setVisible(() -> !allBlocks.getValue());
        enchanting.setVisible(() -> !allBlocks.getValue());
    }

    public boolean shouldBlockAll() {
        return getEnabled() && allBlocks.getValue();
    }
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("NoInteract").getEnabled();
    }
    public static NoInteract itz() {
        return ravex.manager.ModuleManager.delegate(NoInteract.class);
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