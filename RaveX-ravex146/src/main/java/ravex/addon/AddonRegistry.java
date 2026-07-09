package ravex.addon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AddonRegistry {
    private final List<Addon> addons = new ArrayList<>();

    public void register(Addon addon) {
        addons.add(addon);
    }

    public void unregister(Addon addon) {
        addons.remove(addon);
    }

    public List<Addon> getAddons() {
        return Collections.unmodifiableList(addons);
    }
}
