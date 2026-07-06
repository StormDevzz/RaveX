package ravex.modules.combat;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import ravex.RaveX;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.ActionParameter;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import ravex.utility.nativelib.NativeLibrary;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.rotation.RotationUtility;
import ravex.utility.player.rotation.SilentRotation;
public class SelfTrap extends Module {
    public static final SelfTrap INSTANCE = new SelfTrap(); //Xiaomii
    public final ActionParameter blocks = new ActionParameter("Blocks", () -> {
        Minecraft.getInstance().setScreen(new ravex.gui.blockbrowser.BlockBrowserScreen(
            Minecraft.getInstance().screen,
            SelfTrap.INSTANCE::isBlockSelected,
            SelfTrap.INSTANCE::setBlockSelected
        ));
    });
    public final ModeParameter mode = new ModeParameter("Mode", "Full", List.of("Full", "Simple", "Roof"));
    public final ModeParameter speedMode = new ModeParameter("Speed", "Normal", List.of("Legit", "Normal", "Aggressive"));
    public final NumberParameter maxRate = new NumberParameter("MaxRate", 2.0, 1.0, 5.0, 1.0);
    public final NumberParameter placeDelay = new NumberParameter("Delay", 100.0, 0.0, 1000.0, 10.0);
    public final ModeParameter rotate = new ModeParameter("Rotate", "Silent", List.of("Silent", "Normal", "None"));
    public final BooleanParameter strictRotation = new BooleanParameter("StrictRotation", false);
    public final ModeParameter swapMode = new ModeParameter("Swap", "Silent", List.of("Silent", "Normal", "None"));
    public final BooleanParameter swapSwitchBack = new BooleanParameter("SwitchBack", true);
    public final BooleanParameter swapInventory = new BooleanParameter("SwapInv", true);
    public final BooleanParameter autoDisable = new BooleanParameter("AutoDisable", true);
    public final BooleanParameter render = new BooleanParameter("Render", true);
    public final ColorParameter color = new ColorParameter("Color", 0x3F00DDFF);
    private final Set<Identifier> selectedBlocks = new HashSet<>();
    private static final List<BlockPos> selfTrapBlocks = new ArrayList<>();
    private long lastPlaceTime = 0;
    private static final SilentRotation silentRotation = new SilentRotation();
    private static final NativeLibrary NATIVE = NativeLibrary.of("ravex_selftrap");
    static {
        NATIVE.load();
    }
    private SelfTrap() {
        super("SelfTrap");
        maxRate.setVisible(() -> !speedMode.getValue().equals("Legit"));
        strictRotation.setVisible(() -> !rotate.getValue().equals("None"));
        swapSwitchBack.setVisible(() -> !swapMode.getValue().equals("None"));
        swapInventory.setVisible(() -> !swapMode.getValue().equals("None"));
    }
    public static boolean hasSilentRotations() {
        return silentRotation.hasRotation;
    }
    public static float getSilentYaw() {
        return silentRotation.yaw;
    }
    public static float getSilentPitch() {
        return silentRotation.pitch;
    }
    public static List<BlockPos> getSelfTrapBlocks() {
        synchronized (selfTrapBlocks) {
            return new ArrayList<>(selfTrapBlocks);
        }
    }
    public boolean isBlockSelected(Identifier id) {
        return selectedBlocks.contains(id);
    }
    public void setBlockSelected(Block block, boolean selected) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        if (selected) {
            selectedBlocks.add(id);
        } else {
            selectedBlocks.remove(id);
        }
    }
    @Override
    protected void onEnable() {
        lastPlaceTime = 0;
        silentRotation.hasRotation = false;
        synchronized (selfTrapBlocks) {
            selfTrapBlocks.clear();
        }
        if (selectedBlocks.isEmpty()) {
            selectedBlocks.add(BuiltInRegistries.BLOCK.getKey(Blocks.OBSIDIAN));
        }
    }
    @Override
    protected void onDisable() {
        silentRotation.hasRotation = false;
        synchronized (selfTrapBlocks) {
            selfTrapBlocks.clear();
        }
    }
    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;
        silentRotation.hasRotation = false;
        double[] solidBlockData = collectSolidBlocks(mc);
        List<Double> activeSolidBlocks = new ArrayList<>();
        for (double d : solidBlockData) {
            activeSolidBlocks.add(d);
        }
        int modeVal = 0;
        String mStr = mode.getValue();
        if ("Simple".equals(mStr)) modeVal = 1;
        else if ("Roof".equals(mStr)) modeVal = 2;
        int simLimit = 9;
        int simCount = 0;
        List<BlockPos> simulatedBlocks = new ArrayList<>();
        while (simCount < simLimit) {
            double[] currentSolidData = new double[activeSolidBlocks.size()];
            for (int i = 0; i < currentSolidData.length; i++) {
                currentSolidData[i] = activeSolidBlocks.get(i);
            }
            double[] result;
            if (NATIVE.isLoaded()) {
                result = nativeCalculateSelfTrap(
                    mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                    currentSolidData,
                    4.0, 
                    modeVal
                );
            } else {
                result = javaFallbackCalculate(mc, currentSolidData, modeVal);
            }
            if (result == null || result[0] < 0.5) {
                break;
            }
            BlockPos targetBlock = new BlockPos((int) result[5], (int) result[6], (int) result[7]);
            simulatedBlocks.add(targetBlock);
            simCount++;
            activeSolidBlocks.add((double) targetBlock.getX());
            activeSolidBlocks.add((double) targetBlock.getY());
            activeSolidBlocks.add((double) targetBlock.getZ());
        }
        synchronized (selfTrapBlocks) {
            selfTrapBlocks.clear();
            selfTrapBlocks.addAll(simulatedBlocks);
        }
        long now = System.currentTimeMillis();
        boolean checkDelay = !speedMode.getValue().equals("Aggressive");
        if (checkDelay && now - lastPlaceTime < placeDelay.getValue().longValue()) {
            return;
        }
        int blockSlot = findBlockSlot(mc);
        if (blockSlot == -1) return;
        activeSolidBlocks.clear();
        for (double d : solidBlockData) {
            activeSolidBlocks.add(d);
        }
        int limit = maxRate.getValue().intValue();
        if (speedMode.getValue().equals("Legit")) {
            limit = 1;
        }
        int actionsThisTick = 0;
        int originalSlot = InventoryUtility.getSelectedSlot(mc.player);
        boolean placedAny = false;
        while (actionsThisTick < limit) {
            double[] currentSolidData = new double[activeSolidBlocks.size()];
            for (int i = 0; i < currentSolidData.length; i++) {
                currentSolidData[i] = activeSolidBlocks.get(i);
            }
            double[] result;
            if (NATIVE.isLoaded()) {
                result = nativeCalculateSelfTrap(
                    mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                    currentSolidData,
                    4.0,
                    modeVal
                );
            } else {
                result = javaFallbackCalculate(mc, currentSolidData, modeVal);
            }
            if (result == null || result[0] < 0.5) {
                break;
            }
            BlockPos neighborPos = new BlockPos((int) result[1], (int) result[2], (int) result[3]);
            Direction face = Direction.values()[(int) result[4]];
            BlockPos targetBlock = new BlockPos((int) result[5], (int) result[6], (int) result[7]);
            Vec3 hitVec = Vec3.atCenterOf(neighborPos).add(new Vec3(face.getStepX(), face.getStepY(), face.getStepZ()).scale(0.5));
            rotateTo(mc, hitVec);
            boolean isStrict = strictRotation.getValue() || speedMode.getValue().equals("Legit");
            if (isStrict && !isRotationAligned(mc, hitVec)) {
                break;
            }
            String swap = swapMode.getValue();
            if (swap.equals("Normal")) {
                InventoryUtility.selectSlot(mc.player, blockSlot);
            } else if (swap.equals("Silent")) {
                InventoryUtility.silentSelectSlot(mc.player, blockSlot);
            } else if (swap.equals("None")) {
                if (InventoryUtility.getSelectedSlot(mc.player) != blockSlot) {
                    break;
                }
            }
            BlockHitResult hitResult = new BlockHitResult(hitVec, face, neighborPos, false);
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hitResult);
            mc.player.swing(InteractionHand.MAIN_HAND);
            placedAny = true;
            actionsThisTick++;
            activeSolidBlocks.add((double) targetBlock.getX());
            activeSolidBlocks.add((double) targetBlock.getY());
            activeSolidBlocks.add((double) targetBlock.getZ());
        }
        if (placedAny && swapMode.getValue().equals("Silent") && swapSwitchBack.getValue() && originalSlot != -1) {
            InventoryUtility.silentSelectSlot(mc.player, originalSlot);
        }
        if (placedAny) {
            lastPlaceTime = now;
        } else {
            if (autoDisable.getValue() && simulatedBlocks.isEmpty()) {
                setEnabled(false);
            }
        }
    }
    private void rotateTo(Minecraft mc, Vec3 target) {
        if (rotate.getValue().equals("None")) return;
        float[] angles = RotationUtility.anglesTo(mc.player, target);
        if (rotate.getValue().equals("Normal")) {
            mc.player.setYRot(angles[0]);
            mc.player.setXRot(angles[1]);
        } else if (rotate.getValue().equals("Silent")) {
            silentRotation.set(angles[0], angles[1]);
        }
    }
    private boolean isRotationAligned(Minecraft mc, Vec3 target) {
        if (rotate.getValue().equals("None")) return true;
        return silentRotation.isRotationAligned(mc, target, 12.0F);
    }
    private double[] collectSolidBlocks(Minecraft mc) {
        List<Double> data = new ArrayList<>();
        BlockPos playerPos = mc.player.blockPosition();
        int rx = 3;
        int ry = 3;
        int rz = 3;
        for (int dx = -rx; dx <= rx; dx++) {
            for (int dy = -ry; dy <= ry; dy++) {
                for (int dz = -rz; dz <= rz; dz++) {
                    BlockPos pos = playerPos.offset(dx, dy, dz);
                    if (mc.level.isLoaded(pos)) {
                        BlockState state = mc.level.getBlockState(pos);
                        if (!state.isAir() && !state.liquid()) {
                            data.add((double) pos.getX());
                            data.add((double) pos.getY());
                            data.add((double) pos.getZ());
                        }
                    }
                }
            }
        }
        double[] arr = new double[data.size()];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = data.get(i);
        }
        return arr;
    }
    private int findBlockSlot(Minecraft mc) {
        if (selectedBlocks.isEmpty()) return -1;
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(mc.player, i);
            if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)) continue;
            Identifier id = BuiltInRegistries.BLOCK.getKey(blockItem.getBlock());
            if (selectedBlocks.contains(id)) {
                return i;
            }
        }
        if (swapInventory.getValue()) {
            for (int i = 9; i < 36; i++) {
                var stack = InventoryUtility.getItem(mc.player, i);
                if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)) continue;
                Identifier id = BuiltInRegistries.BLOCK.getKey(blockItem.getBlock());
                if (selectedBlocks.contains(id)) {
                    int hotbarSlot = InventoryUtility.getSelectedSlot(mc.player);
                    InventoryUtility.handleInventoryClick(mc, mc.player, i, hotbarSlot, net.minecraft.world.inventory.ClickType.SWAP);
                    return hotbarSlot;
                }
            }
        }
        return -1;
    }
    private double[] javaFallbackCalculate(Minecraft mc, double[] solidBlocksData, int modeVal) {
        BlockPos pf = mc.player.blockPosition();
        List<BlockPos> candidates = new ArrayList<>();
        if (modeVal == 0 || modeVal == 1) {
            candidates.add(pf.north());
            candidates.add(pf.south());
            candidates.add(pf.east());
            candidates.add(pf.west());
            candidates.add(pf.north().above());
            candidates.add(pf.south().above());
            candidates.add(pf.east().above());
            candidates.add(pf.west().above());
        }
        if (modeVal == 0 || modeVal == 2) {
            candidates.add(pf.above(2));
        }
        Set<BlockPos> solids = new HashSet<>();
        for (int i = 0; i + 2 < solidBlocksData.length; i += 3) {
            solids.add(new BlockPos((int)solidBlocksData[i], (int)solidBlocksData[i+1], (int)solidBlocksData[i+2]));
        }
        solids.remove(pf);
        solids.remove(pf.above());
        for (BlockPos c : candidates) {
            if (solids.contains(c)) continue;
            for (Direction face : Direction.values()) {
                BlockPos n = c.relative(face);
                if (solids.contains(n)) {
                    return new double[]{1.0, n.getX(), n.getY(), n.getZ(), face.ordinal(), c.getX(), c.getY(), c.getZ()};
                }
            }
        }
        for (BlockPos c : candidates) {
            if (solids.contains(c)) continue;
            BlockPos support = c.below();
            if (solids.contains(support)) continue;
            for (Direction face : Direction.values()) {
                BlockPos n = support.relative(face);
                if (solids.contains(n)) {
                    return new double[]{1.0, n.getX(), n.getY(), n.getZ(), face.ordinal(), support.getX(), support.getY(), support.getZ()};
                }
            }
        }
        return new double[]{0.0, 0, 0, 0, 0, 0, 0, 0};
    }
    private static native double[] nativeCalculateSelfTrap(
        double playerX, double playerY, double playerZ,
        double[] solidBlockData,
        double range,
        int mode
    );
}
