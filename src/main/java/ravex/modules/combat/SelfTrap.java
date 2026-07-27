package ravex.modules.combat;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import ravex.parameter.ActionParameter;
import ravex.RaveX;
import ravex.utility.misc.block.BlockUtility;
import ravex.utility.misc.PhysicUtility;
import ravex.utility.nativelib.NativeLibraryUtility;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.rotation.RotationUtility;
import ravex.utility.player.rotation.SilentRotationUtility;
import ravex.utility.player.SwingUtility;
import ravex.mcwrapper.MinecraftWrapper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import ravex.mcwrapper.MinecraftWrapper;





@ModuleInfo(name = "SelfTrap", category = "Combat")
public class SelfTrap implements ModuleAccess {
public static final SelfTrap INSTANCE = new SelfTrap();
    public final ActionParameter blocks = new ActionParameter("net.minecraft.world.level.block.Blocks", () -> {
        MinecraftWrapper.getWrapper().setScreen(new ravex.gui.browser.BlockBrowserScreen(
            MinecraftWrapper.getWrapper().getCurrentScreen(),
            ravex.manager.ModuleManager.delegate(ravex.modules.combat.SelfTrap.class)::isBlockSelected,
            ravex.manager.ModuleManager.delegate(ravex.modules.combat.SelfTrap.class)::setBlockSelected
        ));
    });
    @Parameter(name = "Mode", modes = {"Full", "Simple", "Roof"})
    public String mode = "Full";
    @Parameter(name = "Speed", modes = {"Legit", "Normal", "Aggressive"})
    public String speedMode = "Normal";
    @Parameter(name = "MaxRate", min = 1.0, max = 5.0, step = 1.0)
    public double maxRate = 2.0;
    @Parameter(name = "Delay", min = 0.0, max = 1000.0, step = 10.0)
    public double placeDelay = 100.0;
    @Parameter(name = "Rotate", modes = {"Silent", "Normal", "None"})
    public String rotate = "Silent";
    @Parameter(name = "StrictRotation")
    public boolean strictRotation = false;
    @Parameter(name = "Swap", modes = {"Silent", "Normal", "None"})
    public String swapMode = "Silent";
    @Parameter(name = "SwitchBack")
    public boolean swapSwitchBack = true;
    @Parameter(name = "SwapInv")
    public boolean swapInventory = true;
    @Parameter(name = "AutoDisable")
    public boolean autoDisable = true;
    @Parameter(name = "Render")
    public boolean render = true;
    @Parameter(name = "Color", color = true)
    public int color = 0x3F00DDFF;
    private final Set<Identifier> selectedBlocks = new HashSet<>();
    private static final List<net.minecraft.core.BlockPos> selfTrapBlocks = new ArrayList<>();
    private long lastPlaceTime = 0;
    private static final SilentRotationUtility silentRotation = new SilentRotationUtility();
    private static final NativeLibraryUtility NATIVE = NativeLibraryUtility.of("ravex_selftrap");
    static {
        NATIVE.load();
    }
    private SelfTrap() {
        
    }
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("SelfTrap").getEnabled();
    }
    public static SelfTrap itz() {
        return ravex.manager.ModuleManager.delegate(SelfTrap.class);
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
    public static List<net.minecraft.core.BlockPos> getSelfTrapBlocks() {
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
    public void onEnable() {
        lastPlaceTime = 0;
        silentRotation.hasRotation = false;
        synchronized (selfTrapBlocks) {
            selfTrapBlocks.clear();
        }
        if (selectedBlocks.isEmpty()) {
            selectedBlocks.add(BuiltInRegistries.BLOCK.getKey(net.minecraft.world.level.block.Blocks.OBSIDIAN));
        }
    }
    public void onDisable() {
        silentRotation.hasRotation = false;
        synchronized (selfTrapBlocks) {
            selfTrapBlocks.clear();
        }
    }
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getLevel() == null || mc.getGameMode() == null) return;
        silentRotation.hasRotation = false;
        double[] solidBlockData = collectSolidBlocks(mc);
        List<Double> activeSolidBlocks = new ArrayList<>();
        for (double d : solidBlockData) {
            activeSolidBlocks.add(d);
        }
        int modeVal = 0;
        String mStr = mode;
        if ("Simple".equals(mStr)) modeVal = 1;
        else if ("Roof".equals(mStr)) modeVal = 2;
        int simLimit = 9;
        int simCount = 0;
        List<net.minecraft.core.BlockPos> simulatedBlocks = new ArrayList<>();
        while (simCount < simLimit) {
            double[] currentSolidData = new double[activeSolidBlocks.size()];
            for (int i = 0; i < currentSolidData.length; i++) {
                currentSolidData[i] = activeSolidBlocks.get(i);
            }
            double[] result;
            if (NATIVE.isLoaded()) {
                result = nativeCalculateSelfTrap(
                    mc.getPlayer().getX(), mc.getPlayer().getY(), mc.getPlayer().getZ(),
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
            net.minecraft.core.BlockPos targetBlock = new net.minecraft.core.BlockPos((int) result[5], (int) result[6], (int) result[7]);
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
        boolean checkDelay = !speedMode.equals("Aggressive");
        if (checkDelay && now - lastPlaceTime < (long) placeDelay) {
            return;
        }
        int blockSlot = findBlockSlot(mc);
        if (blockSlot == -1) return;
        activeSolidBlocks.clear();
        for (double d : solidBlockData) {
            activeSolidBlocks.add(d);
        }
        int limit = (int) maxRate;
        if (speedMode.equals("Legit")) {
            limit = 1;
        }
        int actionsThisTick = 0;
        int originalSlot = InventoryUtility.getSelectedSlot(mc.getPlayer());
        boolean placedAny = false;
        while (actionsThisTick < limit) {
            double[] currentSolidData = new double[activeSolidBlocks.size()];
            for (int i = 0; i < currentSolidData.length; i++) {
                currentSolidData[i] = activeSolidBlocks.get(i);
            }
            double[] result;
            if (NATIVE.isLoaded()) {
                result = nativeCalculateSelfTrap(
                    mc.getPlayer().getX(), mc.getPlayer().getY(), mc.getPlayer().getZ(),
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
            net.minecraft.core.BlockPos neighborPos = new net.minecraft.core.BlockPos((int) result[1], (int) result[2], (int) result[3]);
            net.minecraft.core.Direction face = net.minecraft.core.Direction.values()[(int) result[4]];
            net.minecraft.core.BlockPos targetBlock = new net.minecraft.core.BlockPos((int) result[5], (int) result[6], (int) result[7]);
            net.minecraft.world.phys.Vec3 hitVec = net.minecraft.world.phys.Vec3.atCenterOf(neighborPos).add(new net.minecraft.world.phys.Vec3(face.getStepX(), face.getStepY(), face.getStepZ()).scale(0.5));
            rotateTo(mc, hitVec);
            boolean isStrict = strictRotation || speedMode.equals("Legit");
            if (isStrict && !isRotationAligned(mc, hitVec)) {
                break;
            }
            String swap = swapMode;
            if (swap.equals("Normal")) {
                InventoryUtility.selectSlot(mc.getPlayer(), blockSlot);
            } else if (swap.equals("Silent")) {
                InventoryUtility.silentSelectSlot(mc.getPlayer(), blockSlot);
            } else if (swap.equals("None")) {
                if (InventoryUtility.getSelectedSlot(mc.getPlayer()) != blockSlot) {
                    break;
                }
            }
            net.minecraft.world.phys.BlockHitResult hitResult = new net.minecraft.world.phys.BlockHitResult(hitVec, face, neighborPos, false);
            mc.getGameMode().useItemOn(mc.getPlayer(), net.minecraft.world.InteractionHand.MAIN_HAND, hitResult);
            SwingUtility.swing(mc.getPlayer(), net.minecraft.world.InteractionHand.MAIN_HAND);
            placedAny = true;
            actionsThisTick++;
            activeSolidBlocks.add((double) targetBlock.getX());
            activeSolidBlocks.add((double) targetBlock.getY());
            activeSolidBlocks.add((double) targetBlock.getZ());
        }
        if (placedAny && swapMode.equals("Silent") && swapSwitchBack && originalSlot != -1) {
            InventoryUtility.silentSelectSlot(mc.getPlayer(), originalSlot);
        }
        if (placedAny) {
            lastPlaceTime = now;
        } else {
            if (autoDisable && simulatedBlocks.isEmpty()) {
                ravex.manager.ModuleManager.INSTANCE.getByName("SelfTrap").setEnabled(false);
            }
        }
    }
    private void rotateTo(MinecraftWrapper mc, net.minecraft.world.phys.Vec3 target) {
        if (rotate.equals("None")) return;
        float[] angles = RotationUtility.anglesTo(mc.getPlayer(), target);
        if (rotate.equals("Normal")) {
            mc.getPlayer().setYRot(angles[0]);
            mc.getPlayer().setXRot(angles[1]);
        } else if (rotate.equals("Silent")) {
            silentRotation.set(angles[0], angles[1]);
        }
    }
    private boolean isRotationAligned(MinecraftWrapper mc, net.minecraft.world.phys.Vec3 target) {
        if (rotate.equals("None")) return true;
        return silentRotation.isRotationAligned(mc, target, 12.0F);
    }
    private double[] collectSolidBlocks(MinecraftWrapper mc) {
        List<Double> data = new ArrayList<>();
        net.minecraft.core.BlockPos playerPos = mc.getPlayer().blockPosition();
        int rx = 3;
        int ry = 3;
        int rz = 3;
        for (int dx = -rx; dx <= rx; dx++) {
            for (int dy = -ry; dy <= ry; dy++) {
                for (int dz = -rz; dz <= rz; dz++) {
                    net.minecraft.core.BlockPos pos = playerPos.offset(dx, dy, dz);
                    if (mc.getLevel().isLoaded(pos)) {
                        net.minecraft.world.level.block.state.BlockState state = mc.getLevel().getBlockState(pos);
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
    private int findBlockSlot(MinecraftWrapper mc) {
        if (selectedBlocks.isEmpty()) return -1;
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(mc.getPlayer(), i);
            if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)) continue;
            Identifier id = BuiltInRegistries.BLOCK.getKey(blockItem.getBlock());
            if (selectedBlocks.contains(id)) {
                return i;
            }
        }
        if (swapInventory) {
            for (int i = 9; i < 36; i++) {
                var stack = InventoryUtility.getItem(mc.getPlayer(), i);
                if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)) continue;
                Identifier id = BuiltInRegistries.BLOCK.getKey(blockItem.getBlock());
                if (selectedBlocks.contains(id)) {
                    int hotbarSlot = InventoryUtility.getSelectedSlot(mc.getPlayer());
                    InventoryUtility.handleInventoryClick(mc, mc.getPlayer(), i, hotbarSlot, net.minecraft.world.inventory.ClickType.SWAP);
                    return hotbarSlot;
                }
            }
        }
        return -1;
    }
    private double[] javaFallbackCalculate(MinecraftWrapper mc, double[] solidBlocksData, int modeVal) {
        net.minecraft.core.BlockPos pf = mc.getPlayer().blockPosition();
        List<net.minecraft.core.BlockPos> candidates = new ArrayList<>();
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
        Set<net.minecraft.core.BlockPos> solids = new HashSet<>();
        for (int i = 0; i + 2 < solidBlocksData.length; i += 3) {
            solids.add(new net.minecraft.core.BlockPos((int)solidBlocksData[i], (int)solidBlocksData[i+1], (int)solidBlocksData[i+2]));
        }
        solids.remove(pf);
        solids.remove(pf.above());
        for (net.minecraft.core.BlockPos c : candidates) {
            if (solids.contains(c)) continue;
            for (net.minecraft.core.Direction face : net.minecraft.core.Direction.values()) {
                net.minecraft.core.BlockPos n = c.relative(face);
                if (solids.contains(n)) {
                    return new double[]{1.0, n.getX(), n.getY(), n.getZ(), face.ordinal(), c.getX(), c.getY(), c.getZ()};
                }
            }
        }
        for (net.minecraft.core.BlockPos c : candidates) {
            if (solids.contains(c)) continue;
            net.minecraft.core.BlockPos support = c.below();
            if (solids.contains(support)) continue;
            for (net.minecraft.core.Direction face : net.minecraft.core.Direction.values()) {
                net.minecraft.core.BlockPos n = support.relative(face);
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