package ravex.modules.combat;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.rotation.RotationUtility;
import ravex.utility.player.rotation.SilentRotationUtility;
import ravex.utility.player.rotation.AimUtility;
import ravex.utility.player.SwingUtility;
import ravex.mcwrapper.MinecraftWrapper;


@Module(name = "WebSelf", category = "Combat")
public class WebSelf {
    @Parameter(name = "Rotate", modes = {"NCP", "Vanilla", "Legit", "None"})
    public String rotate = "NCP";
    @Parameter(name = "Swap", modes = {"NCP", "Vanilla", "Legit", "None"})
    public String swapMode = "NCP";
    @Parameter(name = "Render")
    public boolean render = true;
    @Parameter(name = "Color", color = true)
    public int color = 0x88FFFFFF;
    @Parameter(name = "Delay", min = 0.0, max = 10.0, step = 1.0)
    public double placeDelay = 2.0;
    public static net.minecraft.core.BlockPos targetPos = null;
    public static float renderR = 1.0f, renderG = 1.0f, renderB = 1.0f;
    private int delay = 0;
    private static final SilentRotationUtility silentRotation = new SilentRotationUtility();


    public void onEnable() { targetPos = null; delay = 0; }
    public void onDisable() { targetPos = null; silentRotation.reset(); }
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getLevel() == null) return;
        if (delay > 0) { delay--; return; }
        net.minecraft.core.BlockPos below = mc.getPlayer().blockPosition().below();
        if (!mc.getLevel().getBlockState(below).isAir() && !mc.getLevel().getBlockState(below).is(net.minecraft.world.level.block.Blocks.COBWEB)) {
            targetPos = null; return;
        }
        int webSlot = InventoryUtility.findHotbarSlot(mc.getPlayer(), "cobweb");
        if (webSlot == -1) { targetPos = null; return; }
        targetPos = below;
        if (render) {
            int c = color;
            renderR = ((c >> 16) & 0xFF) / 255.0f;
            renderG = ((c >> 8) & 0xFF) / 255.0f;
            renderB = (c & 0xFF) / 255.0f;
        }
        if (mc.getLevel().getBlockState(below).isAir()) {
            int prevSlot = InventoryUtility.getSelectedSlot(mc.getPlayer());
            net.minecraft.world.phys.Vec3 center = net.minecraft.world.phys.Vec3.atCenterOf(below);
            rotateTo(mc, center);
            String swap = swapMode;
            if (swap.equals("NCP")) {
                if (prevSlot != webSlot) {
                    InventoryUtility.silentSelectSlot(mc.getPlayer(), webSlot);
                }
            } else if (swap.equals("Vanilla") || swap.equals("Legit")) {
                if (prevSlot != webSlot) {
                    InventoryUtility.selectSlot(mc.getPlayer(), webSlot);
                }
            } else {
                if (prevSlot != webSlot) return;
            }
            mc.getGameMode().useItemOn(mc.getPlayer(), net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(center.add(0, -0.5, 0), net.minecraft.core.Direction.UP, below, false));
            SwingUtility.swing(mc.getPlayer(), net.minecraft.world.InteractionHand.MAIN_HAND);
            if (swap.equals("NCP") && prevSlot != webSlot) {
                InventoryUtility.silentSelectSlot(mc.getPlayer(), prevSlot);
            } else if ((swap.equals("Vanilla") || swap.equals("Legit")) && prevSlot != webSlot) {
                InventoryUtility.selectSlot(mc.getPlayer(), prevSlot);
            }
            delay = (int) placeDelay;
        }
    }
    private void rotateTo(MinecraftWrapper mc, net.minecraft.world.phys.Vec3 target) {
        String mode = rotate;
        if (mode.equals("None")) return;
        float[] angles = RotationUtility.anglesTo(mc.getPlayer(), target);
        float currentYaw = mc.getPlayer().getYRot();
        float currentPitch = mc.getPlayer().getXRot();
        if (mode.equals("NCP")) {
            if (!silentRotation.initialized) {
                silentRotation.init(currentYaw, currentPitch);
            }
            currentYaw = silentRotation.lastYaw;
            currentPitch = silentRotation.lastPitch;
            float[] limited = AimUtility.limitAngles(currentYaw, RotationUtility.fixAngle(angles[0]), currentPitch, RotationUtility.fixAngle(angles[1]), 180.0f);
            silentRotation.set(limited[0], limited[1]);
            silentRotation.lastYaw = limited[0];
            silentRotation.lastPitch = limited[1];
        } else if (mode.equals("Vanilla")) {
            mc.getPlayer().setYRot(angles[0]);
            mc.getPlayer().setXRot(80.0f);
        } else if (mode.equals("Legit")) {
            float maxSpeed = 90.0f;
            float[] limited = AimUtility.limitAngles(currentYaw, angles[0], currentPitch, 80.0f, maxSpeed);
            limited = AimUtility.randomize(limited[0], limited[1], 1.5f);
            mc.getPlayer().setYRot(limited[0]);
            mc.getPlayer().setXRot(limited[1]);
        }
    }


}
