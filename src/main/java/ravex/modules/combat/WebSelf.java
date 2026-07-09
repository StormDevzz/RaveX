package ravex.modules.combat;
<<<<<<< HEAD
import ravex.manager.ModuleManager;
=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
<<<<<<< HEAD
=======
import net.minecraft.world.item.Items;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.NumberParameter;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.rotation.RotationUtility;
import ravex.utility.player.SwingUtility;
public class WebSelf extends Module {
<<<<<<< HEAD
=======
    public static final WebSelf INSTANCE = new WebSelf();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    public final BooleanParameter rotate = new BooleanParameter("Rotate", true);
    public final BooleanParameter render = new BooleanParameter("Render", true);
    public final ColorParameter color = new ColorParameter("Color", 0x88FFFFFF);
    public final NumberParameter placeDelay = new NumberParameter("Delay", 2.0, 0.0, 10.0, 1.0);
    public static BlockPos targetPos = null;
    public static float renderR = 1.0f, renderG = 1.0f, renderB = 1.0f;
    private int delay = 0;
<<<<<<< HEAD
    public static boolean maybeEnabled() {
        return maybeEnabled(WebSelf.class);
    }
    public static WebSelf itz() {
        return ModuleManager.get(WebSelf.class);
    }
=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3

    @Override
    protected void onEnable() { targetPos = null; delay = 0; }
    @Override
    protected void onDisable() { targetPos = null; }
    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (delay > 0) { delay--; return; }
        BlockPos below = mc.player.blockPosition().below();
        if (!mc.level.getBlockState(below).isAir() && !mc.level.getBlockState(below).is(Blocks.COBWEB)) {
            targetPos = null; return;
        }
<<<<<<< HEAD
        int webSlot = InventoryUtility.findHotbarSlot(mc.player, "cobweb");
=======
        int webSlot = InventoryUtility.findHotbarSlot(mc.player, Items.COBWEB);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        if (webSlot == -1) { targetPos = null; return; }
        targetPos = below;
        if (render.getValue()) {
            int c = color.getValue();
            renderR = ((c >> 16) & 0xFF) / 255.0f;
            renderG = ((c >> 8) & 0xFF) / 255.0f;
            renderB = (c & 0xFF) / 255.0f;
        }
        if (mc.level.getBlockState(below).isAir()) {
            int prevSlot = InventoryUtility.getSelectedSlot(mc.player);
            InventoryUtility.selectSlot(mc.player, webSlot);
            if (rotate.getValue()) {
                float[] angles = RotationUtility.anglesTo(mc.player, Vec3.atCenterOf(below));
                mc.player.setYRot(angles[0]);
                mc.player.setXRot(80.0f);
            }
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(below).add(0, -0.5, 0), Direction.UP, below, false));
            SwingUtility.swing(mc.player, InteractionHand.MAIN_HAND);
            InventoryUtility.selectSlot(mc.player, prevSlot);
            delay = placeDelay.getValue().intValue();
        }
    }
}
