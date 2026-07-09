package ravex.addon;

import ravex.modules.Category;
import ravex.modules.Module;

public abstract class AddonModule extends Module {
    private final Addon parent;

    public AddonModule(String name, Category category, Addon parent) {
        super(name, category);
        this.parent = parent;
    }

    public Addon getParent() { return parent; }
}
