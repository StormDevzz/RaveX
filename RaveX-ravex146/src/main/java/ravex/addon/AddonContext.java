package ravex.addon;

public class AddonContext {
    private final AddonInfo info;
    private final AddonLogger logger;

    public AddonContext(AddonInfo info) {
        this.info = info;
        this.logger = new AddonLogger(info.getName());
    }

    public AddonInfo getInfo() { return info; }
    public AddonLogger getLogger() { return logger; }
    
    public void registerModule(ravex.modules.Module module) {
        ravex.modules.ModuleManager.INSTANCE.getModules().add(module);
        ravex.RaveX.LOGGER.info("[Addon System] Registered module: " + module.getName() + " from addon " + info.getName());
    }
}
