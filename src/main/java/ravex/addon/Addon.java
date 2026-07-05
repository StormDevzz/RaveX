package ravex.addon;

import ravex.addon.core.AddonContext;
import ravex.addon.core.AddonInfo;

public interface Addon {
    void onLoad(AddonContext context);
    void onUnload();
    AddonInfo getInfo();
}
