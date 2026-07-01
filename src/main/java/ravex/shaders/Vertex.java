package ravex.shaders;

public final class Vertex {
    public Vec3 position;
    public Vec3 normal;
    public Vec2 uv;
    public ColorRGBA color;

    public Vertex() {
        position = new Vec3();
        normal = new Vec3();
        uv = new Vec2();
        color = new ColorRGBA(1, 1, 1, 1);
    }

    public Vertex(Vec3 pos, Vec3 norm, Vec2 uv, ColorRGBA col) {
        this.position = pos;
        this.normal = norm;
        this.uv = uv;
        this.color = col;
    }
}
