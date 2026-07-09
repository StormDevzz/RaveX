package ravex.modules.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.NumberParameter;

public class WebSelf extends Module {
    public static final WebSelf INSTANCE = new WebSelf();

    public final BooleanParameter rotate = new BooleanParameter("Rotate", true);
    public final BooleanParameter render = new BooleanParameter("Render", true);
    public final ColorParameter color = new ColorParameter("Color", 0x88FFFFFF);
    public final NumberParameter placeDelay = new NumberParameter("Delay", 2.0, 0.0, 10.0, 1.0);

    public static BlockPos targetPos = null;
    public static float renderR = 1.0f, renderG = 1.0f, renderB = 1.0f;

    private int delay = 0;

    private WebSelf() {
        super("WebSelf", Category.COMBAT);
        addParameter(rotate);
        addParameter(render);
        addParameter(color);
        addParameter(placeDelay);
    }

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
            targetPos = null;
            return;
        }

        int webSlot = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.is(Items.COBWEB)) { webSlot = i; break; }
        }
        if (webSlot == -1) { targetPos = null; return; }

        targetPos = below;

        if (render.getValue()) {
            int c = color.getValue();
            renderR = ((c >> 16) & 0xFF) / 255.0f;
            renderG = ((c >> 8) & 0xFF) / 255.0f;
            renderB = (c & 0xFF) / 255.0f;
        }

        if (mc.level.getBlockState(below).isAir()) {
            int prevSlot = mc.player.getInventory().getSelectedSlot();
            mc.player.getInventory().setSelectedSlot(webSlot);

            if (rotate.getValue()) {
                double dx = below.getX() + 0.5 - mc.player.getX();
                double dz = below.getZ() + 0.5 - mc.player.getZ();
                mc.player.setYRot((float) Math.toDegrees(Math.atan2(-dx, dz)));
                mc.player.setXRot(80.0f);
            }

            Vec3 hitVec = Vec3.atCenterOf(below).add(0, -0.5, 0);
            BlockHitResult hit = new BlockHitResult(hitVec, Direction.UP, below, false);
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
            mc.player.swing(InteractionHand.MAIN_HAND);

            mc.player.getInventory().setSelectedSlot(prevSlot);
            delay = placeDelay.getValue().intValue();
        }
    }
}
