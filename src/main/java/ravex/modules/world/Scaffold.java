package ravex.modules.world;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.ModeParameter;
import ravex.utility.render.animate.EasingAnimation;
import ravex.utility.render.animate.SlideAnimation;
import ravex.utility.misc.block.BlockUtility;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.rotation.SilentRotation;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import java.util.List;
public class Scaffold extends Module {
    public static final Scaffold INSTANCE = new Scaffold();
    public final ModeParameter mode = new ModeParameter("Mode", "Normal", List.of("Normal", "Expand"));
    public final BooleanParameter tower = new BooleanParameter("Tower", true);
    public final BooleanParameter silentRot = new BooleanParameter("SilentRot", true);
    public final BooleanParameter keepY = new BooleanParameter("KeepY", false);
    public final BooleanParameter render = new BooleanParameter("Render", true);
    public final BooleanParameter animate = new BooleanParameter("Animate", true);
    public final ColorParameter highlightColor = new ColorParameter("Color", 0xFFFF33CC);
    public static Vec3 highlightPos = null;
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

    public net.minecraft.core.BlockPos getCurrentPos() {
        return hasCurr ? ravex.utility.misc.block.BlockUtility.pos(currX, currY, currZ) : null;
    }
    private int lastSlot = -1;
    private double targetY = -1;

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
        if ("Expand".equals(mode.getValue())) {
            double dx = p.getDeltaMovement().x;
            double dz = p.getDeltaMovement().z;
            int offX = dx > 0.05 ? 1 : (dx < -0.05 ? -1 : 0);
            int offZ = dz > 0.05 ? 1 : (dz < -0.05 ? -1 : 0);
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
        var neighbor = (net.minecraft.core.BlockPos) null;
        var placeFace = net.minecraft.core.Direction.UP;
        for (var face : net.minecraft.core.Direction.values()) {
            int sx = tx + face.getStepX(), sy = ty + face.getStepY(), sz = tz + face.getStepZ();
            if (!isAir(sx, sy, sz)) {
                neighbor = BlockUtility.pos(sx, sy, sz);
                placeFace = face.getOpposite();
                break;
            }
        }
        if (neighbor == null) {
            int by2 = BlockUtility.belowY(ty);
            neighbor = BlockUtility.pos(tx, by2, tz);
            placeFace = net.minecraft.core.Direction.UP;
        }
        if (silentRot.getValue()) {
            silentRotation.setAnglesTo(mc, neighbor.getCenter());
        } else {
            silentRotation.hasRotation = false;
        }
        int prevSlot = InventoryUtility.getSelectedSlot(p);
        InventoryUtility.selectSlot(p, slot);
        var center = Vec3.atCenterOf(neighbor);
        var hitVec = center.add(new Vec3(placeFace.getStepX(), placeFace.getStepY(), placeFace.getStepZ()).scale(0.5));
        BlockUtility.useItemOn(mc, new net.minecraft.world.phys.BlockHitResult(hitVec, placeFace, neighbor, false));
        BlockUtility.swing(mc);
        if (slot != prevSlot) InventoryUtility.selectSlot(p, prevSlot);
    }
    private boolean isAir(int x, int y, int z) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;
        var state = BlockUtility.getState(mc.level, x, y, z);
        return state.isAir() || BlockUtility.isBlock(state, "snow") || !state.getFluidState().isEmpty();
    }
    private int findBlockSlot(net.minecraft.client.player.LocalPlayer p) {
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(p, i);
            if (!stack.isEmpty() && InventoryUtility.isBlockItem(stack)) return i;
        }
        return -1;
    }
}
