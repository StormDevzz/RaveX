package ravex.modules.combat;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import ravex.utility.nativelib.NativeLibrary;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.rotation.RotationUtility;
import ravex.utility.player.SwingUtility;
import java.util.ArrayList;
import java.util.List;
public class SelfFill extends Module {
    public static final SelfFill INSTANCE = new SelfFill();
    public final ModeParameter fillMode = new ModeParameter("Fill", "Surround",
        java.util.List.of("Surround", "Full", "Below"));
    public final ModeParameter block = new ModeParameter("Block", "Obsidian",
        java.util.List.of("Obsidian", "Cobblestone", "Endstone", "Web"));
    public final NumberParameter placeDelay = new NumberParameter("Delay", 1, 1, 5, 1);
    public final BooleanParameter autoCenter = new BooleanParameter("AutoCenter", true);
    public final BooleanParameter rotate = new BooleanParameter("Rotate", true);
    public final BooleanParameter onlyInHole = new BooleanParameter("OnlyInHole", true);
    private static final NativeLibrary NATIVE = NativeLibrary.of("ravex_selffill");
    static {
        NATIVE.load();
    }
    private int tickCounter = 0;

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (onlyInHole.getValue() && !isInHole(mc)) return;
        tickCounter++;
        if (tickCounter < placeDelay.getValue().intValue()) return;
        tickCounter = 0;
        List<BlockPos> targets = getTargets(mc);
        int slot = findBlockSlot(mc);
        if (slot == -1 || targets.isEmpty()) return;
        int prevSlot = InventoryUtility.getSelectedSlot(mc.player);
        InventoryUtility.selectSlot(mc.player, slot);
        for (BlockPos pos : targets) {
            if (!mc.level.getBlockState(pos).isAir() && !mc.level.getBlockState(pos).canBeReplaced()) continue;
            if (rotate.getValue()) {
                float yaw;
                if (NATIVE.isLoaded())
                    yaw = (float) nativeGetAngle(mc.player.getX(), mc.player.getY(), mc.player.getZ(), pos.getX(), pos.getY(), pos.getZ());
                else
                    yaw = RotationUtility.yawTo(mc.player, Vec3.atCenterOf(pos));
                mc.player.setYRot(yaw);
                mc.player.setXRot(45.0f);
            }
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
                new BlockHitResult(new Vec3(pos.getX() + 0.5, pos.getY() - 1, pos.getZ() + 0.5), Direction.UP, pos.below(), false));
            SwingUtility.swing(mc.player, InteractionHand.MAIN_HAND);
        }
        InventoryUtility.selectSlot(mc.player, prevSlot);
    }
    private List<BlockPos> getTargets(Minecraft mc) {
        BlockPos feet = mc.player.blockPosition();
        List<BlockPos> list = new ArrayList<>();
        String mode = fillMode.getValue();
        if (mode.equals("Below")) { list.add(feet.below()); return list; }
        int[][] offsets = mode.equals("Surround")
            ? new int[][]{{1,0,0},{-1,0,0},{0,0,1},{0,0,-1}}
            : new int[][]{{1,0,0},{-1,0,0},{0,0,1},{0,0,-1},{1,0,1},{-1,0,1},{1,0,-1},{-1,0,-1}};
        for (int[] o : offsets) list.add(feet.offset(o[0], o[1], o[2]));
        return list;
    }
    private boolean isInHole(Minecraft mc) {
        BlockPos feet = mc.player.blockPosition();
        return !mc.level.getBlockState(feet.below()).isAir() &&
               !mc.level.getBlockState(feet.offset(1,0,0)).isAir() &&
               !mc.level.getBlockState(feet.offset(-1,0,0)).isAir() &&
               !mc.level.getBlockState(feet.offset(0,0,1)).isAir() &&
               !mc.level.getBlockState(feet.offset(0,0,-1)).isAir();
    }
    private int findBlockSlot(Minecraft mc) {
        String b = block.getValue();
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(mc.player, i);
            if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) continue;
            var blk = ((BlockItem) stack.getItem()).getBlock();
            if (b.equals("Obsidian") && blk == Blocks.OBSIDIAN) return i;
            if (b.equals("Cobblestone") && blk == Blocks.COBBLESTONE) return i;
            if (b.equals("Endstone") && blk == Blocks.END_STONE) return i;
            if (b.equals("Web") && blk == Blocks.COBWEB) return i;
        }
        return -1;
    }
    private static native double nativeGetAngle(double px, double py, double pz, double bx, double by, double bz);
}
