package ravex.modules.movement;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.PhysicUtility;
import ravex.utility.network.NetworkUtility;
import ravex.utility.movement.MoveUtility;
import ravex.event.Subscribe;
import ravex.event.movement.VelocityEvent;
import java.util.List;
import java.util.Random;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;
@Module(name = "Velocity", category = "Movement")
public class Velocity {
    @Parameter(name = "Mode", modes = {"Cancel", "Matrix", "NCP", "UNCP", "Grim", "GrimStrict", "Verus"})
    public String mode = "Cancel";
    @Parameter(name = "Horizontal", min = 0.0, max = 1.0, step = 0.05)
    public double horizontal = 0.0;
    @Parameter(name = "Vertical", min = 0.0, max = 1.0, step = 0.05)
    public double vertical = 0.0;
    @Parameter(name = "Explosion")
    public boolean explosion = true;
    @Parameter(name = "GrimHorizontal", min = 0.0, max = 100.0, step = 1.0, visible = "mode=GrimStrict")
    public double grimHorizontal = 70.0;
    @Parameter(name = "GrimVertical", min = 0.0, max = 100.0, step = 1.0, visible = "mode=GrimStrict")
    public double grimVertical = 80.0;

    private final Random random = new Random();
    public int grimTickCounter = 0;
    public boolean grimVelocityActive = false;
    public int grimDelayTicks = 0;
    public net.minecraft.world.phys.Vec3 grimSavedVelocity = net.minecraft.world.phys.Vec3.ZERO;

    @Subscribe
    public void onVelocity(VelocityEvent event) {
        if (!Modules.enabled(Velocity.class)) return;
        String modeVal = mode;
        var cur = event.getVelocity();
        double h = horizontal;
        double v = vertical;

        switch (modeVal) {
            case "Cancel" -> event.setVelocity(net.minecraft.world.phys.Vec3.ZERO);
            case "Matrix" -> {
                double noise = (Math.random() - 0.5) * 0.015;
                event.setVelocity(new net.minecraft.world.phys.Vec3(cur.x * h + noise, cur.y * v, cur.z * h + noise));
            }
            case "NCP" -> event.setVelocity(new net.minecraft.world.phys.Vec3(cur.x * h, cur.y, cur.z * h));
            case "UNCP" -> {
                event.setVelocity(new net.minecraft.world.phys.Vec3(cur.x * h, cur.y * (v == 0.0 ? 0.0 : v), cur.z * h));
                var mc = MinecraftWrapper.getWrapper();
                var player = mc.getPlayer();
                if (player != null) {
                    double ox = (random.nextDouble() - 0.5) * 0.01;
                    double oz = (random.nextDouble() - 0.5) * 0.01;
                    NetworkUtility.sendMoveRelative(
                        player.getX() + ox, player.getY(), player.getZ() + oz,
                        mc.isPlayerOnGround(), mc.isPlayerHorizontalCollision()
                    );
                }
            }
            case "Grim" -> {
                event.setVelocity(new net.minecraft.world.phys.Vec3(cur.x * 0.1, 0.0, cur.z * 0.1));
                var mc = MinecraftWrapper.getWrapper();
                var player = mc.getPlayer();
                if (player != null) {
                    NetworkUtility.sendMoveRelative(
                        player.getX(), player.getY(), player.getZ(),
                        mc.isPlayerOnGround(), mc.isPlayerHorizontalCollision()
                    );
                }
            }
            case "GrimStrict" -> {
                grimSavedVelocity = cur;
                grimDelayTicks = 3;
                grimTickCounter = 0;
                grimVelocityActive = true;
            }
            case "Verus" -> {
                double verusH = h;
                double verusV = v;
                if (verusH == 0.0 && verusV == 0.0) {
                    verusH = 0.3;
                    verusV = 0.01;
                }
                double noiseX = (random.nextDouble() - 0.5) * 0.005;
                double noiseZ = (random.nextDouble() - 0.5) * 0.005;
                event.setVelocity(new net.minecraft.world.phys.Vec3(
                    cur.x * verusH + noiseX,
                    cur.y * verusV,
                    cur.z * verusH + noiseZ
                ));
            }
        }
    }
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player == null) return;
        if ("GrimStrict".equals(mode) && grimVelocityActive) {
            if (grimDelayTicks > 0) {
                grimDelayTicks--;
                if (grimDelayTicks == 0) {
                    double grimH = grimHorizontal / 100.0;
                    double grimV = grimVertical / 100.0;
                    MoveUtility.setMotion(grimSavedVelocity.x * (1.0 - grimH), grimSavedVelocity.y * (1.0 - grimV), grimSavedVelocity.z * (1.0 - grimH));
                }
            }
            grimTickCounter++;
            if (grimTickCounter % 2 == 0) {
                double ox = (random.nextDouble() - 0.5) * 0.011 + 0.001;
                double oz = (random.nextDouble() - 0.5) * 0.011 + 0.001;
                NetworkUtility.sendMoveRelative(
                    player.getX() + ox, player.getY(), player.getZ() + oz,
                    mc.isPlayerOnGround(), mc.isPlayerHorizontalCollision()
                );
            }
        }
    }
}
