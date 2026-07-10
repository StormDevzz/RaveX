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


        if (p.getMainHandItem().is(Items.MACE) && mc.options.keyAttack.isDown()) {
            if (mc.hitResult instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity target) {
                if (!target.isAlive() || target == p) return;

                double h = height.getValue();
                double x = p.getX();
                double y = p.getY();
                double z = p.getZ();



                double step = 0.25;
                int loops = (int) Math.ceil(h / step);

                for (int i = 0; i < loops; i++) {

                    p.connection.send(new ServerboundMovePlayerPacket.Pos(x, y + 0.001, z, false, p.horizontalCollision));

                    p.connection.send(new ServerboundMovePlayerPacket.Pos(x, y - step, z, false, p.horizontalCollision));

                    p.connection.send(new ServerboundMovePlayerPacket.Pos(x, y, z, false, p.horizontalCollision));
                }


                p.connection.send(ServerboundInteractPacket.createAttackPacket(target, p.isShiftKeyDown()));
                p.swing(InteractionHand.MAIN_HAND);


                p.connection.send(new ServerboundMovePlayerPacket.Pos(x, y, z, true, p.horizontalCollision));


                p.fallDistance = (float) h;
            }
        }
    }
}
