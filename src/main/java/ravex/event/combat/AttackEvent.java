package ravex.event.combat;

import ravex.event.CancellableEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class AttackEvent extends CancellableEvent {
    private final Entity target;
    private final AttackType type;
    private float damage;

    public enum AttackType { PRE, POST, MISS }

    public AttackEvent(AttackType type, Entity target, float damage) {
        this.type = type;
        this.target = target;
        this.damage = damage;
    }

    public AttackEvent(Entity attacker, Entity target) {
        this(AttackType.PRE, target, 0.0f);
    }

    public AttackType getType() { return type; }
    public Entity getTarget() { return target; }
    public float getDamage() { return damage; }
    public void setDamage(float damage) { this.damage = damage; }
}
