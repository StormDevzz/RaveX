package ravex.parameter;

import ravex.modules.Module;
import ravex.modules.ModuleManager;

@FunctionalInterface
public interface ModuleCondition {
    boolean canEnable();

    default ModuleCondition and(ModuleCondition other) {
        return () -> this.canEnable() && other.canEnable();
    }

    default ModuleCondition or(ModuleCondition other) {
        return () -> this.canEnable() || other.canEnable();
    }

    default ModuleCondition negate() {
        return () -> !this.canEnable();
    }

    // Static helper conditions to easily construct custom constraints!
    static ModuleCondition requireModule(String moduleName) {
        return () -> {
            Module m = ModuleManager.INSTANCE.getByName(moduleName);
            return m != null && m.getEnabled();
        };
    }

    static ModuleCondition requireModule(Module module) {
        return () -> module != null && module.getEnabled();
    }

    static ModuleCondition conflictWith(String moduleName) {
        return () -> {
            Module m = ModuleManager.INSTANCE.getByName(moduleName);
            return m == null || !m.getEnabled();
        };
    }

    static ModuleCondition conflictWith(Module module) {
        return () -> module == null || !module.getEnabled();
    }

    static <T> ModuleCondition parameterEquals(Parameter<T> parameter, T value) {
        return () -> parameter != null && value.equals(parameter.getValue());
    }

    static <T> ModuleCondition parameterNotEquals(Parameter<T> parameter, T value) {
        return () -> parameter != null && !value.equals(parameter.getValue());
    }

    static ModuleCondition alwaysFalse() {
        return () -> false;
    }
}
