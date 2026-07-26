package ravex.modules.client;

import ravex.modules.annotations.ModuleInfo;
import ravex.parameter.BooleanParameter;
import ravex.parameter.StringParameter;
@ModuleInfo(name = "Commands", category = "Client")
public class Commands extends ravex.modules.Module {
public final StringParameter prefix = new StringParameter("Prefix", ".");
    public final BooleanParameter showFeedback = new BooleanParameter("Feedback", true);
    private Commands() {
        
        enabled = true;
    }

    public static Commands itz() {
        return ravex.manager.ModuleManager.delegate(Commands.class);
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