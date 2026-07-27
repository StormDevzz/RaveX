package ravex.modules.movement;
import ravex.modules.ModuleAccess;
import ravex.event.EventBusHolder;
import ravex.event.network.PacketEvent;
import ravex.event.Subscribe;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.EntityUtility;
import ravex.utility.misc.PhysicUtility;
import ravex.utility.player.SwingUtility;
import net.minecraft.world.entity.vehicle.boat.Boat;
import java.util.ArrayList;
import java.util.List;
import ravex.mcwrapper.MinecraftWrapper;





@ModuleInfo(name = "BoatFly", category = "Movement")
public class BoatFly implements ModuleAccess {
    @Parameter(name = "Mode", modes = {"Packet", "PacketStrict", "Motion"})
    public String mode = "Packet";
    @Parameter(name = "Speed", min = 0.1, max = 25.0, step = 0.1)
    public double speed = 2.0;
    @Parameter(name = "YSpeed", min = 0.0, max = 10.0, step = 0.1)
    public double ySpeed = 1.0;
    @Parameter(name = "BoatScale", min = 0.05, max = 1.0, step = 0.05)
    public double boatScale = 0.1;
    @Parameter(name = "AutoMount")
    public boolean autoMount = true;
    @Parameter(name = "Gravity")
    public boolean gravity = false;
    @Parameter(name = "Phase")
    public boolean phase = false;
    @Parameter(name = "CancelPackets")
    public boolean cancelPackets = true;
    @Parameter(name = "AllowShift")
    public boolean allowShift = true;

    private final ArrayList<net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket> vehiclePackets = new ArrayList<>();
    private float currentScale = 1.0f;

    public static boolean isBoatScaleActive() {
        return ravex.manager.ModuleManager.delegate(BoatFly.class).getEnabled();
    }

    public float getScale() {
        return currentScale;
    }

    private void updateScale() {
        float target = (float) boatScale;
        float speed = 0.15f;
        if (currentScale > target) {
            currentScale = Math.max(target, currentScale - speed);
        } else if (currentScale < target) {
            currentScale = Math.min(target, currentScale + speed);
        }
    }
    public void onEnable() {
        vehiclePackets.clear();
        currentScale = 1.0f;
        EventBusHolder.get().subscribe(this);
        if (autoMount) mountToNearestBoat();
    }
    public void onDisable() {
        vehiclePackets.clear();
        currentScale = 1.0f;
        EventBusHolder.get().unsubscribe(this);
        var mc = MinecraftWrapper.getInstance();
        if (mc.player != null) {
            mc.player.setNoGravity(false);
            net.minecraft.world.entity.Entity vehicle = mc.player.getVehicle();
            if (vehicle != null) {
                vehicle.setNoGravity(false);
            }
        }
    }

    @Subscribe
    public void onPacket(PacketEvent event) {
        var mc = MinecraftWrapper.getInstance();
        if (mc.player == null || mc.player.getVehicle() == null) return;
        if (!(mc.player.getVehicle() instanceof Boat)) return;

        if (event.isSend()) {
            var packet = event.getPacket();

            if (packet instanceof net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket pac && mode.equals("Packet")) {
                if (vehiclePackets.contains(pac)) {
                    vehiclePackets.remove(pac);
                } else {
                    event.setCancelled(true);
                }
            }

            if (allowShift && packet instanceof net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket) {
                event.setCancelled(true);
            }
        }

        if (event.isReceive() && cancelPackets) {
            var packet = event.getPacket();
            if (packet instanceof net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket
                    || packet instanceof net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket) {
                event.setCancelled(true);
            }
        }
    }
    public void onTick() {
        var mc = MinecraftWrapper.getInstance();
        if (mc.player == null) return;

        updateScale();

        net.minecraft.world.entity.Entity vehicle = mc.player.getVehicle();
        if (vehicle == null || !(vehicle instanceof Boat)) {
            if (autoMount) mountToNearestBoat();
            return;
        }

        if (phase) {
            vehicle.noPhysics = true;
            mc.player.noPhysics = true;
        }

        vehicle.setNoGravity(!gravity);
        mc.player.setNoGravity(!gravity);

        vehicle.setYRot(mc.player.getYRot());

        double[] motion = forward(speed);
        double px = vehicle.getX() + motion[0];
        double pz = vehicle.getZ() + motion[1];
        double py = vehicle.getY();

        if (mc.options.keyJump.isDown()) {
            py += ySpeed;
        } else if (mc.options.keyShift.isDown()) {
            py -= ySpeed;
        }

        String currentMode = mode;

        if (currentMode.equals("Motion")) {
            net.minecraft.world.phys.Vec3 vel = vehicle.getDeltaMovement();
            double vy = vel.y + (mc.options.keyJump.isDown() ? ySpeed : (mc.options.keyShift.isDown() ? -ySpeed : 0));
            vehicle.setDeltaMovement(motion[0], vy, motion[1]);
        } else {
            vehicle.setPos(px, py, pz);
            net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket packet = net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket.fromEntity(vehicle);
            vehiclePackets.add(packet);
            mc.player.connection.send(packet);

            if (currentMode.equals("PacketStrict")) {
                mc.player.connection.send(net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket.fromEntity(vehicle));
            }
        }
    }

    private void mountToNearestBoat() {
        var mc = MinecraftWrapper.getInstance();
        if (mc.player == null || mc.level == null) return;
        for (net.minecraft.world.entity.Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof Boat && mc.player.distanceTo(entity) < 5.0) {
                mc.gameMode.interact(mc.player, entity, net.minecraft.world.InteractionHand.MAIN_HAND);
                break;
            }
        }
    }

    private double[] forward(double speed) {
        var mc = MinecraftWrapper.getInstance();
        if (mc.player == null) return new double[]{0, 0};

        float forward = 0;
        float strafe = 0;
        if (mc.options.keyUp.isDown()) forward++;
        if (mc.options.keyDown.isDown()) forward--;
        if (mc.options.keyLeft.isDown()) strafe++;
        if (mc.options.keyRight.isDown()) strafe--;

        float yaw = mc.player.getYRot();

        if (forward != 0) {
            if (strafe > 0) {
                forward += (forward > 0 ? -45 : 45);
            } else if (strafe < 0) {
                forward += (forward > 0 ? 45 : -45);
            }
            strafe = 0;
            if (forward > 1) forward = 1;
            else if (forward < -1) forward = -1;
        }

        double rad = Math.toRadians(yaw + 90.0f);
        double sin = Math.sin(rad);
        double cos = Math.cos(rad);

        double x = forward * speed * cos + strafe * speed * sin;
        double z = forward * speed * sin - strafe * speed * cos;

        return new double[]{x, z};
    }

    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("BoatFly").getEnabled();
    }

    public static BoatFly itz() {
        return ravex.manager.ModuleManager.delegate(BoatFly.class);
    }


}