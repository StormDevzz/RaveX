package ravex.shaders;

public final class Vec3 {
    public float x, y, z;
    public Vec3() {}
    public Vec3(float x, float y, float z) { this.x = x; this.y = y; this.z = z; }

    public Vec3 add(Vec3 o) { return new Vec3(x + o.x, y + o.y, z + o.z); }
    public Vec3 sub(Vec3 o) { return new Vec3(x - o.x, y - o.y, z - o.z); }
    public Vec3 mul(float s) { return new Vec3(x * s, y * s, z * s); }
    public float dot(Vec3 o) { return x * o.x + y * o.y + z * o.z; }
    public float length() { return (float) Math.sqrt(x * x + y * y + z * z); }
    public Vec3 normalize() { float l = length(); return l > 0 ? new Vec3(x/l, y/l, z/l) : new Vec3(); }
    public Vec3 cross(Vec3 o) {
        return new Vec3(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x);
    }
    public Vec3 lerp(Vec3 o, float t) {
        return new Vec3(x + (o.x - x) * t, y + (o.y - y) * t, z + (o.z - z) * t);
    }
    public float dist(Vec3 o) { return sub(o).length(); }
}
