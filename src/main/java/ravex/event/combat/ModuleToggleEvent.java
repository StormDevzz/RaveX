package ravex.event.combat;

import ravex.event.Event;
import ravex.modules.Module;

public class ModuleToggleEvent implements Event {
    private final Module module;
    private final boolean enabled;

    public ModuleToggleEvent(Module module, boolean enabled) {
        this.module = module;
        this.enabled = enabled;
    }

    public Module getModule() { return module; }
    public boolean isEnabled() { return enabled; }
}
