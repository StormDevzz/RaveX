package ravex.shaders;

public final class Matrix4x4 {
    public final float[] m = new float[16];

    public Matrix4x4() {}

    public static Matrix4x4 identity() {
        Matrix4x4 r = new Matrix4x4();
        r.m[0] = r.m[5] = r.m[10] = r.m[15] = 1;
        return r;
    }

    public static Matrix4x4 translate(float x, float y, float z) {
        Matrix4x4 r = identity();
        r.m[12] = x; r.m[13] = y; r.m[14] = z;
        return r;
    }

    public static Matrix4x4 rotate(float angle, float ax, float ay, float az) {
        float c = (float) Math.cos(angle), s = (float) Math.sin(angle), t = 1 - c;
        float l = (float) Math.sqrt(ax * ax + ay * ay + az * az);
        if (l == 0) return identity();
        float x = ax / l, y = ay / l, z = az / l;
        Matrix4x4 r = new Matrix4x4();
        r.m[0] = t * x * x + c;     r.m[1] = t * x * y - s * z;   r.m[2] = t * x * z + s * y;
        r.m[4] = t * x * y + s * z; r.m[5] = t * y * y + c;       r.m[6] = t * y * z - s * x;
        r.m[8] = t * x * z - s * y; r.m[9] = t * y * z + s * x;   r.m[10] = t * z * z + c;
        r.m[15] = 1;
        return r;
    }

    public static Matrix4x4 scale(float x, float y, float z) {
        Matrix4x4 r = identity();
        r.m[0] = x; r.m[5] = y; r.m[10] = z;
        return r;
    }

    public Matrix4x4 mul(Matrix4x4 r) {
        Matrix4x4 res = new Matrix4x4();
        for (int i = 0; i < 4; i++)
            for (int j = 0; j < 4; j++)
                for (int k = 0; k < 4; k++)
                    res.m[i * 4 + j] += m[i * 4 + k] * r.m[k * 4 + j];
        return res;
    }

    public Vec4 transform(Vec4 v) {
        return new Vec4(
            m[0] * v.x + m[1] * v.y + m[2] * v.z + m[3] * v.w,
            m[4] * v.x + m[5] * v.y + m[6] * v.z + m[7] * v.w,
            m[8] * v.x + m[9] * v.y + m[10] * v.z + m[11] * v.w,
            m[12] * v.x + m[13] * v.y + m[14] * v.z + m[15] * v.w
        );
    }
}
