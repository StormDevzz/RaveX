package ravex.modules.world;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.block.BlockUtility;
import ravex.utility.misc.EntityUtility;
import ravex.utility.misc.MobUtility;
import ravex.utility.misc.PhysicUtility;
import ravex.utility.player.InventoryUtility;
import ravex.utility.player.SwingUtility;
import net.minecraft.client.gui.screens.inventory.BlastFurnaceScreen;
import net.minecraft.client.gui.screens.inventory.BrewingStandScreen;
import net.minecraft.client.gui.screens.inventory.FurnaceScreen;
import net.minecraft.client.gui.screens.inventory.SmokerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.BrewingStandBlock;
import net.minecraft.world.phys.AABB;
import java.util.List;
import ravex.mcwrapper.MinecraftWrapper;
import org.jetbrains.annotations.Nullable;





@Module(name = "PVEUtils", category = "World")
public class PVEUtils {
    @Parameter(name = "Mode", modes = {"AutoSmelt", "AutoTame", "BoneMeal", "AutoBrew", "AutoLight"})
    public String mode = "AutoSmelt";
    @Parameter(name = "Range", min = 2.0, max = 6.0, step = 0.1)
    public double range = 4.5;
    @Parameter(name = "AutoFuel")
    public boolean autoFuel = true;
    @Parameter(name = "SmeltRender")
    public boolean smeltRender = true;
    @Parameter(name = "SmeltColor", color = true)
    public int smeltColor = 0x3FFF8800;
    @Parameter(name = "BrewRender")
    public boolean brewRender = true;
    @Parameter(name = "BrewColor", color = true)
    public int brewColor = 0x3FCC44FF;
    @Parameter(name = "Animal", modes = {"Wolf", "Cat", "Llama"})
    public String tameAnimal = "Wolf";
    @Parameter(name = "AutoSwitch")
    public boolean autoSwitch = true;
    @Parameter(name = "LightLevel", min = 0, max = 15, step = 1)
    public double lightLevel = 8;
    @Parameter(name = "Delay", min = 100, max = 2000, step = 50)
    public double lightDelay = 500;
    @Parameter(name = "SilentSwap")
    public boolean silent = true;
    public static net.minecraft.core.BlockPos smeltTarget = null;
    private static int brewTargetX, brewTargetY, brewTargetZ;
    private static boolean hasBrewTarget;
    private long lastLightPlace = 0;
    public void onTick() {
        switch (mode) {
            case "AutoSmelt" -> tickSmelt();
            case "AutoTame" -> tickTame();
            case "AutoBrew" -> tickBrew();
            case "AutoLight" -> tickLight();
        }
    }
    private void tickSmelt() {
        var mc = MinecraftWrapper.getInstance();
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;
        if (!(mc.screen instanceof FurnaceScreen)
            && !(mc.screen instanceof BlastFurnaceScreen)
            && !(mc.screen instanceof SmokerScreen)) {
            smeltTarget = null;
            return;
        }
        if (smeltTarget == null) {
            if (mc.hitResult != null && mc.hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                net.minecraft.core.BlockPos pos = ((net.minecraft.world.phys.BlockHitResult) mc.hitResult).getBlockPos();
                net.minecraft.world.level.block.state.BlockState st = mc.level.getBlockState(pos);
                if (st.getBlock() instanceof AbstractFurnaceBlock) {
                    smeltTarget = pos;
                }
            }
        }
        if (!(mc.player.containerMenu instanceof AbstractFurnaceMenu furnace)) {
            smeltTarget = null;
            return;
        }
        int containerId = furnace.containerId;
        int playerInvStart = 3;
        int hotbarStart = playerInvStart + 27;
        if (furnace.getSlot(2).hasItem()) {
            mc.gameMode.handleInventoryMouseClick(containerId, 2, 0, InventoryUtility.QUICK_MOVE, mc.player);
            return;
        }
        if (furnace.getBurnProgress() > 0.01f) return;
        if (!furnace.getSlot(0).hasItem()) {
            for (int i = playerInvStart; i < furnace.slots.size(); i++) {
                var stack = furnace.slots.get(i).getItem();
                if (stack.isEmpty()) continue;
                if (furnace.canSmelt(stack)) {
                    mc.gameMode.handleInventoryMouseClick(containerId, i, 0, InventoryUtility.QUICK_MOVE, mc.player);
                    return;
                }
            }
            for (int i = hotbarStart; i < furnace.slots.size(); i++) {
                var stack = furnace.slots.get(i).getItem();
                if (stack.isEmpty()) continue;
                if (furnace.canSmelt(stack)) {
                    mc.gameMode.handleInventoryMouseClick(containerId, i, 0, InventoryUtility.QUICK_MOVE, mc.player);
                    return;
                }
            }
        }
        if (autoFuel && !furnace.getSlot(1).hasItem()) {
            for (int i = playerInvStart; i < furnace.slots.size(); i++) {
                var stack = furnace.slots.get(i).getItem();
                if (stack.isEmpty()) continue;
                if (furnace.isFuel(stack)) {
                    mc.gameMode.handleInventoryMouseClick(containerId, i, 0, InventoryUtility.QUICK_MOVE, mc.player);
                    return;
                }
            }
        }
    }
    private void tickTame() {
        var mc = MinecraftWrapper.getInstance();
        net.minecraft.client.player.LocalPlayer p = mc.player;
        if (p == null || mc.level == null) return;
        double r = range;
        AABB box = p.getBoundingBox().inflate(r);
        List<net.minecraft.world.entity.Entity> entities = mc.level.getEntities(p, box, e -> isTameTarget(e) && e.isAlive());
        for (net.minecraft.world.entity.Entity e : entities) {
            var target = MobUtility.asLivingEntity(e);
            if (!p.getItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND).isEmpty()) {
                mc.gameMode.interact(p, target, net.minecraft.world.InteractionHand.MAIN_HAND);
                break;
            } else if (autoSwitch) {
                int slot = findTameItem();
                if (slot != -1) {
                    InventoryUtility.selectSlot(p, slot);
                    mc.gameMode.interact(p, target, net.minecraft.world.InteractionHand.MAIN_HAND);
                    break;
                }
            }
        }
    }
    private boolean isTameTarget(net.minecraft.world.entity.Entity e) {
        return switch (tameAnimal) {
            case "Wolf" -> e instanceof Wolf;
            case "Cat" -> e instanceof Cat;
            case "Llama" -> e instanceof Llama;
            default -> false;
        };
    }
    private int findTameItem() {
        var mc = MinecraftWrapper.getInstance();
        if (mc.player == null) return -1;
        String mode = tameAnimal;
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(mc.player, i);
            if (stack.isEmpty()) continue;
            boolean match = switch (mode) {
                case "Wolf" -> InventoryUtility.isItem(stack, "bone");
                case "Cat" -> InventoryUtility.isItem(stack, "cod") || InventoryUtility.isItem(stack, "salmon");
                case "Llama" -> InventoryUtility.isItem(stack, "hay_block");
                default -> false;
            };
            if (match) return i;
        }
        return -1;
    }
    private void tickBrew() {
        var mc = MinecraftWrapper.getInstance();
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;
        if (!(mc.screen instanceof BrewingStandScreen)) {
            hasBrewTarget = false;
            return;
        }
        if (!hasBrewTarget) {
            if (mc.hitResult != null && mc.hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                var pos = ((net.minecraft.world.phys.BlockHitResult) mc.hitResult).getBlockPos();
                var st = mc.level.getBlockState(pos);
                if (st.getBlock() instanceof BrewingStandBlock) {
                    brewTargetX = pos.getX(); brewTargetY = pos.getY(); brewTargetZ = pos.getZ();
                    hasBrewTarget = true;
                }
            }
        }
        if (!(mc.player.containerMenu instanceof BrewingStandMenu brew)) {
            hasBrewTarget = false;
            return;
        }
        int containerId = brew.containerId;
        int playerInvStart = 5;
        int hotbarStart = playerInvStart + 27;
        if (autoFuel && brew.getFuel() <= 0) {
            if (!brew.getSlot(4).hasItem()) {
                for (int i = playerInvStart; i < brew.slots.size(); i++) {
                    var stack = brew.slots.get(i).getItem();
                    if (InventoryUtility.isItem(stack, "blaze_powder")) {
                        mc.gameMode.handleInventoryMouseClick(containerId, i, 0, InventoryUtility.QUICK_MOVE, mc.player);
                        return;
                    }
                }
            }
        }
        if (brew.getBrewingTicks() > 0) return;
        for (int slot = 0; slot <= 2; slot++) {
            var stack = brew.getSlot(slot).getItem();
            if (!stack.isEmpty() && !InventoryUtility.isItem(stack, "glass_bottle")) {
                mc.gameMode.handleInventoryMouseClick(containerId, slot, 0, InventoryUtility.QUICK_MOVE, mc.player);
                return;
            }
        }
        if (!brew.getSlot(3).hasItem()) {
            for (int i = playerInvStart; i < brew.slots.size(); i++) {
                var stack = brew.slots.get(i).getItem();
                if (stack.isEmpty()) continue;
                if (isBrewIngredient(stack)) {
                    mc.gameMode.handleInventoryMouseClick(containerId, i, 0, InventoryUtility.QUICK_MOVE, mc.player);
                    return;
                }
            }
            for (int i = hotbarStart; i < brew.slots.size(); i++) {
                var stack = brew.slots.get(i).getItem();
                if (stack.isEmpty()) continue;
                if (isBrewIngredient(stack)) {
                    mc.gameMode.handleInventoryMouseClick(containerId, i, 0, InventoryUtility.QUICK_MOVE, mc.player);
                    return;
                }
            }
        }
        for (int slot = 0; slot <= 2; slot++) {
            if (!brew.getSlot(slot).hasItem()) {
                for (int i = playerInvStart; i < brew.slots.size(); i++) {
                    var stack = brew.slots.get(i).getItem();
                    if (InventoryUtility.isItem(stack, "glass_bottle")) {
                        mc.gameMode.handleInventoryMouseClick(containerId, i, 0, InventoryUtility.QUICK_MOVE, mc.player);
                        return;
                    }
                }
            }
        }
    }
    private static boolean isBrewIngredient(net.minecraft.world.item.ItemStack stack) {
        String name = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return name.equals("nether_wart") || name.equals("glowstone_dust") || name.equals("redstone")
            || name.equals("gunpowder") || name.equals("fermented_spider_eye") || name.equals("blaze_powder")
            || name.equals("ghast_tear") || name.equals("magma_cream") || name.equals("sugar")
            || name.equals("rabbit_foot") || name.equals("glistering_melon_slice") || name.equals("golden_carrot")
            || name.equals("spider_eye") || name.equals("pufferfish") || name.equals("phantom_membrane")
            || name.equals("dragon_breath") || name.equals("turtle_helmet");
    }
    @Nullable
    public static net.minecraft.core.BlockPos getBrewTarget() {
        if (!hasBrewTarget) return null;
        return BlockUtility.pos(brewTargetX, brewTargetY, brewTargetZ);
    }
    private void tickLight() {
        var mc = MinecraftWrapper.getInstance();
        if (mc.player == null || mc.level == null) return;
        long now = System.currentTimeMillis();
        if (now - lastLightPlace < lightDelay) return;
        int torchSlot = -1;
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(mc.player, i);
            if (InventoryUtility.isItem(stack, "torch") || InventoryUtility.isItem(stack, "soul_torch")) {
                torchSlot = i;
                break;
            }
        }
        if (torchSlot == -1) return;
        double r = range;
        var playerPos = mc.player.blockPosition();
        int minX = (int) Math.floor(playerPos.getX() - r);
        int maxX = (int) Math.ceil(playerPos.getX() + r);
        int minY = (int) Math.floor(playerPos.getY() - r);
        int maxY = (int) Math.ceil(playerPos.getY() + r);
        int minZ = (int) Math.floor(playerPos.getZ() - r);
        int maxZ = (int) Math.ceil(playerPos.getZ() + r);
        int targetLight = (int) lightLevel;
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    var pos = BlockUtility.pos(x, y, z);
                    var state = mc.level.getBlockState(pos);
                    if (state.isAir()) continue;
                    if (mc.level.getMaxLocalRawBrightness(pos) > targetLight) continue;
                    int aboveY = BlockUtility.aboveY(y);
                    var placeOn = BlockUtility.pos(x, aboveY, z);
                    if (!mc.level.getBlockState(placeOn).isAir()) continue;
                    if (state.getShape(mc.level, pos).isEmpty()) continue;
                    var center = net.minecraft.world.phys.Vec3.atCenterOf(placeOn);
                    if (center.distanceToSqr(mc.player.getEyePosition()) > r * r) continue;
                    int prevSlot = InventoryUtility.getSelectedSlot(mc.player);
                    InventoryUtility.selectSlot(mc.player, torchSlot);
                    BlockUtility.useItemOn(ravex.mcwrapper.MinecraftWrapper.getWrapper(), new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos), net.minecraft.core.Direction.UP, pos, false));
                    if (silent) {
                        InventoryUtility.selectSlot(mc.player, prevSlot);
                    }
                    lastLightPlace = now;
                    return;
                }
            }
        }
    }
    public void onDisable() {
        smeltTarget = null;
        hasBrewTarget = false;
    }





}