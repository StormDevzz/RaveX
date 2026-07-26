package ravex.modules.world;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.MobUtility;
import ravex.utility.player.InventoryUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import java.util.List;



@ModuleInfo(name = "AutoShear", category = "World")
public class AutoShear implements ModuleAccess {
    @Parameter(name = "SilentSwap")
    public boolean silent = true;
    @Parameter(name = "Range", min = 3.0, max = 6.0, step = 0.1)
    public double range = 4.5;
    @Parameter(name = "ExploitType", modes = {"Client", "Packet"})
    public String exploitType = "Packet";
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        var p = mc.player;
        if (p == null || mc.level == null || mc.gameMode == null) return;
        var target = (net.minecraft.world.entity.animal.sheep.Sheep) null;
        double closestDist = range;
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
        if ("Packet".equals(exploitType)) {
            if (shearSlot != prevSlot) {
                p.connection.send(new ServerboundSetCarriedItemPacket(shearSlot));
            }
            p.connection.send(net.minecraft.network.protocol.game.ServerboundInteractPacket.createInteractionPacket(target, p.isShiftKeyDown(), net.minecraft.world.InteractionHand.MAIN_HAND));
            p.connection.send(new ServerboundSwingPacket(net.minecraft.world.InteractionHand.MAIN_HAND));
            if (silent && shearSlot != prevSlot) {
                p.connection.send(new ServerboundSetCarriedItemPacket(prevSlot));
            }
        } else {
            InventoryUtility.selectSlot(p, shearSlot);
            mc.gameMode.interact(p, target, net.minecraft.world.InteractionHand.MAIN_HAND);
            ravex.utility.player.SwingUtility.swingMainHand(p);
            if (silent && shearSlot != prevSlot) {
                InventoryUtility.selectSlot(p, prevSlot);
            }
        }
    }
    public static AutoShear itz() {
        return ravex.manager.ModuleManager.delegate(AutoShear.class);
    }


}