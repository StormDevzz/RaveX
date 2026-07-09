package ravex.addon;

public class AddonLogger {
    private final String prefix;

    public AddonLogger(String name) {
        this.prefix = "[" + name + "] ";
    }

    public void info(String msg) {
        ravex.RaveX.LOGGER.info(prefix + msg);
    }

    public void warn(String msg) {
        ravex.RaveX.LOGGER.warn(prefix + msg);
    }

    public void error(String msg) {
        ravex.RaveX.LOGGER.error(prefix + msg);
    }
}
