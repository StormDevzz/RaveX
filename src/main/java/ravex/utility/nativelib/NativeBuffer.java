package ravex.utility.nativelib;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

public class NativeBuffer {
    public static ByteBuffer alloc(int capacity) {
        return ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder());
    }

    public static FloatBuffer allocFloat(int count) {
        return alloc(count * 4).asFloatBuffer();
    }

    public static IntBuffer allocInt(int count) {
        return alloc(count * 4).asIntBuffer();
    }

    public static LongBuffer allocLong(int count) {
        return alloc(count * 8).asLongBuffer();
    }

    public static ByteBuffer allocFloatArray(float... values) {
        ByteBuffer buf = alloc(values.length * 4);
        FloatBuffer fb = buf.asFloatBuffer();
        fb.put(values);
        return buf;
    }

    public static ByteBuffer allocIntArray(int... values) {
        ByteBuffer buf = alloc(values.length * 4);
        IntBuffer ib = buf.asIntBuffer();
        ib.put(values);
        return buf;
    }

    public static float[] readFloats(ByteBuffer buf, int count) {
        float[] result = new float[count];
        buf.asFloatBuffer().get(result);
        return result;
    }

    public static int[] readInts(ByteBuffer buf, int count) {
        int[] result = new int[count];
        buf.asIntBuffer().get(result);
        return result;
    }
}
