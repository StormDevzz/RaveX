package ravex.addon;

public class AddonUI {
    public static void registerTab(String title, Runnable onClick) {
        ravex.RaveX.LOGGER.info("[Addon UI] Registered tab: " + title);
    }
}
