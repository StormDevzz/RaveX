package ravex.modules.world;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import ravex.utility.nativelib.NativeLibrary;
import java.util.List;
public class ECFarmer extends Module {
    public static final ECFarmer INSTANCE = new ECFarmer();
    public final NumberParameter range = new NumberParameter("Range", 4.5, 1.0, 6.0, 0.5);
    public final ModeParameter swapMode = new ModeParameter("Swap", "Silent", List.of("Silent", "Normal"));
    public final BooleanParameter render = new BooleanParameter("Render", true);
    public final ColorParameter color = new ColorParameter("Color", 0x3F8800FF);
    public static BlockPos currentTarget = null;
    private enum State { IDLE, FIND_BREAK, BREAKING, FIND_PLACE, PLACING }
    private State state = State.IDLE;
    private BlockPos ecPos = null;
    private long lastActionTime = 0;
    private long breakStartTime = 0;
    private int prevSlot = -1;
    private static final NativeLibrary NATIVE = NativeLibrary.of("ravex_ecfarmer");
    static {
        NATIVE.load();
    }

    @Override
    protected void onEnable() {
        state = State.IDLE;
        ecPos = null;
        currentTarget = null;
        prevSlot = -1;
    }
    @Override
    protected void onDisable() {
        if (ecPos != null) {
            BlockState st = Minecraft.getInstance().level.getBlockState(ecPos);
            if (st.is(Blocks.ENDER_CHEST)) {
                Minecraft.getInstance().gameMode.stopDestroyBlock();
            }
        }
        if (prevSlot != -1) swapBack(Minecraft.getInstance(), prevSlot);
        ecPos = null;
        currentTarget = null;
        prevSlot = -1;
        state = State.IDLE;
    }
    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;
        long now = System.currentTimeMillis();
        switch (state) {
            case IDLE -> state = State.FIND_BREAK;
            case FIND_BREAK -> findBreakTarget(mc);
            case BREAKING -> doBreak(mc, now);
            case FIND_PLACE -> findPlaceTarget(mc);
            case PLACING -> doPlace(mc, now);
        }
    }
    private void findBreakTarget(Minecraft mc) {
        BlockPos found = scanForEC(mc);
        if (found != null) {
            ecPos = found;
            currentTarget = found;
            state = State.BREAKING;
            breakStartTime = 0;
            prevSlot = -1;
            return;
        }
        state = State.FIND_PLACE;
    }
    private void findPlaceTarget(Minecraft mc) {
        int ecSlot = findECSlot(mc);
        if (ecSlot == -1) return;
        BlockPos placeOn = findPlacePos(mc);
        if (placeOn == null) return;
        ecPos = placeOn.above();
        currentTarget = ecPos;
        state = State.PLACING;
    }
    private void doPlace(Minecraft mc, long now) {
        if (now - lastActionTime < 100) return;
        lastActionTime = now;
        if (ecPos == null || !mc.level.getBlockState(ecPos).isAir()) {
            state = State.IDLE;
            return;
        }
        int ecSlot = findECSlot(mc);
        if (ecSlot == -1) {
            state = State.IDLE;
            return;
        }
        int original = mc.player.getInventory().getSelectedSlot();
        if (!doSwap(mc, ecSlot)) {
            state = State.IDLE;
            return;
        }
        BlockPos below = ecPos.below();
        BlockHitResult hit = new BlockHitResult(
            Vec3.atCenterOf(below), Direction.UP, below, false
        );
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
        mc.player.swing(InteractionHand.MAIN_HAND);
        swapBack(mc, original);
        state = State.IDLE;
    }
    private void doBreak(Minecraft mc, long now) {
        if (ecPos == null || !mc.level.getBlockState(ecPos).is(Blocks.ENDER_CHEST)) {
            if (prevSlot != -1) swapBack(mc, prevSlot);
            ecPos = null;
            currentTarget = null;
            prevSlot = -1;
            state = State.IDLE;
            return;
        }
        if (breakStartTime == 0) {
            int pickSlot = findPickaxeSlot(mc);
            if (pickSlot == -1) {
                state = State.IDLE;
                return;
            }
            prevSlot = mc.player.getInventory().getSelectedSlot();
            if (!doSwap(mc, pickSlot)) {
                prevSlot = -1;
                state = State.IDLE;
                return;
            }
            breakStartTime = now;
            Direction dir = getDirection(mc.player.getEyePosition(), ecPos);
            mc.gameMode.startDestroyBlock(ecPos, dir);
            mc.player.swing(InteractionHand.MAIN_HAND);
            return;
        }
        Direction dir = getDirection(mc.player.getEyePosition(), ecPos);
        mc.gameMode.continueDestroyBlock(ecPos, dir);
        mc.player.swing(InteractionHand.MAIN_HAND);
        BlockState st = mc.level.getBlockState(ecPos);
        if (st.isAir() || !st.is(Blocks.ENDER_CHEST)) {
            if (prevSlot != -1) swapBack(mc, prevSlot);
            ecPos = null;
            currentTarget = null;
            prevSlot = -1;
            state = State.IDLE;
        }
    }
    private boolean doSwap(Minecraft mc, int targetSlot) {
        String mode = swapMode.getValue();
        if (mode.equals("Normal")) {
            mc.player.getInventory().setSelectedSlot(targetSlot);
            return true;
        } else if (mode.equals("Silent")) {
            if (mc.player.connection != null) {
                mc.player.connection.send(new ServerboundSetCarriedItemPacket(targetSlot));
                return true;
            }
            return false;
        }
        return false;
    }
    private void swapBack(Minecraft mc, int originalSlot) {
        if (originalSlot == -1) return;
        String mode = swapMode.getValue();
        if (mode.equals("Normal")) {
            mc.player.getInventory().setSelectedSlot(originalSlot);
        } else if (mode.equals("Silent")) {
            if (mc.player.connection != null) {
                mc.player.connection.send(new ServerboundSetCarriedItemPacket(originalSlot));
            }
        }
    }
    private BlockPos scanForEC(Minecraft mc) {
        double r = range.getValue();
        Vec3 eye = mc.player.getEyePosition();
        BlockPos pPos = mc.player.blockPosition();
        int minX = (int) Math.floor(pPos.getX() - r);
        int maxX = (int) Math.ceil(pPos.getX() + r);
        int minY = (int) Math.max(mc.level.getMinY(), Math.floor(pPos.getY() - r));
        int maxY = (int) Math.min(mc.level.getMaxY(), Math.ceil(pPos.getY() + r));
        int minZ = (int) Math.floor(pPos.getZ() - r);
        int maxZ = (int) Math.ceil(pPos.getZ() + r);
        BlockPos closest = null;
        double closestDist = Double.MAX_VALUE;
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState st = mc.level.getBlockState(pos);
                    if (st.is(Blocks.ENDER_CHEST)) {
                        double dist = pos.distToCenterSqr(eye);
                        if (dist < closestDist) {
                            closestDist = dist;
                            closest = pos;
                        }
                    }
                }
            }
        }
        return closest;
    }
    private int findECSlot(Minecraft mc) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).is(Items.ENDER_CHEST)) return i;
        }
        for (int i = 9; i < 36; i++) {
            if (mc.player.getInventory().getItem(i).is(Items.ENDER_CHEST)) {
                int free = findEmptyHotbarSlot(mc);
                if (free != -1) {
                    mc.player.getInventory().setSelectedSlot(free);
                    mc.gameMode.handleInventoryMouseClick(
                        mc.player.containerMenu.containerId, i, free,
                        net.minecraft.world.inventory.ClickType.SWAP, mc.player
                    );
                    return free;
                }
            }
        }
        return -1;
    }
    private int findEmptyHotbarSlot(Minecraft mc) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).isEmpty()) return i;
        }
        return -1;
    }
    private int findPickaxeSlot(Minecraft mc) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.is(Items.NETHERITE_PICKAXE) || stack.is(Items.DIAMOND_PICKAXE)
                || stack.is(Items.IRON_PICKAXE) || stack.is(Items.STONE_PICKAXE)
                || stack.is(Items.WOODEN_PICKAXE)) return i;
        }
        return -1;
    }
    private BlockPos findPlacePos(Minecraft mc) {
        Vec3 eye = mc.player.getEyePosition();
        Direction facing = mc.player.getDirection();
        double r = range.getValue();
        BlockPos start = mc.player.blockPosition();
        for (int f = 1; f <= 3; f++) {
            for (int dy = -1; dy <= 1; dy++) {
                BlockPos pos = start.offset(facing.getStepX() * f, dy, facing.getStepZ() * f);
                if (pos.distToCenterSqr(eye.x, eye.y, eye.z) > r * r) continue;
                BlockState below = mc.level.getBlockState(pos.below());
                BlockState target = mc.level.getBlockState(pos);
                if (below.isCollisionShapeFullBlock(mc.level, pos.below()) && target.isAir()) {
                    return pos.below();
                }
            }
        }
        return null;
    }
    public static Direction getDirection(Vec3 eye, BlockPos pos) {
        Vec3 center = Vec3.atCenterOf(pos);
        double dx = eye.x - center.x;
        double dy = eye.y - pos.getY() - 0.5;
        double dz = eye.z - center.z;
        double absX = Math.abs(dx);
        double absY = Math.abs(dy);
        double absZ = Math.abs(dz);
        if (absY <= absX && absY <= absZ) {
            if (absX >= absZ) return dx > 0 ? Direction.EAST : Direction.WEST;
            else return dz > 0 ? Direction.SOUTH : Direction.NORTH;
        } else if (absX <= absY && absX <= absZ) {
            if (absY >= absZ) return dy > 0 ? Direction.DOWN : Direction.UP;
            else return dz > 0 ? Direction.SOUTH : Direction.NORTH;
        } else {
            if (absY >= absX) return dy > 0 ? Direction.DOWN : Direction.UP;
            else return dx > 0 ? Direction.EAST : Direction.WEST;
        }
    }
    private static native double nativeCalcBreakTime(String toolId, int efficiency, int haste, int durability, int maxDura);
    private static native int nativeCalcDurabilityLoss(String toolId, int efficiency);
}
