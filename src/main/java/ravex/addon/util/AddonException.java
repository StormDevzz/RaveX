package ravex.addon.util;

public class AddonException extends Exception {
    public AddonException(String message) {
        super(message);
    }
    public AddonException(String message, Throwable cause) {
        super(message, cause);
    }
}
