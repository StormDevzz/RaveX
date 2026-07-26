package ravex.modules.player;

import ravex.modules.annotations.ModuleInfo;
@ModuleInfo(name = "MultiTask", category = "net.minecraft.world.entity.player.Player")
public class MultiTask extends ravex.modules.Module {
public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("MultiTask").getEnabled();
    }
    public static MultiTask itz() {
        return ravex.manager.ModuleManager.delegate(MultiTask.class);
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