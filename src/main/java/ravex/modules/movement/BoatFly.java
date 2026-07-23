package ravex.modules.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import ravex.event.EventBusHolder;
import ravex.event.Subscribe;
import ravex.event.network.PacketEvent;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;

import java.util.ArrayList;
import java.util.List;

public class BoatFly extends Module {
    public final ModeParameter mode = new ModeParameter("Mode", "Packet",
            List.of("Packet", "PacketStrict", "Motion"));
    public final NumberParameter speed = new NumberParameter("Speed", 2.0, 0.1, 25.0, 0.1);
    public final NumberParameter ySpeed = new NumberParameter("YSpeed", 1.0, 0.0, 10.0, 0.1);
    public final NumberParameter boatScale = new NumberParameter("BoatScale", 0.1, 0.05, 1.0, 0.05);
    public final BooleanParameter autoMount = new BooleanParameter("AutoMount", true);
    public final BooleanParameter gravity = new BooleanParameter("Gravity", false);
    public final BooleanParameter phase = new BooleanParameter("Phase", false);
    public final BooleanParameter cancelPackets = new BooleanParameter("CancelPackets", true);
    public final BooleanParameter allowShift = new BooleanParameter("AllowShift", true);

    private final ArrayList<ServerboundMoveVehiclePacket> vehiclePackets = new ArrayList<>();
    private float currentScale = 1.0f;

    public static boolean isBoatScaleActive() {
        return maybeEnabled(BoatFly.class);
    }

    public float getScale() {
        return currentScale;
    }

    private void updateScale() {
        float target = boatScale.getValue().floatValue();
        float speed = 0.15f;
        if (currentScale > target) {
            currentScale = Math.max(target, currentScale - speed);
        } else if (currentScale < target) {
            currentScale = Math.min(target, currentScale + speed);
        }
    }

    @Override
    protected void onEnable() {
        vehiclePackets.clear();
        currentScale = 1.0f;
        EventBusHolder.get().subscribe(this);
        if (autoMount.getValue()) mountToNearestBoat();
    }

    @Override
    protected void onDisable() {
        vehiclePackets.clear();
        currentScale = 1.0f;
        EventBusHolder.get().unsubscribe(this);
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.setNoGravity(false);
            Entity vehicle = mc.player.getVehicle();
            if (vehicle != null) {
                vehicle.setNoGravity(false);
            }
        }
    }

    @Subscribe
    public void onPacket(PacketEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.getVehicle() == null) return;
        if (!(mc.player.getVehicle() instanceof Boat)) return;

        if (event.isSend()) {
            var packet = event.getPacket();

            if (packet instanceof ServerboundMoveVehiclePacket pac && mode.getValue().equals("Packet")) {
                if (vehiclePackets.contains(pac)) {
                    vehiclePackets.remove(pac);
                } else {
                    event.setCancelled(true);
                }
            }

            if (allowShift.getValue() && packet instanceof net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket) {
                event.setCancelled(true);
            }
        }

        if (event.isReceive() && cancelPackets.getValue()) {
            var packet = event.getPacket();
            if (packet instanceof net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket
                    || packet instanceof net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket) {
                event.setCancelled(true);
            }
        }
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        updateScale();

        Entity vehicle = mc.player.getVehicle();
        if (vehicle == null || !(vehicle instanceof Boat)) {
            if (autoMount.getValue()) mountToNearestBoat();
            return;
        }

        if (phase.getValue()) {
            vehicle.noPhysics = true;
            mc.player.noPhysics = true;
        }

        vehicle.setNoGravity(!gravity.getValue());
        mc.player.setNoGravity(!gravity.getValue());

        vehicle.setYRot(mc.player.getYRot());

        double[] motion = forward(speed.getValue());
        double px = vehicle.getX() + motion[0];
        double pz = vehicle.getZ() + motion[1];
        double py = vehicle.getY();

        if (mc.options.keyJump.isDown()) {
            py += ySpeed.getValue();
        } else if (mc.options.keyShift.isDown()) {
            py -= ySpeed.getValue();
        }

        String currentMode = mode.getValue();

        if (currentMode.equals("Motion")) {
            Vec3 vel = vehicle.getDeltaMovement();
            double vy = vel.y + (mc.options.keyJump.isDown() ? ySpeed.getValue() : (mc.options.keyShift.isDown() ? -ySpeed.getValue() : 0));
            vehicle.setDeltaMovement(motion[0], vy, motion[1]);
        } else {
            vehicle.setPos(px, py, pz);
            ServerboundMoveVehiclePacket packet = ServerboundMoveVehiclePacket.fromEntity(vehicle);
            vehiclePackets.add(packet);
            mc.player.connection.send(packet);

            if (currentMode.equals("PacketStrict")) {
                mc.player.connection.send(ServerboundMoveVehiclePacket.fromEntity(vehicle));
            }
        }
    }

    private void mountToNearestBoat() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof Boat && mc.player.distanceTo(entity) < 5.0) {
                mc.gameMode.interact(mc.player, entity, InteractionHand.MAIN_HAND);
                break;
            }
        }
    }

    private double[] forward(double speed) {
        Minecraft mc = Minecraft.getInstance();
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
        return maybeEnabled(BoatFly.class);
    }

    public static BoatFly itz() {
        return ModuleManager.get(BoatFly.class);
    }
}
