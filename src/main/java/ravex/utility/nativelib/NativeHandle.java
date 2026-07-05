package ravex.utility.nativelib;

import java.util.function.LongConsumer;

public class NativeHandle implements AutoCloseable {
    private long ptr;
    private final LongConsumer disposer;

    public NativeHandle(long ptr, LongConsumer disposer) {
        this.ptr = ptr;
        this.disposer = disposer;
    }

    public long get() {
        if (ptr == 0) throw new NativeException("Native handle is closed or null");
        return ptr;
    }

    public boolean isValid() {
        return ptr != 0;
    }

    public void close() {
        if (ptr != 0) {
            disposer.accept(ptr);
            ptr = 0;
        }
    }
}
