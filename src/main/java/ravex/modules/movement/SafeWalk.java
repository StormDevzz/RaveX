package ravex.modules.movement;
<<<<<<< HEAD
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.NumberParameter;
public class SafeWalk extends Module {
    public final NumberParameter threshold = new NumberParameter("Threshold", 0.001, 0.0, 0.5, 0.001);
    public static boolean maybeEnabled() {
        return maybeEnabled(SafeWalk.class);
    }
    public static SafeWalk itz() {
        return ModuleManager.get(SafeWalk.class);
    }
=======
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;
import ravex.utility.nativelib.NativeLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
public class SafeWalk extends Module {
    public static final SafeWalk INSTANCE = new SafeWalk();
    public final NumberParameter threshold = new NumberParameter("Threshold", 0.001, 0.0, 0.5, 0.001);
    public final BooleanParameter sneak = new BooleanParameter("Sneak", true);

    static {
        NativeLoader.load();
    }
    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) return;
        boolean nearEdge = checkEdge(p);
        if (nearEdge && sneak.getValue()) {
            p.setShiftKeyDown(true);
        }
    }
    private boolean checkEdge(LocalPlayer p) {
        try {
            int[] blocks = getSurroundingBlocks(p);
            return nativeIsNearEdge(p.getX(), p.getY(), p.getZ(), blocks, threshold.getValue());
        } catch (UnsatisfiedLinkError e) {
            return fallbackEdgeCheck(p);
        }
    }
    private boolean fallbackEdgeCheck(LocalPlayer p) {
        Minecraft mc = Minecraft.getInstance();
        int bx = (int) Math.floor(p.getX());
        int by = (int) Math.floor(p.getY());
        int bz = (int) Math.floor(p.getZ());
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int[][] checks = {{bx-1,bz},{bx+1,bz},{bx,bz-1},{bx,bz+1}};
        for (int[] c : checks) {
            pos.set(c[0], by, c[1]);
            if (!mc.level.getBlockState(pos).isAir()) {
                pos.set(c[0], by-1, c[1]);
                if (mc.level.getBlockState(pos).isAir()) return true;
            }
        }
        return false;
    }
    private int[] getSurroundingBlocks(LocalPlayer p) {
        Minecraft mc = Minecraft.getInstance();
        int bx = (int) Math.floor(p.getX());
        int by = (int) Math.floor(p.getY());
        int bz = (int) Math.floor(p.getZ());
        java.util.List<Integer> list = new java.util.ArrayList<>();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = -1; dy <= 2; dy++) {
                    pos.set(bx + dx, by + dy, bz + dz);
                    if (!mc.level.getBlockState(pos).isAir()) {
                        list.add(bx + dx);
                        list.add(by + dy);
                        list.add(bz + dz);
                    }
                }
            }
        }
        int[] arr = new int[list.size()];
        for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
        return arr;
    }
    private native boolean nativeIsNearEdge(double x, double y, double z, int[] blocks, double threshold);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
}
