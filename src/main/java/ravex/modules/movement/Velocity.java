package ravex.modules.movement;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import ravex.event.Subscribe;
import ravex.event.movement.VelocityEvent;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import java.util.List;
import java.util.Random;
public class Velocity extends Module {
    public final ModeParameter mode       = new ModeParameter("Mode", "Cancel",
            List.of("Cancel", "Matrix", "NCP", "Grim", "GrimStrict"));
    public final NumberParameter horizontal = new NumberParameter("Horizontal", 0.0, 0.0, 1.0, 0.05);
    public final NumberParameter vertical   = new NumberParameter("Vertical",   0.0, 0.0, 1.0, 0.05);
    public final BooleanParameter explosion = new BooleanParameter("Explosion", true);
    public final NumberParameter grimHorizontal = new NumberParameter("GrimHorizontal", 70.0, 0.0, 100.0, 1.0);
    public final NumberParameter grimVertical = new NumberParameter("GrimVertical", 80.0, 0.0, 100.0, 1.0);

    private final Random random = new Random();
    public int grimTickCounter = 0;
    public boolean grimVelocityActive = false;
    public int grimDelayTicks = 0;
    public Vec3 grimSavedVelocity = Vec3.ZERO;

    private Velocity() {
        super("Velocity");
        horizontal.setVisible(() -> !mode.getValue().equals("Cancel"));
        vertical.setVisible(() -> !mode.getValue().equals("Cancel"));
        grimHorizontal.setVisible(() -> "GrimStrict".equals(mode.getValue()));
        grimVertical.setVisible(() -> "GrimStrict".equals(mode.getValue()));
    }

    @Subscribe
    public void onVelocity(VelocityEvent event) {
        if (!getEnabled()) return;
        String modeVal = mode.getValue();
        Vec3 cur = event.getVelocity();
        double h = horizontal.getValue();
        double v = vertical.getValue();

        switch (modeVal) {
            case "Cancel" -> event.setVelocity(Vec3.ZERO);
            case "Matrix" -> {
                double noise = (Math.random() - 0.5) * 0.015;
                event.setVelocity(new Vec3(cur.x * h + noise, cur.y * v, cur.z * h + noise));
            }
            case "NCP" -> event.setVelocity(new Vec3(cur.x * h, cur.y, cur.z * h));
            case "Grim" -> {
                event.setVelocity(new Vec3(cur.x * 0.1, 0.0, cur.z * 0.1));
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(
                        mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                        mc.player.onGround(), mc.player.horizontalCollision
                    ));
                }
            }
            case "GrimStrict" -> {
                grimSavedVelocity = cur;
                grimDelayTicks = 3;
                grimTickCounter = 0;
                grimVelocityActive = true;
            }
        }
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if ("GrimStrict".equals(mode.getValue()) && grimVelocityActive) {
            if (grimDelayTicks > 0) {
                grimDelayTicks--;
                if (grimDelayTicks == 0) {
                    double grimH = grimHorizontal.getValue() / 100.0;
                    double grimV = grimVertical.getValue() / 100.0;
                    mc.player.setDeltaMovement(grimSavedVelocity.x * (1.0 - grimH), grimSavedVelocity.y * (1.0 - grimV), grimSavedVelocity.z * (1.0 - grimH));
                }
            }
            grimTickCounter++;
            if (grimTickCounter % 2 == 0) {
                double ox = (random.nextDouble() - 0.5) * 0.011 + 0.001;
                double oz = (random.nextDouble() - 0.5) * 0.011 + 0.001;
                mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(
                    mc.player.getX() + ox, mc.player.getY(), mc.player.getZ() + oz,
                    mc.player.onGround(), mc.player.horizontalCollision
                ));
            }
        }
    }
    public static boolean maybeEnabled() {
        return maybeEnabled(Velocity.class);
    }
    public static Velocity itz() {
        return ModuleManager.get(Velocity.class);
    }
}
