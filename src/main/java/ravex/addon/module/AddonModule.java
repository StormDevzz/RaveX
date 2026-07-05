package ravex.addon.module;

import ravex.addon.Addon;
import ravex.modules.Category;
import ravex.modules.Module;

public abstract class AddonModule extends Module {
    private final Addon parent;

    public AddonModule(String name, Category category, Addon parent) {
        super(name);
        setCategory(category);
        this.parent = parent;
    }

    public Addon getParent() { return parent; }
}
