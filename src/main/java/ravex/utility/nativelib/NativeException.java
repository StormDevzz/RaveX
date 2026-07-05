package ravex.utility.nativelib;

public class NativeException extends RuntimeException {
    public NativeException(String message) {
        super(message);
    }

    public NativeException(String message, Throwable cause) {
        super(message, cause);
    }
}
