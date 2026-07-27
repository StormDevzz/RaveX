package ravex.modules.combat;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.block.BlockUtility;
import ravex.utility.misc.PhysicUtility;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.rotation.RotationUtility;
import ravex.utility.player.SwingUtility;
import ravex.mcwrapper.MinecraftWrapper;




@Module(name = "WebSelf", category = "Combat")
public class WebSelf {
    @Parameter(name = "Rotate")
    public boolean rotate = true;
    @Parameter(name = "Render")
    public boolean render = true;
    @Parameter(name = "Color", color = true)
    public int color = 0x88FFFFFF;
    @Parameter(name = "Delay", min = 0.0, max = 10.0, step = 1.0)
    public double placeDelay = 2.0;
    public static net.minecraft.core.BlockPos targetPos = null;
    public static float renderR = 1.0f, renderG = 1.0f, renderB = 1.0f;
    private int delay = 0;


    public void onEnable() { targetPos = null; delay = 0; }
    public void onDisable() { targetPos = null; }
    public void onTick() {
        var mc = MinecraftWrapper.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (delay > 0) { delay--; return; }
        net.minecraft.core.BlockPos below = mc.player.blockPosition().below();
        if (!mc.level.getBlockState(below).isAir() && !mc.level.getBlockState(below).is(net.minecraft.world.level.block.Blocks.COBWEB)) {
            targetPos = null; return;
        }
        int webSlot = InventoryUtility.findHotbarSlot(mc.player, "cobweb");
        if (webSlot == -1) { targetPos = null; return; }
        targetPos = below;
        if (render) {
            int c = color;
            renderR = ((c >> 16) & 0xFF) / 255.0f;
            renderG = ((c >> 8) & 0xFF) / 255.0f;
            renderB = (c & 0xFF) / 255.0f;
        }
        if (mc.level.getBlockState(below).isAir()) {
            int prevSlot = InventoryUtility.getSelectedSlot(mc.player);
            InventoryUtility.selectSlot(mc.player, webSlot);
            if (rotate) {
                float[] angles = RotationUtility.anglesTo(mc.player, net.minecraft.world.phys.Vec3.atCenterOf(below));
                mc.player.setYRot(angles[0]);
                mc.player.setXRot(80.0f);
            }
            mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(below).add(0, -0.5, 0), net.minecraft.core.Direction.UP, below, false));
            SwingUtility.swing(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND);
            InventoryUtility.selectSlot(mc.player, prevSlot);
            delay = (int) placeDelay;
        }
    }


}