package ravex.modules.world;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.EntityUtility;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.PlayerUtility;
import ravex.utility.network.NetworkUtility;
import ravex.mcwrapper.MinecraftWrapper;

@Module(name = "AutoShear", category = "World")
public class AutoShear {
    @Parameter(name = "SilentSwap")
    public boolean silent = true;
    @Parameter(name = "Range", min = 3.0, max = 6.0, step = 0.1)
    public double range = 4.5;
    @Parameter(name = "ExploitType", modes = {"Client", "Packet"})
    public String exploitType = "Packet";
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        var p = mc.getPlayer();
        if (p == null || mc.getLevel() == null || mc.getGameMode() == null) return;
        var target = (net.minecraft.world.entity.animal.sheep.Sheep) null;
        double closestDist = range;
        for (var entity : mc.getLevel().entitiesForRendering()) {
            if (entity instanceof net.minecraft.world.entity.animal.sheep.Sheep sheep && EntityUtility.isShearable(sheep)) {
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
                NetworkUtility.sendSetCarriedItem(shearSlot);
            }
            NetworkUtility.sendInteract(target, PlayerUtility.isSneaking(p), net.minecraft.world.InteractionHand.MAIN_HAND);
            NetworkUtility.sendSwing(net.minecraft.world.InteractionHand.MAIN_HAND);
            if (silent && shearSlot != prevSlot) {
                NetworkUtility.sendSetCarriedItem(prevSlot);
            }
        } else {
            InventoryUtility.selectSlot(p, shearSlot);
            var gm = mc.getGameMode();
            if (gm != null) gm.interact(p, target, net.minecraft.world.InteractionHand.MAIN_HAND);
            ravex.utility.player.SwingUtility.swingMainHand(p);
            if (silent && shearSlot != prevSlot) {
                InventoryUtility.selectSlot(p, prevSlot);
            }
        }
    }
}
