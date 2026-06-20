package ravex.addon;

public interface Addon {
    void onLoad(AddonContext context);
    void onUnload();
    AddonInfo getInfo();
}
