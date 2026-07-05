package ravex.modules.world;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;
import ravex.parameter.ModeParameter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionHand;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import java.util.List;
public class AutoShear extends Module {
    public static final AutoShear INSTANCE = new AutoShear();
    public final BooleanParameter silent = new BooleanParameter("Silent Swap", true);
    public final NumberParameter range = new NumberParameter("Range", 4.5, 3.0, 6.0, 0.1);
    public final ModeParameter exploitType = new ModeParameter("Exploit Type", "Packet", List.of("Client", "Packet"));

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null || mc.gameMode == null) return;
        Sheep target = null;
        double closestDist = range.getValue();
        for (var entity : mc.level.entitiesForRendering()) {
            if (entity instanceof Sheep sheep) {
                if (sheep.isAlive() && !sheep.isBaby() && !sheep.isSheared()) {
                    double dist = p.distanceTo(sheep);
                    if (dist < closestDist) {
                        closestDist = dist;
                        target = sheep;
                    }
                }
            }
        }
        if (target == null) return;
        int shearSlot = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = p.getInventory().getItem(i);
            if (stack.is(Items.SHEARS)) {
                shearSlot = i;
                break;
            }
        }
        if (shearSlot == -1) return; 
        int prevSlot = p.getInventory().getSelectedSlot();
        if ("Packet".equals(exploitType.getValue())) {
            if (shearSlot != prevSlot) {
                p.connection.send(new ServerboundSetCarriedItemPacket(shearSlot));
            }
            p.connection.send(ServerboundInteractPacket.createInteractionPacket(target, p.isShiftKeyDown(), InteractionHand.MAIN_HAND));
            p.connection.send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
            if (silent.getValue() && shearSlot != prevSlot) {
                p.connection.send(new ServerboundSetCarriedItemPacket(prevSlot));
            }
        } else {
            p.getInventory().setSelectedSlot(shearSlot);
            mc.gameMode.interact(p, target, InteractionHand.MAIN_HAND);
            p.swing(InteractionHand.MAIN_HAND);
            if (silent.getValue() && shearSlot != prevSlot) {
                p.getInventory().setSelectedSlot(prevSlot);
            }
        }
    }
}
