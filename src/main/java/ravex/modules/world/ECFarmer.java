package ravex.modules.world;
import net.minecraft.client.Minecraft;
<<<<<<< HEAD
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
=======
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
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import ravex.utility.nativelib.NativeLibrary;
<<<<<<< HEAD
import ravex.manager.ModuleManager;
import ravex.utility.player.InventoryUtility;
import ravex.utility.misc.block.BlockUtility;
import java.util.List;
public class ECFarmer extends Module {
=======
import java.util.List;
public class ECFarmer extends Module {
    public static final ECFarmer INSTANCE = new ECFarmer();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    public final NumberParameter range = new NumberParameter("Range", 4.5, 1.0, 6.0, 0.5);
    public final ModeParameter swapMode = new ModeParameter("Swap", "Silent", List.of("Silent", "Normal"));
    public final BooleanParameter render = new BooleanParameter("Render", true);
    public final ColorParameter color = new ColorParameter("Color", 0x3F8800FF);
<<<<<<< HEAD
    private enum State { IDLE, FIND_BREAK, BREAKING, FIND_PLACE, PLACING }
    private State state = State.IDLE;
    private int ecX, ecY, ecZ;
    private boolean hasEc;
    private long lastActionTime = 0;
    private long breakStartTime = 0;
    private int prevSlot = -1;
    private static int targetX, targetY, targetZ;
    private static boolean hasRenderTarget;
=======
    public static BlockPos currentTarget = null;
    private enum State { IDLE, FIND_BREAK, BREAKING, FIND_PLACE, PLACING }
    private State state = State.IDLE;
    private BlockPos ecPos = null;
    private long lastActionTime = 0;
    private long breakStartTime = 0;
    private int prevSlot = -1;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    private static final NativeLibrary NATIVE = NativeLibrary.of("ravex_ecfarmer");
    static {
        NATIVE.load();
    }

<<<<<<< HEAD
    public static net.minecraft.core.BlockPos getCurrentTarget() {
        if (!hasRenderTarget) return null;
        return BlockUtility.pos(targetX, targetY, targetZ);
    }

    @Override
    protected void onEnable() {
        state = State.IDLE;
        hasEc = false;
        hasRenderTarget = false;
=======
    @Override
    protected void onEnable() {
        state = State.IDLE;
        ecPos = null;
        currentTarget = null;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        prevSlot = -1;
    }
    @Override
    protected void onDisable() {
<<<<<<< HEAD
        if (hasEc) {
            var st = BlockUtility.getState(Minecraft.getInstance().level, ecX, ecY, ecZ);
            if (BlockUtility.isBlock(st, "ender_chest")) {
=======
        if (ecPos != null) {
            BlockState st = Minecraft.getInstance().level.getBlockState(ecPos);
            if (st.is(Blocks.ENDER_CHEST)) {
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
                Minecraft.getInstance().gameMode.stopDestroyBlock();
            }
        }
        if (prevSlot != -1) swapBack(Minecraft.getInstance(), prevSlot);
<<<<<<< HEAD
        hasEc = false;
        hasRenderTarget = false;
=======
        ecPos = null;
        currentTarget = null;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
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
<<<<<<< HEAD
        int[] found = scanForEC(mc);
        if (found != null) {
            ecX = found[0]; ecY = found[1]; ecZ = found[2];
            hasEc = true;
            targetX = ecX; targetY = ecY; targetZ = ecZ; hasRenderTarget = true;
=======
        BlockPos found = scanForEC(mc);
        if (found != null) {
            ecPos = found;
            currentTarget = found;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
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
<<<<<<< HEAD
        int[] placeOn = findPlacePos(mc);
        if (placeOn == null) return;
        ecX = placeOn[0]; ecY = placeOn[1] + 1; ecZ = placeOn[2];
        hasEc = true;
        targetX = ecX; targetY = ecY; targetZ = ecZ; hasRenderTarget = true;
=======
        BlockPos placeOn = findPlacePos(mc);
        if (placeOn == null) return;
        ecPos = placeOn.above();
        currentTarget = ecPos;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        state = State.PLACING;
    }
    private void doPlace(Minecraft mc, long now) {
        if (now - lastActionTime < 100) return;
        lastActionTime = now;
<<<<<<< HEAD
        if (!hasEc || !BlockUtility.isAir(mc.level, ecX, ecY, ecZ)) {
=======
        if (ecPos == null || !mc.level.getBlockState(ecPos).isAir()) {
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            state = State.IDLE;
            return;
        }
        int ecSlot = findECSlot(mc);
        if (ecSlot == -1) {
            state = State.IDLE;
            return;
        }
<<<<<<< HEAD
        int original = InventoryUtility.getSelectedSlot(mc.player);
=======
        int original = mc.player.getInventory().getSelectedSlot();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        if (!doSwap(mc, ecSlot)) {
            state = State.IDLE;
            return;
        }
<<<<<<< HEAD
        var below = BlockUtility.pos(ecX, ecY - 1, ecZ);
        BlockUtility.useItemOn(mc, new net.minecraft.world.phys.BlockHitResult(
            Vec3.atCenterOf(below), Direction.UP, below, false));
        BlockUtility.swing(mc);
=======
        BlockPos below = ecPos.below();
        BlockHitResult hit = new BlockHitResult(
            Vec3.atCenterOf(below), Direction.UP, below, false
        );
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
        mc.player.swing(InteractionHand.MAIN_HAND);
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        swapBack(mc, original);
        state = State.IDLE;
    }
    private void doBreak(Minecraft mc, long now) {
<<<<<<< HEAD
        if (!hasEc) {
            state = State.IDLE;
            return;
        }
        var cur = BlockUtility.getState(mc.level, ecX, ecY, ecZ);
        if (!BlockUtility.isBlock(cur, "ender_chest")) {
            if (prevSlot != -1) swapBack(mc, prevSlot);
            hasEc = false;
            hasRenderTarget = false;
=======
        if (ecPos == null || !mc.level.getBlockState(ecPos).is(Blocks.ENDER_CHEST)) {
            if (prevSlot != -1) swapBack(mc, prevSlot);
            ecPos = null;
            currentTarget = null;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
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
<<<<<<< HEAD
            prevSlot = InventoryUtility.getSelectedSlot(mc.player);
=======
            prevSlot = mc.player.getInventory().getSelectedSlot();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            if (!doSwap(mc, pickSlot)) {
                prevSlot = -1;
                state = State.IDLE;
                return;
            }
            breakStartTime = now;
<<<<<<< HEAD
            var dir = getDirection(mc.player.getEyePosition(), ecX, ecY, ecZ);
            mc.gameMode.startDestroyBlock(BlockUtility.pos(ecX, ecY, ecZ), dir);
            BlockUtility.swing(mc);
            return;
        }
        var dir2 = getDirection(mc.player.getEyePosition(), ecX, ecY, ecZ);
        mc.gameMode.continueDestroyBlock(BlockUtility.pos(ecX, ecY, ecZ), dir2);
        BlockUtility.swing(mc);
        var st = BlockUtility.getState(mc.level, ecX, ecY, ecZ);
        if (st.isAir() || !BlockUtility.isBlock(st, "ender_chest")) {
            if (prevSlot != -1) swapBack(mc, prevSlot);
            hasEc = false;
            hasRenderTarget = false;
=======
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
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            prevSlot = -1;
            state = State.IDLE;
        }
    }
    private boolean doSwap(Minecraft mc, int targetSlot) {
        String mode = swapMode.getValue();
        if (mode.equals("Normal")) {
<<<<<<< HEAD
            InventoryUtility.selectSlot(mc.player, targetSlot);
            return true;
        } else if (mode.equals("Silent")) {
            InventoryUtility.silentSelectSlot(mc.player, targetSlot);
            return true;
=======
            mc.player.getInventory().setSelectedSlot(targetSlot);
            return true;
        } else if (mode.equals("Silent")) {
            if (mc.player.connection != null) {
                mc.player.connection.send(new ServerboundSetCarriedItemPacket(targetSlot));
                return true;
            }
            return false;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        }
        return false;
    }
    private void swapBack(Minecraft mc, int originalSlot) {
        if (originalSlot == -1) return;
        String mode = swapMode.getValue();
        if (mode.equals("Normal")) {
<<<<<<< HEAD
            InventoryUtility.selectSlot(mc.player, originalSlot);
        } else if (mode.equals("Silent")) {
            InventoryUtility.silentSelectSlot(mc.player, originalSlot);
        }
    }
    private int[] scanForEC(Minecraft mc) {
        double r = range.getValue();
        var eye = mc.player.getEyePosition();
        var pPos = mc.player.blockPosition();
=======
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
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        int minX = (int) Math.floor(pPos.getX() - r);
        int maxX = (int) Math.ceil(pPos.getX() + r);
        int minY = (int) Math.max(mc.level.getMinY(), Math.floor(pPos.getY() - r));
        int maxY = (int) Math.min(mc.level.getMaxY(), Math.ceil(pPos.getY() + r));
        int minZ = (int) Math.floor(pPos.getZ() - r);
        int maxZ = (int) Math.ceil(pPos.getZ() + r);
<<<<<<< HEAD
        int[] closest = null;
=======
        BlockPos closest = null;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        double closestDist = Double.MAX_VALUE;
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
<<<<<<< HEAD
                    var st = BlockUtility.getState(mc.level, x, y, z);
                    if (BlockUtility.isBlock(st, "ender_chest")) {
                        double dist = BlockUtility.distToSqr(mc.level, x, y, z, eye.x, eye.y, eye.z);
                        if (dist < closestDist) {
                            closestDist = dist;
                            closest = new int[]{x, y, z};
=======
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState st = mc.level.getBlockState(pos);
                    if (st.is(Blocks.ENDER_CHEST)) {
                        double dist = pos.distToCenterSqr(eye);
                        if (dist < closestDist) {
                            closestDist = dist;
                            closest = pos;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
                        }
                    }
                }
            }
        }
        return closest;
    }
    private int findECSlot(Minecraft mc) {
<<<<<<< HEAD
        int slot = InventoryUtility.findHotbarSlot(mc.player, "ender_chest");
        if (slot != -1) return slot;
        slot = InventoryUtility.findSlot(mc.player, "ender_chest", 9, 36);
        if (slot != -1) {
            int free = InventoryUtility.findEmptyHotbarSlot(mc.player);
            if (free != -1) {
                InventoryUtility.selectSlot(mc.player, free);
                InventoryUtility.handleInventoryClick(mc, mc.player, slot, free, net.minecraft.world.inventory.ClickType.SWAP);
                return free;
=======
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
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            }
        }
        return -1;
    }
<<<<<<< HEAD
    private int findPickaxeSlot(Minecraft mc) {
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(mc.player, i);
            if (InventoryUtility.isItem(stack, "netherite_pickaxe") || InventoryUtility.isItem(stack, "diamond_pickaxe")
                || InventoryUtility.isItem(stack, "iron_pickaxe") || InventoryUtility.isItem(stack, "stone_pickaxe")
                || InventoryUtility.isItem(stack, "wooden_pickaxe")) return i;
        }
        return -1;
    }
    private int[] findPlacePos(Minecraft mc) {
        var eye = mc.player.getEyePosition();
        var facing = mc.player.getDirection();
        double r = range.getValue();
        var start = mc.player.blockPosition();
        int sx = start.getX(), sy = start.getY(), sz = start.getZ();
        for (int f = 1; f <= 3; f++) {
            for (int dy = -1; dy <= 1; dy++) {
                int px = sx + facing.getStepX() * f;
                int py = sy + dy;
                int pz = sz + facing.getStepZ() * f;
                if (BlockUtility.distToSqr(mc.level, px, py, pz, eye.x, eye.y, eye.z) > r * r) continue;
                if (BlockUtility.isSolid(mc.level, px, py - 1, pz) && BlockUtility.isAir(mc.level, px, py, pz)) {
                    return new int[]{px, py - 1, pz};
=======
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
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
                }
            }
        }
        return null;
    }
<<<<<<< HEAD
    private static Direction getDirection(Vec3 eye, int x, int y, int z) {
        var center = Vec3.atCenterOf(BlockUtility.pos(x, y, z));
        double dx = eye.x - center.x;
        double dy = eye.y - y - 0.5;
=======
    public static Direction getDirection(Vec3 eye, BlockPos pos) {
        Vec3 center = Vec3.atCenterOf(pos);
        double dx = eye.x - center.x;
        double dy = eye.y - pos.getY() - 0.5;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
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
<<<<<<< HEAD

    public static boolean maybeEnabled() {
        return maybeEnabled(ECFarmer.class);
    }
    public static ECFarmer itz() {
        return ModuleManager.get(ECFarmer.class);
    }
=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
}
