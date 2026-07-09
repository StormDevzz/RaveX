package ravex.modules.world;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BrewingStandScreen;
import net.minecraft.client.gui.screens.inventory.FurnaceScreen;
import net.minecraft.client.gui.screens.inventory.BlastFurnaceScreen;
import net.minecraft.client.gui.screens.inventory.SmokerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.BrewingStandBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import ravex.utility.misc.MobUtility;
import ravex.utility.misc.block.BlockUtility;
import ravex.manager.ModuleManager;
import ravex.utility.player.InventoryUtility;
import java.util.List;
public class PVEUtils extends Module {
    public final ModeParameter mode = new ModeParameter("Mode", "AutoSmelt",
        List.of("AutoSmelt", "AutoTame", "BoneMeal", "AutoBrew", "AutoLight"));
    public final NumberParameter range = new NumberParameter("Range", 4.5, 2.0, 6.0, 0.1);
    public final BooleanParameter autoFuel = new BooleanParameter("AutoFuel", true);
    public final BooleanParameter smeltRender = new BooleanParameter("SmeltRender", true);
    public final ColorParameter smeltColor = new ColorParameter("SmeltColor", 0x3FFF8800);
    public final BooleanParameter brewRender = new BooleanParameter("BrewRender", true);
    public final ColorParameter brewColor = new ColorParameter("BrewColor", 0x3FCC44FF);
    public final ModeParameter tameAnimal = new ModeParameter("Animal", "Wolf",
        List.of("Wolf", "Cat", "Llama"));
    public final BooleanParameter autoSwitch = new BooleanParameter("AutoSwitch", true);
    public final NumberParameter lightLevel = new NumberParameter("LightLevel", 8, 0, 15, 1);
    public final NumberParameter lightDelay = new NumberParameter("Delay", 500, 100, 2000, 50);
    public final BooleanParameter silent = new BooleanParameter("SilentSwap", true);
    public static BlockPos smeltTarget = null;
    private static int brewTargetX, brewTargetY, brewTargetZ;
    private static boolean hasBrewTarget;
    private long lastLightPlace = 0;
    private PVEUtils() {
        super("PVEUtils");
        tameAnimal.setVisible(() -> mode.getValue().equals("AutoTame"));
        autoSwitch.setVisible(() -> mode.getValue().equals("AutoTame"));
        autoFuel.setVisible(() -> mode.getValue().equals("AutoSmelt") || mode.getValue().equals("AutoBrew"));
        smeltRender.setVisible(() -> mode.getValue().equals("AutoSmelt"));
        smeltColor.setVisible(() -> mode.getValue().equals("AutoSmelt") && smeltRender.getValue());
        brewRender.setVisible(() -> mode.getValue().equals("AutoBrew"));
        brewColor.setVisible(() -> mode.getValue().equals("AutoBrew") && brewRender.getValue());
        lightLevel.setVisible(() -> mode.getValue().equals("AutoLight"));
        lightDelay.setVisible(() -> mode.getValue().equals("AutoLight"));
        silent.setVisible(() -> mode.getValue().equals("AutoLight"));
        range.setVisible(() -> mode.getValue().equals("AutoTame") || mode.getValue().equals("AutoLight") || mode.getValue().equals("BoneMeal") || mode.getValue().equals("AutoBrew") || mode.getValue().equals("AutoSmelt"));
    }
    @Override
    public void onTick() {
        switch (mode.getValue()) {
            case "AutoSmelt" -> tickSmelt();
            case "AutoTame" -> tickTame();
            case "AutoBrew" -> tickBrew();
            case "AutoLight" -> tickLight();
        }
    }
    private void tickSmelt() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;
        if (!(mc.screen instanceof FurnaceScreen)
            && !(mc.screen instanceof BlastFurnaceScreen)
            && !(mc.screen instanceof SmokerScreen)) {
            smeltTarget = null;
            return;
        }
        if (smeltTarget == null) {
            if (mc.hitResult != null && mc.hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                BlockPos pos = ((net.minecraft.world.phys.BlockHitResult) mc.hitResult).getBlockPos();
                BlockState st = mc.level.getBlockState(pos);
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
            mc.gameMode.handleInventoryMouseClick(containerId, 2, 0, ClickType.QUICK_MOVE, mc.player);
            return;
        }
        if (furnace.getBurnProgress() > 0.01f) return;
        if (!furnace.getSlot(0).hasItem()) {
            for (int i = playerInvStart; i < furnace.slots.size(); i++) {
                var stack = furnace.slots.get(i).getItem();
                if (stack.isEmpty()) continue;
                if (furnace.canSmelt(stack)) {
                    mc.gameMode.handleInventoryMouseClick(containerId, i, 0, ClickType.QUICK_MOVE, mc.player);
                    return;
                }
            }
            for (int i = hotbarStart; i < furnace.slots.size(); i++) {
                var stack = furnace.slots.get(i).getItem();
                if (stack.isEmpty()) continue;
                if (furnace.canSmelt(stack)) {
                    mc.gameMode.handleInventoryMouseClick(containerId, i, 0, ClickType.QUICK_MOVE, mc.player);
                    return;
                }
            }
        }
        if (autoFuel.getValue() && !furnace.getSlot(1).hasItem()) {
            for (int i = playerInvStart; i < furnace.slots.size(); i++) {
                var stack = furnace.slots.get(i).getItem();
                if (stack.isEmpty()) continue;
                if (furnace.isFuel(stack)) {
                    mc.gameMode.handleInventoryMouseClick(containerId, i, 0, ClickType.QUICK_MOVE, mc.player);
                    return;
                }
            }
        }
    }
    private void tickTame() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) return;
        double r = range.getValue();
        AABB box = p.getBoundingBox().inflate(r);
        List<Entity> entities = mc.level.getEntities(p, box, e -> isTameTarget(e) && e.isAlive());
        for (Entity e : entities) {
            var target = MobUtility.asLivingEntity(e);
            if (!p.getItemInHand(InteractionHand.MAIN_HAND).isEmpty()) {
                mc.gameMode.interact(p, target, InteractionHand.MAIN_HAND);
                break;
            } else if (autoSwitch.getValue()) {
                int slot = findTameItem();
                if (slot != -1) {
                    InventoryUtility.selectSlot(p, slot);
                    mc.gameMode.interact(p, target, InteractionHand.MAIN_HAND);
                    break;
                }
            }
        }
    }
    private boolean isTameTarget(Entity e) {
        return switch (tameAnimal.getValue()) {
            case "Wolf" -> e instanceof Wolf;
            case "Cat" -> e instanceof Cat;
            case "Llama" -> e instanceof Llama;
            default -> false;
        };
    }
    private int findTameItem() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return -1;
        String mode = tameAnimal.getValue();
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
        Minecraft mc = Minecraft.getInstance();
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
        if (autoFuel.getValue() && brew.getFuel() <= 0) {
            if (!brew.getSlot(4).hasItem()) {
                for (int i = playerInvStart; i < brew.slots.size(); i++) {
                    var stack = brew.slots.get(i).getItem();
                    if (InventoryUtility.isItem(stack, "blaze_powder")) {
                        mc.gameMode.handleInventoryMouseClick(containerId, i, 0, ClickType.QUICK_MOVE, mc.player);
                        return;
                    }
                }
            }
        }
        if (brew.getBrewingTicks() > 0) return;
        for (int slot = 0; slot <= 2; slot++) {
            var stack = brew.getSlot(slot).getItem();
            if (!stack.isEmpty() && !InventoryUtility.isItem(stack, "glass_bottle")) {
                mc.gameMode.handleInventoryMouseClick(containerId, slot, 0, ClickType.QUICK_MOVE, mc.player);
                return;
            }
        }
        if (!brew.getSlot(3).hasItem()) {
            for (int i = playerInvStart; i < brew.slots.size(); i++) {
                var stack = brew.slots.get(i).getItem();
                if (stack.isEmpty()) continue;
                if (isBrewIngredient(stack)) {
                    mc.gameMode.handleInventoryMouseClick(containerId, i, 0, ClickType.QUICK_MOVE, mc.player);
                    return;
                }
            }
            for (int i = hotbarStart; i < brew.slots.size(); i++) {
                var stack = brew.slots.get(i).getItem();
                if (stack.isEmpty()) continue;
                if (isBrewIngredient(stack)) {
                    mc.gameMode.handleInventoryMouseClick(containerId, i, 0, ClickType.QUICK_MOVE, mc.player);
                    return;
                }
            }
        }
        for (int slot = 0; slot <= 2; slot++) {
            if (!brew.getSlot(slot).hasItem()) {
                for (int i = playerInvStart; i < brew.slots.size(); i++) {
                    var stack = brew.slots.get(i).getItem();
                    if (InventoryUtility.isItem(stack, "glass_bottle")) {
                        mc.gameMode.handleInventoryMouseClick(containerId, i, 0, ClickType.QUICK_MOVE, mc.player);
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
    public static BlockPos getBrewTarget() {
        if (!hasBrewTarget) return null;
        return BlockUtility.pos(brewTargetX, brewTargetY, brewTargetZ);
    }
    private void tickLight() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        long now = System.currentTimeMillis();
        if (now - lastLightPlace < lightDelay.getValue()) return;
        int torchSlot = -1;
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(mc.player, i);
            if (InventoryUtility.isItem(stack, "torch") || InventoryUtility.isItem(stack, "soul_torch")) {
                torchSlot = i;
                break;
            }
        }
        if (torchSlot == -1) return;
        double r = range.getValue();
        var playerPos = mc.player.blockPosition();
        int minX = (int) Math.floor(playerPos.getX() - r);
        int maxX = (int) Math.ceil(playerPos.getX() + r);
        int minY = (int) Math.floor(playerPos.getY() - r);
        int maxY = (int) Math.ceil(playerPos.getY() + r);
        int minZ = (int) Math.floor(playerPos.getZ() - r);
        int maxZ = (int) Math.ceil(playerPos.getZ() + r);
        int targetLight = lightLevel.getValue().intValue();
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
                    var center = Vec3.atCenterOf(placeOn);
                    if (center.distanceToSqr(mc.player.getEyePosition()) > r * r) continue;
                    int prevSlot = InventoryUtility.getSelectedSlot(mc.player);
                    InventoryUtility.selectSlot(mc.player, torchSlot);
                    BlockUtility.useItemOn(mc, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
                    if (silent.getValue()) {
                        InventoryUtility.selectSlot(mc.player, prevSlot);
                    }
                    lastLightPlace = now;
                    return;
                }
            }
        }
    }
    @Override
    protected void onDisable() {
        smeltTarget = null;
        hasBrewTarget = false;
    }

    public static boolean maybeEnabled() {
        return maybeEnabled(PVEUtils.class);
    }
    public static PVEUtils itz() {
        return ModuleManager.get(PVEUtils.class);
    }
}
