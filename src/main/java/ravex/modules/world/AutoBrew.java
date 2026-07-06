package ravex.modules.world;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BrewingStandScreen;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.ClickType;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.NumberParameter;
import ravex.utility.player.InventoryUtility;
public class AutoBrew extends Module {
    public static final AutoBrew INSTANCE = new AutoBrew();
    public final NumberParameter range = new NumberParameter("Range", 5.0, 1.0, 6.0, 0.5);
    public final BooleanParameter autoFuel = new BooleanParameter("AutoFuel", true);
    public final BooleanParameter render = new BooleanParameter("Render", true);
    public final ColorParameter color = new ColorParameter("Color", 0x3FCC44FF);
    private static int targetX, targetY, targetZ;
    private static boolean hasRenderTarget;

    public static net.minecraft.core.BlockPos getCurrentTarget() {
        if (!hasRenderTarget) return null;
        return ravex.utility.misc.block.BlockUtility.pos(targetX, targetY, targetZ);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;
        if (!(mc.screen instanceof BrewingStandScreen)) {
            hasRenderTarget = false;
            return;
        }
        if (!hasRenderTarget) {
            if (mc.hitResult != null && mc.hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                var pos = ((net.minecraft.world.phys.BlockHitResult) mc.hitResult).getBlockPos();
                var st = mc.level.getBlockState(pos);
                if (st.getBlock() instanceof net.minecraft.world.level.block.BrewingStandBlock) {
                    targetX = pos.getX(); targetY = pos.getY(); targetZ = pos.getZ();
                    hasRenderTarget = true;
                }
            }
        }
        if (!(mc.player.containerMenu instanceof BrewingStandMenu brew)) {
            hasRenderTarget = false;
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
                        mc.gameMode.handleInventoryMouseClick(
                            containerId, i, 0, ClickType.QUICK_MOVE, mc.player
                        );
                        return;
                    }
                }
            }
        }
        if (brew.getBrewingTicks() > 0) return;
        for (int slot = 0; slot <= 2; slot++) {
            var stack = brew.getSlot(slot).getItem();
            if (!stack.isEmpty() && !InventoryUtility.isItem(stack, "glass_bottle")) {
                mc.gameMode.handleInventoryMouseClick(
                    containerId, slot, 0, ClickType.QUICK_MOVE, mc.player
                );
                return;
            }
        }
        if (!brew.getSlot(3).hasItem()) {
            for (int i = playerInvStart; i < brew.slots.size(); i++) {
                var stack = brew.slots.get(i).getItem();
                if (stack.isEmpty()) continue;
                if (isIngredient(stack)) {
                    mc.gameMode.handleInventoryMouseClick(
                        containerId, i, 0, ClickType.QUICK_MOVE, mc.player
                    );
                    return;
                }
            }
            for (int i = hotbarStart; i < brew.slots.size(); i++) {
                var stack = brew.slots.get(i).getItem();
                if (stack.isEmpty()) continue;
                if (isIngredient(stack)) {
                    mc.gameMode.handleInventoryMouseClick(
                        containerId, i, 0, ClickType.QUICK_MOVE, mc.player
                    );
                    return;
                }
            }
        }
        for (int slot = 0; slot <= 2; slot++) {
            if (!brew.getSlot(slot).hasItem()) {
                for (int i = playerInvStart; i < brew.slots.size(); i++) {
                    var stack = brew.slots.get(i).getItem();
                    if (InventoryUtility.isItem(stack, "glass_bottle")) {
                        mc.gameMode.handleInventoryMouseClick(
                            containerId, i, 0, ClickType.QUICK_MOVE, mc.player
                        );
                        return;
                    }
                }
            }
        }
    }
    @Override
    protected void onDisable() {
        hasRenderTarget = false;
    }
    private static boolean isIngredient(net.minecraft.world.item.ItemStack stack) {
        String name = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return name.equals("nether_wart") || name.equals("glowstone_dust") || name.equals("redstone")
            || name.equals("gunpowder") || name.equals("fermented_spider_eye") || name.equals("blaze_powder")
            || name.equals("ghast_tear") || name.equals("magma_cream") || name.equals("sugar")
            || name.equals("rabbit_foot") || name.equals("glistering_melon_slice") || name.equals("golden_carrot")
            || name.equals("spider_eye") || name.equals("pufferfish") || name.equals("phantom_membrane")
            || name.equals("dragon_breath") || name.equals("turtle_helmet");
    }
}
