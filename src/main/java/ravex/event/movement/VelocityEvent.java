package ravex.event.movement;

import ravex.event.CancellableEvent;
import net.minecraft.world.phys.Vec3;

public class VelocityEvent extends CancellableEvent {
    private Vec3 velocity;
    private final Type type;

    public enum Type { KNOCKBACK, EXPLOSION, PUSH, FALLDAMAGE, ENTITY }

    public VelocityEvent(Type type, Vec3 velocity) {
        this.type = type;
        this.velocity = velocity;
    }

    public VelocityEvent(double x, double y, double z) {
        this(Type.KNOCKBACK, new Vec3(x, y, z));
    }

    public Type getType() { return type; }
    public Vec3 getVelocity() { return velocity; }
    public void setVelocity(Vec3 velocity) { this.velocity = velocity; }
    public double getX() { return velocity.x; }
    public double getY() { return velocity.y; }
    public double getZ() { return velocity.z; }
}
