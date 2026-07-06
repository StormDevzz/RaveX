package ravex.modules.world;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;
import ravex.parameter.ModeParameter;
import ravex.utility.player.InventoryUtility;
import ravex.utility.misc.MobUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import java.util.List;
public class AutoShear extends Module {
    public static final AutoShear INSTANCE = new AutoShear();
    public final BooleanParameter silent = new BooleanParameter("SilentSwap", true);
    public final NumberParameter range = new NumberParameter("Range", 4.5, 3.0, 6.0, 0.1);
    public final ModeParameter exploitType = new ModeParameter("ExploitType", "Packet", List.of("Client", "Packet"));

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        var p = mc.player;
        if (p == null || mc.level == null || mc.gameMode == null) return;
        var target = (net.minecraft.world.entity.animal.sheep.Sheep) null;
        double closestDist = range.getValue();
        for (var entity : mc.level.entitiesForRendering()) {
            if (entity instanceof net.minecraft.world.entity.animal.sheep.Sheep sheep && MobUtility.isShearable(sheep)) {
                double dist = p.distanceTo(sheep);
                if (dist < closestDist) {
                    closestDist = dist;
                    target = sheep;
                }
            }
        }
        if (target == null) return;
        int shearSlot = -1;
        for (int i = 0; i < 9; i++) {
            if (InventoryUtility.isItemInSlot(p, i, "shears")) {
                shearSlot = i;
                break;
            }
        }
        if (shearSlot == -1) return;
        int prevSlot = InventoryUtility.getSelectedSlot(p);
        if ("Packet".equals(exploitType.getValue())) {
            if (shearSlot != prevSlot) {
                p.connection.send(new ServerboundSetCarriedItemPacket(shearSlot));
            }
            p.connection.send(ServerboundInteractPacket.createInteractionPacket(target, p.isShiftKeyDown(), net.minecraft.world.InteractionHand.MAIN_HAND));
            p.connection.send(new ServerboundSwingPacket(net.minecraft.world.InteractionHand.MAIN_HAND));
            if (silent.getValue() && shearSlot != prevSlot) {
                p.connection.send(new ServerboundSetCarriedItemPacket(prevSlot));
            }
        } else {
            InventoryUtility.selectSlot(p, shearSlot);
            mc.gameMode.interact(p, target, net.minecraft.world.InteractionHand.MAIN_HAND);
            ravex.utility.player.SwingUtility.swingMainHand(p);
            if (silent.getValue() && shearSlot != prevSlot) {
                InventoryUtility.selectSlot(p, prevSlot);
            }
        }
    }
}
