package ravex.utility.shaders.nativec;

// jni bridge to the native shader engine
public final class ShaderNative {
    private ShaderNative() {}

    // runs one vertex through the engine
    public static native void nFxVertex(int effect, float time, int color, int alpha,
        float x, float y, float z, float nx, float ny, float nz, float u, float v, float[] out);

    // flat tint for whole models, returns the packed argb
    public static native int nFxTint(int effect, float time, int color, int alpha,
        float x, float y, float z);
}
