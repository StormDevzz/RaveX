package ravex.modules.movement;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.movement.MoveUtility;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.PlayerUtility;
import ravex.mcwrapper.MinecraftWrapper;
@Module(name = "TridentBoost", category = "Movement")
public class TridentBoost {
    @Parameter(name = "Mode", modes = {"Normal", "Always"})
    public String mode = "Normal";
    @Parameter(name = "Speed", min = 0.5, max = 3.0, step = 0.1)
    public double speed = 1.0;
    @Parameter(name = "Vertical", min = 0.0, max = 2.0, step = 0.1)
    public double vertical = 0.5;
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player == null) return;
        var main = InventoryUtility.getMainHand(player);
        if (!InventoryUtility.isTrident(main)) return;
        if (!InventoryUtility.hasEnchantment(main, "riptide")) return;
        if (!PlayerUtility.isUsingItem(player)) return;
        String m = mode;
        if (m.equals("Normal") && !player.isInWaterOrRain()) return;
        float yaw = player.getYRot() * ((float)Math.PI / 180F);
        float pitch = player.getXRot() * ((float)Math.PI / 180F);
        double mult = speed;
        double vert = vertical;
        double dx = -Math.sin(yaw) * Math.cos(pitch) * mult;
        double dy = -Math.sin(pitch) * vert;
        double dz = Math.cos(yaw) * Math.cos(pitch) * mult;
        MoveUtility.setMotion(new net.minecraft.world.phys.Vec3(dx, dy, dz));
        player.hurtMarked = true;
    }
}
