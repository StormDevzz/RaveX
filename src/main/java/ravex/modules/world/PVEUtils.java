package ravex.modules.world;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.EntityUtility;
import ravex.utility.misc.PhysicUtility;
import ravex.utility.misc.block.BlockUtility;
import ravex.utility.player.ContainerUtility;
import ravex.utility.player.InventoryUtility;

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
    @Parameter(name = "SmeltColor", color = true, visible = "smeltRender")
    public int smeltColor = 0x3FFF8800;
    @Parameter(name = "BrewRender")
    public boolean brewRender = true;
    @Parameter(name = "BrewColor", color = true, visible = "brewRender")
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
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getLevel() == null || mc.getGameMode() == null) return;
        if (!ContainerUtility.isFurnaceScreenOpen()) {
            ContainerUtility.clearSmeltTarget();
            return;
        }
        ContainerUtility.rememberSmeltTarget();
        if (!ContainerUtility.isFurnaceMenuOpen()) {
            ContainerUtility.clearSmeltTarget();
            return;
        }
        if (ContainerUtility.takeFurnaceOutput()) return;
        if (ContainerUtility.furnaceBurnProgress() > 0.01f) return;
        if (ContainerUtility.putSmeltInput()) return;
        if (autoFuel && ContainerUtility.putFurnaceFuel()) return;
    }
    private void tickTame() {
        var mc = MinecraftWrapper.getWrapper();
        var p = mc.getPlayer();
        if (p == null || mc.getLevel() == null || mc.getGameMode() == null) return;
        double r = range;
        for (var e : mc.getLevel().entitiesForRendering()) {
            if (!e.isAlive()) continue;
            boolean tameable = switch (tameAnimal) {
                case "Wolf" -> EntityUtility.isWolf(e);
                case "Cat" -> EntityUtility.isCat(e);
                case "Llama" -> EntityUtility.isLlama(e);
                default -> false;
            };
            if (!tameable || p.distanceToSqr(e) > r * r) continue;
            var target = EntityUtility.asLivingEntity(e);
            if (!InventoryUtility.getMainHand(p).isEmpty()) {
                EntityUtility.interact(mc, target);
                break;
            } else if (autoSwitch) {
                int slot = findTameItem();
                if (slot != -1) {
                    InventoryUtility.selectSlot(p, slot);
                    EntityUtility.interact(mc, target);
                    break;
                }
            }
        }
    }
    private int findTameItem() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null) return -1;
        String mode = tameAnimal;
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(mc.getPlayer(), i);
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
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getLevel() == null || mc.getGameMode() == null) return;
        if (!ContainerUtility.isBrewingStandOpen()) {
            ContainerUtility.clearBrewTarget();
            return;
        }
        ContainerUtility.rememberBrewTarget();
        if (!ContainerUtility.isBrewingMenuOpen()) {
            ContainerUtility.clearBrewTarget();
            return;
        }
        if (autoFuel && ContainerUtility.putBrewingFuel()) return;
        if (ContainerUtility.brewTicks() > 0) return;
        if (ContainerUtility.takeFinishedPotions()) return;
        if (ContainerUtility.putBrewIngredient()) return;
        if (ContainerUtility.putGlassBottle()) return;
    }
    private void tickLight() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getLevel() == null) return;
        long now = System.currentTimeMillis();
        if (now - lastLightPlace < lightDelay) return;
        int torchSlot = -1;
        for (int i = 0; i < 9; i++) {
            var stack = InventoryUtility.getItem(mc.getPlayer(), i);
            if (InventoryUtility.isItem(stack, "torch") || InventoryUtility.isItem(stack, "soul_torch")) {
                torchSlot = i;
                break;
            }
        }
        if (torchSlot == -1) return;
        double r = range;
        var playerPos = mc.getPlayer().blockPosition();
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
                    var state = BlockUtility.getState(mc.getLevel(), pos);
                    if (state.isAir()) continue;
                    if (mc.getLevel().getMaxLocalRawBrightness(pos) > targetLight) continue;
                    int aboveY = BlockUtility.aboveY(y);
                    var placeOn = BlockUtility.pos(x, aboveY, z);
                    if (!BlockUtility.isAir(mc.getLevel(), placeOn)) continue;
                    if (state.getShape(mc.getLevel(), pos).isEmpty()) continue;
                    var center = PhysicUtility.centerOf(placeOn);
                    if (center.distanceToSqr(mc.getPlayer().getEyePosition()) > r * r) continue;
                    int prevSlot = InventoryUtility.getSelectedSlot(mc.getPlayer());
                    InventoryUtility.selectSlot(mc.getPlayer(), torchSlot);
                    BlockUtility.useItemOn(mc, BlockUtility.placeOnTop(mc, pos));
                    if (silent) {
                        InventoryUtility.selectSlot(mc.getPlayer(), prevSlot);
                    }
                    lastLightPlace = now;
                    return;
                }
            }
        }
    }
    public void onDisable() {
        ContainerUtility.clearSmeltTarget();
        ContainerUtility.clearBrewTarget();
    }
}
