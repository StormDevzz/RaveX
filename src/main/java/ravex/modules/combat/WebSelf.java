package ravex.modules.combat;

import ravex.modules.annotations.ModuleInfo;
import net.minecraft.client.Minecraft;
import ravex.utility.misc.block.BlockUtility;
import ravex.utility.player.SwingUtility;
import net.minecraft.world.phys.BlockHitResult;
import ravex.utility.misc.PhysicUtility;

import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.NumberParameter;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.rotation.RotationUtility;
@ModuleInfo(name = "WebSelf", category = "Combat")
public class WebSelf extends ravex.modules.Module {
public final BooleanParameter rotate = new BooleanParameter("Rotate", true);
    public final BooleanParameter render = new BooleanParameter("Render", true);
    public final ColorParameter color = new ColorParameter("Color", 0x88FFFFFF);
    public final NumberParameter placeDelay = new NumberParameter("Delay", 2.0, 0.0, 10.0, 1.0);
    public static net.minecraft.core.BlockPos targetPos = null;
    public static float renderR = 1.0f, renderG = 1.0f, renderB = 1.0f;
    private int delay = 0;
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("WebSelf").getEnabled();
    }
    public static WebSelf itz() {
        return ravex.manager.ModuleManager.delegate(WebSelf.class);
    }
    protected void onEnable() { targetPos = null; delay = 0; }
    protected void onDisable() { targetPos = null; }
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (delay > 0) { delay--; return; }
        net.minecraft.core.BlockPos below = mc.player.blockPosition().below();
        if (!mc.level.getBlockState(below).isAir() && !mc.level.getBlockState(below).is(net.minecraft.world.level.block.Blocks.COBWEB)) {
            targetPos = null; return;
        }
        int webSlot = InventoryUtility.findHotbarSlot(mc.player, "cobweb");
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
                float[] angles = RotationUtility.anglesTo(mc.player, net.minecraft.world.phys.Vec3.atCenterOf(below));
                mc.player.setYRot(angles[0]);
                mc.player.setXRot(80.0f);
            }
            mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND,
                new BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(below).add(0, -0.5, 0), net.minecraft.core.Direction.UP, below, false));
            SwingUtility.swing(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND);
            InventoryUtility.selectSlot(mc.player, prevSlot);
            delay = placeDelay.getValue().intValue();
        }
    }

    public java.util.List<ravex.parameter.Parameter<?>> getParameters() {
        java.util.List<ravex.parameter.Parameter<?>> list = new java.util.ArrayList<>();
        for (java.lang.reflect.Field field : getClass().getDeclaredFields()) {
            if (ravex.parameter.Parameter.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    list.add((ravex.parameter.Parameter<?>) field.get(this));
                } catch (Exception ignored) {}
            }
        }
        return list;
    }
}