package ravex.modules.combat;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.NumberParameter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.world.InteractionHand;

public class MaceAura extends Module {
    public static final MaceAura INSTANCE = new MaceAura();

    public final NumberParameter height = new NumberParameter("Height", 10.0, 2.0, 40.0, 1.0);

    private MaceAura() {
        super("MaceAura", Category.COMBAT);
        addParameter(height);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) return;

        // Trigger only when holding a Mace and pressing attack button
        if (p.getMainHandItem().is(Items.MACE) && mc.options.keyAttack.isDown()) {
            if (mc.hitResult instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity target) {
                if (!target.isAlive() || target == p) return;

                double h = height.getValue();
                double x = p.getX();
                double y = p.getY();
                double z = p.getZ();

                // Genuine server-side fall distance spoofing using loops of micro-movements.
                // This builds up massive fallDistance on the server without triggering anti-cheat speed/flight limits.
                double step = 0.25;
                int loops = (int) Math.ceil(h / step);

                for (int i = 0; i < loops; i++) {
                    // 1. Send micro-packet upwards
                    p.connection.send(new ServerboundMovePlayerPacket.Pos(x, y + 0.001, z, false, p.horizontalCollision));
                    // 2. Send micro-packet downwards to accumulate fallDistance
                    p.connection.send(new ServerboundMovePlayerPacket.Pos(x, y - step, z, false, p.horizontalCollision));
                    // 3. Return cleanly to the original coordinate
                    p.connection.send(new ServerboundMovePlayerPacket.Pos(x, y, z, false, p.horizontalCollision));
                }

                // Deliver critical attack packet at peak accumulated spoofed fallDistance
                p.connection.send(ServerboundInteractPacket.createAttackPacket(target, p.isShiftKeyDown()));
                p.swing(InteractionHand.MAIN_HAND);

                // Finalize on-ground packet to cleanly resolve state and apply critical smash damage
                p.connection.send(new ServerboundMovePlayerPacket.Pos(x, y, z, true, p.horizontalCollision));

                // Visual smash client effect
                p.fallDistance = (float) h;
            }
        }
    }
}
