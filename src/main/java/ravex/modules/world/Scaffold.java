package ravex.modules.world;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import ravex.utility.misc.block.BlockUtility;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.rotation.AimUtility;
import ravex.utility.player.rotation.RotationUtility;
import ravex.utility.player.rotation.SilentRotation;
import ravex.utility.render.animate.EasingAnimation;
import ravex.utility.player.SwingUtility;
import ravex.utility.render.animate.SlideAnimation;
import net.minecraft.client.Minecraft;
import java.util.List;
public class Scaffold extends Module {
    public final ModeParameter mode = new ModeParameter("Mode", "Vanilla", List.of("Vanilla", "Grim"));
    public final BooleanParameter expand = new BooleanParameter("Expand", false);
    public final NumberParameter expandLength = new NumberParameter("ExpandLength", 4.0, 1.0, 10.0, 1.0);
    public final NumberParameter rotationSpeed = new NumberParameter("RotationSpeed", 120.0, 10.0, 360.0, 5.0);
    public final BooleanParameter tower = new BooleanParameter("Tower", true);
    public final BooleanParameter eagle = new BooleanParameter("Eagle", true);
    public final BooleanParameter keepY = new BooleanParameter("KeepY", false);
    public final BooleanParameter render = new BooleanParameter("Render", true);
    public final BooleanParameter animate = new BooleanParameter("Animate", true);
    public final ColorParameter highlightColor = new ColorParameter("Color", 0xFFFF33CC);
    public static net.minecraft.world.phys.Vec3 highlightPos = null;
    public static float renderAlpha = 0.0f;
    public static double renderSize = 0.0;
    public static float renderR = 1.0f;
    public static float renderG = 0.2f;
    public static float renderB = 0.8f;
    public static final SilentRotation silentRotation = new SilentRotation();
    private final EasingAnimation fadeAnim = new EasingAnimation();
    private final EasingAnimation sizeAnim = new EasingAnimation();
    private final SlideAnimation slideAnim = new SlideAnimation();
    private int currX, currY, currZ;
    private boolean hasCurr;

    private static net.minecraft.core.BlockPos pendingPos;
    private static net.minecraft.core.Direction pendingFace;
    private static net.minecraft.core.BlockPos pendingNeighbor;
    private static boolean hasPending;

    public Scaffold() {
        expandLength.setVisible(() -> expand.getValue());
    }

    public net.minecraft.core.BlockPos getCurrentPos() {
        return hasCurr ? BlockUtility.pos(currX, currY, currZ) : null;
    }
    private int lastSlot = -1;
    private double targetY = -1;

    public static void onPreTick() {
        if (!hasPending) return;
        Minecraft mc = Minecraft.getInstance();
        var p = mc.player;
        if (p == null || mc.level == null) return;
        int slot = findBlockSlot(p);
        if (slot == -1) {
            hasPending = false;
            return;
        }
        if (!BlockUtility.isAir(mc.level, pendingPos)) {
            hasPending = false;
            return;
        }
        var center = SwingUtility.centerOf(pendingNeighbor);
        var hitVec = center.add(SwingUtility.vec3(
            pendingFace.getStepX(), pendingFace.getStepY(), pendingFace.getStepZ()
        ).scale(0.5));
        float[] exact = RotationUtility.anglesTo(p.getEyePosition(), center);
        p.setYRot(exact[0]);
        p.setXRot(exact[1]);
        int prevSlot = InventoryUtility.getSelectedSlot(p);
        InventoryUtility.selectSlot(p, slot);
        BlockUtility.useItemOn(mc, SwingUtility.hitResult(hitVec, pendingFace, pendingNeighbor));
        BlockUtility.swing(mc);
        if (slot != prevSlot) InventoryUtility.selectSlot(p, prevSlot);
        hasPending = false;
    }

    @Override
    protected void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            targetY = Math.floor(mc.player.getY());
        } else {
            targetY = -1;
        }
        highlightPos = null;
        renderAlpha = 0.0f;
        renderSize = 0.0;
        hasCurr = false;
        hasPending = false;
        fadeAnim.reset();
        sizeAnim.reset();
        slideAnim.reset();
    }
    @Override
    protected void onDisable() {
        highlightPos = null;
        renderAlpha = 0.0f;
        renderSize = 0.0;
        hasCurr = false;
        hasPending = false;
    }
    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        var p = mc.player;
        if (p == null || mc.level == null) return;
        if (p.onGround()) {
            targetY = Math.floor(p.getY());
        }
        if (tower.getValue() && mc.options.keyJump.isDown()) {
            p.setDeltaMovement(p.getDeltaMovement().x, 0.42, p.getDeltaMovement().z);
            targetY = Math.floor(p.getY());
        }
        int slot = findBlockSlot(p);
        if (slot == -1) {
            hasCurr = false;
            return;
        }
        int bx = (int) Math.floor(p.getX());
        int by = (int) ((keepY.getValue() && targetY != -1) ? (targetY - 1) : (p.getY() - 1));
        int bz = (int) Math.floor(p.getZ());
        int tx = bx, ty = by, tz = bz;
        if (expand.getValue()) {
            double dx = p.getDeltaMovement().x;
            double dz = p.getDeltaMovement().z;
            int len = (int) Math.round(expandLength.getValue());
            int offX = dx > 0.05 ? len : (dx < -0.05 ? -len : 0);
            int offZ = dz > 0.05 ? len : (dz < -0.05 ? -len : 0);
            int ex = bx + offX, ez = bz + offZ;
            if (isAir(ex, by, ez)) { tx = ex; tz = ez; }
        }
        if (!isAir(tx, ty, tz)) {
            hasCurr = false;
            return;
        }
        currX = tx; currY = ty; currZ = tz; hasCurr = true;
        if (render.getValue()) {
            int hc = highlightColor.getValue();
            renderR = ((hc >> 16) & 0xFF) / 255.0f;
            renderG = ((hc >> 8) & 0xFF) / 255.0f;
            renderB = (hc & 0xFF) / 255.0f;
        }
        boolean isGrim = "Grim".equals(mode.getValue());
        var neighbor = findNeighbor(tx, ty, tz, isGrim);
        if (neighbor == null || neighbor.neighbor == null) {
            int by2 = BlockUtility.belowY(ty);
            var fallback = BlockUtility.pos(tx, by2, tz);
            if (isAir(tx, by2, tz)) return;
            neighbor = null;
            boolean fallbackOk = false;
            if (!isGrim) {
                fallbackOk = true;
            } else {
                var eye = p.getEyePosition();
                if (eye.y >= by2 + 2) fallbackOk = true;
            }
            if (fallbackOk) {
                var center = SwingUtility.centerOf(fallback);
                float speed = rotationSpeed.getValue().floatValue();
                smoothRotate(p, center, speed);
                pendingPos = BlockUtility.pos(tx, ty, tz);
                pendingFace = net.minecraft.core.Direction.UP;
                pendingNeighbor = fallback;
                hasPending = true;
            }
            return;
        }
        var nb = neighbor.neighbor;
        var face = neighbor.face;
        var nbCenter = SwingUtility.centerOf(nb);
        float speed = rotationSpeed.getValue().floatValue();
        smoothRotate(p, nbCenter, speed);
        pendingPos = BlockUtility.pos(tx, ty, tz);
        pendingFace = face;
        pendingNeighbor = nb;
        hasPending = true;
        if (eagle.getValue()) {
            var feetPos = BlockUtility.pos(bx, by, bz);
            if (mc.level.getBlockState(feetPos).isAir()) {
                mc.options.keyShift.setDown(true);
            }
        }
    }
    private void smoothRotate(net.minecraft.client.player.LocalPlayer p, net.minecraft.world.phys.Vec3 targetCenter, float speed) {
        float[] target = RotationUtility.anglesTo(p.getEyePosition(), targetCenter);
        float[] limited = AimUtility.limitAngles(
            p.getYRot(), target[0],
            p.getXRot(), target[1],
            speed / 20f
        );
        p.setYRot(limited[0]);
        p.setXRot(limited[1]);
    }

    private NeighborResult findNeighbor(int tx, int ty, int tz, boolean grim) {
        var eye = Minecraft.getInstance().player != null
            ? Minecraft.getInstance().player.getEyePosition()
            : null;
        var bestNeighbor = (net.minecraft.core.BlockPos) null;
        var bestFace = net.minecraft.core.Direction.UP;
        double bestDist = Double.MAX_VALUE;

        for (var face : net.minecraft.core.Direction.values()) {
            int sx = tx + face.getStepX(), sy = ty + face.getStepY(), sz = tz + face.getStepZ();
            if (isAir(sx, sy, sz)) continue;
            var candidate = BlockUtility.pos(sx, sy, sz);
            var clickFace = face.getOpposite();
            if (grim && eye != null) {
                boolean safe = switch (clickFace) {
                    case UP -> eye.y >= candidate.getY() + 1;
                    case DOWN -> eye.y <= candidate.getY();
                    case NORTH -> eye.z <= candidate.getZ();
                    case SOUTH -> eye.z >= candidate.getZ() + 1;
                    case WEST -> eye.x <= candidate.getX();
                    case EAST -> eye.x >= candidate.getX() + 1;
                    default -> false;
                };
                if (!safe) continue;
            }
            double dist = eye != null
                ? eye.distanceToSqr(SwingUtility.centerOf(candidate))
                : 0;
            if (dist < bestDist) {
                bestDist = dist;
                bestNeighbor = candidate;
                bestFace = clickFace;
            }
        }
        if (bestNeighbor == null) return null;
        return new NeighborResult(bestNeighbor, bestFace);
    }

    private record NeighborResult(net.minecraft.core.BlockPos neighbor, net.minecraft.core.Direction face) {}

    private boolean isAir(int x, int y, int z) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;
        var state = BlockUtility.getState(mc.level, x, y, z);
        return state.isAir() || BlockUtility.isBlock(state, "snow") || !state.getFluidState().isEmpty();
    }
    private static int findBlockSlot(net.minecraft.client.player.LocalPlayer p) {
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(p, i);
            if (!stack.isEmpty() && InventoryUtility.isBlockItem(stack)) return i;
        }
        return -1;
    }

    public static boolean maybeEnabled() {
        return maybeEnabled(Scaffold.class);
    }
    public static Scaffold itz() {
        return ModuleManager.get(Scaffold.class);
    }
}
