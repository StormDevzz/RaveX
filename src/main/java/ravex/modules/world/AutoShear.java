package ravex.modules.world;
<<<<<<< HEAD
import ravex.manager.ModuleManager;
=======
import ravex.modules.Category;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
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
<<<<<<< HEAD
=======
    public static final AutoShear INSTANCE = new AutoShear();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    public final BooleanParameter silent = new BooleanParameter("SilentSwap", true);
    public final NumberParameter range = new NumberParameter("Range", 4.5, 3.0, 6.0, 0.1);
    public final ModeParameter exploitType = new ModeParameter("ExploitType", "Packet", List.of("Client", "Packet"));

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        var p = mc.player;
        if (p == null || mc.level == null || mc.gameMode == null) return;
<<<<<<< HEAD
        var target = (net.minecraft.world.entity.animal.sheep.Sheep) null;
=======
        Sheep target = null;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
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
<<<<<<< HEAD
        if (shearSlot == -1) return;
        int prevSlot = InventoryUtility.getSelectedSlot(p);
=======
        if (shearSlot == -1) return; 
        int prevSlot = p.getInventory().getSelectedSlot();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        if ("Packet".equals(exploitType.getValue())) {
            if (shearSlot != prevSlot) {
                p.connection.send(new ServerboundSetCarriedItemPacket(shearSlot));
            }
<<<<<<< HEAD
            p.connection.send(ServerboundInteractPacket.createInteractionPacket(target, p.isShiftKeyDown(), net.minecraft.world.InteractionHand.MAIN_HAND));
            p.connection.send(new ServerboundSwingPacket(net.minecraft.world.InteractionHand.MAIN_HAND));
=======
            p.connection.send(ServerboundInteractPacket.createInteractionPacket(target, p.isShiftKeyDown(), InteractionHand.MAIN_HAND));
            p.connection.send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            if (silent.getValue() && shearSlot != prevSlot) {
                p.connection.send(new ServerboundSetCarriedItemPacket(prevSlot));
            }
        } else {
<<<<<<< HEAD
            InventoryUtility.selectSlot(p, shearSlot);
            mc.gameMode.interact(p, target, net.minecraft.world.InteractionHand.MAIN_HAND);
            ravex.utility.player.SwingUtility.swingMainHand(p);
=======
            p.getInventory().setSelectedSlot(shearSlot);
            mc.gameMode.interact(p, target, InteractionHand.MAIN_HAND);
            p.swing(InteractionHand.MAIN_HAND);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            if (silent.getValue() && shearSlot != prevSlot) {
                InventoryUtility.selectSlot(p, prevSlot);
            }
        }
    }
    public static AutoShear itz() {
        return ModuleManager.get(AutoShear.class);
    }
}
