package ravex.utility.shaders.nativec;

import ravex.utility.nativelib.NativeLibrary;

public final class ShaderNative {
    private static boolean available = false;

    public static boolean isAvailable() { return available; }


    public static native float nVec3Length(float x, float y, float z);
    public static native float nVec3Dot(float x1, float y1, float z1, float x2, float y2, float z2);
    public static native void nVec3Normalize(float[] v);
    public static native void nMatrixMul(float[] a, float[] b, float[] out);
    public static native void nMatrixTransform(float[] m, float x, float y, float z, float w, float[] out);


    public static native float nPerlinNoise(float x, float y, float z);
    public static native float nFbmNoise(float x, float y, float z, int octaves, float lacunarity, float gain);
    public static native float nSimplexNoise(float x, float y, float z);
    public static native float nValueNoise2D(float x, float y);
    public static native float nCellularNoise(float x, float y, float z);


    public static native void nHsbToRgb(float h, float s, float v, float[] out);
    public static native void nRgbToHsb(float r, float g, float b, float[] out);
    public static native int nBlendColors(int c1, int c2, float t);


    public static float vec3Length(float x, float y, float z) {
        if (available) { try { return nVec3Length(x, y, z); } catch (UnsatisfiedLinkError e) { available = false; } }
        return (float) Math.sqrt(x * x + y * y + z * z);
    }

    public static float vec3Dot(float x1, float y1, float z1, float x2, float y2, float z2) {
        if (available) { try { return nVec3Dot(x1, y1, z1, x2, y2, z2); } catch (UnsatisfiedLinkError e) { available = false; } }
        return x1 * x2 + y1 * y2 + z1 * z2;
    }

    public static float perlinNoise(float x, float y, float z) {
        if (available) { try { return nPerlinNoise(x, y, z); } catch (UnsatisfiedLinkError e) { available = false; } }
        return noise3D(x, y, z);
    }

    public static float fbmNoise(float x, float y, float z, int octaves, float lacunarity, float gain) {
        if (available) { try { return nFbmNoise(x, y, z, octaves, lacunarity, gain); } catch (UnsatisfiedLinkError e) { available = false; } }
        float value = 0, amplitude = 0.5f, frequency = 1;
        for (int i = 0; i < octaves; i++) {
            value += amplitude * noise3D(x * frequency, y * frequency, z * frequency);
            frequency *= lacunarity;
            amplitude *= gain;
        }
        return value;
    }

    public static float valueNoise2D(float x, float y) {
        if (available) { try { return nValueNoise2D(x, y); } catch (UnsatisfiedLinkError e) { available = false; } }
        return noise2D(x, y);
    }

    public static int blendColors(int c1, int c2, float t) {
        if (available) { try { return nBlendColors(c1, c2, t); } catch (UnsatisfiedLinkError e) { available = false; } }
        int a1 = (c1 >> 24) & 0xFF, r1 = (c1 >> 16) & 0xFF, g1 = (c1 >> 8) & 0xFF, b1 = c1 & 0xFF;
        int a2 = (c2 >> 24) & 0xFF, r2 = (c2 >> 16) & 0xFF, g2 = (c2 >> 8) & 0xFF, b2 = c2 & 0xFF;
        int a = (int)(a1 + (a2 - a1) * t);
        int r = (int)(r1 + (r2 - r1) * t);
        int g = (int)(g1 + (g2 - g1) * t);
        int b = (int)(b1 + (b2 - b1) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }


    private static float hash(int a, int b) {
        int n = a * 157 + b * 113 + 137;
        return ((n << 13) ^ n) & 0xff;
    }

    private static float noise2D(float x, float y) {
        int ix = (int) Math.floor(x);
        int iy = (int) Math.floor(y);
        float fx = x - ix, fy = y - iy;
        fx = fx * fx * (3 - 2 * fx);
        fy = fy * fy * (3 - 2 * fy);
        float v00 = hash(ix, iy) / 255f;
        float v10 = hash(ix + 1, iy) / 255f;
        float v01 = hash(ix, iy + 1) / 255f;
        float v11 = hash(ix + 1, iy + 1) / 255f;
        return v00 + (v10 - v00) * fx + (v01 - v00) * fy + (v00 - v10 - v01 + v11) * fx * fy;
    }

    private static float noise3D(float x, float y, float z) {
        return noise2D(x + y * 3.7f, y + z * 5.1f + x * 1.3f);
    }
}
