package ravex.modules.render;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import ravex.event.Subscribe;
import ravex.event.network.PacketEvent;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.render.FontRenderUtility;
import ravex.utility.render.Render2DUtility;
import ravex.utility.render.Render3DUtility;

@Module(name = "LogoutSpots", category = "Render")
public class LogoutSpots {
    @Parameter(name = "Mode", modes = {"Simple", "Shape"})
    public String mode = "Shape";

    @Parameter(name = "Range", min = 30.0, max = 300.0, step = 10.0)
    public double range = 150.0;

    @Parameter(name = "Time", min = 1, max = 120, step = 1)
    public double timeMinutes = 30;

    @Parameter(name = "Names")
    public boolean names = true;

    @Parameter(name = "Color", color = true)
    public int color = 0xFF40A9F8;

    @Parameter(name = "FillAlpha", min = 0.05, max = 0.8, step = 0.05)
    public double fillAlpha = 0.25;

    @Parameter(name = "Outline")
    public boolean outline = true;

    @Parameter(name = "Test")
    public boolean test = false;

    public static class Spot {
        public final UUID uuid;
        public final String name;
        public final double x, y, z;
        public final float bodyYaw, headYaw, pitch;
        public final float health;
        public final long timeMs;

        public Spot(UUID uuid, String name, double x, double y, double z, float bodyYaw, float headYaw, float pitch, float health, long timeMs) {
            this.uuid = uuid;
            this.name = name;
            this.x = x;
            this.y = y;
            this.z = z;
            this.bodyYaw = bodyYaw;
            this.headYaw = headYaw;
            this.pitch = pitch;
            this.health = health;
            this.timeMs = timeMs;
        }
    }

    public static class PlayerSnapshot {
        public final double x, y, z;
        public final float bodyYaw, headYaw, pitch;
        public final float health;
        public final String name;

        public PlayerSnapshot(double x, double y, double z, float bodyYaw, float headYaw, float pitch, float health, String name) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.bodyYaw = bodyYaw;
            this.headYaw = headYaw;
            this.pitch = pitch;
            this.health = health;
            this.name = name;
        }
    }

    public static class Label {
        public final float x, y;
        public final String text;
        public final int color;

        public Label(float x, float y, String text, int color) {
            this.x = x;
            this.y = y;
            this.text = text;
            this.color = color;
        }
    }

    private static final Map<UUID, PlayerSnapshot> snapshots = new ConcurrentHashMap<>();
    private static final List<UUID> leftQueue = new CopyOnWriteArrayList<>();
    private static final List<UUID> joinedQueue = new CopyOnWriteArrayList<>();
    public static final Map<UUID, Spot> spots = new LinkedHashMap<>();
    public static final List<Label> labels = new ArrayList<>();
    private static final UUID TEST_ID = UUID.randomUUID();
    private static boolean testSpawned = false;

    public static int statueTint() {
        LogoutSpots m = Modules.get(LogoutSpots.class);
        if (m == null) return 0xFFFFFFFF;
        return m.color;
    }

    public static void appendGhostStates(LevelRenderState levelState, float partialTick) {
    }

    public void onTick() {
        MinecraftWrapper mc = MinecraftWrapper.getWrapper();
        if (mc.getLevel() == null || mc.getPlayer() == null) return;

        for (Player p : mc.getLevel().players()) {
            if (p == mc.getPlayer() || p.getId() <= -10000) continue;
            UUID id = p.getUUID();
            snapshots.put(id, new PlayerSnapshot(
                p.getX(), p.getY(), p.getZ(),
                p.yBodyRot, p.getYHeadRot(), p.getXRot(),
                p.getHealth() + p.getAbsorptionAmount(),
                p.getGameProfile().name()
            ));
        }

        for (UUID id : joinedQueue) {
            spots.remove(id);
        }
        joinedQueue.clear();

        for (UUID id : leftQueue) {
            if (spots.containsKey(id)) continue;
            PlayerSnapshot snap = snapshots.get(id);
            if (snap == null) continue;
            spots.put(id, new Spot(
                id, snap.name, snap.x, snap.y, snap.z,
                snap.bodyYaw, snap.headYaw, snap.pitch, snap.health,
                System.currentTimeMillis()
            ));
        }
        leftQueue.clear();

        if (test) {
            if (!testSpawned) {
                testSpawned = true;
                LocalPlayer self = mc.getPlayer();
                spots.put(TEST_ID, new Spot(
                    TEST_ID, self.getGameProfile().name(),
                    self.getX(), self.getY(), self.getZ(),
                    self.yBodyRot, self.getYHeadRot(), self.getXRot(),
                    self.getHealth() + self.getAbsorptionAmount(),
                    System.currentTimeMillis()
                ));
            }
        } else if (testSpawned) {
            testSpawned = false;
            spots.remove(TEST_ID);
        }

        long expireMs = (long) (timeMinutes * 60_000L);
        long nowMs = System.currentTimeMillis();
        Iterator<Spot> it = spots.values().iterator();
        while (it.hasNext()) {
            Spot s = it.next();
            if (nowMs - s.timeMs > expireMs) it.remove();
        }
    }

    public void onDisable() {
        spots.clear();
        labels.clear();
        snapshots.clear();
        leftQueue.clear();
        joinedQueue.clear();
        testSpawned = false;
    }

    @Subscribe
    public void onPacket(PacketEvent e) {
        if (!e.isReceive()) return;
        if (e.getPacket() instanceof ClientboundPlayerInfoRemovePacket remove) {
            for (UUID id : remove.profileIds()) {
                leftQueue.add(id);
            }
        } else if (e.getPacket() instanceof ClientboundPlayerInfoUpdatePacket update) {
            for (ClientboundPlayerInfoUpdatePacket.Entry entry : update.entries()) {
                joinedQueue.add(entry.profileId());
            }
        }
    }

    public static void renderWorld(Camera camera, Matrix4f modelViewMatrix, Matrix4f projectionMatrix) {
        labels.clear();
        LogoutSpots m = Modules.get(LogoutSpots.class);
        if (m == null || spots.isEmpty()) return;
        MinecraftWrapper mc = MinecraftWrapper.getWrapper();
        if (mc.getLevel() == null || mc.getPlayer() == null) return;

        Vec3 camPos = camera.position();
        int c = m.color;
        float r = ((c >> 16) & 0xFF) / 255.0f;
        float g = ((c >> 8) & 0xFF) / 255.0f;
        float b = (c & 0xFF) / 255.0f;
        float a = ((c >> 24) & 0xFF) / 255.0f;
        if (a <= 0.01f) a = 1.0f;

        float fillA = a * (float) m.fillAlpha;
        float lineA = a * 0.95f;
        float lineWidth = 1.5f;
        boolean isShape = "Shape".equals(m.mode);

        for (Spot s : spots.values()) {
            double distSq = mc.getPlayer().distanceToSqr(s.x, s.y, s.z);
            if (distSq > m.range * m.range) continue;

            float dx = (float) (s.x - camPos.x);
            float dy = (float) (s.y - camPos.y);
            float dz = (float) (s.z - camPos.z);

            if (isShape) {
                Matrix4f bodyMat = new Matrix4f(modelViewMatrix);
                bodyMat.translate(dx, dy, dz);
                bodyMat.rotateY((float) Math.toRadians(180.0f - s.bodyYaw));

                Render3DUtility.batchBox(bodyMat, -0.25f, 0.75f, -0.125f, 0.25f, 1.50f, 0.125f, r, g, b, fillA, true);
                if (m.outline) {
                    Render3DUtility.batchBoxWireframe(bodyMat, -0.25f, 0.75f, -0.125f, 0.25f, 1.50f, 0.125f, r, g, b, lineA, lineWidth, true);
                }

                Render3DUtility.batchBox(bodyMat, 0.25f, 0.75f, -0.125f, 0.50f, 1.50f, 0.125f, r, g, b, fillA, true);
                if (m.outline) {
                    Render3DUtility.batchBoxWireframe(bodyMat, 0.25f, 0.75f, -0.125f, 0.50f, 1.50f, 0.125f, r, g, b, lineA, lineWidth, true);
                }

                Render3DUtility.batchBox(bodyMat, -0.50f, 0.75f, -0.125f, -0.25f, 1.50f, 0.125f, r, g, b, fillA, true);
                if (m.outline) {
                    Render3DUtility.batchBoxWireframe(bodyMat, -0.50f, 0.75f, -0.125f, -0.25f, 1.50f, 0.125f, r, g, b, lineA, lineWidth, true);
                }

                Render3DUtility.batchBox(bodyMat, 0.00f, 0.00f, -0.125f, 0.25f, 0.75f, 0.125f, r, g, b, fillA, true);
                if (m.outline) {
                    Render3DUtility.batchBoxWireframe(bodyMat, 0.00f, 0.00f, -0.125f, 0.25f, 0.75f, 0.125f, r, g, b, lineA, lineWidth, true);
                }

                Render3DUtility.batchBox(bodyMat, -0.25f, 0.00f, -0.125f, 0.00f, 0.75f, 0.125f, r, g, b, fillA, true);
                if (m.outline) {
                    Render3DUtility.batchBoxWireframe(bodyMat, -0.25f, 0.00f, -0.125f, 0.00f, 0.75f, 0.125f, r, g, b, lineA, lineWidth, true);
                }

                Matrix4f headMat = new Matrix4f(bodyMat);
                headMat.translate(0.0f, 1.50f, 0.0f);
                headMat.rotateY((float) Math.toRadians(s.bodyYaw - s.headYaw));
                headMat.rotateX((float) Math.toRadians(s.pitch));

                Render3DUtility.batchBox(headMat, -0.25f, 0.00f, -0.25f, 0.25f, 0.50f, 0.25f, r, g, b, fillA, true);
                if (m.outline) {
                    Render3DUtility.batchBoxWireframe(headMat, -0.25f, 0.00f, -0.25f, 0.25f, 0.50f, 0.25f, r, g, b, lineA, lineWidth, true);
                }
            } else {
                Matrix4f boxMat = new Matrix4f(modelViewMatrix);
                boxMat.translate(dx, dy, dz);
                boxMat.rotateY((float) Math.toRadians(180.0f - s.bodyYaw));
                Render3DUtility.batchBox(boxMat, -0.3f, 0.0f, -0.3f, 0.3f, 1.8f, 0.3f, r, g, b, fillA, true);
                if (m.outline) {
                    Render3DUtility.batchBoxWireframe(boxMat, -0.3f, 0.0f, -0.3f, 0.3f, 1.8f, 0.3f, r, g, b, lineA, lineWidth, true);
                }
            }

            if (m.names) {
                Vector4f v = new Vector4f(dx, dy + 2.2f, dz, 1.0f);
                modelViewMatrix.transform(v);
                projectionMatrix.transform(v);
                if (v.w > 0.01f && v.z / v.w < 1.0f) {
                    float sx = (v.x / v.w * 0.5f + 0.5f) * mc.getScreenWidth();
                    float sy = (1.0f - (v.y / v.w * 0.5f + 0.5f)) * mc.getScreenHeight();
                    float hp = Math.max(0.0f, s.health);
                    String hpText;
                    if (Math.abs(hp - Math.round(hp)) < 0.05f) {
                        hpText = Math.round(hp) + "hp";
                    } else {
                        hpText = String.format(Locale.ROOT, "%.1fhp", hp);
                    }
                    String text = s.name + " logout spot (" + hpText + ")";
                    labels.add(new Label(sx, sy, text, m.color));
                }
            }
        }
    }

    public static void renderLabels(GuiGraphics context) {
        if (spots.isEmpty() || labels.isEmpty()) return;
        LogoutSpots m = Modules.get(LogoutSpots.class);
        if (m == null || !m.names) return;
        int fontH = FontRenderUtility.getFontHeight();
        for (Label label : labels) {
            int w = FontRenderUtility.getStringWidth(label.text);
            int rx = (int) (label.x - w / 2f);
            int ry = (int) label.y;
            int padX = 5;
            int padY = 3;
            int bgX = rx - padX;
            int bgY = ry - padY;
            int bgW = w + padX * 2;
            int bgH = fontH + padY * 2;
            Render2DUtility.drawRound(context, bgX, bgY, bgW, bgH, 3, 0x900A0E18);
            FontRenderUtility.drawString(context, label.text, rx, ry, label.color, true);
        }
    }
}