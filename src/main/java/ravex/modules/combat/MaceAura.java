package ravex.modules.combat;

import ravex.modules.annotations.ModuleInfo;
import ravex.parameter.NumberParameter;
import ravex.utility.player.InventoryUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import net.minecraft.world.phys.EntityHitResult;
import ravex.utility.misc.MobUtility;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import ravex.utility.player.SwingUtility;
@ModuleInfo(name = "MaceAura", category = "Combat")
public class MaceAura extends ravex.modules.Module {
public final NumberParameter height = new NumberParameter("Height", 10.0, 2.0, 40.0, 1.0);
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) return;
        if (InventoryUtility.isItem(p.getMainHandItem(), "mace") && mc.options.keyAttack.isDown()) {
            if (mc.hitResult instanceof EntityHitResult hit) {
                net.minecraft.world.entity.LivingEntity target = MobUtility.asLivingEntity(hit.getEntity());
                if (target == null) return;
                if (MobUtility.isDead(target) || MobUtility.isSelf(target)) return;
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
                p.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                p.connection.send(new ServerboundMovePlayerPacket.Pos(x, y, z, true, p.horizontalCollision));
                p.fallDistance = (float) h;
            }
        }
    }
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("MaceAura").getEnabled();
    }
    public static MaceAura itz() {
        return ravex.manager.ModuleManager.delegate(MaceAura.class);
    }

    public java.util.List<ravex.parameter.Parameter<?>> getParameters() {
        java.util.List<ravex.parameter.Parameter<?>> list = new java.util.ArrayList<>();
        for (java.lang.reflect.Field field : getClass().getDeclaredFields()) {
            if (ravex.parameter.Parameter.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    list.add((ravex.parameter.Parameter<?>) field.get(this));
                } catch (Exception ignored) {}
            }
        }
        return list;
    }
}